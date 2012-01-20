
package cubetech.input;

import cubetech.common.Common;
import cubetech.misc.Ref;
import org.lwjgl.input.Controller;



/**
 *
 * @author Mads
 */
public class Joystick {
    private Controller ctrl;
    // Controller layout
    private String name;
    private String[] axisNames;
    private String[] buttonNames;

    // Controller state
    private ControllerState state;
    private int buttonState = 0;
    private float[] axisState;
    
    // axis as buttons
    public boolean[] baxisChanged;
    public int[] baxisValue;

    Joystick(Controller ctrl) {
        this.ctrl = ctrl;
        state = new ControllerState("JOY" + (ctrl.getIndex()+1));
        int buttonCount = ctrl.getButtonCount();
        name = ctrl.getName();
        buttonNames = new String[buttonCount];
        for (int i = 0; i < buttonCount; i++) {
            buttonNames[i] = ctrl.getButtonName(i);
        }

        int nAxis = ctrl.getAxisCount() + 2;
        String axisnames = "";
        axisNames = new String[nAxis];
        axisState = new float[nAxis];
        baxisChanged = new boolean[nAxis];
        baxisValue = new int[nAxis];
        for (int i= 0; i < nAxis-2; i++) {
            axisNames[i] = ctrl.getAxisName(i); 
            axisnames += axisNames[i] + ", ";
        }
        axisNames[nAxis-2] = "POVX";
        axisNames[nAxis-1] = "POVY";
        axisnames += "POVX, POVY";
        Common.Log("[Input] New joystick '%s' registered. %d buttons. %d axis, %s",
                name, buttonCount, nAxis, axisnames);
    }
    
    public int getIndex() {
        return ctrl.getIndex();
    }
    
    public ControllerState getState() {
        return state;
    }

    public void update() {
        ctrl.poll();

        // Update button bitvector
        int oldbuttons = buttonState;
        buttonState = 0;
        for (int i = 0; i < buttonNames.length; i++) {
            boolean isDown = ctrl.isButtonPressed(i);
            if(isDown) buttonState |= (1 << i);

            // Check for statechange
            boolean wasDown = (oldbuttons & (1 << i)) != 0;
            if(wasDown != isDown) {
                int keyIndex = Binds.KEY_JOY1 + ctrl.getIndex() * 32 + i;
                Key k = Ref.Input.GetKey(keyIndex);
                k.Changed = true;
                k.Pressed = isDown;
                k.Time = Ref.client.realtime;
                KeyEvent evt = new KeyEvent(k);
                Ref.Input.FireKeyEvent(evt);
            }
        }

        // Update axis states
        for (int i = 0; i < axisNames.length-2; i++) {
            axisState[i] = ctrl.getAxisValue(i);
        }
        axisState[axisNames.length-2] = ctrl.getPovX();
        axisState[axisNames.length-1] = ctrl.getPovY();

        // Apply deadzone to analog inputs
        float deadZoneScale = 1f/(1f-Ref.Input.j_deadzone.fValue);
        for (int i = 0; i < axisNames.length; i++) {
            if(Math.abs(axisState[i]) < Ref.Input.j_deadzone.fValue) axisState[i] = 0f;
            else axisState[i] = (axisState[i]-Math.signum(axisState[i])*Ref.Input.j_deadzone.fValue)*deadZoneScale;
        }
        
        // calculate buttonstates for axis. Used in menus, etc.
        for (int i = 0; i < axisNames.length; i++) {
            int bstate = (int)Math.round(axisState[i]);
            if(baxisValue[i] != bstate) {
                float oldValue = baxisValue[i];
                int oldval = (int)Math.round(oldValue);
                baxisValue[i] = bstate;
                baxisChanged[i] = true;
                
                boolean pressed = Math.round(Math.abs(baxisValue[i])) == 1;
                // need to check if we need to send a button-up event, since each
                // analog axis is two buttons (on xbox controller)
                if(pressed && oldval != 0) {
                    // to JOY<num>_a<num><p/n>
                    int keyIndex = Binds.KEY_JOY1 + ctrl.getIndex() * 32 + 16 + i*2 + (oldval < 0 ? 1 : 0);
                    Key k = Ref.Input.GetKey(keyIndex);
                    k.Changed = true;
                    k.Pressed = false;
                    k.Time = Ref.client.realtime;
                    KeyEvent evt = new KeyEvent(k);
                    Ref.Input.FireKeyEvent(evt);
                }
                
                // to JOY<num>_a<num><p/n>
                int add = pressed ? (baxisValue[i] < 0 ? 1 : 0) : (oldval < 0 ? 1 : 0);
                int keyIndex = Binds.KEY_JOY1 + ctrl.getIndex() * 32 + 16 + i*2 + add;
                Key k = Ref.Input.GetKey(keyIndex);
                k.Changed = true;
                k.Pressed = pressed;
                k.Time = Ref.client.realtime;
                KeyEvent evt = new KeyEvent(k);
                Ref.Input.FireKeyEvent(evt);
            } else {
                baxisChanged[i] = false;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(64);
        b.append("[Controller ");
        b.append(name);
        b.append("]: buttons:[ ");
        for (int i = 0; i < buttonNames.length; i++) {
            String bname = buttonNames[i];
            boolean on = (buttonState & (1 << i)) != 0;
            b.append(bname);
            b.append(on?":ON, " : ":OFF, ");
        }

        b.append(" ] axis:[ ");
        for (int i = 0; i < axisState.length; i++) {
            String bname = axisNames[i];
            float value = axisState[i];
            b.append(bname);
            b.append(":");
            b.append(value);
            b.append(", ");
        }
        b.append(" ]");
        return b.toString();
    }

    public float getAxis(int axisIndex) {
        if(axisIndex < 0 || axisIndex >= axisState.length) return 0f;
        return axisState[axisIndex];
    }
}
