/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.input;

import org.lwjgl.input.Keyboard;

/**
 *
 * @author mads
 */
public class Key {
        public int key;
        public String Name;
        public char Char;

        // State
        public boolean Pressed;
        public long Time;

        public Key(int key) {
            this.key = key;
            Name = Keyboard.getKeyName(key);
        }
    }
