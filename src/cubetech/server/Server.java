/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.server;

/**
 *
 * @author mads
 */
public class Server {
    public boolean running = false;
    int timeResidual;
    int time;

    
    public void Frame(int msec) {
        if(!running)
            return;

        int frameMsec = (int)(1000f / 60f);
        timeResidual += msec;

        while(timeResidual >= frameMsec) {
            timeResidual -= frameMsec;
            time += frameMsec;

            // Game run frame
        }

        // Check timeout

        // sendclientmessages

    }

    public void PacketEvent(Object data) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
