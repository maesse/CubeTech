/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;

import cubetech.common.PlayerState;
import cubetech.entities.EntityState;

/**
 *
 * @author mads
 */
public class Snapshot {
    public static final int MAX_ENTITIES_IN_SNAPSHOT = 256;
    
    public int snapFlags;			// SNAPFLAG_RATE_DELAYED, etc
    public int	ping;
    public int	serverTime;		// server time the message is valid for (in msec)

    // complete information about the current players at this time
    public int[] lcIndex = new int[] {0,-1,-1,-1};
    public PlayerState[] pss = new PlayerState[4];
    public int numPS;

    public int numEntities;			// all of the entities that need to be presented
    public EntityState[] entities = new EntityState[MAX_ENTITIES_IN_SNAPSHOT];	// 256 at the time of this snapshot

    public int numServerCommands;		// text based server commands to execute when this
    public int serverCommandSequence;	// snapshot becomes current
}
