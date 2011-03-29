package cubetech.input;

import cubetech.common.Commands;
import cubetech.common.Common;
import cubetech.common.ICommand;
import cubetech.misc.Ref;
import java.util.HashMap;

/**
 * Keeps track of binds
 * @author mads
 */
public class Binds implements KeyEventListener {
    // Custom keys not found in Lwjgl's Keyboard implementation
    public static final int KEY_MOUSE1 = 256;
    public static final int KEY_MOUSE2 = 257;
    public static final int KEY_MOUSE3 = 258;
    public static final int KEY_MOUSE4 = 259;
    public static final int KEY_MOUSE5 = 260;
    public static final int KEY_MWHEELUP = 261;
    public static final int KEY_MWHEELDOWN = 262;
    public static final int KEY_EXTENSION_END = 263;

    // Current binds
    HashMap<Integer, String> binds = new HashMap<Integer, String>();

    // Maps strings to Keyboard.KEY_ values
    HashMap<String, Integer> stringToKeyMap = new HashMap<String, Integer>();
    HashMap<Integer, String> keyToStringMap = new HashMap<Integer, String>();

    public Binds(Input parent) {
        InitKeys();
        Ref.commands.AddCommand("bind", new Cmd_Bind());
        Ref.commands.AddCommand("unbindall", cmd_unbindall);
    }

    private ICommand cmd_unbindall = new ICommand() {
        public void RunCommand(String[] args) {
            binds.clear();
        }
    };

    

    public void KeyPressed(KeyEvent evt) {
        Key realevt = (Key)evt.getSource();
        ParseBinding(realevt.key, realevt.Pressed, realevt.Time);
    }

    public void BindKey(String key, String bind) {
        int keyIndex = StringToKey(key);
        if(keyIndex == 0) {
            Common.Log(String.format("Binds.BindKey(%s, %s): Cannot bind, key not found.", key, bind));
            return;
        }

        BindKey(keyIndex, bind);
    }

    public void BindKey(int key, String bind) {
        // Check if key exists
        if(KeyToString(key).equals("NONE")) {
            Common.Log(String.format("Binds.BindKey(%d, %s): Cannot bind, key not found.", key, bind));
            return;
        }

        // bind it
        binds.put(key, bind);
    }

    // Handle incomming keypresses from input system.
    public void ParseBinding(int key, boolean down, int time) {
        // Check if bound
        if(!binds.containsKey(key))
            return; // not bound

        String bind = binds.get(key);
        // Parse bind, as it can have multiple commands
        while(bind != null && !bind.isEmpty()) {
            // Skip whitespace
            String bindCmd = null;
            int cmdEnd = bind.indexOf(';');
            if(cmdEnd < 1) {
                // End of bind commands..
                bindCmd = bind;
                bind = null;
            }
            else {
                // Has more commands..
                bindCmd = bind.substring(0, cmdEnd);
                bind = bind.substring(cmdEnd+1).trim();
                
            }
            bindCmd = bindCmd.trim();

            if(bindCmd.isEmpty())
                continue;

            // Handle command
            if(bindCmd.startsWith("+")) {
                // button commands add keynum and time as parameters
                // so that multiple sources can be discriminated and
                // subframe corrected
                String cmd = String.format("%c%s %d %d\n", down?'+':'-', bindCmd.substring(1), key, time);
                Ref.commands.AddText(cmd + '\n');
            } else if(down) {
                Ref.commands.AddText(bindCmd+'\n');
            }
        }
    }

    // Returns the keyindex of the key, or 0 if it doesn't exist
    public int StringToKey(String str) {
        str = str.toUpperCase();
        if(stringToKeyMap.containsKey(str))
            return stringToKeyMap.get(str);
        Common.Log("Binds.StringToKey(" + str + "): Unknown key");
        return 0;
    }

    // Returns the name of the key, or NONE if it is unknown.
    public String KeyToString(int key) {
        if(keyToStringMap.containsKey(key))
            return keyToStringMap.get(key);
        Common.Log("Binds.KeyToString(" + key + "): Unknown key");
        return "NONE";
    }

    // Key init helper
    private void RegisterKey(String str, int keyIndex) {
        stringToKeyMap.put(str, keyIndex);
        keyToStringMap.put(keyIndex, str);
    }

    // Write all binds to this stringbuilder, for creating the config file
    public void WriteBinds(StringBuilder dst) {
        dst.append("unbindall\r\n");
        for (Integer integer : binds.keySet()) {
            String key = KeyToString(integer);

            String value = binds.get(integer);

            dst.append("bind ");
            dst.append(key);
            dst.append(" \"");
            dst.append(value);
            dst.append("\"\r\n");
        }
    }

    private class Cmd_Bind implements ICommand {
        public void RunCommand(String[] args) {
            if(args.length < 2) {
                Common.Log("bind <key> [command] : attach a command to a key");
                return;
            }

            // Try to parse key
            int key = StringToKey(args[1]);
            if(key == 0) // unknown/invalid
                return;

            if(args.length == 2) {
                // Display bind
                if(binds.containsKey(key))
                    Common.Log(String.format("\"%s\" = \"%s\"", args[1], binds.get(key)));
                else
                    Common.Log(String.format("\"%s\" is not bound.", args[1]));
                return;
            }

            // Set bind
            String newCmd = Commands.ArgsFrom(args, 2);
            BindKey(key, newCmd);
        }
    }

