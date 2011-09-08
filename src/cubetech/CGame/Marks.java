package cubetech.CGame;

import cubetech.collision.ChunkAreaQuery;
import cubetech.collision.CubeChunk;
import cubetech.collision.CubeMap;
import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Common.ErrorCode;
import cubetech.common.Helper;
import cubetech.gfx.CubeTexture;
import cubetech.misc.Ref;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import org.lwjgl.util.vector.Vector3f;

/**
 * Impact marks, etc..
 * @author mads
 */
public class Marks {
    private static final int MAX_MARK_FRAGMENTS = 128;
    private static final int MAX_MARK_POINTS = 384;
    private static final int MAX_VERTS_ON_POLY = 64;

    CVar cg_marks = Ref.cvars.Get("cg_marks", "1000", EnumSet.of(CVarFlags.ARCHIVE));


    private int[] fragmentData = new int[MAX_MARK_FRAGMENTS*2];
    private Vector3f[] pointData = new Vector3f[MAX_MARK_POINTS];

    private Queue<MarkPoly> activeMarks = new LinkedList<MarkPoly>();
    private Queue<MarkPoly> freeMarks = new LinkedList<MarkPoly>();

    private class MarkPoly {
        public int time;
        public CubeTexture texture;
        public PolyVert[] verts = new PolyVert[10];
        public int numVerts;
        public RenderEntity ent = new RenderEntity(REType.POLY);

        public void clear() {
            time = 0;
            texture = null;
            numVerts = 0;
        }

        private void set(CubeTexture mat, int numPoints, PolyVert[] verts) {
            time = Ref.cgame.cg.time;
            texture = mat;
            numVerts = numPoints;
            System.arraycopy(verts, 0, this.verts, 0, numPoints);

            // set renderentity
            ent.verts = verts;
            ent.frame = numVerts;
            ent.mat = texture.asMaterial();
            ent.flags = RenderEntity.FLAG_NOSHADOW;
        }
    }

    public class PolyVert {
        Vector3f xyz;
        float s;
        float t;
    }

    private class MarkRun {
        public Vector3f[] inPoints;
        public Vector3f projection;
        public Vector3f mins, maxs;


        public int maxPoints;
        public Vector3f[] pointBuffer;

        public int maxFragments;
        public int[] fragmentBuffer;

        public int returnedVerts;
        public int returnedFragments;
    }

    public Marks() {
    }

    public void addMarks() {
        if(cg_marks.modified){
            // Change max decal count
            cg_marks.modified = false;
            activeMarks.clear();
            freeMarks.clear();
            for (int i= 0; i < cg_marks.iValue; i++) {
                freeMarks.add(new MarkPoly());
            }
        }

        Iterator<MarkPoly> it = activeMarks.iterator();
        while(it.hasNext()) {
            MarkPoly markPoly = it.next();
            if(markPoly.time + 100000 < Ref.cgame.cg.time) {
                // Add to free list
                freeMark(markPoly);
                // remove from active
                it.remove();
                // move on
                continue;
            }
            Ref.render.addRefEntity(markPoly.ent);
        }
    }

    private MarkPoly allocMark() {
        if(freeMarks.isEmpty()) {
            freeMark(activeMarks.poll());
        }

        MarkPoly mark = freeMarks.poll();
        if(mark != null) {
            activeMarks.add(mark);
        }
        return mark;
    }

    private void freeMark(MarkPoly mark) {
        if(mark != null) {
            mark.clear();
            freeMarks.add(mark);
        }
    }

