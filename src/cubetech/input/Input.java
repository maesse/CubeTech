package cubetech.input;

import cubetech.misc.Ref;
import javax.swing.event.EventListenerList;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector2f;




/**
 * Handles input diff'ing from frame to frame and assembles UserCommands
 * @author mads
 */
public class Input {
    public PlayerInput playerInput;
    Vector2f mouseDelta;
    Key[] keys;
    protected EventListenerList listenerList = new EventListenerList();
    protected EventListenerList listenerListMouse = new EventListenerList();

    public Input() {
        
    }

    public Key GetKey(int index) {
        return keys[index];
    }

    public void AddKeyEventListener(KeyEventListener listener)
    {
        listenerList.add(KeyEventListener.class, listener);
    }

    public void RemoveKeyEventListener(KeyEventListener listener) {
        listenerList.remove(KeyEventListener.class, listener);
    }

    public void AddMouseEventListener(MouseEventListener listener)
    {
        listenerListMouse.add(MouseEventListener.class, listener);
    }

    public void RemoveMouseEventListener(MouseEventListener listener) {
        listenerListMouse.remove(MouseEventListener.class, listener);
    }

    void FireKeyEvent(KeyEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i= 0; i < listeners.length; i++) {
            if(listeners[i] == KeyEventListener.class) {
                ((KeyEventListener)listeners[i+1]).KeyPressed(evt);
            }
        }
    }

    void FireMouseEvent(MouseEvent evt) {
        Object[] listeners = listenerListMouse.getListenerList();
        for (int i= 0; i < listeners.length; i++) {
            if(listeners[i] == MouseEventListener.class) {
                ((MouseEventListener)listeners[i+1]).GotMouseEvent(evt);
            }
        }
    }

    public void Init() throws LWJGLException {
        // Keyboard init
        Keyboard.create();
        Keyboard.enableRepeatEvents(false);
        if(!Keyboard.isCreated())
            throw new LWJGLException("Keyboard not created.");
        keys = new Key[255];
        for (int i= 0; i < keys.length; i++) {
            keys[i] = new Key(i);
        }

        // Mouse init
        Mouse.create();

        playerInput = new PlayerInput();
        mouseDelta = new Vector2f();
    }
    public void Update() {
        Display.processMessages();
        MouseUpdate();
        KeyboardUpdate();
        UpdateUserInput();
    }

    void UpdateUserInput() {
        playerInput.Up = IsKeyPressed(Keyboard.KEY_UP);
        playerInput.Down = IsKeyPressed(Keyboard.KEY_DOWN);
        playerInput.Left = IsKeyPressed(Keyboard.KEY_LEFT);
        playerInput.Right = IsKeyPressed(Keyboard.KEY_RIGHT);
        playerInput.Jump = IsKeyPressed(Keyboard.KEY_SPACE);
    }

    public boolean IsKeyPressed(int key) {
        if(key <0 || key >= keys.length)
            return false;
        return keys[key].Pressed;
    }

    void MouseUpdate() {
        // Update mouse
        
       //Mouse.poll();

        if(playerInput.Mouse1Diff)
            playerInput.Mouse1Diff = false;
        if(playerInput.Mouse2Diff)
            playerInput.Mouse2Diff = false;
        if(playerInput.Mouse3Diff)
            playerInput.Mouse3Diff = false;
        
        Vector2f delta = new Vector2f();
        boolean event = false;
        while(Mouse.next()) {
            event = true;
            // Add up delta
            delta.x += Mouse.getEventDX();
            delta.y += Mouse.getEventDY();

            // Set Position
            float mousex = (float)Mouse.getEventX();
            float mousey = (float)Mouse.getEventY();
            
            if(!Float.isInfinite(mousey) && !Float.isInfinite(mousex)) {
                playerInput.MousePos.x = mousex;
                playerInput.MousePos.y = mousey;
            }

            playerInput.WheelDelta += Mouse.getEventDWheel();

            int button = Mouse.getEventButton();
            boolean pressed = false;
            if(button != -1)
                pressed = Mouse.getEventButtonState();
            switch(button) {
                case 0:
                    playerInput.Mouse1Diff =  playerInput.Mouse1 != pressed;
                    playerInput.Mouse1 = pressed;
                    break;
                case 1:
                    playerInput.Mouse2Diff =  playerInput.Mouse2 != pressed;
                    playerInput.Mouse2 = pressed;
                    break;
                case 2:
                    playerInput.Mouse3Diff =  playerInput.Mouse3 != pressed;
                    playerInput.Mouse3 = pressed;
                    break;
            }

            FireMouseEvent(new MouseEvent(button, pressed, new Vector2f(delta.x, delta.y), new Vector2f(playerInput.MousePos.x,playerInput.MousePos.y)));
        }
        if(!event) {
            playerInput.WheelDelta = 0;
        }

        // Set delta
        playerInput.MouseDelta = delta;
    }

    void KeyboardUpdate() {
        
        for (int i= 0; i < keys.length; i++) {
            if(keys[i] != null)
                keys[i].Changed = false;
        }
        
        int nProcessed = 0;
        while(Keyboard.next()) {
            boolean pressed = Keyboard.getEventKeyState();
            int key = Keyboard.getEventKey();
            char c = Keyboard.getEventCharacter();
            long msec = Keyboard.getEventNanoseconds()/(1000*1000);

            Key currKey = keys[key];
            
            if(currKey.Pressed != pressed && currKey.Time < msec) {
                // Key changes state
                currKey.Pressed = pressed;
                currKey.Changed = true;
                currKey.Time = msec;
                currKey.Char = c;
                if(pressed) {
                    //System.out.println("Pressed: " + c);
                    FireKeyEvent(new KeyEvent(currKey));
                }
            }

            nProcessed++;
        }
        
    }

}