    // Inits all keyindex <-> keyname relationships
    private void InitKeys() {
        RegisterKey("NONE", 0);
        RegisterKey("ESCAPE", 1);
        RegisterKey("1", 2);
        RegisterKey("2", 3);
        RegisterKey("3", 4);
        RegisterKey("4", 5);
        RegisterKey("5", 6);
        RegisterKey("6", 7);
        RegisterKey("7", 8);
        RegisterKey("8", 9);
        RegisterKey("9", 10);
        RegisterKey("0", 11);
        RegisterKey("MINUS", 12);
        RegisterKey("EQUALS", 13);
        RegisterKey("BACK", 14);
        RegisterKey("TAB", 15);
        RegisterKey("Q", 16);
        RegisterKey("W", 17);
        RegisterKey("E", 18);
        RegisterKey("R", 19);
        RegisterKey("T", 20);
        RegisterKey("Y", 21);
        RegisterKey("U", 22);
        RegisterKey("I", 23);
        RegisterKey("O", 24);
        RegisterKey("P", 25);
        RegisterKey("LBRACKET", 26);
        RegisterKey("RBRACKET", 27);
        RegisterKey("RETURN", 28);
        RegisterKey("LCONTROL", 29);
        RegisterKey("A", 30);
        RegisterKey("S", 31);
        RegisterKey("D", 32);
        RegisterKey("F", 33);
        RegisterKey("G", 34);
        RegisterKey("H", 35);
        RegisterKey("J", 36);
        RegisterKey("K", 37);
        RegisterKey("L", 38);
        RegisterKey("SEMICOLON", 39);
        RegisterKey("APOSTROPHE", 40);
        RegisterKey("GRAVE", 41);
        RegisterKey("LSHIFT", 42);
        RegisterKey("BACKSLASH", 43);
        RegisterKey("Z", 44);
        RegisterKey("X", 45);
        RegisterKey("C", 46);
        RegisterKey("V", 47);
        RegisterKey("B", 48);
        RegisterKey("N", 49);
        RegisterKey("M", 50);
        RegisterKey("COMMA", 51);
        RegisterKey("PERIOD", 52);
        RegisterKey("SLASH", 53);
        RegisterKey("RSHIFT", 54);
        RegisterKey("MULTIPLY", 55);
        RegisterKey("LMENU", 56);
        RegisterKey("SPACE", 57);
        RegisterKey("CAPITAL", 58);
        RegisterKey("F1", 59);
        RegisterKey("F2", 60);
        RegisterKey("F3", 61);
        RegisterKey("F4", 62);
        RegisterKey("F5", 63);
        RegisterKey("F6", 64);
        RegisterKey("F7", 65);
        RegisterKey("F8", 66);
        RegisterKey("F9", 67);
        RegisterKey("F10", 68);
        RegisterKey("NUMLOCK", 69);
        RegisterKey("SCROLL", 70);
        RegisterKey("NUMPAD7", 71);
        RegisterKey("NUMPAD8", 72);
        RegisterKey("NUMPAD9", 73);
        RegisterKey("SUBTRACT", 74);
        RegisterKey("NUMPAD4", 75);
        RegisterKey("NUMPAD5", 76);
        RegisterKey("NUMPAD6", 77);
        RegisterKey("ADD", 78);
        RegisterKey("NUMPAD1", 79);
        RegisterKey("NUMPAD2", 80);
        RegisterKey("NUMPAD3", 81);
        RegisterKey("NUMPAD0", 82);
        RegisterKey("DECIMAL", 83);
        RegisterKey("F11", 87);
        RegisterKey("F12", 88);
        RegisterKey("F13", 100);
        RegisterKey("F14", 101);
        RegisterKey("F15", 102);
        RegisterKey("KANA", 112);
        RegisterKey("CONVERT", 121);
        RegisterKey("NOCONVERT", 123);
        RegisterKey("YEN", 125);
        RegisterKey("NUMPADEQUALS", 141);
        RegisterKey("CIRCUMFLEX", 144);
        RegisterKey("AT", 145);
        RegisterKey("COLON", 146);
        RegisterKey("UNDERLINE", 147);
        RegisterKey("KANJI", 148);
        RegisterKey("STOP", 149);
        RegisterKey("AX", 150);
        RegisterKey("UNLABELED", 151);
        RegisterKey("NUMPADENTER", 156);
        RegisterKey("RCONTROL", 157);
        RegisterKey("NUMPADCOMMA", 179);
        RegisterKey("DIVIDE", 181);
        RegisterKey("SYSRQ", 183);
        RegisterKey("RMENU", 184);
        RegisterKey("PAUSE", 197);
        RegisterKey("HOME", 199);
        RegisterKey("UP", 200);
        RegisterKey("PRIOR", 201);
        RegisterKey("LEFT", 203);
        RegisterKey("RIGHT", 205);
        RegisterKey("END", 207);
        RegisterKey("DOWN", 208);
        RegisterKey("NEXT", 209);
        RegisterKey("INSERT", 210);
        RegisterKey("DELETE", 211);
        RegisterKey("LMETA", 219);
        RegisterKey("LWIN", 219);
        RegisterKey("RMETA", 220);
        RegisterKey("RWIN", 220);
        RegisterKey("APPS", 221);
        RegisterKey("POWER", 222);
        RegisterKey("SLEEP", 223);

        // Mouse keys
        RegisterKey("MOUSE1", KEY_MOUSE1);
        RegisterKey("MOUSE2", KEY_MOUSE2);
        RegisterKey("MOUSE3", KEY_MOUSE3);
        RegisterKey("MOUSE4", KEY_MOUSE4);
        RegisterKey("MOUSE5", KEY_MOUSE5);
        RegisterKey("MWHEELUP", KEY_MWHEELUP);
        RegisterKey("MWHEELDOWN", KEY_MWHEELDOWN);
    }

    
}
