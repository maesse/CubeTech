package cubetech.input;

import cubetech.common.Commands.ExecType;
import cubetech.common.*;
import cubetech.gfx.GLRef;
import cubetech.misc.Ref;
import cubetech.net.ConnectState;
import cubetech.ui.UI.MENU;
import java.util.EnumSet;
import java.util.HashMap;
import javax.swing.event.EventListenerList;
import net.java.games.input.DirectInputEnvironmentPlugin;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Controllers;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector2f;

/**
 * Handles input from frame to frame
 * @author mads
 */
public class Input {
    // keycatchers ordered by importance
    public final static int KEYCATCH_NONE = 0;
    public final static int KEYCATCH_CGAME = 1;
    public final static int KEYCATCH_UI = 2;
    public final static int KEYCATCH_MESSAGE = 4;
    public final static int KEYCATCH_OVERLAY = 8;
    public final static int KEYCATCH_CONSOLE = 16;
    
    
    public final static int ANGLE_PITCH = 0; // up/down
    public final static int ANGLE_YAW = 1; // left/right
    public final static int ANGLE_ROLL = 2; // fall over
    
    protected HashMap<Integer, EventListenerList> listenerList = new HashMap<Integer, EventListenerList>();
    protected HashMap<Integer, EventListenerList> listenerListMouse = new HashMap<Integer, EventListenerList>();
    
    private boolean initialized = false;
    private int KeyCatcher = 0;
    public int frame_msec;
    
    public ClientInput[] clientInputs;
    public Binds binds;
    
    private int keyboardClient = 0; // client who's getting keyboard/mouse input
    
    // Custom mouse wheel code
    private int mWheelUpTime = 0;
    private int mWheelDownTime = 0;
    private Vector2f[] mouseDelta = new Vector2f[] {new Vector2f(), new Vector2f()};
    private int mouseIndex = 0;
    private Vector2f mousePosition = new Vector2f();
    private int totalWheelDelta = 0;
    private boolean[] mouseButtonStates;
    
    private Key[] keys;
    private int backSpaceTime = 0;
    private int backSpaceGracePeriod = 200;
    private int backSpaceRepeat = 30;
    
    private ControllerState kbState;
    private Joystick[] joysticks = null;
    // Joystick index -> client mapping
    private int[] joystickMapping = new int[] {-1,-1,-1,-1};
    private int lastControllerCheck = 0; // last new-controller check
    private int controllerCheckTimeout = 1000; // how often to check for new controllers

    public CVar sensitivity;
    private CVar in_mouselook;
    private CVar in_smooth;
    private CVar in_debug;
    private CVar in_nojoy;
    public CVar j_pitch;
    public CVar j_yaw;
    public CVar j_forward;
    public CVar j_side;
    public CVar j_pitch_axis;
    public CVar j_yaw_axis;
    public CVar j_forward_axis;
    public CVar j_side_axis;
    public CVar j_deadzone;
    
    private InputOverlay overlay;

