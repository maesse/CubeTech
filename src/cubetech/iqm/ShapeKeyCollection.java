package cubetech.iqm;

import java.util.Collection;
import java.util.HashMap;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class ShapeKeyCollection {
    private Collection<ShapeKeyObject> objects;
    private HashMap<String, Float> keyStates = new HashMap<String, Float>();
    private IQMModel model = null;

    public ShapeKeyCollection(Collection<ShapeKeyObject> objects) {
        this.objects = objects;
    }

    public void setShapeKey(String name, float frac) {
        keyStates.put(name, frac);
    }

    // Called by iqmModel, applies all the shapekeys to the model data
    public void applyShapes(Vector3f[] modelData) {
        for (ShapeKeyObject shapeKeyObject : objects) {
            shapeKeyObject.applyShapeKeys(modelData, keyStates);
        }
    }

    // Prepare the shape keys for use on this model
    public void attachToModel(IQMModel model) {
        if(this.model != null) {
            System.out.println("Model already attached");
            return;
        } else if(objects == null) {
            System.out.println("No shapekey objects to attach");
            return;
        }
        
        for (ShapeKeyObject shapeKeyObject : objects) {
            // Initialize sub objects
            shapeKeyObject.attachToModel(model);
        }
        this.model = model;
        model.shapeKeys = this;
    }
}
