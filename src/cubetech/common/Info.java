/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

import java.util.Map;
import java.util.TreeMap;



/**
 *
 * @author mads
 */
public class Info {

    static String blacklist = "\\;\"";

    public static Map<String, String> GetPairs(String s) {
        Map<String, String> map = new TreeMap<String, String>();

        boolean key = true;
        String[] tokens = s.split("\\\\");
        String skey = "";
        for (int i= 0; i < tokens.length; i++) {
            if(tokens[i].length() == 0)
                continue;
            
            if(key)
                skey = tokens[i];
            else
                map.put(skey, tokens[i]);

            key = !key;
        }

        return map;
    }

    public static String ValueForKey(String s, String key) {
        if(s == null || key == null)
            return "";

        for (int i= 0; i < blacklist.length(); i++) {
            if(key.contains(blacklist.subSequence(i, i+1))) {
                System.out.println("ValueForKey: Can't get keys with " + blacklist);
                return "";
            }
        }

        String[] spl = s.split("\\\\"); // regexp \\
        boolean keyIndex = true;
        for (int i= 0; i < spl.length; i++) {
            if(i == 0 && spl[i].length() == 0)
                continue;
            
            if(keyIndex && key.equals(spl[i])) {
                if(spl.length > i+1)
                    return spl[i+1];
                else
                    return "";
            }
            keyIndex = !keyIndex;
        }
        return "";
    }

    public static String SetValueForKey(String s, String key, String value) {
        if(s == null || key == null)
            return s;

        for (int i= 0; i < blacklist.length(); i++) {
            if(key.contains(blacklist.subSequence(i, i+1))) {
                System.out.println("ValueForKey: Can't get keys with " + blacklist);
                return s;
            }
        }

        String cleaned = RemoveKey(s, key);
//        if(cleaned.length() == 0)
//            return "";

        String newPair = String.format("\\%s\\%s", key, value);
        cleaned += newPair;

        return cleaned;
    }

    public static String RemoveKey(String s, String key) {
        if(key.contains("\\"))
        {
            System.out.println("RemoveKey: Key can't contain \\");
            return s;
        }

        int index = s.indexOf("\\" + key);
        if(index >= 0) {
            int keyEnd = s.indexOf("\\", index +1);

            // Got data after?
            if(keyEnd >= 0) {
                int valueEnd = s.indexOf("\\", keyEnd + 1);
                if(valueEnd >= 0) {
                    if(index == 0) {
                        // Cut beginning
                        return s.substring(valueEnd);
                    } else {
                        // Key to remove is in middle of other values
                        return s.substring(0, index) + s.substring(valueEnd );
                    }
                }
                else {
                    // End of source
                    if(index != 0)
                        return s.substring(0, index);
                    return "";
                }
            }
        }
        return s;

    }
}
