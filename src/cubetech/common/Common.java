package cubetech.common;

import cubetech.misc.Event;
import cubetech.misc.ExitException;
import cubetech.misc.FrameException;
import cubetech.misc.Ref;
import cubetech.net.Packet;
import java.applet.Applet;
import java.awt.Canvas;
import java.util.EnumSet;
import javax.swing.JOptionPane;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;

/**
 *
 * @author mads
 */
public class Common {
    // Constants
    public static final int MAX_GENTITYBITS= 12;
    public static final int MAX_GENTITIES = (1 << MAX_GENTITYBITS);
    public static final int ENTITYNUM_NONE = MAX_GENTITIES - 1;
    public static final int ENTITYNUM_WORLD = MAX_GENTITIES - 2;
    public static final int ENTITYNUM_MAX_NORMAL = MAX_GENTITIES - 2;
    public static final int MAX_PS_EVENTS = 2;
    public static final int EVENT_VALID_MSEC = 300;
    public static final int EV_EVENT_BIT1 = 0x00000100;
    public static final int EV_EVENT_BIT2 = 0x00000200;
    public static final int EV_EVENT_BITS = EV_EVENT_BIT1 | EV_EVENT_BIT2;
    public static final int DEFAULT_GRAVITY = 800;

    // Cvars
    public CVar sv_running; // current server status
    public CVar cl_running; // current client status
    public CVar cl_paused;
    public CVar sv_paused;
    public CVar maxfps; // cap framerate
    public CVar errorMessage; // Will be set when an error occurs
    public CVar developer;

    public CVar com_timer; // 1: LWJGLs timer, 2: Javas nano-seconds
    public CVar com_sleepy; // Enables thread sleeping when not running vsync
    
    public CVar com_sleepPrecision; // sleep and yield precision can vary
    public CVar com_yieldPrecision; // for different platforms/computers

    public int frametime; // the time this frame
    public ItemList items = new ItemList();
    
    private int lasttime; // the time last frame
    private int framemsec; // delta time between frames
    private boolean useSysTimer = true; // Controls the current timer. com_timer sets this.
    private Event tempevt = new Event();
    

    public enum ErrorCode {
        FATAL, // exit the entire game with a popup window
        DROP, // print to console and disconnect from game
        SERVERDISCONNECT, // don't kill server
        DISCONNECT // client disconnected from the server
    }

    public void Init() {
        lasttime = Milliseconds();
        // Set up cvars
        maxfps = Ref.cvars.Get("maxfps", "100", EnumSet.of(CVarFlags.ARCHIVE));
        developer = Ref.cvars.Get("developer", "1", EnumSet.of(CVarFlags.ARCHIVE));
        cl_running = Ref.cvars.Get("cl_running", "0", EnumSet.of(CVarFlags.ROM));
        sv_running = Ref.cvars.Get("sv_running", "0", EnumSet.of(CVarFlags.ROM));
        cl_paused = Ref.cvars.Get("cl_paused", "0", EnumSet.of(CVarFlags.ROM));
        sv_paused = Ref.cvars.Get("sv_paused", "0", EnumSet.of(CVarFlags.ROM));
        errorMessage = Ref.cvars.Get("errorMessage", "", EnumSet.of(CVarFlags.ROM));
        com_sleepy = Ref.cvars.Get("com_sleepy", "0", EnumSet.of(CVarFlags.TEMP));
        com_sleepPrecision = Ref.cvars.Get("com_sleepPrecision", "4", EnumSet.of(CVarFlags.TEMP));
        com_yieldPrecision = Ref.cvars.Get("com_yieldPrecision", "1", EnumSet.of(CVarFlags.TEMP));
        com_timer = Ref.cvars.Get("com_timer", "2", EnumSet.of(CVarFlags.TEMP));
        com_timer.Max = 2;
        com_timer.Min = 1;
        useSysTimer = com_timer.iValue == 1;
        errorMessage.modified = false;
        // Init client and server
        Ref.server.Init();
        Ref.client.Init();
    }

    public static void LogDebug(String str) {
        if(Ref.common.isDeveloper())
            System.out.println("[D] " + str);
    }

    public static void Log(String str) {
        System.out.println(str);
    }

    // Where the program starts
    public static void Startup(Canvas parentDisplay, Applet applet) {
        // Init
        try {
            Ref.InitRef();

            Ref.glRef.InitWindow(parentDisplay, applet);
            Ref.Input.Init(); // Initialize mouse and keyboard
            Ref.common.Init();
        } catch (Exception ex) {
            System.out.println("Fatal crash: " + ex.toString());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(Display.getParent(), ex);

            if(Ref.glRef != null)
                Ref.glRef.Destroy();
            Display.destroy();
            System.exit(-1);
        }
        
        // Run
        try {
            while (!Display.isCloseRequested()) {
                Ref.common.Frame();
            }
        } catch(ExitException ex) {
        }

        // Clean up
        try {
            // Allow the client to send a disconnect command, if appropiate
            Ref.common.Error(ErrorCode.DROP, "Client quit");
        } catch (Exception e) {}

        Display.destroy();
        System.exit(0);
    }

