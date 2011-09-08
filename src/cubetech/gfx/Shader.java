package cubetech.gfx;

import cubetech.Game.Gentity;
import cubetech.common.Common;
import cubetech.common.IThinkMethod;
import cubetech.misc.FileWatcher;
import cubetech.misc.Ref;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.*;

/**
 *
 * @author mads
 */
public class Shader {
    public static final int INDICE_POSITION = 0;
    public static final int INDICE_NORMAL = 1;
    public static final int INDICE_COLOR = 6;
    public static final int INDICE_COORDS = 7;
    public static final int INDICE_COORDS2 = 8;
    
    // Shader handles
    private int shaderId = -1; // given to us by openGL
    private int vertShader = -1; // Vertex shader
    private int fragShader = -1; // Fragment shader
    private int geomShader = -1; // Geometry shader, if any
    private String shaderName = "NONE";

    private int[] textureUniforms = new int[8];
    HashMap<String, Integer> attributes = new HashMap<String, Integer>();
    HashMap<String, Integer> uniformMap = new HashMap<String, Integer>();
    private boolean validated = false;
    private boolean linked = false;
    private ShaderBuilder builder;
    private FileWatcher watcher;
    private Shader thisShader;
    private IThinkMethod onFileChange = new IThinkMethod() {
        public void think(Gentity ent) {

            if(Ref.cvars.Find("developer").isTrue()) {
                Ref.glRef.enqueueShaderRecompile(thisShader);
            }
        }
    };

    private enum Target {
        VERTEX_SHADER,
        FRAGMENT_SHADER,
        GEOMETRY_SHADER
    }

    public Shader(ShaderBuilder builder) throws Exception {
        if(!Ref.glRef.isInitalized()) throw new Exception("OpenGL is not initialized");
        thisShader = this;
        this.builder = builder;

        // Create program
        shaderName = "gfx/shaders/" + builder.getName();
        shaderId = ARBShaderObjects.glCreateProgramObjectARB();
        
        GLRef.checkError();
        // Load vertex and fragment shader
        if(shaderId > 0) {
            vertShader = createShader(shaderName, Target.VERTEX_SHADER);
            fragShader = createShader(shaderName, Target.FRAGMENT_SHADER);
        } else throw new Exception("Could not create shader");

        attributes.putAll(builder.getAttributes());
        link();
        String[] builderUniforms = builder.getTextureUniforms();
        for (int i= 0; i < builderUniforms.length; i++) {
            if(builderUniforms[i] == null) continue;

            mapTextureUniform(builderUniforms[i], i);
        }
        validate();

        Ref.glRef.shaders.put(builder.getName(), this);

        if(!Ref.glRef.isApplet()) {
            watcher = new FileWatcher(new File(Ref.cvars.Find("devpath").sValue+shaderName+".vert"), onFileChange);
            watcher = new FileWatcher(new File(Ref.cvars.Find("devpath").sValue+shaderName+".frag"), onFileChange);
        }
    }
    
    public void link() {
        if(linked) return;
        
        if(vertShader > 0 && fragShader > 0) {
            ARBShaderObjects.glAttachObjectARB(shaderId, vertShader);
            GLRef.checkError();
            ARBShaderObjects.glAttachObjectARB(shaderId, fragShader);

            GLRef.checkError();
            if(geomShader > 0) {
                ARBShaderObjects.glAttachObjectARB(shaderId, geomShader);
                GLRef.checkError();
                ARBGeometryShader4.glProgramParameteriARB(shaderId, ARBGeometryShader4.GL_GEOMETRY_INPUT_TYPE_ARB, GL11.GL_POINTS);

                ARBGeometryShader4.glProgramParameteriARB(shaderId, ARBGeometryShader4.GL_GEOMETRY_OUTPUT_TYPE_ARB, GL11.GL_POINTS);
                ARBGeometryShader4.glProgramParameteriARB(shaderId, ARBGeometryShader4.GL_GEOMETRY_VERTICES_OUT_ARB, 4);
                GLRef.checkError();
            }
            
            for (String string : attributes.keySet()) {
                int index = attributes.get(string);
                if(index == -1) continue;
                ARBVertexShader.glBindAttribLocationARB(shaderId, index, string);
            }
            GLRef.checkError();
            ARBShaderObjects.glLinkProgramARB(shaderId);

            
            
        } else {
            validated = false;
            Common.Log("Shader validation: Could not load fragment or vertex shader");
            return;
        }
        linked = true;
    }