    public Input() {
        binds = new Binds(this);
        in_debug = Ref.cvars.Get("in_debug", "0", EnumSet.of(CVarFlags.NONE));
        in_nojoy = Ref.cvars.Get("in_nojoy", "0", EnumSet.of(CVarFlags.NONE));
        sensitivity = Ref.cvars.Get("sensitivity", "3", EnumSet.of(CVarFlags.ARCHIVE));
        in_mouselook = Ref.cvars.Get("in_mouselook", "1", EnumSet.of(CVarFlags.ARCHIVE));
        in_smooth = Ref.cvars.Get("in_smooth", "1", EnumSet.of(CVarFlags.ARCHIVE));
        
        binds.BindKey("W", "+KBforward");
        binds.BindKey("S", "+KBback");
        binds.BindKey("A", "+KBleft");
        binds.BindKey("D", "+KBright");
        binds.BindKey("SPACE", "+KBup");
        binds.BindKey("C", "+KBdown");
        binds.BindKey("UP", "+KBforward");
        binds.BindKey("DOWN", "+KBback");
        binds.BindKey("LEFT", "+KBleft");
        binds.BindKey("RIGHT", "+KBright");
        binds.BindKey("F10", "console");
        binds.BindKey("BACKSLASH", "console");
        binds.BindKey("TAB", "+KBbutton3");
        binds.BindKey("RETURN", "message");
        binds.BindKey("y", "message");
        binds.BindKey("1", "weapon 0 1");
        binds.BindKey("2", "weapon 0 2");
        binds.BindKey("3", "weapon 0 3");
        binds.BindKey("4", "weapon 0 4");
        binds.BindKey("5", "weapon 0 5");
        binds.BindKey("mouse1", "+KBbutton0");
        binds.BindKey("mouse2", "+KBbutton1");
        binds.BindKey("LCONTROL", "+KBbutton2");
        binds.BindKey("E", "use");
        binds.BindKey("F11", "screenshot");
        binds.BindKey("F12", "toggleoverlay");
        binds.BindKey("g", "dropweapon");
        
        binds.BindKey("JOY1_7", "toggleoverlay");
        binds.BindKey("JOY2_7", "toggleoverlay");
        binds.BindKey("JOY3_7", "toggleoverlay");
        
        binds.BindKey("JOY1_a5p", "weapon 1 next");
        binds.BindKey("JOY1_a5n", "weapon 1 prev");
        binds.BindKey("JOY1_a4n", "+JOY1button0");
        binds.BindKey("JOY1_a4p", "+JOY1button1");
        
        binds.BindKey("JOY2_a5p", "weapon 2 next");
        binds.BindKey("JOY2_a5n", "weapon 2 prev");
        binds.BindKey("JOY2_a4n", "+JOY2button0");
        binds.BindKey("JOY2_a4p", "+JOY2button1");
        
        binds.BindKey("JOY3_a5p", "weapon 3 next");
        binds.BindKey("JOY3_a5n", "weapon 3 prev");
        binds.BindKey("JOY3_a4n", "+JOY3button0");
        binds.BindKey("JOY3_a4p", "+JOY3button1");
        
        for (int i = 0; i < 4; i++) {
            binds.BindKey("JOY" + (i+1) + "_7", "toggleoverlay");
            binds.BindKey("JOY" + (i+1) + "_9", "+JOY" + (i+1) + "up");
            binds.BindKey("JOY" + (i+1) + "_0", "+JOY" + (i+1) + "up");
            
            String aliasname = "joy" + (i+1) + "togglecrouch";
            String aliascaller = String.format("alias %s %son", aliasname, aliasname);
            String aliason = String.format("alias %son \"+joy%dbutton2; alias %s %soff\"", aliasname, (i+1), aliasname, aliasname);
            String aliasoff = String.format("alias %soff \"-joy%dbutton2; alias %s %son\"", aliasname, (i+1), aliasname, aliasname);
            Ref.commands.ExecuteText(ExecType.NOW, aliason);
            Ref.commands.ExecuteText(ExecType.NOW, aliasoff);
            Ref.commands.ExecuteText(ExecType.NOW, aliascaller);
            binds.BindKey("JOY" + (i+1) + "_8", aliasname);
        }

        j_pitch = Ref.cvars.Get("j_pitch", "0.1", null);
    	j_yaw = Ref.cvars.Get("j_yaw", "-0.2", null);
    	j_forward = Ref.cvars.Get("j_forward", "-1", null);
    	j_side = Ref.cvars.Get("j_side", "1", null);
    	j_pitch_axis = Ref.cvars.Get("j_pitch_axis", "2", null);
    	j_yaw_axis = Ref.cvars.Get("j_yaw_axis", "3", null);
    	j_forward_axis = Ref.cvars.Get("j_forward_axis", "0", null);
    	j_side_axis = Ref.cvars.Get("j_side_axis", "1", null);
        j_deadzone = Ref.cvars.Get("j_deadzone", "0.2", null);
        
        clientInputs = new ClientInput[4];
        for (int i = 0; i < 4; i++) {
            clientInputs[i] = new ClientInput(i);
        }
        
         overlay = new InputOverlay(this);
    }

    public void addKeyCatcher(int catcher) {
        SetKeyCatcher(GetKeyCatcher() | catcher);
    }
    
    public void removeKeyCatcher(int catcher) {
        SetKeyCatcher(GetKeyCatcher() & ~catcher);
    }
    
    public void SetKeyCatcher(int catcher) {
        if(catcher == KeyCatcher) return;

        // Was in game
        if(KeyCatcher == KEYCATCH_NONE && in_mouselook.isTrue()) {
            Mouse.setGrabbed(false);
        }

        ClearKeys();
        
        // Pause/unpause when entering/exiting menu
        if((KeyCatcher & KEYCATCH_UI) > 0 && (catcher & KEYCATCH_UI) == 0)
            Ref.cvars.Set2("cl_paused", "0", true);
        else if((KeyCatcher & KEYCATCH_UI) == 0 && (catcher & KEYCATCH_UI) > 0)
            Ref.cvars.Set2("cl_paused", "1", true);
        
        KeyCatcher = catcher;
        
        // was in menu and is now in game
        if(KeyCatcher == KEYCATCH_NONE && in_mouselook.isTrue()) {
            Mouse.setGrabbed(true);
        }
    }

