package cubetech.input;

import org.lwjgl.input.Keyboard;

/**
 *
 * @author mads
 */
public class Key {
        public int key;
        public String Name;
        public char Char = Keyboard.CHAR_NONE;

        // State
        public boolean Pressed;
        public int Time;
        public boolean Changed;

        public Key(int key) {
            this.key = key;
            if(key < 255)
                Name = Keyboard.getKeyName(key);
            else
                Name = "EXTENDED";
        }
    }
