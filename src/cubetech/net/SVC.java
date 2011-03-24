/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.net;

/**
 *
 * @author mads
 */
public class SVC {
    // Opcodes
    public static final int OPS_BAD = 0;
    public static final int OPS_NOP = 1;
    public static final int OPS_GAMESTATE = 2;
    public static final int OPS_CONFIGSTRING = 3;
    public static final int OPS_SERVERCOMMAND = 4;
    public static final int OPS_SNAPSHOT = 5;
    public static final int OPS_EOF = 6;
    public static final int OPS_BASELINE = 7;
    public static final int OPS_DOWNLOAD = 8;
}
