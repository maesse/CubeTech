package cubetech.input;

import cubetech.misc.Ref;

/**
 * Each controller in the game (kb/mouse and joysticks) has
 * a corresponding controllerstate.
 * Main purpose is to decouple client index from controller button states
 * @author Mads
 */
public class ControllerState {
    protected ButtonState in_left, in_right, in_forward, in_back, in_up, in_down;
    protected ButtonState[] in_buttons = new ButtonState[30]; // Custom buttons
    
    // controller identifier should be either KB or JOY<num>
    public ControllerState(String controllerIdentifier) {
        in_forward = new ButtonState();
        in_back = new ButtonState();
        in_left = new ButtonState();
        in_right = new ButtonState();
        in_up = new ButtonState();
        in_down = new ButtonState();
        String ctrl = controllerIdentifier;
        Ref.commands.AddCommand("+" + ctrl + "forward", in_forward.KeyDownHook);
        Ref.commands.AddCommand("-" + ctrl + "forward", in_forward.KeyUpHook);
        Ref.commands.AddCommand("+" + ctrl + "back", in_back.KeyDownHook);
        Ref.commands.AddCommand("-" + ctrl + "back", in_back.KeyUpHook);
        Ref.commands.AddCommand("+" + ctrl + "left", in_left.KeyDownHook);
        Ref.commands.AddCommand("-" + ctrl + "left", in_left.KeyUpHook);
        Ref.commands.AddCommand("+" + ctrl + "right", in_right.KeyDownHook);
        Ref.commands.AddCommand("-" + ctrl + "right", in_right.KeyUpHook);
        Ref.commands.AddCommand("+" + ctrl + "up", in_up.KeyDownHook);
        Ref.commands.AddCommand("-" + ctrl + "up", in_up.KeyUpHook);
        Ref.commands.AddCommand("+" + ctrl + "down", in_down.KeyDownHook);
        Ref.commands.AddCommand("-" + ctrl + "down", in_down.KeyUpHook);

        for (int i= 0; i < in_buttons.length; i++) {
            in_buttons[i] = new ButtonState();
            Ref.commands.AddCommand("+" + ctrl + "button" + i, in_buttons[i].KeyDownHook);
            Ref.commands.AddCommand("-" + ctrl + "button" + i, in_buttons[i].KeyUpHook);
        }
    }
}
