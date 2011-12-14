package cubetech.iqm;

import java.util.HashMap;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class ShapeKeyObject {
    String name;
    private HashMap<String, ShapeKey> shapeKeys = new HashMap<String, ShapeKey>();
    int[] basisIndices = null;
    IQMModel model = null;

    ShapeKeyObject(String name) {
        this.name = name;
    }

    // Used by loader
    void addShapeKey(ShapeKey key) {
        shapeKeys.put(key.name, key);
        key.object = this;
    }

    // Init
    void attachToModel(IQMModel model) {
        int firstVertex = 0;
        int vertexCount = model.in_position.length;
        for (IQMMesh iQMMesh : model.meshes) {
            if(iQMMesh.name.equalsIgnoreCase(name)) {
                firstVertex = iQMMesh.first_vertex;
                vertexCount = iQMMesh.num_vertexes;
                break;
            }
        }

        // Map shapekey vertices to mesh vertices
        // shapekey vertices id -> model vertex index
        ShapeKey basis = shapeKeys.get("Basis");
        if(basis == null) {
            System.out.println("No basis keyshape for object " + name);
            return;
        }
        basisIndices = basis.mapVertices(model.in_position, firstVertex, vertexCount);
        this.model = model;
    }

    /**
     * Applies the shapekey to the current model
     * @param key the name of the skapekey to use
     * @param frac how much of the shapekey to apply. 0->1
     */
    void applyShapeKey(Vector3f[] data, String key, float frac) {
        // Not attached
        if(model == null || basisIndices == null) {
            System.out.println("Tried to use a non-attached shapekey");
            return;
        }

        // Shapekey doesn't influence the mesh
        if(frac == 0f) return; 

        // Check if the shapekey exists
        ShapeKey shapeKey = shapeKeys.get(key);
        if(shapeKey == null) return;

        shapeKey.applyToData(data, frac);
    }

    // Helper method
    void applyShapeKeys(Vector3f[] data, HashMap<String, Float> keyStates) {
        for (String shapeName : keyStates.keySet()) {
            applyShapeKey(data, shapeName, keyStates.get(shapeName));
        }
    }
}
