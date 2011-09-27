package cubetech.gfx;

import cubetech.CGame.ViewParams;
import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.common.ICommand;
import cubetech.common.IThinkMethod;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Profiler.SecTag;
import cubetech.misc.Ref;
import java.util.EnumSet;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTTextureArray;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Ported from DirectX Cascaded Shadows Sample.
 * 
 */
public class ShadowManager {
    private static final float cascadePartitionsMax = 100;
    // Settings
    private float[] cascadePartitionCoverage = new float[] {7,20,75,100,100,100,100,100};

    private int nLevels = 1;
    private int shadow_resolution = 1024;
    private boolean fitToCascades = true; // tighten bounding boxes around cascades
    private boolean moveLightTexelSize = true; // decreases shadow jitter

    private CVar shadow_levels = null;
    private CVar shadow_res = null;
    private CVar shadow_enable;
    private CVar shadow_factor;
    private CVar shadow_filter;
    private CVar shadow_kernel;
    
    // Temp vectors
    private Vector4f[] tempFrustumPoints = new Vector4f[8];
    public Matrix4f[] shadowProjections = new Matrix4f[8];
    public Vector3f lightCameraOthoMin = new Vector3f();
    public Vector3f lightCameraOthoMax = new Vector3f();
    public float minz, maxz;
    public Vector3f[] shadowFrustum = new Vector3f[8];
    // state
    private boolean rendering = false;

    // Shadow textures
    FrameBuffer depthBuffer;
    float[] cascadeEyespaceDepths = new float[8];

    public ShadowManager() {
        for (int i= 0; i < tempFrustumPoints.length; i++) {
            tempFrustumPoints[i] = new Vector4f(0,0,0,1);
        }
        for (int i= 0; i < shadowFrustum.length; i++) {
            shadowFrustum[i] = new Vector3f();
        }
        for (int i = 0; i < shadowProjections.length; i++) {
            shadowProjections[i] = new Matrix4f();
        }
        Ref.cvars.Get("shadow_view", "0", EnumSet.of(CVarFlags.NONE));

        shadow_resolution = Helper.get2Fold(shadow_resolution);

        Ref.cvars.Get("shadow_bias", "0.002", EnumSet.of(CVarFlags.NONE));
        shadow_res = Ref.cvars.Get("shadow_resolution", ""+shadow_resolution, EnumSet.of(CVarFlags.NONE));
        shadow_res.modified = false;
        shadow_levels = Ref.cvars.Get("shadow_levels", ""+nLevels, EnumSet.of(CVarFlags.NONE));

        Ref.commands.AddCommand("shadow_setlevel", shadow_setlevel);

        shadow_enable = Ref.cvars.Get("shadow_enable", "1", EnumSet.of(CVarFlags.NONE));
        shadow_enable.modified = false;
        shadow_factor = Ref.cvars.Get("shadow_factor", "0.5", EnumSet.of(CVarFlags.NONE));
        shadow_filter = Ref.cvars.Get("shadow_filter", "point", EnumSet.of(CVarFlags.ARCHIVE));
        shadow_kernel = Ref.cvars.Get("shadow_kernel", "0.5", EnumSet.of(CVarFlags.NONE));

        createDepthBuffer();
    }

    private void createDepthBuffer() {
        depthBuffer = new FrameBuffer(shadow_resolution, nLevels, 24);
        boolean point = true;
        String val = shadow_filter.sValue.toLowerCase();
        if(val.equals("linear")) {
            point = false;
        }
        setDepthFiltering(point);
    }

    public boolean isEnabled() {
        return shadow_enable.isTrue();
    }

    ICommand shadow_setlevel = new ICommand() {
        public void RunCommand(String[] args) {
            if(args.length != 3) {
                Common.Log("shadow_setlevel usage: <level> <0-100 percent depth>");
                return;
            }

            int lvl, value;
            try {
                lvl = Integer.parseInt(args[1]);
                value = Integer.parseInt(args[2]);
            } catch(NumberFormatException ex) {
                Common.Log("shadow_setlevel usage: <level> <0-100 percent depth>");
                return;
            }

            cascadePartitionCoverage[lvl] = value;
            for (int i = lvl; i < cascadePartitionCoverage.length; i++) {
                if(cascadePartitionCoverage[i] < value) {
                    cascadePartitionCoverage[i] = value;
                }
            }
        }
    };

