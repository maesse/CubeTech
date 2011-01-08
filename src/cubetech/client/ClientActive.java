/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.client;

import cubetech.input.PlayerInput;
import java.util.HashMap;

/**
 *
 * @author mads
 */
public class ClientActive {
    public int serverTime;
    public int oldServerTime;
    public int oldFrameServerTime;
    public int serverTimeDelta;
    public boolean extrapolatedSnapshot;
    public boolean newsnapshots;
    public String mapname;
    public PlayerInput[] cmds = new PlayerInput[64];
    public int cmdNumber;
    public HashMap<Integer, String> GameState = new HashMap<Integer, String>();
}
