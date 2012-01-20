package cubetech.client;

import cubetech.common.ICommand;
import cubetech.input.*;
import cubetech.misc.Ref;
import cubetech.net.ConnectState;
import org.lwjgl.input.Keyboard;

/**
 *
 * @author mads
 */
public class Message implements KeyEventListener {
    public MessageField chatField = new MessageField(null);
    private MessageField currentField = null;

    public Message() {
        Ref.commands.AddCommand("message", new Cmd_Message());
        // Let input know where to send stuff directed to KEYCATCH_MESSAGE
        Ref.Input.AddKeyEventListener(this, Input.KEYCATCH_MESSAGE);
    }

    public void SetField(MessageField field) {
        currentField = field;
        if(field != null)
            Ref.Input.SetKeyCatcher(Ref.Input.GetKeyCatcher() | Input.KEYCATCH_MESSAGE);
        else
            Ref.Input.SetKeyCatcher(Ref.Input.GetKeyCatcher() & ~Input.KEYCATCH_MESSAGE);
    }

    public boolean isChatMessageUp() {
        return currentField == chatField;
    }

    public MessageField getCurrentMessage() {
        return currentField;
    }

    public boolean KeyPressed(KeyEvent evt) {
        // There should always be a field when we get key events
        if(currentField == null)
        {
            Ref.Input.SetKeyCatcher(Ref.Input.GetKeyCatcher() & ~Input.KEYCATCH_MESSAGE);
            return false;
        }
        
        Key key = (Key)evt.getSource();

        // Break out of message keycatcher if escape key pressed
        if(key.key == Keyboard.KEY_ESCAPE) {
            Ref.Input.SetKeyCatcher(Ref.Input.GetKeyCatcher() & ~Input.KEYCATCH_MESSAGE);
            if(currentField == currentField)
                chatField.Clear();
            currentField = null;
            return false;
        }

        // if enter key pressed, clear keycatcher, and if currentfield
        // is chat, then also send a chatmessage
        if(key.key == Keyboard.KEY_RETURN) {
            if(currentField == chatField) {
                if(chatField.buffer.length() > 0 && Ref.client.clc.state == ConnectState.ACTIVE) {
                    String cmd = String.format("say \"%s\"", chatField.buffer.toString());
                    Ref.client.clc.AddReliableCommand(cmd, false);
                }
                chatField.Clear();
            }
            
            Ref.Input.SetKeyCatcher(Ref.Input.GetKeyCatcher() & ~Input.KEYCATCH_MESSAGE);
            currentField = null;
            return false;
        }

        // Other keys are handled by the field
        currentField.KeyPressed(evt);
        return false;
    }

    private class Cmd_Message implements ICommand {
        public void RunCommand(String[] args) {
            currentField = chatField;
            chatField.Clear();
            // Hook into key events
            Ref.Input.SetKeyCatcher(Ref.Input.GetKeyCatcher() | Input.KEYCATCH_MESSAGE);
        }
    }

}