    public Vector4f getCascadeDepths() {
        //Vector4f[] out = new Vector4f[2];
        return new Vector4f(cascadeEyespaceDepths[0],
                cascadeEyespaceDepths[1],
                cascadeEyespaceDepths[2],
                cascadeEyespaceDepths[3]);
//        out[1] = new Vector4f(cascadeEyespaceDepths[4],
//                cascadeEyespaceDepths[5],
//                cascadeEyespaceDepths[6],
//                cascadeEyespaceDepths[7]);
        //return out;
    }

    public CubeTexture getDepthTexture() {
        FrameBuffer depthFrameBuffer = depthBuffer;
        CubeTexture tex = new CubeTexture(depthFrameBuffer.getTextureTarget(), 
                                            depthFrameBuffer.getTextureId(),
                                            "depth");
        tex.loaded = true;
        return tex;
    }

    private void startShadowFrame(int level) {
        depthBuffer.Bind();
        // Binds the depth framebuffer
        EXTTextureArray.glFramebufferTextureLayerEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
                depthBuffer.getTextureId(), 0, level);
        
        GLRef.checkError();

        // Clear depth of render target
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        // push original viewport on first cascade
        if(level == 0) {
            GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);
        }
        GL11.glViewport(0, 0, shadow_resolution, shadow_resolution);
        GLRef.checkError();

        // Set projection
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        if(level == 0) GL11.glPushMatrix(); // push on first level
        applyShadowProjection(level);
        GLRef.checkError();

        // Set view
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        if(level == 0) GL11.glPushMatrix();
        Matrix4f viewM = Matrix4f.load(Ref.glRef.getLightViewMatrix(), null);
        viewM.store(Ref.glRef.matrixBuffer);
        Ref.glRef.matrixBuffer.position(0);
        GL11.glLoadMatrix(Ref.glRef.matrixBuffer);
        Ref.glRef.matrixBuffer.clear();
        GLRef.checkError();

        // Enable front-face culling
        GL11.glCullFace(GL11.GL_FRONT);