    public void validate(){
        // Bind it all up and validate
        link();

        validated = true;
        Bind();
        validated =false;
        ARBShaderObjects.glValidateProgramARB(shaderId);
        GLRef.checkError();
        
        // Do a check
        boolean infoString = checkShader(shaderId, true);
        if(!infoString) {
            Common.Log("[%s] Warning: Shader link error.", shaderName);
        }

        // Check if shader is valid for use
        if(GL20.glGetProgram(shaderId, GL20.GL_VALIDATE_STATUS)!=GL11.GL_TRUE)
            Ref.common.Error(Common.ErrorCode.FATAL, "Shader assembly failed.");
        GLRef.checkError();

        for (String string : attributes.keySet()) {
            int attrIndex = ARBVertexShader.glGetAttribLocationARB(shaderId, string);
            int wantedIndex = attributes.get(string);
            if(attrIndex != wantedIndex) {
                Common.Log("[%s] Fixing attribute index for %s (%d to %d)", shaderName, string, wantedIndex, attrIndex);
                attributes.put(string, attrIndex);
            }
        }

        validated = true;
    }

    public void Bind() {
        if(!validated) Ref.common.Error(Common.ErrorCode.FATAL, "Shader.Bind(): Tried to bind an unvalidated shader");
        ARBShaderObjects.glUseProgramObjectARB(shaderId);
        setTextureUniforms();
        GLRef.checkError();
    }

    public static void Release() {
        ARBShaderObjects.glUseProgramObjectARB(0);
        GLRef.checkError();
    }

    public void recompile() {
        boolean currentBound = Ref.glRef.shader == this;
        if(currentBound) Release();

        Common.Log("Reloading/recompiling shader " + shaderName);

        // Delete vertex and fragment shader
        int _shaderId = shaderId;
        int _vertShader = vertShader;
        int _fragShader = fragShader;
        

        vertShader = fragShader = shaderId = 0;
        shaderId = ARBShaderObjects.glCreateProgramObjectARB();
        linked = false;
        validated = false;

        GLRef.checkError();
        // Load vertex and fragment shader
        vertShader = createShader(shaderName, Target.VERTEX_SHADER);
        fragShader = createShader(shaderName, Target.FRAGMENT_SHADER);
        GLRef.checkError();

        if(vertShader > 0 && fragShader > 0) {
            ARBShaderObjects.glDetachObjectARB(_shaderId, _vertShader);
            ARBShaderObjects.glDetachObjectARB(_shaderId, _fragShader);
            ARBShaderObjects.glDeleteObjectARB(_vertShader);
            ARBShaderObjects.glDeleteObjectARB(_fragShader);
            ARBShaderObjects.glDeleteObjectARB(_shaderId);
            uniformMap.clear();
        } else {
            ARBShaderObjects.glDeleteObjectARB(shaderId);
            shaderId = _shaderId;
            vertShader  = _vertShader;
            fragShader = _fragShader;
            linked = true;
            validated = true;
            Common.Log("[Shader] Reverted to last successful compile");
            return;
        }

        link();
        String[] builderUniforms = builder.getTextureUniforms();
        for (int i= 0; i < builderUniforms.length; i++) {
            if(builderUniforms[i] == null) continue;

            mapTextureUniform(builderUniforms[i], i);
        }
        validate();

        if(currentBound) Ref.glRef.shader.Bind();
    }
    