    public void impactMark(Vector3f origin, Vector3f dir, float radius, boolean temporary, CubeTexture mat) {
        if(!cg_marks.isTrue() || radius <= 0) {
            return;
        }

        Helper.Normalize(dir);
        Vector3f[] axis = new Vector3f[3];
        axis[0] = dir;
        axis[1] = Helper.perpendicularVector(axis[0], null);
        axis[2] = Vector3f.cross(axis[1], axis[0], null);

        // Create the polygon
        Vector3f[] orgPoints = new Vector3f[4];
        orgPoints[0] = Helper.VectorMA(origin, -radius, axis[1], null);
        orgPoints[0] = Helper.VectorMA(orgPoints[0], -radius, axis[2], orgPoints[0]);
        orgPoints[1] = Helper.VectorMA(origin, radius, axis[1], null);
        orgPoints[1] = Helper.VectorMA(orgPoints[1], -radius, axis[2], orgPoints[1]);
        orgPoints[2] = Helper.VectorMA(origin, radius, axis[1], null);
        orgPoints[2] = Helper.VectorMA(orgPoints[2], radius, axis[2], orgPoints[2]);
        orgPoints[3] = Helper.VectorMA(origin, -radius, axis[1], null);
        orgPoints[3] = Helper.VectorMA(orgPoints[3], radius, axis[2], orgPoints[3]);

        // Grab the fragments
        Vector3f projection = new Vector3f(dir);
        projection.scale(-20);
        
        int nFrags = markFragments(orgPoints, projection, MAX_MARK_POINTS, pointData, MAX_MARK_FRAGMENTS, fragmentData);

        float texCoordScale = 0.5f * 1.0f / radius;

        int nQuad = 0;

        Vector3f delta = new Vector3f();
        for (int i= 0; i < nFrags; i++) {
            int firstPoint = fragmentData[i*2];
            int numPoints = fragmentData[i*2+1];
            PolyVert[] verts = new PolyVert[numPoints];
            
            // we have an upper limit on the complexity of polygons
            // that we store persistantly
            if(numPoints > 10) {
                numPoints = 10;
            }
            
            for (int j= 0; j < numPoints; j++) {
                Vector3f.sub(pointData[firstPoint + j], origin, delta);
                float s = 0.5f + Vector3f.dot(delta, axis[1]) * texCoordScale;
                float t = 0.5f + Vector3f.dot(delta, axis[2]) * texCoordScale;

                PolyVert pv = new PolyVert();
                pv.s = s;
                pv.t = t;
                pv.xyz = new Vector3f(pointData[firstPoint + j]);
                verts[j] = pv;
            }

            if(temporary) {
                // Derp
                continue;
            }


            MarkPoly mark = allocMark(); // can be null
            if(mark == null) {
                return; // ran out of marks?
            }
            mark.set(mat, numPoints, verts);
            
            nQuad += numPoints / 4;
        }

        int test = 2;
    }

    private int markFragments(Vector3f[] points, Vector3f projection, int maxPoints, Vector3f[] pointBuffer,
            int maxFragments, int[] fragmentBuffer) {

        Vector3f projectDir = new Vector3f(projection);
        projectDir.normalise();

        Vector3f mins = new Vector3f(), maxs = new Vector3f();
        Helper.clearBounds(mins, maxs);

        for (int i= 0; i < points.length; i++) {
            Helper.AddPointToBounds(points[i], mins, maxs);
            Vector3f temp = Vector3f.add(points[i], projection, null);
            Helper.AddPointToBounds(temp, mins, maxs);
            // make sure we get all the leafs (also the one(s) in front of the hit surface)
            Helper.VectorMA(points[i], -20, projectDir, temp);
            Helper.AddPointToBounds(temp, mins, maxs);
        }

        Vector3f[] normals = new Vector3f[MAX_VERTS_ON_POLY+2];
        float[] dists = new float[MAX_VERTS_ON_POLY+2];
        // create the bounding planes for the to be projected polygon
        for (int i= 0; i < points.length; i++) {
            Vector3f v1 = Vector3f.sub(points[(i+1) % points.length], points[i], null);
            Vector3f v2 = Vector3f.add(points[i], projection, null);
            Vector3f.sub(points[i], v2, v2);
            normals[i] = Vector3f.cross(v1, v2, normals[i]);
            Helper.Normalize(normals[i]);
            dists[i] = Vector3f.dot(normals[i], points[i]);
        }

        // add near and far clipping planes for projection
        normals[points.length] = new Vector3f(projectDir);
        dists[points.length] = Vector3f.dot(normals[points.length], points[0]) - 32; // why -32?
        normals[points.length+1] = new Vector3f(projectDir);
        normals[points.length+1].scale(-1f);
        dists[points.length+1] = Vector3f.dot(normals[points.length+1], points[0]) - 20;

        int nPlanes = points.length + 2;


        Vector3f[][] clipPoints = new Vector3f[2][MAX_VERTS_ON_POLY];

        MarkRun markArgs = new MarkRun();
        markArgs.fragmentBuffer = fragmentBuffer;
        markArgs.maxFragments = maxFragments;
        markArgs.pointBuffer = pointBuffer;
        markArgs.maxPoints = maxPoints;
        markArgs.mins = mins;
        markArgs.maxs = maxs;
        markArgs.projection = projection;
        markArgs.inPoints = points;
        
        ChunkAreaQuery q = boxSurfaces(mins, maxs);
        int[] data = new int[3];
        while(q.getNext(data) != null) {
            // Check against the 6 faces
            for (int i= 0; i < 6; i++) {
                Vector3f normal = axisNormals[i];
//                // check the normal of this face
                if(Vector3f.dot(normal, projectDir) > -0.5f) {
                    continue;
                }
                
                // Get the vertices from this face
                getPoints(i, clipPoints, data);

                addMarkFragments(4, clipPoints, nPlanes, normals, dists, markArgs);
                if(markArgs.returnedFragments == maxFragments) {
                    return markArgs.returnedFragments; // not enough space for more fragments
                }
            }
        }
        return markArgs.returnedFragments;
    }

