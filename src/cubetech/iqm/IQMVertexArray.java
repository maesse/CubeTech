package cubetech.iqm;

import java.nio.ByteBuffer;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class IQMVertexArray {
    // all vertex array entries must ordered as defined below, if present
    // i.e. position comes before normal comes before ... comes before custom
    // where a format and size is given, this means models intended for portable use should use these
    // an IQM implementation is not required to honor any other format/size than those recommended
    // however, it may support other format/size combinations for these types if it desire
    public static final int TYPE_POSITION     = 0;  // float, 3
    public static final int TYPE_TEXCOORD     = 1;  // float, 2
    public static final int TYPE_NORMAL       = 2;  // float, 3
    public static final int TYPE_TANGENT      = 3;  // float, 4
    public static final int TYPE_BLENDINDEXES = 4;  // ubyte, 4
    public static final int TYPE_BLENDWEIGHTS = 5;  // ubyte, 4
    public static final int TYPE_COLOR        = 6;  // ubyte, 4
    // all values up to CUSTOM are reserved for future use
    // any value >= CUSTOM is interpreted as CUSTOM type
    // the value then defines an offset into the string table, where offset = value - CUSTOM
    // this must be a valid string naming the type
    public static final int TYPE_CUSTOM       = 0x10;

    // vertex array format
    public static final int FORMAT_BYTE   = 0;
    public static final int FORMAT_UBYTE  = 1;
    public static final int FORMAT_SHORT  = 2;
    public static final int FORMAT_USHORT = 3;
    public static final int FORMAT_INT    = 4;
    public static final int FORMAT_UINT   = 5;
    public static final int FORMAT_HALF   = 6;
    public static final int FORMAT_FLOAT  = 7;
    public static final int FORMAT_DOUBLE = 8;

    int type;   // type or custom name
    int flags;
    int format; // component format

    int size;   // number of components (pr. vertex)
    int offset; // offset to array of tightly packed components, with num_vertexes * size total entries

    static void loadArrays(IQMModel model, ByteBuffer buffer) {
        if(model.header.num_vertexarrays == 0 || model.header.ofs_vertexarrays == 0) return;

        buffer.position(model.header.ofs_vertexarrays);
        model.vertexarrays = new IQMVertexArray[model.header.num_vertexarrays];
        for (int i= 0; i < model.header.num_vertexarrays; i++) {
            IQMVertexArray v = new IQMVertexArray();
            v.type = buffer.getInt();
            v.flags = buffer.getInt();
            v.format = buffer.getInt();
            v.size = buffer.getInt();
            v.offset = buffer.getInt();

            model.vertexarrays[i] = v;
        }

        for (int i= 0; i < model.vertexarrays.length; i++) {
            IQMVertexArray v = model.vertexarrays[i];
            buffer.position(v.offset);
            
            switch(v.type) {
                case TYPE_POSITION:
                    if(v.format != FORMAT_FLOAT || v.size != 3) throw new IllegalArgumentException("Invalid Vertex format");
                    model.in_position = new Vector3f[model.header.num_vertexes];
                    model.out_position = new Vector3f[model.header.num_vertexes];
                    for (int j= 0; j < model.in_position.length; j++) {
                        model.in_position[j] = new Vector3f(buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
                    }
                    break;
                case TYPE_NORMAL:
                    if(v.format != FORMAT_FLOAT || v.size != 3) throw new IllegalArgumentException("Invalid Vertex format");
                    model.in_normal = new Vector3f[model.header.num_vertexes];
                    model.out_normal = new Vector3f[model.header.num_vertexes];
                    for (int j= 0; j < model.in_position.length; j++) {
                        model.in_normal[j] = new Vector3f(buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
                    }
                    break;
                case TYPE_TEXCOORD:
                    if(v.format != FORMAT_FLOAT || v.size != 2) throw new IllegalArgumentException("Invalid Vertex format");
                    model.in_texcoord = new Vector2f[model.header.num_vertexes];
                    for (int j= 0; j < model.in_texcoord.length; j++) {
                        model.in_texcoord[j] = new Vector2f(buffer.getFloat(), buffer.getFloat());
                    }
                    break;
                case TYPE_TANGENT:
                    if(v.format != FORMAT_FLOAT || v.size != 4) throw new IllegalArgumentException("Invalid Vertex format");
                    model.in_tangent = new Vector4f[model.header.num_vertexes];
                    model.out_tangent = new Vector3f[model.header.num_vertexes];
                    model.out_bitangent = new Vector3f[model.header.num_vertexes];
                    for (int j= 0; j < model.in_position.length; j++) {
                        model.in_tangent[j] = new Vector4f(buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
                    }
                    break;
                case TYPE_BLENDINDEXES:
                    if(v.format != FORMAT_UBYTE || v.size != 4) throw new IllegalArgumentException("Invalid Vertex format");
                    model.in_blendindex = new byte[model.header.num_vertexes*4];
                    for (int j= 0; j < model.in_blendindex.length; j++) {
                        model.in_blendindex[j] = buffer.get();
                    }
                    break;
                case TYPE_BLENDWEIGHTS:
                    if(v.format != FORMAT_UBYTE || v.size != 4) throw new IllegalArgumentException("Invalid Vertex format");
                    model.in_blendweight = new byte[model.header.num_vertexes*4];
                    for (int j= 0; j < model.in_blendweight.length; j++) {
                        model.in_blendweight[j] = buffer.get();
                    }
                    break;
            } 
        }
    }

    private static short ubyteToShort(byte b) {
        if(b >= 0) return b;
        return (short) (256 + b);
    }
}