    private int createShader(String name, Target t) {
        int target = targetToOpenGL(t);
        // Create the shader object
        int shaderid = ARBShaderObjects.glCreateShaderObjectARB(target);
        if(shaderid <= 0) {
                System.err.println("Could not create vertex shader");
                return shaderid;
        }

        String fileExtension = targetToFileExtention(t);

        // Read shader file
        ByteBuffer buf = null;
        try {
            buf = ResourceManager.OpenFileAsNetBuffer(name+fileExtension, true).getKey().GetBuffer();
        } catch (Exception ex) {
            System.err.println(ex);
            return 0;
        }

        // Compile
        ARBShaderObjects.glShaderSourceARB(shaderid, buf);
        ARBShaderObjects.glCompileShaderARB(shaderid);

        // Check it
        boolean info = checkShader(shaderid, false);
        GLRef.checkError();
        if(!info) {
            System.err.println(String.format("Shader check failed. (%s)", t.toString()));
            return 0;
        }
        return shaderid;
    }

    private boolean checkShader(int shader, boolean checklink) {
        // OpenGL returns len > 1 when everythings OK.
        IntBuffer buf = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(shader, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB, buf);
        GLRef.checkError();
        int lenght = buf.get();
        if(lenght > 1) {
            // Got some output
            ByteBuffer bytebuf = BufferUtils.createByteBuffer(lenght);
            buf.flip(); // flip the int buffer for reuse
            ARBShaderObjects.glGetInfoLogARB(shader, buf, bytebuf);
            GLRef.checkError();
            byte[] infoBytes = new byte[lenght];
            bytebuf.get(infoBytes);
            String log = new String(infoBytes).trim();
            String[] lines = log.split("\n");
            boolean nonsuccess = false;
            for (String string : lines) {
                if(!string.contains("successful")) {
                    nonsuccess = true;
                    break;
                }
            }
            if(nonsuccess) {
                for (String string : lines) {
                    if(!string.contains("successful")) {
                        Common.Log("[%s] Validation Output: %s", shaderName, string);
                    }
                }
            }
        }

        if(checklink)
            return GL20.glGetProgram(shader, GL20.GL_LINK_STATUS)==GL11.GL_TRUE;

        return GL20.glGetShader(shader, GL20.GL_COMPILE_STATUS)==GL11.GL_TRUE;

    }

    public void mapTextureUniform(String name, int textureIndex) {
        textureUniforms[textureIndex] = ARBShaderObjects.glGetUniformLocationARB(shaderId, name);
        GLRef.checkError();
    }

    public int GetTextureIndex(int index) {
        return textureUniforms[index];
    }

    private int targetToOpenGL(Target t) {
        switch(t) {
            case VERTEX_SHADER:
                return ARBVertexShader.GL_VERTEX_SHADER_ARB;
            case FRAGMENT_SHADER:
                return ARBFragmentShader.GL_FRAGMENT_SHADER_ARB;
            case GEOMETRY_SHADER:
                return ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB;
            default:
                throw new IllegalArgumentException();
        }
    }

    private String targetToFileExtention(Target t) {
        switch(t) {
            case VERTEX_SHADER:
                return ".vert";
            case FRAGMENT_SHADER:
                return ".frag";
            case GEOMETRY_SHADER:
                return ".geom";
            default:
                throw new IllegalArgumentException();
        }
    }

    public int getShaderId() {
        return shaderId;
    }

    public void setAttribute(String name, int index) {
        attributes.put(name, index);
    }

    public void setUniform(String name, float value) {
        int index = getUniformIndex(name);
        ARBShaderObjects.glUniform1fARB(index, value);
        GLRef.checkError();
    }

    public void setUniform(int name, int value) {
        ARBShaderObjects.glUniform1iARB(name, value);
        GLRef.checkError();
    }

    public void setUniform(String name, int value) {
        int index = getUniformIndex(name);
        ARBShaderObjects.glUniform1iARB(index, value);
        GLRef.checkError();
    }

