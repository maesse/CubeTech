/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.client;

/**
 *
 * @author mads
 */
public class SnapShot {
    public static final int SF_DELAYED = 1;
    public static final int SF_NOT_ACTIVE = 2;
    public static final int SF_SERVERCOUNT = 4;

    public int SnapFlag;
    public int Ping;
    public int ServerTime;

    public int NumEntities;

    public int numServerCommands;
    public int serverCommandSequence;
}