    public int GetKeyCatcher() {
        return KeyCatcher;
    }

    public Key GetKey(int index) {
        return keys[index];
    }

    void FireKeyEvent(KeyEvent evt) {
        Key k = evt.getSource();
        if(in_debug.isTrue()) Common.LogDebug("Key %s: %s", k.Pressed?"Pressed":"Released", binds.KeyToString(k.key));
        
        Object[] listeners = null;
        if((KeyCatcher & KEYCATCH_CONSOLE) > 0 && listenerList.containsKey(KEYCATCH_CONSOLE)) {
            listeners = listenerList.get(KEYCATCH_CONSOLE).getListenerList();
        } else if((KeyCatcher & KEYCATCH_OVERLAY) > 0 && listenerList.containsKey(KEYCATCH_OVERLAY)) {
            listeners = listenerList.get(KEYCATCH_OVERLAY).getListenerList();
        } else if((KeyCatcher & KEYCATCH_MESSAGE) > 0 && listenerList.containsKey(KEYCATCH_MESSAGE)) {
            listeners = listenerList.get(KEYCATCH_MESSAGE).getListenerList();
        } else if((KeyCatcher & KEYCATCH_UI) > 0 && listenerList.containsKey(KEYCATCH_UI)) {
            listeners = listenerList.get(KEYCATCH_UI).getListenerList();
        } else if((KeyCatcher & KEYCATCH_CGAME) > 0 && listenerList.containsKey(KEYCATCH_CGAME)) {
            listeners = listenerList.get(KEYCATCH_CGAME).getListenerList();
        }
        
        boolean continueToBinds = true;
        if(listeners != null) {
            for (int i= 0; i < listeners.length; i++) {
                if(listeners[i] == KeyEventListener.class) {
                    continueToBinds = continueToBinds && ((KeyEventListener)listeners[i+1]).KeyPressed(evt);
                }
            }
        }
        
        // standard handler + some listeners don't want to block binds
        if(continueToBinds) {
            Key key = (Key)evt.getSource();
            binds.ParseBinding(key.key, key.Pressed, key.Time);
        }
    }

    void FireMouseEvent(MouseEvent evt) {
        Object[] listeners = null;
        if((KeyCatcher & KEYCATCH_CONSOLE) > 0) {
            if(listenerListMouse.containsKey(KEYCATCH_CONSOLE))
                listeners = listenerListMouse.get(KEYCATCH_CONSOLE).getListenerList();
        } else if((KeyCatcher & KEYCATCH_OVERLAY) > 0 && listenerListMouse.containsKey(KEYCATCH_OVERLAY)) {
            listeners = listenerListMouse.get(KEYCATCH_OVERLAY).getListenerList();
        } else if((KeyCatcher & KEYCATCH_MESSAGE) > 0 && listenerListMouse.containsKey(KEYCATCH_MESSAGE)) {
            listeners = listenerListMouse.get(KEYCATCH_MESSAGE).getListenerList();
        } else if((KeyCatcher & KEYCATCH_UI) > 0 && listenerListMouse.containsKey(KEYCATCH_UI)) {
            listeners = listenerListMouse.get(KEYCATCH_UI).getListenerList();
        } else if((KeyCatcher & KEYCATCH_CGAME) > 0 && listenerListMouse.containsKey(KEYCATCH_CGAME)) {
            listeners = listenerListMouse.get(KEYCATCH_CGAME).getListenerList();
        } 

        if(listeners == null)
            return;

        for (int i= 0; i < listeners.length; i++) {
            if(listeners[i] == MouseEventListener.class) {
                ((MouseEventListener)listeners[i+1]).GotMouseEvent(evt);
            }
        }
    }

    public void Init() throws LWJGLException {
        if(initialized) return;
        
        // Keyboard init
        Keyboard.create();
        Keyboard.enableRepeatEvents(false);
        if(!Keyboard.isCreated()) throw new LWJGLException("Keyboard not created.");
        keys = new Key[Binds.KEY_EXTENSION_END]; // Mouse keys extends the set
        for (int i= 0; i < keys.length; i++) {
            keys[i] = new Key(i);
        }

        // Mouse init
        Mouse.create();
        mouseButtonStates = new boolean[Mouse.getButtonCount()];

        // Controllers init
        Controllers.create();
        
        kbState = new ControllerState("KB");
        
        Ref.commands.AddCommand("toggleoverlay", cmd_toggleoverlay);

        initialized = true;
    }
    