    public void setUniform(String name, Vector3f value) {
        int index = getUniformIndex(name);
        ARBShaderObjects.glUniform3fARB(index, value.x, value.y, value.z);
        GLRef.checkError();
    }

    public void setUniform(String name, Vector2f value) {
        int index = getUniformIndex(name);
        ARBShaderObjects.glUniform2fARB(index, value.x, value.y);
        GLRef.checkError();
    }

     public void setUniform(String name, Vector4f value) {
        int index = getUniformIndex(name);
        ARBShaderObjects.glUniform4fARB(index, value.x, value.y, value.z, value.w);
        GLRef.checkError();
    }

     public void setUniform(String name, Vector4f[] values) {
        int index = getUniformIndex(name);

        // We've got an index, yay
        FloatBuffer buf = Ref.glRef.matrixBuffer;
        if(values.length > 4) {
            buf = BufferUtils.createFloatBuffer(4*values.length);
        }
        buf.position(0);
         for (int i= 0; i < values.length; i++) {
             buf.put(values[i].x);
             buf.put(values[i].y);
             buf.put(values[i].z);
             buf.put(values[i].w);
         }
        buf.position(0);

        ARBShaderObjects.glUniform4ARB(index, buf);
        GLRef.checkError();
    }

     public void setUniform(String name, Matrix4f value) {
        int index = getUniformIndex(name);

        // We've got an index, yay
        Ref.glRef.matrixBuffer.position(0);
        value.store(Ref.glRef.matrixBuffer);
        Ref.glRef.matrixBuffer.position(0);
        ARBShaderObjects.glUniformMatrix4ARB(index, false, Ref.glRef.matrixBuffer);

        GLRef.checkError();
    }

     public void setUniform(String name, Matrix4f[] values) {
        int index = getUniformIndex(name);

        // We've got an index, yay
        FloatBuffer arrayBuffer = BufferUtils.createFloatBuffer(16*values.length);
        arrayBuffer.position(0);
         for (int i= 0; i < values.length; i++) {
             if(values[i] == null) break; // Stop when hitting empty values

             values[i].store(arrayBuffer);
         }

        arrayBuffer.position(0);

        ARBShaderObjects.glUniformMatrix4ARB(index, false, arrayBuffer);

        GLRef.checkError();
    }

     public void setUniformMat3(String string, FloatBuffer viewbuffer) {
        int index = getUniformIndex(string);
        ARBShaderObjects.glUniformMatrix3ARB(index, false, viewbuffer);
        GLRef.checkError();
    }
     public void setUniformMat4(String string, FloatBuffer viewbuffer) {
        int index = getUniformIndex(string);
        ARBShaderObjects.glUniformMatrix4ARB(index, false, viewbuffer);
        GLRef.checkError();
    }

     public void setUniform(String name, Matrix3f value) {
        int index = getUniformIndex(name);

        // We've got an index, yay
        Ref.glRef.matrixBuffer.position(0);
        value.store(Ref.glRef.matrixBuffer);
        Ref.glRef.matrixBuffer.flip();
        ARBShaderObjects.glUniformMatrix3ARB(index, false, Ref.glRef.matrixBuffer);

        GLRef.checkError();
    }

    private int getUniformIndex(String name) {
        Integer index = uniformMap.get(name);
        if(index == null) {
            // try to bind it
            int pos = ARBShaderObjects.glGetUniformLocationARB(shaderId, name);
            // Handle error
            GLRef.checkError();
            if(pos < 0) {
                Common.LogDebug("Warning: Shader.getUniformIndex(): Uniform '%s' doesn't exist in %s - get index %d", name, shaderName, pos);
            }
            // cache it
            index = pos;
            uniformMap.put(name, index);
        }
        return index;
    }

    public void setTextureUniforms() {
        for (int i= 0; i < textureUniforms.length; i++) {
            if(textureUniforms[i] == 0) continue;
            ARBShaderObjects.glUniform1iARB(textureUniforms[i], i);
        }
    }
}