    private void addMarkFragments(int numClipPoints, Vector3f[][] clipPoints, int numPlanes, Vector3f[] normals,
            float[] dists, MarkRun args) {
        // chop the surface by all the bounding planes of the to be projected polygon

        int pingpong = 0;
        for (int i= 0; i < numPlanes; i++) {
            numClipPoints = chopPolyBehindPlane(numClipPoints, clipPoints[pingpong], clipPoints[pingpong^1], normals[i], dists[i], 0.5f);
            pingpong ^= 1;
            if(numClipPoints == 0) {
                break;
            }
        }

        // completely clipped away?
        if(numClipPoints == 0) {
            return;
        }

        if(numClipPoints + args.returnedVerts > args.maxPoints) {
            return; // not enough space for this polygon
        }

        args.fragmentBuffer[args.returnedFragments*2] = args.returnedVerts;
        args.fragmentBuffer[args.returnedFragments*2+1] = numClipPoints;

        System.arraycopy(clipPoints[pingpong], 0, args.pointBuffer, args.returnedVerts, numClipPoints);

        args.returnedVerts += numClipPoints;
        args.returnedFragments++;
    }

    // Used by chop
    private float[] c_dists = new float[MAX_VERTS_ON_POLY+4];
    private int[] sides = new int[MAX_VERTS_ON_POLY+4];
    // Flags
    private static final int SIDE_FRONT = 0;
    private static final int SIDE_BACK = 1;
    private static final int SIDE_ON = 2;
    private int chopPolyBehindPlane(int numInPoints, Vector3f[] inPoints, Vector3f[] outPoints,
            Vector3f normal, float dist, float epsilon) {
        

        // don't clip if it might overflow
        if(numInPoints >= MAX_VERTS_ON_POLY-2) {
            return 0;
        }

        

        int[] counts = new int[3];
        for (int i= 0; i < numInPoints; i++) {
            float dot = Vector3f.dot(inPoints[i], normal);
            dot -= dist;
            c_dists[i] = dot;
            if(dot > epsilon) {
                sides[i] = SIDE_FRONT;
            } else if(dot < -epsilon) {
                sides[i] = SIDE_BACK;
            } else {
                sides[i] = SIDE_ON;
            }

            counts[sides[i]]++;
        }
        sides[numInPoints] = sides[0];
        c_dists[numInPoints] = c_dists[0];

        int numOutPoints = 0;

        if(counts[0] == 0) {
            return numOutPoints;
        }

        if(counts[1] == 0) {
            numOutPoints = numInPoints;
            System.arraycopy(inPoints, 0, outPoints, 0, numInPoints);
            return numOutPoints;
        }

        for (int i= 0; i < numInPoints; i++) {
            Vector3f p1 = inPoints[i];

            if(sides[i] == SIDE_ON) {
                outPoints[numOutPoints] = new Vector3f(p1);
                numOutPoints++;
                continue;
            }

            if(sides[i] == SIDE_FRONT) {
                outPoints[numOutPoints] = new Vector3f(p1);
                numOutPoints++;
            }

            if(sides[i+1] == SIDE_ON || sides[i+1] == sides[i]) {
                continue;
            }

            // generate a split point
            Vector3f p2 = inPoints[(i+1)%numInPoints];
            float d = c_dists[i] - c_dists[i+1];
            float dot;
            if(d == 0) {
                dot = 0;
            } else {
                dot = c_dists[i] / d;
            }

            // clip xyz
            outPoints[numOutPoints] = Helper.VectorMA(p1, dot, Vector3f.sub(p2, p1, null), null);
            numOutPoints++;

        }

        return numOutPoints;
    }

    