//        GL11.glFrontFace(GL11.GL_CW);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(1f, 0);
        rendering = true;
    }

    public Vector4f[] getPCFoffsets() {
        return pcfOffsets;
    }

    private void setPcfOffsets() {
        float dist = shadow_kernel.fValue/shadow_resolution;

        pcfOffsets[0].x = -dist;
        pcfOffsets[0].y = -dist;

        pcfOffsets[1].x = dist;
        pcfOffsets[1].y = -dist;

        pcfOffsets[2].x = -dist;
        pcfOffsets[2].y = dist;

        pcfOffsets[3].x = dist;
        pcfOffsets[3].y = dist;

    }

    private Vector4f[] pcfOffsets = new Vector4f[] {
        new Vector4f(),
        new Vector4f(),
        new Vector4f(),
        new Vector4f()
    };

    public Vector4f[] getCascadeColors() {
        return cascadeColors;
    }

    private static final Vector4f[] cascadeColors = new Vector4f[] {
        new Vector4f(1.5f,0,0,1),
        new Vector4f(0,1.5f,0,1),
        new Vector4f(0,0,1.5f,1),
        new Vector4f(1.5f,1.5f,0,1),
        new Vector4f(1.5f,0,1.5f,1),
        new Vector4f(0,1.5f,1.5f,1),
        new Vector4f(1.0f,0,0,1),
        new Vector4f(0,1,0,1)
    };

    public void renderShadowCascades(ViewParams refdef, IThinkMethod render) {
        if(render == null) return;
        SecTag s = Profiler.EnterSection(Sec.SHADOWS);
        initFrame(refdef);
        for (int i= 0; i < nLevels; i++) {
            startShadowFrame(i);
            render.think(null);
        }
        
        finishShadowFrame();

        CVar shadow_view = Ref.cvars.Find("shadow_view");
        if(shadow_view != null && shadow_view.isTrue()) {
            int level = shadow_view.iValue;
            applyShadowProjection(level);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            Matrix4f viewM = Matrix4f.load(Ref.glRef.getLightViewMatrix(), null);
            viewM.store(Ref.glRef.matrixBuffer);
            Ref.glRef.matrixBuffer.position(0);
            GL11.glLoadMatrix(Ref.glRef.matrixBuffer);
            Ref.glRef.matrixBuffer.clear();
        }

        s.ExitSection();
    }

    public Matrix4f[] getShadowViewProjections(int max) {
        Matrix4f[] out = new Matrix4f[max];
        if(nLevels < max) max = nLevels;
        for (int i= 0; i < max; i++) {
            out[i] = getShadowViewProjection(i);
        }
        return out;
    }

    public Matrix4f getShadowViewProjection(int cascadeIndex) {
        Matrix4f scaleBias = new Matrix4f();
        scaleBias.setIdentity();
        scaleBias.m00 = 0.5f; scaleBias.m11 = 0.5f; scaleBias.m22 = 0.5f;
        scaleBias.m30 = 0.5f;
        scaleBias.m31 = 0.5f;
        scaleBias.m32 = 0.5f;
        Matrix4f m = shadowProjections[cascadeIndex];
        Matrix4f viewM = Matrix4f.load(Ref.glRef.getLightViewMatrix(), null);

        Matrix4f.mul(scaleBias, m, scaleBias);
        Matrix4f.mul(scaleBias, viewM, scaleBias);
        return scaleBias;
    }

    private void finishShadowFrame() {
        // Revert to original matrices
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glPopAttrib();

        // unbind last level
        depthBuffer.Unbind();
        rendering = false;
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        
        if(Ref.glRef.srgbBuffer != null) Ref.glRef.srgbBuffer.Bind();
    }

    private void setDepthFiltering(boolean point) {
        if(depthBuffer == null || depthBuffer.getDepthTextureId() < 0) {
            return;
        }
        GL11.glBindTexture(depthBuffer.getTextureTarget(), depthBuffer.getDepthTextureId());
        GL11.glTexParameteri(depthBuffer.getTextureTarget(), GL11.GL_TEXTURE_MAG_FILTER, 
                point?GL11.GL_NEAREST:GL11.GL_LINEAR);
        GL11.glBindTexture(depthBuffer.getTextureTarget(), 0);
    }

    private void initFrame(ViewParams refdef) {
        if(shadow_levels.modified) {
            nLevels = shadow_levels.iValue;
            if(nLevels <= 0 || nLevels > 8) nLevels = 1;
            Ref.cvars.Set2("shadow_levels", ""+nLevels, true);
            shadow_levels.modified = false;

            if(!shadow_res.modified) { // don't destory twice in one frame
                depthBuffer.destroy();
                createDepthBuffer();
            }
        }

        if(shadow_res.modified) {
            shadow_resolution = Helper.get2Fold(shadow_res.iValue);
            Ref.cvars.Set2("shadow_resolution", ""+shadow_resolution, true);
            shadow_res.modified = false;

            // Resize FBO
            depthBuffer.destroy();
            createDepthBuffer();
//            for (int i= 0; i < nLevels; i++) {
//                depthBuffer.resize(shadow_resolution, shadow_resolution);
//            }
        }

        if(shadow_filter.modified) {
            // parse it
            String str = shadow_filter.sValue.toLowerCase().trim();
            if(str.equals("point")) {
                setDepthFiltering(true);
            } else if(str.equals("linear")) {
                setDepthFiltering(false);
            } else {
                Common.Log("Unknown shadow_filter value. Use point or linear");
                shadow_filter.set("point");
            }

            shadow_filter.modified = false;
        }

        setPcfOffsets();

        
        float near = Ref.cvars.Find("cg_depthnear").iValue;
        float far = Ref.cvars.Find("cg_depthfar").iValue;
        float cameraNearFarRange = far - near;

        Matrix4f viewCameraProjection = refdef.ProjectionMatrix;
        Matrix4f viewCameraView = refdef.viewMatrix;
        Matrix4f inverseViewCamera = Matrix4f.invert(viewCameraView, null);
//        Matrix4f.mul(inverseViewCamera, ViewParams.flipMatrix2, inverseViewCamera);
        Matrix4f lightView = Ref.glRef.getLightViewMatrix();
        for (int i= 0; i < 8; i++) {
            cascadeEyespaceDepths[i] = 10000000;
        }

        // We loop over the cascades to calculate the orthographic projection for each cascade.
        for (int cascadeIndex= 0; cascadeIndex < nLevels; cascadeIndex++) {
            float frustumIntervalBegin = 0f;
            // Calculate the interval of the View Frustum that this cascade covers. We measure the interval 
            // the cascade covers as a Min and Max distance along the Z Axis.
            if(fitToCascades) {
                // Because we want to fit the orthogrpahic projection tightly around the Cascade, we set the Mimiumum cascade
                // value to the previous Frustum end Interval
                if(cascadeIndex == 0) frustumIntervalBegin = 0;
                else frustumIntervalBegin = cascadePartitionCoverage[cascadeIndex-1];
            } else {
                // In the FIT_TO_SCENE technique the Cascades overlap eachother.  In other words, interval 1 is coverd by
                // cascades 1 to 8, interval 2 is covered by cascades 2 to 8 and so forth.
                frustumIntervalBegin = 0f;
            }

            // Scale the intervals between 0 and 1. They are now percentages that we can scale with.
            float frustumIntervalEnd = cascadePartitionCoverage[cascadeIndex];
            frustumIntervalBegin /= cascadePartitionsMax;
            frustumIntervalEnd /= cascadePartitionsMax;
            frustumIntervalBegin *= cameraNearFarRange;
            frustumIntervalEnd *= cameraNearFarRange;

            // This function takes the began and end intervals along with the projection matrix and returns the 8
            // points that repreresent the cascade Interval
            createFrustumPointsFromCascadeInterval(frustumIntervalBegin, frustumIntervalEnd,
                    viewCameraProjection,tempFrustumPoints);

            lightCameraOthoMin.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
            lightCameraOthoMax.set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

            // This next section of code calculates the min and max values for the orthographic projection.
            
//            invLightView.transpose();
            for (int i= 0; i < 8; i++) {
                // Transform the frustum from camera view space to world space.
                Matrix4f.transform(inverseViewCamera, tempFrustumPoints[i], tempFrustumPoints[i]);

                // Transform the point from world space to Light Camera Space.
                Vector4f tempTranslatedCorner =  Matrix4f.transform(lightView, tempFrustumPoints[i], null);

                Vector3f translatedCorner = shadowFrustum[i].set(tempTranslatedCorner);

                Vector3f temp = new Vector3f(tempFrustumPoints[i].x, tempFrustumPoints[i].y, tempFrustumPoints[i].z);
                shadowFrustum[i] = Helper.transform(lightView, temp, translatedCorner);
                // Find the closest point.
                Helper.AddPointToBounds(translatedCorner, lightCameraOthoMin, lightCameraOthoMax);
            }

//
            Matrix4f invLightView = (Matrix4f) Matrix4f.invert(lightView, null);
            Vector3f camCoords = new Vector3f();
            shadowFrustum[0] = Helper.transform(invLightView, lightCameraOthoMin, shadowFrustum[0]);
            camCoords.set(lightCameraOthoMin.x, lightCameraOthoMax.y, lightCameraOthoMin.z);
            shadowFrustum[1] = Helper.transform(invLightView, camCoords, shadowFrustum[1]);
            camCoords.set(lightCameraOthoMax.x, lightCameraOthoMax.y, lightCameraOthoMin.z);
            shadowFrustum[2] = Helper.transform(invLightView, camCoords, shadowFrustum[2]);
            camCoords.set(lightCameraOthoMax.x, lightCameraOthoMin.y, lightCameraOthoMin.z);
            shadowFrustum[3] = Helper.transform(invLightView, camCoords, shadowFrustum[3]);

            camCoords.set(lightCameraOthoMin.x, lightCameraOthoMin.y, lightCameraOthoMax.z);
            shadowFrustum[4] = Helper.transform(invLightView, camCoords, shadowFrustum[4]);
            camCoords.set(lightCameraOthoMin.x, lightCameraOthoMax.y, lightCameraOthoMax.z);
            shadowFrustum[5] = Helper.transform(invLightView, camCoords, shadowFrustum[5]);
            shadowFrustum[6] = Helper.transform(invLightView, lightCameraOthoMax, shadowFrustum[6]);
            camCoords.set(lightCameraOthoMax.x, lightCameraOthoMin.y, lightCameraOthoMax.z);
            shadowFrustum[7] = Helper.transform(invLightView, camCoords, shadowFrustum[7]);


            // This code removes the shimmering effect along the edges of shadows due to
            // the light changing to fit the camera.
            // Fit the ortho projection to the cascades far plane and a near plane of zero.
            // Pad the projection to be the size of the diagonal of the Frustum partition.
            //
            // To do this, we pad the ortho transform so that it is always big enough to cover
            // the entire camera view frustum.
            Vector3f diagonal = Vector3f.sub(new Vector3f(tempFrustumPoints[0]), new Vector3f(tempFrustumPoints[6]), null);
            diagonal.scale(1.2f);
//            drawFrustum(tempFrustumPoints);

            // The bound is the length of the diagonal of the frustum interval.
            float cascadeBound = diagonal.length();
            diagonal.set(cascadeBound,cascadeBound,cascadeBound); // emulating XMVector3Length

            minz = lightCameraOthoMin.z;
            maxz = lightCameraOthoMax.z;

//             if(minz > maxz) {
//                 maxz = minz;
//                 if(maxz < 0) maxz = -maxz;
//                 minz = lightCameraOthoMax.z;
//             }

            // The offset calculated will pad the ortho projection so that it is always the same size
            // and big enough to cover the entire cascade interval.
            Vector3f borderOffset = Vector3f.sub(lightCameraOthoMax, lightCameraOthoMin, null);
            Vector3f.sub(diagonal, borderOffset, borderOffset);
//            borderOffset.x = Math.abs(borderOffset.x);
//            borderOffset.y = Math.abs(borderOffset.y);
//            borderOffset.z = Math.abs(borderOffset.z);
            borderOffset.scale(0.5f);

            // Set the Z and W components to zero.
            borderOffset.z = 0;

            // Add the offsets to the projection.
            Vector3f.sub(lightCameraOthoMin, borderOffset, lightCameraOthoMin);
            Vector3f.add(lightCameraOthoMax, borderOffset, lightCameraOthoMax);

            // The world units per texel are used to snap the shadow the orthographic projection
            // to texel sized increments.  This keeps the edges of the shadows from shimmering.
            Vector3f worldUnitsPerTexel = new Vector3f(cascadeBound / shadow_resolution,cascadeBound / shadow_resolution,cascadeBound / shadow_resolution);

            float lightCameraOrthographicMinZ = lightCameraOthoMin.z;
            if(moveLightTexelSize ) {
                // We snape the camera to 1 pixel increments so that moving the camera does not cause the shadows to jitter.
                // This is a matter of integer dividing by the world space size of a texel
                lightCameraOthoMin.x /= worldUnitsPerTexel.x;
                lightCameraOthoMin.y /= worldUnitsPerTexel.y;
                lightCameraOthoMin.z /= worldUnitsPerTexel.z;
                Helper.VectorFloor(lightCameraOthoMin);
                lightCameraOthoMin.x *= worldUnitsPerTexel.x;
                lightCameraOthoMin.y *= worldUnitsPerTexel.y;
                lightCameraOthoMin.z *= worldUnitsPerTexel.z;

                lightCameraOthoMax.x /= worldUnitsPerTexel.x;
                lightCameraOthoMax.y /= worldUnitsPerTexel.y;
                lightCameraOthoMax.z /= worldUnitsPerTexel.z;
                Helper.VectorFloor(lightCameraOthoMax);
                lightCameraOthoMax.x *= worldUnitsPerTexel.x;
                lightCameraOthoMax.y *= worldUnitsPerTexel.y;
                lightCameraOthoMax.z *= worldUnitsPerTexel.z;
            }

            //These are the unconfigured near and far plane values.  They are purposly awful to show
            // how important calculating accurate near and far planes is.
            float nearPlane = 0f;
            float farPlane = 10000;
            // NEAR/FAR + AABB
            if(true){
//                Vector3f lightSpaceAABBmin = new Vector3f(Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE);
//                Vector3f lightSpaceAABBmax = new Vector3f(-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE);
//                // We calculate the min and max vectors of the scene in light space. The min and max "Z" values of the
//                // light space AABB can be used for the near and far plane. This is easier than intersecting the scene with the AABB
//                // and in some cases provides similar results.
//                for (int i= 0; i < 8; i++) {
//                    Helper.AddPointToBounds(frustumPointsLightSpace[i], lightSpaceAABBmin, lightSpaceAABBmax);
//                }

                // The min and max z values are the near and far planes.
                nearPlane = -lightCameraOthoMin.z ;
                farPlane = nearPlane+(lightCameraOthoMax.z-lightCameraOthoMin.z);
                nearPlane -= 1500;
            }

            minz = nearPlane;
            maxz = farPlane;

            // Craete the orthographic projection for this cascade.
//            Matrix4f shadowProj = Helper.createOthoMatrix(lightCameraOthoMin.x, lightCameraOthoMax.x,
//                    lightCameraOthoMin.y, lightCameraOthoMax.y,
//                    nearPlane, farPlane);
            Matrix4f shadowProj = Helper.createOthoMatrix(lightCameraOthoMin.x, lightCameraOthoMax.x,
                    lightCameraOthoMin.y, lightCameraOthoMax.y,
                    nearPlane, farPlane);
            shadowProjections[cascadeIndex] = shadowProj;

            cascadeEyespaceDepths[cascadeIndex] = frustumIntervalEnd;
        }
    }

    private static void createFrustumPointsFromCascadeInterval(float begin, float end, Matrix4f projection, Vector4f[] data) {
        Frustum f = computeFrustumFromProjection(projection);
        f.near = begin;
        f.far = -end;

        Vector4f rightTop = new Vector4f(f.rightSlope, f.topSlope, 1f,1f);
        Vector4f LeftBottom = new Vector4f(f.leftSlope, f.bottomSlope, 1f,1f);
        Vector4f near = new Vector4f(f.near, f.near, f.near,1f);
        Vector4f far = new Vector4f(f.far,f.far, f.far,1f);
        Vector4f rightTopNear = Helper.VectorMult(rightTop, near, null);
        Vector4f rightTopFar = Helper.VectorMult(rightTop, far, null);
        Vector4f leftBottomNear = Helper.VectorMult(LeftBottom, near, null);
        Vector4f leftBottomFar = Helper.VectorMult(LeftBottom, far, null);

        
        data[0].set(rightTopNear);
        data[1].set(rightTopNear);
        data[1].x  = leftBottomNear.x;
        data[2].set(leftBottomNear);
        data[3].set(rightTopNear);
        data[3].y  = leftBottomNear.y;

        data[4].set(rightTopFar);
        data[5].set(rightTopFar);
        data[5].x  = leftBottomFar.x;
        data[6].set(leftBottomFar);
        data[7].set(rightTopFar);
        data[7].y  = leftBottomFar.y;
    }

    // Corners of the projection frustum in homogenous space.
    private static Vector4f[] homogenousPoints = new Vector4f[] {
        new Vector4f(1,0,1,1), // right (at far plane)
        new Vector4f(-1,0,1,1), // left
        new Vector4f(0,1,1,1), // top
        new Vector4f(0,-1,1,1),  // bottom
        new Vector4f(0,0,0,1), // near
        new Vector4f(0,0,1,1) // far
    };
    
    private static Frustum computeFrustumFromProjection(Matrix4f proj) {
        Matrix4f matInverse = Matrix4f.invert(proj, null);

        // Compute the frustum corners in world space.
        Vector4f[] points = new Vector4f[6];
        for (int i= 0; i < 6; i++) {
            points[i] = Matrix4f.transform(matInverse, homogenousPoints[i], null);
        }

        Frustum f = new Frustum();
        f.Origin = new Vector3f();
        f.Orientation = new Vector4f(0, 0, 0, 1);

        // Compute the slopes.
        points[0].scale(1f / points[0].z);
        points[1].scale(1f / points[1].z);
        points[2].scale(1f / points[2].z);
        points[3].scale(1f / points[3].z);

        f.rightSlope = points[0].x;
        f.leftSlope = points[1].x;
        f.topSlope = points[2].y;
        f.bottomSlope = points[3].y;

        // Compute near and far.
        points[4].scale(1f / points[4].w);
        points[5].scale(1f / points[5].w);

        f.near = points[4].z;
        f.far = points[5].z;
        return f;
    }

    public void applyShadowProjection(int index) {
        if(shadowProjections[index] == null) return;
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        Matrix4f m = shadowProjections[index];
//        Matrix4f.mul(m, ViewParams.flipMatrix2, m);
        Ref.glRef.matrixBuffer.clear();
//        m = Helper.createOthoMatrix(, 1,10000);
        
        m.store(Ref.glRef.matrixBuffer);
        Ref.glRef.matrixBuffer.position(0);
        GL11.glLoadMatrix(Ref.glRef.matrixBuffer);
        
//        GLU.gluOrtho2D(-4000,300, -18000, -13000f);
        
    }

    public void drawFrustum() {
        drawFrustum(tempFrustumPoints);

        Helper.col(1, 0, 1);
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        //GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glBegin(GL11.GL_LINES);
        for (int i= 0; i < shadowFrustum.length/2; i++) {
            Vector3f start = shadowFrustum[i];
            Vector3f end = shadowFrustum[i+4];
            GL11.glVertex3f(start.x, start.y, start.z);
            GL11.glVertex3f(end.x, end.y, end.z);
        }
        for (int i= 0; i < shadowFrustum.length/2-1; i++) {
            Vector3f start = shadowFrustum[i];
            Vector3f end = shadowFrustum[i+1];
            GL11.glVertex3f(start.x, start.y, start.z);
            GL11.glVertex3f(end.x, end.y, end.z);
        }
        for (int i= 0; i < shadowFrustum.length/2 ; i++) {
            Vector3f start = shadowFrustum[i+4];
            int endp = i +5;
            if(endp >= shadowFrustum.length) endp = 4;
            Vector3f end = shadowFrustum[endp];
            GL11.glVertex3f(start.x, start.y, start.z);
            GL11.glVertex3f(end.x, end.y, end.z);
        }
        GL11.glEnd();
        //GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
//
//        GL11.glBegin(GL11.GL_LINES);
//        Helper.col(0, 1, 1);
//        GL11.glVertex3f(shadowFrustum.x, ta.y, ta.z);
//        Helper.col(0, 1, 0);
//        GL11.glVertex3f(tb.x, tb.y, tb.z);
//        GL11.glEnd();
        Helper.col(1, 1, 1);
    }

    private void drawFrustum(Vector4f[] tempFrustumPoints) {
        Shader s = Ref.glRef.getShader("sprite");
        Ref.glRef.PushShader(s);

        Ref.ResMan.getWhiteTexture().Bind();
        Helper.col(1, 0, 0);
        
        GL11.glBegin(GL11.GL_LINES);
        for (int i= 0; i < tempFrustumPoints.length/2; i++) {
            Vector4f start = tempFrustumPoints[i];
            Vector4f end = tempFrustumPoints[i+4];
            GL11.glVertex3f(start.x, start.y, start.z);
            GL11.glVertex3f(end.x, end.y, end.z);
        }
        GL11.glEnd();
        
//        Helper.col(1, 1, 0,0.5f);
//        GL11.glDisable(GL11.GL_CULL_FACE);
//        GL11.glDepthMask(false);
//        GL11.glBegin(GL11.GL_QUADS);
//        for (int i= 0; i < tempFrustumPoints.length/2; i++) {
//            Vector4f end = tempFrustumPoints[i+4];
//            GL11.glVertex3f(end.x, end.y, end.z);
//        }
//        GL11.glEnd();
//        GL11.glDepthMask(true);
//        GL11.glEnable(GL11.GL_CULL_FACE);
        Helper.col(1, 1, 1);

        Ref.glRef.PopShader();
    }
    
    public boolean isRendering() {
        return rendering;
    }

    

    public static class Frustum {
        public Vector3f Origin;
        public Vector4f Orientation;

        public float rightSlope;
        public float leftSlope;
        public float topSlope;
        public float bottomSlope;
        public float near, far;
    }
}