    private void updateControllerCount() {
        if(true) return; // broken. need to modify lwjgl/jinput code to fix this
        if(lastControllerCheck + controllerCheckTimeout > Ref.client.realtime) return;
        lastControllerCheck = Ref.client.realtime;
        
        // Hax: LWJGL doesn't detect new controllers at runtime, so keep an eye on controller count
        // and re-init the controllers when the count changes
        
        // Fix: This shit creates a dummy windows that never gets released, awesome right?
        DirectInputEnvironmentPlugin lolwut = new DirectInputEnvironmentPlugin ();
        
        int currentCount = Controllers.isCreated() ? Controllers.getControllerCount() : 0;
        int count = 0;
        for (net.java.games.input.Controller c : lolwut.getControllers()) {
            if ( (!c.getType().equals(net.java.games.input.Controller.Type.KEYBOARD)) &&
                 (!c.getType().equals(net.java.games.input.Controller.Type.MOUSE)) ) {
                    count++;
            }
        }
        if(currentCount != count) {
            Controllers.destroy();
            try {
                Controllers.create();
            } catch (LWJGLException ex) {
                Common.Log("Controller error: " + Common.getExceptionString(ex));
                Ref.cvars.Set2("in_nojoy", "1", true);
            }
        }
    }

    private void updateControllers() {
        if(in_nojoy.isTrue()) return;
        updateControllerCount();
        if(in_nojoy.isTrue()) return;
        
        int nControllers = Controllers.getControllerCount();
        if(nControllers == 0) return;

        // First run, initialize
        if(joysticks == null) {
            joysticks = new Joystick[nControllers];
            for (int i = 0; i < nControllers; i++) {
                joysticks[i] = new Joystick(Controllers.getController(i));
            }
        }
        
        for (int i = 0; i < joysticks.length; i++) {
            joysticks[i].update();
        }
    }
    
    public Joystick getJoystick(int index) {
        if(joysticks == null) return null;
        else if(index < 0 || index >= joysticks.length) return null;
        return joysticks[index];
    }
    
    public ClientInput getClient(int index) {
        if(index < 0 || index >= 4) Ref.common.Error(Common.ErrorCode.DROP, "Invalid clientindex " + index);
        return clientInputs[index];
    }

    public void Update() {
        GLRef.checkError();
        
        Display.processMessages();
        MouseUpdate();
        KeyboardUpdate();
        updateControllers();
        for (int i = 0; i < clientInputs.length; i++) {
            ClientInput input = clientInputs[i];
            if(i == keyboardClient) {
                // let this input client know of the mouse changes.
                float dx, dy;
                if(in_smooth.isTrue()) {
                    dx = (mouseDelta[0].x + mouseDelta[1].x) * 0.5f;
                    dy = (mouseDelta[0].y + mouseDelta[1].y) * 0.5f;
                } else {
                    dx = mouseDelta[mouseIndex].x;
                    dy = mouseDelta[mouseIndex].y;
                }
                input.updateFromKeyboardMouse(kbState, mouseButtonStates, mousePosition, dx, dy, totalWheelDelta);
            } else {
                // Ensure that there isn't any mousedown even stuck if keyboardclient is switched around
                input.clearKeyboardMouseState();
            }
        }

        // Repeat backspace events
        if(Keyboard.isKeyDown(Keyboard.KEY_BACK)) {
            if(backSpaceTime == 0) backSpaceTime = Ref.client.realtime + backSpaceGracePeriod;
            // Send keydown-event
            else if(Ref.client.realtime > backSpaceTime) {
                backSpaceTime = Ref.client.realtime + backSpaceRepeat;
                FireKeyEvent(new KeyEvent(GetKey(Keyboard.KEY_BACK)));
            }
        } else backSpaceTime = 0;

        // Send mwheel keyUp events if it's time
        if(mWheelDownTime != 0 && Ref.client.realtime - mWheelDownTime > 50) {
            binds.ParseBinding(binds.StringToKey("MWHEELDOWN"), false, (int) (Ref.client.realtime));
            mWheelDownTime = 0;
        }
        if(mWheelUpTime != 0 && Ref.client.realtime - mWheelUpTime > 50) {
            binds.ParseBinding(binds.StringToKey("MWHEELUP"), false, (int) (Ref.client.realtime));
            mWheelUpTime = 0;
        }
        
        
    }

