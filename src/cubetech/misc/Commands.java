/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mads
 */
public class Commands {
    public static final int SVC_OPS_BAD = 0;
    public static final int SVC_OPS_NOP = 1;
    public static final int SVC_OPS_GAMESTATE = 2;
    public static final int SVC_OPS_CONFIGSTRING = 3;
    public static final int SVC_OPS_SERVERCOMMAND = 4;
    public static final int SVC_OPS_SNAPSHOT = 5;
    public static final int SVC_OPS_EOF = 6;

    public static final int CLC_OPS_BAD = 0;
    public static final int CLC_OPS_NOP = 1;
    public static final int CLC_OPS_MOVE = 2;
    public static final int CLC_OPS_MOVENODELTA = 3;
    public static final int CLC_OPS_CLIENTCOMMAND = 4;
    public static final int CLC_OPS_EOF = 5;

    public static String[] TokenizeString(String str, boolean ignoreQuotes) {
        byte[] data = str.getBytes();
        ArrayList<String> tokens = new ArrayList<String>();
        String text_out = "";

        if(str == null || str.length() == 0)
            return null;

        int offset = 0;
        int len = str.length();
        while(true) {
            if(tokens.size() == 1024)
            {
                String[] dst = new String[1024];
                return tokens.toArray(dst);
            }
            text_out = "";
            while(true) {
                // skip whitespace
                while(offset < len && data[offset] <= ' ')
                    offset++;

                if(offset >= len)
                {
                    String[] dst = new String[tokens.size()];
                    return tokens.toArray(dst);
                }

                // skip // comments
                if(data[offset] == '/' && data[offset+1] == '/') {
                    String[] dst = new String[tokens.size()];
                    return tokens.toArray(dst);
                }

                // skip /* */ comments
                if(data[offset] == '/' && data[offset+1] == '*') {
                    while(offset < len && (data[offset] != '*' || data[offset]+1 != '/'))
                        offset++;
                    if(offset >= len)
                    {
                        String[] dst = new String[tokens.size()];
                        return tokens.toArray(dst);
                    }
                    offset += 2;
                } else
                    break;
            }

            // Handle quoted string
            if(!ignoreQuotes && data[offset] == '"') {
                offset++;
                while(offset < len && data[offset] != '"')
                    text_out += data[offset++];
                tokens.add(text_out);

                if(offset >= len) {
                    String[] dst = new String[tokens.size()];
                    return tokens.toArray(dst);
                }

                offset++;
                continue;
            }

            while(offset < len && data[offset] > ' ') {
                if(!ignoreQuotes && data[offset] == '"')
                    break;

                if(data[offset] == '/' && data[offset+1] == '/')
                    break;

                if(data[offset] == '/' && data[offset+1] == '*')
                    break;

                text_out += data[offset];
                offset++;
            }

            tokens.add(text_out);
            if(offset >= len)
            {
                String[] dst = new String[tokens.size()];
                return tokens.toArray(dst);
            }
        }
    }
}