    private static final Vector3f[] axisNormals = new Vector3f[] {
        new Vector3f(1,0,0),
        new Vector3f(-1,0,0),
        new Vector3f(0,1,0),
        new Vector3f(0,-1,0),
        new Vector3f(0,0,1),
        new Vector3f(0,0,-1)
    };


    private static void getPoints(int normalIdx, Vector3f[][] clipPoints, int[] absmin) {
        switch(normalIdx) {
            case 0:
                // X+ normal
                grabPoints(3, 7, 2, 6, absmin, clipPoints);
                break;
            case 1:
                // X- normal
                grabPoints(0,4,1,5, absmin, clipPoints);
                break;
            case 2:
                // Y+ normal
                grabPoints(5,1,7,3, absmin, clipPoints);
                break;
            case 3:
                // Y- normal
                grabPoints(2,6,0,4, absmin, clipPoints);
                break;
            case 4:
                // Z+ normal
                grabPoints(1,0,2,3, absmin, clipPoints);
                break;
            case 5:
                // Z- normal
                grabPoints(4,5,6,7, absmin, clipPoints);
                break;
        }
    }

    private static void grabPoints(int a, int b, int c, int d, int[] absmin, Vector3f[][] clipPoints) {
        clipPoints[0][0] = grabPoint(a, absmin);
        clipPoints[0][1] = grabPoint(b, absmin);
        clipPoints[0][2] = grabPoint(c, absmin);
        clipPoints[0][3] = grabPoint(d, absmin);
    }

    private static Vector3f grabPoint(int index, int[] absmin) {
        int cubeSize = CubeChunk.BLOCK_SIZE;
        // Indice -> vertex
        switch(index) {
            case 0:
                return new Vector3f(absmin[0], absmin[1], absmin[2]+cubeSize);
            case 1:
                return new Vector3f(absmin[0], absmin[1]+cubeSize, absmin[2]+cubeSize);
            case 2:
                return new Vector3f(absmin[0]+cubeSize, absmin[1], absmin[2]+cubeSize);
            case 3:
                return new Vector3f(absmin[0]+cubeSize, absmin[1]+cubeSize, absmin[2]+cubeSize);
            case 4:
                return new Vector3f(absmin[0], absmin[1], absmin[2]);
            case 5:
                return new Vector3f(absmin[0], absmin[1]+cubeSize, absmin[2]);
            case 6:
                return new Vector3f(absmin[0]+cubeSize, absmin[1], absmin[2]);
            case 7:
                return new Vector3f(absmin[0]+cubeSize, absmin[1]+cubeSize, absmin[2]);
            default:
                Ref.common.Error(ErrorCode.DROP, "Invalid indice index " + index);
                return null;
        }
    }


    private ChunkAreaQuery boxSurfaces(Vector3f mins, Vector3f maxs) {
        ChunkAreaQuery q = CubeMap.getCubesInVolume(mins, maxs, Ref.cgame.map.chunks, false);
        return q;
        
    }
}