package cubetech.iqm;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.ResourceManager;
import cubetech.misc.Ref;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 *
 * @author mads
 */
public class IQMMesh {
    //int name;     // unique name for the mesh, if desired
    String name;
    String material; // set to a name of a non-unique material or texture
    CubeTexture tex = null;
    CubeTexture normalmap = null;
    CubeTexture specularmap = null;
    int first_vertex, num_vertexes;
    int first_triangle, num_triangles;

    private String modelPath = "";


    static void loadMesh(IQMModel model, ByteBuffer buffer) {
        if(model.header.num_meshes == 0 || model.header.ofs_meshes == 0)
            return;
        


        buffer.position(model.header.ofs_meshes);
        model.meshes = new IQMMesh[model.header.num_meshes];
        for (int i= 0; i < model.header.num_meshes; i++) {
            IQMMesh m = new IQMMesh();
            m.modelPath = model.header.pathName;
            int name = buffer.getInt();
            if(name >= 0) {
                int end = name;
                while(end < model.text.length && model.text[end] != '\0') end++;
                try {
                    m.name = new String(model.text, name, end-name, "US-ASCII");
                } catch (UnsupportedEncodingException ex) {
                }
            }
            int material = buffer.getInt();
            if(material >= 0) {
                int end = material;
                while(end < model.text.length && model.text[end] != '\0') end++;
                try {
                    m.material = new String(model.text, material, end-material, "US-ASCII");
                } catch (UnsupportedEncodingException ex) {
                }
            }
            m.first_vertex= buffer.getInt() & 0xffffffff;
            m.num_vertexes= buffer.getInt()& 0xffffffff;
            m.first_triangle= buffer.getInt()& 0xffffffff;
            m.num_triangles= buffer.getInt()& 0xffffffff;

            model.meshes[i] = m;
        }
    }

    void bindTextures() {
        if(tex == null) {
            String path = modelPath + material;
            if(!material.contains(".")) {
                path = modelPath + material + ".png";
                if(!ResourceManager.FileExists(path)) {
                    path = modelPath + material + ".tga";
                }
            }
            tex = Ref.ResMan.LoadTexture(path);
        }
        if(normalmap == null && tex != null) {
            int lastdot = tex.name.lastIndexOf(".");
            String cleanname = tex.name.substring(0, lastdot);
            cleanname += "_normal" + tex.name.substring(lastdot);
            if(ResourceManager.FileExists(cleanname)) {
                normalmap = Ref.ResMan.LoadTexture(cleanname);
                normalmap.textureSlot = 3;
            } else {
                Ref.ResMan.getNoNormalTexture().Bind();
            }
        } 
        if(specularmap == null && tex != null) {
            int lastdot = tex.name.lastIndexOf(".");
            String cleanname = tex.name.substring(0, lastdot);
            cleanname += "_specular" + tex.name.substring(lastdot);
            if(ResourceManager.FileExists(cleanname)) {
                specularmap = Ref.ResMan.LoadTexture(cleanname);
                specularmap.textureSlot = 4;
            } else {
                Ref.ResMan.getNoSpecularTexture().Bind();
            }
        }
        
        if(tex != null) tex.Bind();
        if(normalmap != null) normalmap.Bind();
        if(specularmap != null) specularmap.Bind();
    }

    void unbindTextures() {

    }
}
