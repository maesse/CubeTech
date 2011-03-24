package cubetech.gfx;

import cubetech.misc.Ref;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 *
 * @author mads
 */
public class Shader {
    // Shader handles
    private int shaderId = -1; // given to us by openGL
    private int vertShader = -1; // Vertex shader
    private int fragShader = -1; // Fragment shader
    private int geomShader = -1; // Geometry shader, if any

    // GLSL Shader variable positions
    private int uniform_texture = 0;
    private int attr_position = 0;
    private int attr_coords = 0;
    private int attr_color = 0;

    public Shader(String name) throws Exception {
        if(!Ref.glRef.isInitalized())
            throw new Exception("OpenGL is not initialized");

        // Create program
        shaderId = ARBShaderObjects.glCreateProgramObjectARB();

        // Load vertex and fragment shader
        if(shaderId > 0) {
            vertShader = createVertShader(name);
            fragShader = createFragShader(name);
//            geomShader = createGeomShader(name);
        } else
            throw new Exception("Could not create shader");

        // Bind it all up and validate
        if(vertShader > 0 && fragShader > 0) {
            ARBShaderObjects.glAttachObjectARB(shaderId, vertShader);
            ARBShaderObjects.glAttachObjectARB(shaderId, fragShader);
            if(geomShader > 0) {
                ARBShaderObjects.glAttachObjectARB(shaderId, geomShader);
                ARBGeometryShader4.glProgramParameteriARB(shaderId, ARBGeometryShader4.GL_GEOMETRY_INPUT_TYPE_ARB, GL11.GL_POINTS);
                ARBGeometryShader4.glProgramParameteriARB(shaderId, ARBGeometryShader4.GL_GEOMETRY_OUTPUT_TYPE_ARB, GL11.GL_POINTS);
                ARBGeometryShader4.glProgramParameteriARB(shaderId, ARBGeometryShader4.GL_GEOMETRY_VERTICES_OUT_ARB, 4);

            }
            GL20.glBindAttribLocation(shaderId, 0, "v_position");
            GL20.glBindAttribLocation(shaderId, 1, "v_color");
            GL20.glBindAttribLocation(shaderId, 2, "v_coords");
            
            ARBShaderObjects.glLinkProgramARB(shaderId);
            ARBShaderObjects.glValidateProgramARB(shaderId);
        } else throw new Exception("Could not load fragment or vertex shader");

        boolean infoString = checkShader(shaderId);
        if(!infoString)
            throw new Exception("Shader assembly failed.");
        
        uniform_texture = ARBShaderObjects.glGetUniformLocationARB(shaderId, "tex");
        attr_position = GL20.glGetAttribLocation(shaderId, "v_position");
        attr_coords = GL20.glGetAttribLocation(shaderId, "v_coords");
        attr_color = GL20.glGetAttribLocation(shaderId, "v_color");
    }

    public void Bind() {
        ARBShaderObjects.glUseProgramObjectARB(shaderId);
        ARBShaderObjects.glUniform1iARB(uniform_texture, 0); // Bind TEXTURE0 to "tex"
    }

    public static void Release() {
        ARBShaderObjects.glUseProgramObjectARB(0);
    }

    public int GetTextureIndex() {
        return uniform_texture;
    }

    private int createVertShader(String name) {
        // Create the shader object
        vertShader = ARBShaderObjects.glCreateShaderObjectARB(ARBVertexShader.GL_VERTEX_SHADER_ARB);
        if(vertShader <= 0) {
                System.err.println("Could not create vertex shader");
                return vertShader;
        }
        
        // Read shader file
        ByteBuffer buf = null;
        try {
            buf = ResourceManager.OpenFileAsNetBuffer(name+".vert", true).getKey().GetBuffer();
        } catch (Exception ex) {
            System.err.println(ex);
            return 0;
        }

        // Compile
        ARBShaderObjects.glShaderSourceARB(vertShader, buf);
        ARBShaderObjects.glCompileShaderARB(vertShader);

        // Check it
        boolean info = checkShader(vertShader);
        if(!info) {
            System.err.println("Vertex shader check failed.");
            return 0;
        }
        return vertShader;
    }

    private int createGeomShader(String name) {
        // Create the shader object
        geomShader = ARBShaderObjects.glCreateShaderObjectARB(ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB);
        if(geomShader <= 0) return geomShader;

        // Read shader file
        ByteBuffer buf = null;
        try {
            buf = ResourceManager.OpenFileAsNetBuffer(name+".geom", true).getKey().GetBuffer();
        } catch (Exception ex) {
            System.err.println(ex);
            return 0;
        }

        // Compile
        ARBShaderObjects.glShaderSourceARB(geomShader, buf);
        ARBShaderObjects.glCompileShaderARB(geomShader);

        // Check it
        boolean info = checkShader(geomShader);
        if(!info) {
            System.err.println("Geometry shader check failed.");
            return 0;
        }
        return geomShader;
    }

    private int createFragShader(String name) {
        // Create the shader object
        fragShader = ARBShaderObjects.glCreateShaderObjectARB(ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
        if(fragShader <= 0) {
                System.err.println("Could not create fragment shader");
                return fragShader;
        }

        // Read shader file
        ByteBuffer buf = null;
        try {
            buf = ResourceManager.OpenFileAsNetBuffer(name+".frag", true).getKey().GetBuffer();
        } catch (Exception ex) {
            System.err.println(ex);
            return 0;
        }

        // Compile
        ARBShaderObjects.glShaderSourceARB(fragShader, buf);
        ARBShaderObjects.glCompileShaderARB(fragShader);

        // Check it
        boolean info = checkShader(fragShader);
        if(!info) {
            System.err.println("Fragment shader check failed.");
            return 0;
        }
        return fragShader;
    }

    private boolean checkShader(int shader) {
        // OpenGL returns len > 1 when everythings OK.
        IntBuffer buf = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(shader, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB, buf);

        int lenght = buf.get();
        if(lenght > 1) {
            // Got some output
            ByteBuffer bytebuf = BufferUtils.createByteBuffer(lenght);
            buf.flip(); // flip the int buffer for reuse
            ARBShaderObjects.glGetInfoLogARB(shader, buf, bytebuf);
            byte[] infoBytes = new byte[lenght];
            bytebuf.get(infoBytes);
            System.out.println(new String(infoBytes).trim());
            //return new String(infoBytes);
        }

        return GL20.glGetShader(shader, GL20.GL_COMPILE_STATUS)==GL11.GL_TRUE;

        //return null;
    }

}
