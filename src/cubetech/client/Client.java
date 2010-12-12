/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.client;

import cubetech.misc.Ref;

/**
 *
 * @author mads
 */
public class Client {
    int frametime;
    int realtime;
    public State state = State.DISCONNECTED;

    public void PacketEvent(Object data) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public enum State {
        DISCONNECTED,
        CONNECTED
    }
    
    public void Frame(int msec) {
        frametime = msec;
        realtime += msec;

        //CheckUserInfo();

        //CheckTimeout();

        Ref.Input.SendCommand();
        // Update input
        // Send command

        // Checkforresend

        // setcgametime

        // updatescreen
        // endframe
    }
}
