package cubetech.gfx;

/**
 *
 * @author mads
 */
public class Resource {
    public String Name;
    public Object Data;
    public ResourceType Type;
    public boolean loaded;

    //
    public int target;

    public Resource(String name, ResourceType type, int target) {
        this.Name = name;
        this.Type = type;
        this.target = target;
    }

    public enum ResourceType {
        TEXTURE,
        SOUND,
        MODEL
    }
}
