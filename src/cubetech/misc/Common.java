package cubetech.misc;

import cubetech.client.Client;
import cubetech.net.Packet;
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
        Ref.net.PumpNet();
        while(true) {
            ev = GetEvent();

            if(ev.Type == Event.EventType.NONE)
                return ev.Time;

            switch(ev.Type) {
                case NONE:
                    break;
                case PACKET:
                    if(ev.Value2 == 0)
                        server.PacketEvent((Packet)ev.data);
                    else
                        client.PacketEvent((Packet)ev.data);
                    break;
            }
        }
    }

    Event GetEvent() {
        // Try to get packet
        Packet packet = Ref.net.GetPacket();
        if(packet != null) {
            // We have a packet!
            return CreateEvent(packet.Time, Event.EventType.PACKET, 0, packet.type==Packet.SourceType.CLIENT?1:0, packet);
        }

        Event evt = new Event();
        evt.Time = Milliseconds();
        return evt;
    }

    Event CreateEvent(int time, Event.EventType type, int value, int value2, Object data) {
        Event evt = new Event();
        if(time == 0)
            time = Milliseconds();

        evt.Time = time;
        evt.Type = type;
        evt.Value = value;
        evt.Value2 = value2;
       // evt.datasize = dataSize;
        evt.data = data;

        return evt;
    }

    public int Milliseconds() {
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
