/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.server;

import cubetech.net.Packet;

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

    public void PacketEvent(Packet data) {
        
        System.out.println(data.buf.ReadString());
    }
}
