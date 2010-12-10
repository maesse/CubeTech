/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

/**
 *
 * @author mads
 */
public class Resource {
    public String Name;
    public Object Data;
    public ResourceType Type;
    
    public enum ResourceType {
        TEXTURE,
        SOUND
    }
}
