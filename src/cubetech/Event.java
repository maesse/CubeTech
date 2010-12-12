/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

/**
 *
 * @author mads
 */
public class Event {
    public int Time;
    public EventType Type;
    public int Value, Value2;
    public int datasize;
    public Object data;

    public Event() {
        Type = EventType.NONE;
    }
    
    public enum EventType {
        NONE,
        PACKET
    }
}
