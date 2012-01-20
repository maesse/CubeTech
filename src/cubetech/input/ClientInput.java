package cubetech.input;

import cubetech.common.Helper;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author Mads
 */
public class ClientInput {
    public static final int BUTTON_MOUSE1 = 0;
    public static final int BUTTON_MOUSE2 = 1;
    public static final int BUTTON_CROUCH = 2;
    public static final int BUTTON_SCOREBOARD = 3;
    public static final int BUTTON_USE = 4;
    private static final boolean noButtons[] = new boolean[3];
    
    private int clientIndex;
    private PlayerInput playerInput = new PlayerInput();
    private ControllerState kbState = null; // non-null if keyboard is controlling this client
    
    // Client side button state

    private float[] viewangles = new float[3]; // viewangle this frame
    private float[] oldangles = new float[3]; // viewangles from last frame
    
    public ClientInput(int clientIndex) {
        this.clientIndex = clientIndex;
        viewangles[0] = 90;
        
    }
    
    private void MouseMove(PlayerInput cmd) {
        if(cmd.MouseDelta[0] == 0 && cmd.MouseDelta[1] == 0) {
            for (int i= 0; i < 3; i++) {
                cmd.angles[i] = Helper.Angle2Short(viewangles[i]);
            }
            return; // No change
        }

        // Cast to float
        float mx = cmd.MouseDelta[0];
        float my = cmd.MouseDelta[1];

        // Multiply by sensitivity
        mx *= Ref.Input.sensitivity.fValue;
        my *= Ref.Input.sensitivity.fValue;

        viewangles[Input.ANGLE_YAW] -= 0.022f * mx;
        viewangles[Input.ANGLE_PITCH] -= 0.022f * my;
    }
    
    private void Joystickmove(Joystick stick, PlayerInput cmd) {
        if(stick == null) return;
        float anglespeed = Ref.client.frametime;
        viewangles[1] += anglespeed * Ref.Input.j_yaw.fValue * stick.getAxis(Ref.Input.j_yaw_axis.iValue);
        viewangles[0] += anglespeed * Ref.Input.j_pitch.fValue * stick.getAxis(Ref.Input.j_pitch_axis.iValue);
        
        cmd.side = Helper.addAndClamp(cmd.side, Helper.toClampedByte((Ref.Input.j_side.fValue * stick.getAxis(Ref.Input.j_side_axis.iValue))));
        cmd.forward = Helper.addAndClamp(cmd.forward, Helper.toClampedByte((Ref.Input.j_forward.fValue * stick.getAxis(Ref.Input.j_forward_axis.iValue))));
        
        // Pack button states
        ControllerState state = stick.getState();
        for (int i= 0; i < state.in_buttons.length; i++) {
            int bstate = (int) state.in_buttons[i].KeyState();
            playerInput.buttons[i] = playerInput.buttons[i] || bstate > 0;
        }
        playerInput.Up = playerInput.Up || state.in_up.KeyState() > 0.0f;
    }
    
    public PlayerInput getInput() {
        return playerInput;
    }
    
    public PlayerInput CreateCmd() {
        System.arraycopy(viewangles, 0, oldangles, 0, viewangles.length);
        for (int i = 0; i < playerInput.buttons.length; i++) {
            playerInput.buttons[i] = false;
        }
        
        KeyboardMove();
        MouseMove(playerInput);
        // Joystick buttons and movement is additive to keyboard and mouse movement
        Joystickmove(Ref.Input.getJoystickForClient(clientIndex), playerInput);
        
        PlayerInput cmd = playerInput.Clone();
        
        // limit pitch
        if(viewangles[Input.ANGLE_PITCH] > 179f) viewangles[Input.ANGLE_PITCH] = 179f;
        else if(viewangles[Input.ANGLE_PITCH] < 0.1f) viewangles[Input.ANGLE_PITCH] = 0.1f;            

        // Ensure angles have not been wrapped
        cmd.angles[0] = Helper.Angle2Short(viewangles[0]);
        cmd.angles[1] = Helper.Angle2Short(viewangles[1]);
        cmd.angles[2] = Helper.Angle2Short(viewangles[2]);
        
        // Store current weapon
        cmd.weapon = Ref.client.cl.userCmd_weapon;
        
        return cmd;
    }
    
    private void updateMouse(boolean[] buttonStates, Vector2f mousePos, int dx, int dy, int wheelDelta) {
        playerInput.Mouse1Diff = false;
        playerInput.Mouse2Diff = false;
        playerInput.Mouse3Diff = false;
        
        if(buttonStates[0] != playerInput.Mouse1) {
            playerInput.Mouse1Diff = true;
            playerInput.Mouse1 = buttonStates[0];
        }
        
        if(buttonStates[1] != playerInput.Mouse2) {
            playerInput.Mouse2Diff = true;
            playerInput.Mouse2 = buttonStates[1];
        }
        
        if(buttonStates[2] != playerInput.Mouse3) {
            playerInput.Mouse3Diff = true;
            playerInput.Mouse3 = buttonStates[2];
        }

        // Set delta
        if(mousePos != null) playerInput.MousePos.set(mousePos);
        playerInput.MouseDelta[0] = dx;
        playerInput.MouseDelta[1] = dy;
        playerInput.WheelDelta = wheelDelta;
    }
    
    public void clearKeyboardMouseState() {
        updateFromKeyboardMouse(null, noButtons, null, 0, 0, 0);
    }
    
    public void updateFromKeyboardMouse(ControllerState kbState, boolean[] buttonStates, Vector2f mousePos, int dx, int dy, int wheelDelta) {
        updateMouse(buttonStates, mousePos, dx, dy, wheelDelta);
        this.kbState = kbState;
    }
    
    private void KeyboardMove() {
        if(kbState == null) return;
        
        float side = kbState.in_right.KeyState() - kbState.in_left.KeyState();
        float forward = kbState.in_forward.KeyState() - kbState.in_back.KeyState();
        
        playerInput.side = Helper.toClampedByte(side);
        playerInput.forward = Helper.toClampedByte(forward);
        
        playerInput.Up = ((int)Math.ceil(kbState.in_up.KeyState()) == 1)?true:false;
        playerInput.Down = ((int)Math.ceil(kbState.in_down.KeyState()) == 1)?true:false;
        
        // Pack button states
        for (int i= 0; i < kbState.in_buttons.length; i++) {
            int state = (int) kbState.in_buttons[i].KeyState();
            playerInput.buttons[i] = state > 0;
        }
    }
}