    public void Frame() {
        if(com_timer.modified) {
            useSysTimer = com_timer.iValue == 1;
            lasttime = Milliseconds(); // Different timers might use different timebases
            com_timer.modified = false;
        }

        // Ensure applets aren't using up the CPU when not active
        boolean capfps = (!Ref.glRef.isScreenFocused() && Ref.glRef.isApplet());
        // Cap framerate
        int _maxfps = maxfps.iValue;
        if(capfps)
            _maxfps = 20;
        int minMsec = (1000/_maxfps);
        int msec = minMsec;
        
        boolean sleepy = (com_sleepy.iValue == 1 && Ref.cvars.Find("r_vsync").iValue != 1) || capfps;
        do {
            // Sleepy frees up the CPU when not running vsync.
            if(sleepy) {
                int remaining = minMsec - msec;
                try {
                    if(remaining >= com_sleepPrecision.iValue)
                        Thread.sleep(1);
                    else if(remaining >= com_yieldPrecision.iValue)
                        Thread.sleep(0);
                } catch (InterruptedException ex) {
                    System.out.println(ex);
                }
            }
            frametime = EventLoop(); // Handle packets while we wait
            if(lasttime > frametime)
                lasttime = frametime;
            msec = frametime - lasttime;
        } while(msec < minMsec);
        Ref.commands.Execute(); // Pump commands
        lasttime = frametime;
        framemsec = msec;
        if(msec > 200)
            msec = 200;

        try {
            // Server Frame
            Ref.server.Frame(msec);

            // Allow server packets to arrive instantly if running server
            EventLoop();
            Ref.commands.Execute(); // Pump commands

            // Client Frame
            Ref.client.Frame(msec);
        } catch(FrameException ex) {
            // FrameException is a special RuntimeException that only has
            // one purpose - to exit the current frame.
            return;
        }
    }

    public void HunkClear() {
        Ref.client.ShutdownCGame();
        Ref.server.ShutdownGameProgs();
    }

    public void Error(ErrorCode code, String str) {
        Ref.cvars.Set2("errorMessage", str, true);
        System.err.println("Error: " + str);
        if(code == ErrorCode.DISCONNECT || code == ErrorCode.SERVERDISCONNECT) {
            Ref.server.Shutdown("Server disconnected.");
            Ref.client.Disconnect(true);
            Ref.client.FlushMemory();
            throw new FrameException(str);
        } else if(code == ErrorCode.DROP) {
            Ref.server.Shutdown("Server shutdown: " + str);
            Ref.client.Disconnect(true);
            Ref.client.FlushMemory();
            throw new FrameException(str);
        } else {
            // Fatal
            System.out.println("Fatal crash");
            JOptionPane.showMessageDialog(null, str);
            if(Ref.glRef != null)
                Ref.glRef.Destroy();
            Shutdown();
        }
        
    }

    public void Shutdown() {
        throw new ExitException("Shutdown");
    }

    public boolean isDeveloper() {
        return developer.iValue == 1;
    }

    

    // This pumps the network system and hands off packets to the client and server
    // to handle. When there is no more packets, it returns the time.
    public int EventLoop() {
        Event ev;
        
        while(true) {
            Ref.net.PumpNet();
            ev = GetEvent();

            if(ev.Type == Event.EventType.NONE)
                return ev.Time;

            switch(ev.Type) {
                case NONE:
                    break;
                case PACKET:
                    // packet value2: 0 for server, 1 for client
                    if(ev.Value2 == 0) {
                        if(sv_running.iValue == 1)
                            Ref.server.PacketEvent((Packet)ev.data);
                    } else {

                        Ref.client.PacketEvent((Packet)ev.data);
                    }
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

        // Return an empty event
        Event evt = tempevt;
        evt.Time = Milliseconds();
        evt.Type = Event.EventType.NONE;
        return evt;
    }
    
    
    Event CreateEvent(int time, Event.EventType type, int value, int value2, Object data) {
        Event evt = tempevt;
        if(time == 0)
            time = Milliseconds();

        evt.Time = time;
        evt.Type = type;
        evt.Value = value;
        evt.Value2 = value2;
        evt.data = data;

        return evt;
    }

    // Get current time in milliseconds.
    public int Milliseconds() {
        if(useSysTimer) {
            return (int)((Sys.getTime()*1000)/Sys.getTimerResolution());
        } else {
            return (int)(System.nanoTime() / 1000000);
        }
    }

    
}
