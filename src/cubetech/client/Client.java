package cubetech.client;

import cubetech.common.items.Weapon;
import cubetech.CGame.CGame;
import cubetech.collision.ClientCubeMap;
import cubetech.common.*;
import cubetech.common.Common.ErrorCode;
import cubetech.gfx.GLRef;
import cubetech.input.Input;
import cubetech.input.PlayerInput;
import cubetech.misc.FinishedUpdatingListener;
import cubetech.misc.Ref;
import cubetech.net.*;
import cubetech.net.NetChan.*;
import cubetech.ui.DebugUI;
import cubetech.ui.UI;
import java.net.InetSocketAddress;
import java.util.AbstractMap;
import java.util.EnumSet;
/**
 *
 * @author mads
 */
public class Client {
    // Current connection
    public ClientConnect clc = new ClientConnect();
    public ClientActive cl = new ClientActive();
    public ClientRender clRender = new ClientRender();
    
    // CVars
    CVar cl_updaterate;
    private CVar cl_timeout;
    public CVar cl_cmdrate;
    CVar cl_timenudge;
    CVar cl_debugui;
    CVar cl_showfps;
    public CVar cl_demorecord;
    public CVar cl_netquality; // 0, less lag more jitter - ~50-100, more lag, less time jitter
    public CVar cl_nodelta;
    public CVar cl_cmdbackup;

    // Misc
    DebugUI debugUI = null;
    public FinishedUpdatingListener updateListener = null;
    public DemoRecorder demo;
    public Message message = new Message();
    public ServerBrowser serverBrowser = new ServerBrowser();

    // Timing stuff. Used by usercmd assembly, etc.
    public int framecount;
    public int oldframetime;
    public int frametime;
    public int realtime;
    public int nonbasedTime;

    public void Init() {
        Common.Log("--- Client Initialization ---");
        demo = new DemoRecorder();
        
        // Initialize cvars
        Ref.cvars.Get("name", "^3Running^4Man^0", EnumSet.of(CVarFlags.USER_INFO, CVarFlags.ARCHIVE));
        Ref.cvars.Get("rate", "25000", EnumSet.of(CVarFlags.USER_INFO, CVarFlags.ARCHIVE));
        Ref.cvars.Get("model", "cubeguyTextured", EnumSet.of(CVarFlags.USER_INFO, CVarFlags.ARCHIVE));
        cl_updaterate = Ref.cvars.Get("cl_updaterate", "30", EnumSet.of(CVarFlags.USER_INFO, CVarFlags.ARCHIVE));
        cl_updaterate.Min = 15;
        cl_updaterate.Max = 115;
        cl_timeout = Ref.cvars.Get("cl_timeout", "120", EnumSet.of(CVarFlags.ARCHIVE));
        cl_cmdrate = Ref.cvars.Get("cl_cmdrate", "101", EnumSet.of(CVarFlags.ARCHIVE));
        cl_cmdrate.Min = 20;
        cl_cmdrate.Max = 115;
        cl_timenudge = Ref.cvars.Get("cl_timenudge", "0", EnumSet.of(CVarFlags.ARCHIVE));
        cl_timenudge.Min = -30;
        cl_timenudge.Max = 30;
        cl_nodelta = Ref.cvars.Get("cl_nodelta", "0", EnumSet.of(CVarFlags.NONE));
        cl_cmdbackup = Ref.cvars.Get("cl_cmdbackup", "1", EnumSet.of(CVarFlags.ARCHIVE));
        cl_cmdbackup.Min = 0;
        cl_cmdbackup.Max = 5;
        cl_debugui = Ref.cvars.Get("cl_debugui", "0", EnumSet.of(CVarFlags.ARCHIVE));
        cl_debugui.modified = false;
        cl_showfps  = Ref.cvars.Get("cl_showfps", "0", EnumSet.of(CVarFlags.ARCHIVE));
        cl_netquality = Ref.cvars.Get("cl_netquality", "50", EnumSet.of(CVarFlags.ARCHIVE)); // allow 50ms cgame delta
        cl_demorecord = Ref.cvars.Get("cl_demorecord", "0", EnumSet.of(CVarFlags.NONE));

        Ref.cvars.Set2("cl_running", "1", true);
    }


