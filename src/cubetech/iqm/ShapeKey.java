package cubetech.iqm;

import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class ShapeKey {
    private static final int MAX_MAPS_PR_VERTEX = 4;
    String name;
    float[] vertices;
    int[] indices;
    ShapeKeyObject object;
    
    ShapeKey(String name) {
        this.name = name;
    }

    int[] mapVertices(Vector3f[] in_position, int offset, int len) {
        int vertexCount = vertices.length/3;
        int[] mapIndices = new int[vertexCount*MAX_MAPS_PR_VERTEX];
        for (int i= 0; i < vertexCount; i++) {
            float x = vertices[i * 3 + 0];
            float y = vertices[i * 3 + 1];
            float z = vertices[i * 3 + 2];
            // Find this vertex in the in_position array
            int j;
            int jsub = 0;
            for (j= offset; j < offset + len; j++) {
                if(in_position[j].x != x) continue;
                if(in_position[j].y != y) continue;
                if(in_position[j].z != z) continue;
                // found a match!
                mapIndices[i*MAX_MAPS_PR_VERTEX + jsub] = j;
                jsub++;
                if(jsub >= MAX_MAPS_PR_VERTEX) {
                    break;
                }
            }
            if(j == in_position.length && jsub == 0) {
                // not vertex found that matched our basis vertex
                System.out.println("Missed a vertex");
            }
            // zero out (-1) unused indices
            for (int k= jsub; k < MAX_MAPS_PR_VERTEX; k++) {
                mapIndices[i*MAX_MAPS_PR_VERTEX+k] = -1;
            }
        }
        return mapIndices;
    }

    void applyToData(Vector3f[] data, float frac) {
        if(object == null) {
            System.out.println("ShapeKey not hooked up to an ShapeKeyObject");
            return;
        }
        
        int vertexCount = vertices.length/3;
        int[] indiceMap = object.basisIndices;
        for (int i= 0; i < vertexCount; i++) {
            // Get data vertex indice
            for (int j= 0; j < MAX_MAPS_PR_VERTEX; j++) {
                int mappedIndice = indiceMap[indices[i]*MAX_MAPS_PR_VERTEX + j];
                if(mappedIndice < 0 || mappedIndice >= data.length) break;

                // This is the vertex we're modifying
                Vector3f v = data[mappedIndice];

                // Grab our delta vertex
                v.x += vertices[i * 3 + 0] * frac;
                v.y += vertices[i * 3 + 1] * frac;
                v.z += vertices[i * 3 + 2] * frac;
            }
            
        }
    }
}
