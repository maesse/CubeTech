/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.client.Client;
import cubetech.server.Server;
import org.lwjgl.Sys;

/**
 *
 * @author mads
 */
public class Common {
    int frametime;
    int lasttime;
    int framemsec;
    long starttime;
    public Server server;
    public Client client;

    public void Frame() {
            int minMsec = (int)(1000f/100f);
            int msec = minMsec;
            do {
                frametime = EventLoop();

                if(lasttime > frametime)
                    lasttime = frametime;
                msec = frametime - lasttime;
            } while(msec < minMsec);

            framemsec = msec;
            if(msec > 200)
                msec = 200;

            server.Frame(msec);

            EventLoop();
            client.Frame(msec);

            lasttime = frametime;
    }

    int EventLoop() {
        Event ev;
        while(true) {
            ev = GetEvent();

            if(ev.Type == Event.EventType.NONE)
                return ev.Time;

            switch(ev.Type) {
                case NONE:
                    break;
                case PACKET:
                    if(ev.Value2 == 0)
                        server.PacketEvent(ev.data);
                    else
                        client.PacketEvent(ev.data);
                    break;
            }
        }
    }

    Event GetEvent() {
        // Try to get packet

        Event evt = new Event();
        evt.Time = Milliseconds();
        return evt;
    }

    Event CreateEvent(int time, Event.EventType type, int value, int value2, int dataSize, Object data) {
        Event evt = new Event();
        if(time == 0)
            time = Milliseconds();

        evt.Time = time;
        evt.Type = type;
        evt.Value = value;
        evt.Value2 = value2;
        evt.datasize = dataSize;
        evt.data = data;

        return evt;
    }

    int Milliseconds() {
        if(starttime == 0)
            starttime = (long)(Sys.getTime()/(long)(Sys.getTimerResolution()/1000f));
        return (int)((long)(Sys.getTime()/(long)(Sys.getTimerResolution()/1000f)) - starttime);
    }

    public void Init() {
        server = new Server();
        client = new Client();

        frametime = Milliseconds();
    }
}