    public void Frame(int msec) { 
        if(!Ref.common.cl_running.isTrue()) return;

        // Force UI to be visible when not connected
        if(clc.state == ConnectState.DISCONNECTED && 
                (Ref.Input.GetKeyCatcher() & Input.KEYCATCH_UI) == 0) {
            Ref.ui.SetActiveMenu(UI.MENU.MAINMENU);
        }
        
        // decide the simulation time
        oldframetime = frametime;
        frametime = msec;
        realtime += msec;  
        nonbasedTime = Ref.common.Milliseconds();
        
        Ref.soundMan.Update((int)frametime);
        Ref.glRef.Update(); // Update graphics system
        Ref.ResMan.Update(); // Do a bit of texture loading

        // see if we need to update any userinfo
        CheckUserInfo();
        
        // if we haven't gotten a packet in a long time,
        // drop the connection
        CheckTimeout();
        
        // send intentions now
        // Get delta msecs since last frame from the common subsystem
        Ref.Input.frame_msec = frametime;

        // if running less than 5fps, truncate the extra time to prevent
        // unexpected moves after a hitch
        if(Ref.Input.frame_msec > 200) Ref.Input.frame_msec = 200;
        Ref.Input.Update();
        clc.SendCommand();
        
        // resend a connection request if necessary
        clc.CheckForResend();
        
        // decide on the serverTime to render
        cl.SetCGameTime();

        // update the screen
        clRender.UpdateScreen();

        // Handle debugUI
        if(cl_debugui.modified) {
            if(debugUI == null && cl_debugui.isTrue()) debugUI = new DebugUI();
            debugUI.setVisible(cl_debugui.isTrue());
            cl_debugui.modified = false;
        }
        if(updateListener != null) updateListener.FinishedUpdating();

        framecount++;
    }

    

    public void MapLoading() {
        if(Ref.common.cl_running.iValue == 0)
            return;

        Ref.Console.Close();
        Ref.Input.SetKeyCatcher(Input.KEYCATCH_NONE);

        // if we are already connected to the local host, stay connected
        if(clc.state.ordinal() >= ConnectState.CONNECTED.ordinal() && clc.servername.equalsIgnoreCase("localhost"))
        {
            clc.state = ConnectState.CONNECTED;
            clc.servermessage = "";
            cl.GameState.clear();
            clc.LastPacketSentTime = -9999;
            clRender.UpdateScreen();
        } else {
            Ref.cvars.Set2("nextmap", "", true);
            Disconnect(true);
            clc.servername = "localhost";
            clc.state = ConnectState.CHALLENGING;
            Ref.Input.SetKeyCatcher(Input.KEYCATCH_NONE);
            GLRef.checkError();
            clRender.UpdateScreen();
            GLRef.checkError();
            clc.ConnectTime = -3000;
            clc.ServerAddr = new InetSocketAddress("localhost", Ref.net.net_svport.iValue);
            clc.CheckForResend();
        }
    }

    

    public void Disconnect(boolean showMainMenu) {
        if(Ref.common.cl_running.iValue == 0) return; 

        // Show UI
        Ref.cvars.Set2("ui_fullscreen", "1", true);
        if(showMainMenu) Ref.ui.SetActiveMenu(UI.MENU.MAINMENU);

        // send a disconnect message to the server
        // send it a few times in case one is dropped
        if(clc.state.ordinal() >= ConnectState.CONNECTED.ordinal()) {
            clc.AddReliableCommand("disconnect", true);
            clc.WritePacket();
            clc.WritePacket();
            clc.WritePacket();
        }

        // Handle demo
        if(demo.isRecording()) {
            demo.cmd_stoprecord.RunCommand(null);
        } else if(demo.isPlaying()) {
            demo.playCompleted(false);
        }

        // wipe the client connection
        Ref.net.disconnectClient();
        cl = new ClientActive();
        clc = new ClientConnect();
    }

    public void ShutdownAll() {
        if(Ref.cgame != null) {
            Ref.cgame.Shutdown();
            if(!demo.isPlaying()) Ref.cgame = null;
        }
        Ref.Input.SetKeyCatcher(Ref.Input.GetKeyCatcher() & ~Input.KEYCATCH_UI);
        //Ref.cgame.CG_Shutdown();
    }

