package cubetech.input;

import cubetech.common.ICommand;
import cubetech.misc.Ref;

/**
 * Provides an interface for a virtual button, like "jump" or "moveforward".
 * @author mads
 */
public class ButtonState {
    private int[] down = new int[2]; // current keys holding this button down
    private int downtime; // msec timestamp
    private int msec; // msec down this frame if both a down and up happened
    private boolean active; // current state
    private boolean wasPressed; // set when down, not cleared when up

    public ICommand KeyDownHook;
    public ICommand KeyUpHook;

    public ButtonState() {
        KeyDownHook = new Cmd_Down();
        KeyUpHook = new Cmd_Up();
    }

    private class Cmd_Down implements ICommand {
        public void RunCommand(String[] args) {
            KeyDown(args);
        }
    }

    private class Cmd_Up implements ICommand {
        public void RunCommand(String[] args) {
            KeyUp(args);
        }
    }

    public float KeyState() {
        int msecBackup = msec;
        msec = 0;

        if(active) {
            // still down
            if(downtime == 0)
                msecBackup = Ref.common.frametime;
            else
                msecBackup += Ref.common.frametime - downtime;
            downtime = Ref.common.frametime;
        }

        float val = (float)msecBackup / Ref.client.frame_msec;
        if(val < 0)
            val = 0;
        if(val > 1)
            val = 1;
        return val;
    }

    private void KeyUp(String[] tokens) {
        int key = -1;
        if(tokens.length > 1) {
            String sKey = tokens[1];
            try {
                key = Integer.parseInt(sKey);
            } catch(NumberFormatException e) {
            }
        } else {
            // types from console, asume unstick all
            down[0] = down[1] = 0;
            active = false;
            return;
        }

        if(down[0] == key)
            down[0] = 0;
        else if(down[1] == key)
            down[1] = 0;
        else
            return;  // key up without coresponding down (menu pass through)

        if(down[0] != 0 || down[1] != 0)
            return; // some other key is still holding it down

        active = false;

        // save timestamp for partial frame summing
        int time = 0;
        try {
            if(tokens.length > 2)
                time = Integer.parseInt(tokens[2]);
        } catch(NumberFormatException e) {
        }
        if(time == 0)
            msec += Ref.client.frame_msec / 2;
        else
            msec += time - downtime;
    }

    private void KeyDown(String[] tokens) {
        int key = -1;
        if(tokens.length > 1) {
            String sKey = tokens[1];
            try {
                key = Integer.parseInt(sKey);
            } catch(NumberFormatException e) {
                key = 0;
            }
        }

        if(key == down[0] || key == down[1])
            return; // repeat key

        if(down[0] == 0)
            down[0] = key;
        else if(down[1] == 0)
            down[1] = key;
        else {
            System.out.println("Three keys down for a button!");
            return;
        }

        if(active)
            return; // already down

        // save timestamp for partial frame summing
        int time = Ref.client.realtime;
        try {
            if(tokens.length > 2)
                time = Integer.parseInt(tokens[2]);
        } catch(NumberFormatException e) {
        }
        downtime = time;
        active = true;
        wasPressed = true;
    }
}
