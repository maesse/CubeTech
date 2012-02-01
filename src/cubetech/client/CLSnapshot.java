package cubetech.client;

import cubetech.common.PlayerState;

/**
 * Snapshot assemblied in Client system. CGame copies values from this
 * @author mads
 */
public class CLSnapshot {
    public static final int SF_DELAYED = 1;
    public static final int SF_NOT_ACTIVE = 2;
    public static final int SF_SERVERCOUNT = 4;
    public static final int SF_MULTIPS = 8; // contains multiple player states

    public boolean valid; // cleared if delta parsing was invalid
    public int snapFlag; // rate delayed and dropped commands
    public int ping; // time from when cmdNum-1 was sent to time packet was reeceived
    public int serverTime; // server time the message is valid for (in msec)
    public int messagenum; // copied from netchan.incoming_sequence
    public int deltanum; // messageNum the delta is from
    public int cmdnum; // the next cmdNum the server is expecting
    
    public int numEntities;           // all of the entities that need to be presented
    public int parseEntitiesNum;      // at the time of this snapshot
    public int serverCommandSequence; // execute all commands up to this before
                                      // making the snapshot current
    // Variable amount of playerstates can be recieved
    public int[] lcIndex = new int[] {0,-1,-1,-1};
    public PlayerState[] pss = new PlayerState[4];
    public int numPS;
}