    public void ClearKeys() {
        if(keys == null)
            return;
        for (int i= 0; i < keys.length; i++) {
            if(keys[i].Pressed)
                keys[i].Changed = true;
            keys[i].Pressed = false;
        }
    }

    public boolean IsKeyPressed(int key) {
        if(key <0 || key >= keys.length)
            return false;
        return keys[key].Pressed;
    }

    private void MouseUpdate() {
        mouseIndex ^= 1;
        mouseDelta[mouseIndex].set(0,0);
        totalWheelDelta = 0;
        while(Mouse.next() && !Ref.common.com_unfocused.isTrue()) {
            // Add up delta
            int evtDx = Mouse.getEventDX();
            int evtDy = Mouse.getEventDY();
            mouseDelta[mouseIndex].x += evtDx;
            mouseDelta[mouseIndex].y += evtDy;

            // Set Position
            float mousex = (float)Mouse.getEventX() / (float)Ref.glRef.GetResolution().x;
            float mousey = (float)Mouse.getEventY() / (float)Ref.glRef.GetResolution().y;
            
            if(!Float.isInfinite(mousey) && !Float.isInfinite(mousex)) {
                mousePosition.x = mousex;
                mousePosition.y = mousey;
                
            }

            Helper.Clamp(mousePosition, 0f, 1f);

            int wheelDelta = Mouse.getEventDWheel();
            totalWheelDelta += wheelDelta;
            wheelDelta = Helper.Clamp(wheelDelta, -1, 1);

            int button = Mouse.getEventButton();
            boolean pressed = false;
            
            if(button != -1) {
                pressed = Mouse.getEventButtonState();
                mouseButtonStates[button] = pressed;
            }
            
            FireMouseEvent(new MouseEvent(button, pressed, wheelDelta, evtDx, evtDy, new Vector2f(mousePosition)));
            
            // Also fire a key event for button presses
            if(button != -1) {
                // Fire regular mouse button event
                String buttonStr = "MOUSE" + (button + 1);
                int keyIndex = binds.StringToKey(buttonStr);
                Key key = keys[keyIndex];
                key.Changed = true;
                key.Pressed = pressed;
                key.Time = Ref.client.realtime;
                key.Name = buttonStr;
                FireKeyEvent(new KeyEvent(key));
            }

            // event can also contain mousewheel info
            // Wheeldelta don't have key-up events, so create it artificially
            if(wheelDelta < 0) {
                int keyIndex = binds.StringToKey("MWHEELDOWN");
                Key key = keys[keyIndex];
                key.Changed = true;
                key.Pressed = true;
                key.Time = Ref.client.realtime;
                key.Name = "MWHEELDOWN";
                FireKeyEvent(new KeyEvent(key));
                mWheelDownTime = Ref.client.realtime;
            } else if(wheelDelta > 0) {
                int keyIndex = binds.StringToKey("MWHEELUP");
                Key key = keys[keyIndex];
                key.Changed = true;
                key.Pressed = true;
                key.Time = Ref.client.realtime;
                key.Name = "MWHEELUP";
                FireKeyEvent(new KeyEvent(key));
                mWheelUpTime = Ref.client.realtime;
            }
        }
        
        
    }

