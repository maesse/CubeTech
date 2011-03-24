/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

/**
 *
 * @author mads
 */
public class Event {
    public int Time;
    public EventType Type;
    public int Value, Value2;
    public Object data;

    public Event() {
        Type = EventType.NONE;
    }
    
    public enum EventType {
        NONE,
        PACKET
    }
}