    private void CheckUserInfo() {
        // don't add reliable commands when not yet connected
        if(clc.state.ordinal() < ConnectState.CHALLENGING.ordinal()) return;

        // don't overflow the reliable command buffer when paused
        if(CheckPaused()) return;

        // send a reliable userinfo update if needed
        if(Ref.cvars.modifiedFlags.contains(CVarFlags.USER_INFO)) {
            Ref.cvars.modifiedFlags.remove(CVarFlags.USER_INFO);
            clc.AddReliableCommand(String.format("userinfo \"%s\"", Ref.cvars.InfoString(CVarFlags.USER_INFO)), false);
        }
    }

    private void CheckTimeout() {
        if( (!CheckPaused() || Ref.common.sv_paused.iValue == 0) &&
                clc.state.ordinal() >= ConnectState.CONNECTED.ordinal() && 
                realtime - clc.LastPacketTime > cl_timeout.fValue * 1000f) {
            if(++cl.timeoutCount > 5) {
                Ref.common.Error(ErrorCode.DROP, "Server connection timed out.");
                //Disconnect(true);
                return;
            }
        } else cl.timeoutCount = 0;
    }

    
    public void InitCGame() {
        String mapname = Info.ValueForKey(cl.GameState.get(0), "mapname");
        cl.mapname = mapname;
        clc.state = ConnectState.LOADING;

        // init for this gamestate
        // use the lastExecutedServerCommand instead of the serverCommandSequence
        // otherwise server commands sent just before a gamestate are dropped
        ClientCubeMap map = null;
        if(demo.isPlaying() && Ref.cgame != null && Ref.cgame.map != null) map = Ref.cgame.map;
        Ref.cgame = new CGame();
        if(map != null) Ref.cgame.map = map;
        Ref.cgame.Init(clc.serverMessageSequence, clc.lastExecutedServerCommand, clc.ClientNum);
        // we will send a usercmd this frame, which
        // will cause the server to send us the first snapshot
        clc.state = ConnectState.PRIMED;
    }

    // if cl_paused->modified is set, the cvar has only been changed in
    // this frame. Keep paused in this frame to ensure the server doesn't
    // lag behind.
    public boolean CheckPaused() {
        return (Ref.common.cl_paused.isTrue() || Ref.common.cl_paused.modified);
    }

    public int ScaledMilliseconds() {
        return (int) (Ref.common.Milliseconds() * Ref.common.com_timescale.fValue);
    }

    // CGame tells us what weapon we're using and if we should scale mouse sens (when cgame is zooming for instance)
    public void setUserCommand(Weapon weapon, float sensitivityScale) {
        cl.userCmd_weapon = weapon;
        cl.userCmd_sens = sensitivityScale;
    }

    public void FlushMemory() {
        ShutdownAll();

        // if not running a server clear the whole hunk
        if(Ref.common.sv_running.iValue == 0) {
            Ref.common.HunkClear();
            // clear collision map data
            Ref.cm.ClearCubeMap();
        }
    }

    public void ShutdownCGame() {
        Ref.Input.SetKeyCatcher(Ref.Input.GetKeyCatcher() & ~Input.KEYCATCH_CGAME);
        if(Ref.cgame != null)
        {
            Ref.cgame.Shutdown();
            if(!demo.isPlaying()) Ref.cgame = null;
        }
        
    }

    public PlayerInput GetUserCommand(int cmdNum) {
        if(cmdNum > cl.cmdNumber)
            Ref.common.Error(Common.ErrorCode.DROP, "GetUserCommand(): cmdnum > cl.cmdNumber");

        // the usercmd has been overwritten in the wrapping
        // buffer because it is too far out of date
        if(cmdNum <= cl.cmdNumber - 64)
            return null;

        return cl.cmds[cmdNum & 63];
    }

    public ServerBrowser getBrowser() {
        return serverBrowser;
    }

    // Returns the messagenum and servertime from the current snapshot
    public AbstractMap.SimpleEntry<Integer, Integer> GetCurrentSnapshotNumber() {
        return new AbstractMap.SimpleEntry<Integer, Integer>(cl.snap.messagenum, cl.snap.serverTime);
    }   
}