    void KeyboardUpdate() {
        for (int i= 0; i < keys.length; i++) {
            if(keys[i] != null)
                keys[i].Changed = false;
        }
        
        int nProcessed = 0;
//        int timeDelta = Ref.common.Milliseconds() - Ref.common.frametime;
        
        while(Keyboard.next()) {
            boolean pressed = Keyboard.getEventKeyState();
            int key = Keyboard.getEventKey();
            char c = Keyboard.getEventCharacter();
            int eventTime = (int)(Keyboard.getEventNanoseconds() / (1000*1000));
            eventTime += frame_msec;
            Key currKey = keys[key];
            
            if(currKey.Pressed != pressed && currKey.Time <= eventTime) {
                // Key changes state
                currKey.Pressed = pressed;
                currKey.Changed = true;
                currKey.Time = eventTime;
                currKey.Char = c;
                
                

                if(pressed) {
                    // Special case handling for escape
                    // Toggle console
                    String keybind = binds.getBindForKey(key);
                    if((key == Keyboard.KEY_ESCAPE && IsKeyPressed(Keyboard.KEY_LSHIFT))
                            || (keybind != null && keybind.equalsIgnoreCase("console"))) {
                        Ref.commands.ExecuteText(ExecType.NOW, "console");
                        continue;
                    }
                    else if(key == Keyboard.KEY_ESCAPE) {
                        if((GetKeyCatcher() & KEYCATCH_CONSOLE) > 0) {
                            // Close console
                            Ref.Console.Close();
                            continue;
                        }

                        if((GetKeyCatcher() & KEYCATCH_MESSAGE) > 0) {
                            // TODO: Clear message
                            continue;
                        }

//                        if((GetKeyCatcher() & KEYCATCH_CGAME) > 0) {
////                            SetKeyCatcher(GetKeyCatcher() & ~KEYCATCH_CGAME);
//                            // TODO: Send null event to cgame?
//                            continue;
//                        }

                        if((GetKeyCatcher() & KEYCATCH_UI) == 0) {
                            if(Ref.client.clc.state == ConnectState.ACTIVE)
                                Ref.ui.SetActiveMenu(MENU.MAINMENU);
                            else if(Ref.client.clc.state != ConnectState.DISCONNECTED) {
                                // Escape can abort an connection attempt
                                Ref.commands.ExecuteText(ExecType.NOW, "disconnect\n");
                                Ref.ui.SetActiveMenu(MENU.MAINMENU);
                            }
                            continue;
                        } else {
                            SetKeyCatcher(GetKeyCatcher() & ~KEYCATCH_UI);
                        }

                        // TODO: Send keyevent to ui
                    }
                    // Alt+Enter toggles fullscreen
                    else if(key == Keyboard.KEY_RETURN && IsKeyPressed(Keyboard.KEY_LMENU)) {
                        int fs = Ref.cvars.Find("r_fullscreen").iValue + 1;
                        Ref.cvars.Set2("r_fullscreen", ""+ (fs&1), true);
                        continue;
                    }
                    

                    //System.out.println("Pressed: " + c);
                    
                }
                FireKeyEvent(new KeyEvent(currKey));
            }

            nProcessed++;
        }
        
    }

    

    public void AddKeyEventListener(KeyEventListener listener, int targetCatcher)
    {
        if(!listenerList.containsKey(targetCatcher))
            listenerList.put(targetCatcher, new EventListenerList());

        listenerList.get(targetCatcher).add(KeyEventListener.class, listener);
    }

    public void RemoveKeyEventListener(KeyEventListener listener, int target) {
        if(!listenerList.containsKey(target))
            return;
        listenerList.get(target).remove(KeyEventListener.class, listener);
    }

    public void AddMouseEventListener(MouseEventListener listener, int targetCatcher)
    {
        if(!listenerListMouse.containsKey(targetCatcher))
            listenerListMouse.put(targetCatcher, new EventListenerList());

        listenerListMouse.get(targetCatcher).add(MouseEventListener.class, listener);

    }

    public void RemoveMouseEventListener(MouseEventListener listener, int target) {
        if(!listenerListMouse.containsKey(target))
            return;
        listenerListMouse.get(target).remove(MouseEventListener.class, listener);

    }

    public PlayerInput getKeyboardInput() {
        return clientInputs[keyboardClient].getInput();
    }
    
    public void setKeyboardClient(int index) {
        if(index < 0 || index >= 4) Ref.common.Error(Common.ErrorCode.DROP, "Invalid client index " + index);
        keyboardClient = index;
    }
    
    public void setJoystickClient(Joystick stick, int clientIndex) {
        if(clientIndex < 0 || clientIndex >= 4) Ref.common.Error(Common.ErrorCode.DROP, "Invalid client index " + clientIndex);
        joystickMapping[stick.getIndex()] = clientIndex;
    }
    
    public int getJoystickMapping(int joystickIndex) {
        return joystickMapping[joystickIndex];
    }
    
    
    private ICommand cmd_toggleoverlay = new ICommand() {
        public void RunCommand(String[] args) {
            overlay.toggleVisible();
        }
    };

    public InputOverlay getOverlay() {
        return overlay;
    }

    public Joystick getJoystickForClient(int clientIndex) {
        if(joysticks == null) return null;
        for (int i = 0; i < joystickMapping.length; i++) {
            if(joystickMapping[i] == clientIndex && joysticks.length > i) return joysticks[i];
        }
        return null;
    }

    public int getKeyboardClient() {
        return keyboardClient;
    }
}
