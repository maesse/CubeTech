package cubetech.server;

import cubetech.Game.GameClient;
import cubetech.common.Common;
import cubetech.entities.SharedEntity;
import cubetech.entities.SvEntity;
import java.util.HashMap;

/**
 * What a horrible name
 * @author mads
 */
public class ServerRun {
    public int restartedServerId;
    public enum ServerState {
        DEAD,
        LOADING,
        GAME
    }

    public ServerState state;
    public boolean restarting; // if true, send configstring changes during SS_LOADING
    public int serverid; // changes each server start
    public int snapshotCounter; // incremented for each snapshot built
    public int timeResidual; // <= 1000 / sv_frame->value
    public int nextFrameTime; // when time > nextFrameTime, process world
    public HashMap<Integer, String> configstrings = new HashMap<Integer, String>();
    public SvEntity[] svEntities = new SvEntity[Common.MAX_GENTITIES];

    // the game virtual machine will update these on init and changes
    public SharedEntity[] gentities;
    public int gentitySize;
    public int num_entities;

    public GameClient[] gameClients;
    public int gameClientSize;

    public int restartTime;
    public int time;

    public ServerRun() {
        for (int i= 0; i < svEntities.length; i++) {
            svEntities[i] = new SvEntity(i);
        }
    }
}
