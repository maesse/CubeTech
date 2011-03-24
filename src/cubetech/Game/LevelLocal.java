package cubetech.Game;

import cubetech.entities.SharedEntity;

/**
 *
 * @author mads
 */
public class LevelLocal {
    public GameClient[] clients;
    public Gentity[] gentities;
    public SharedEntity[] sentities;
    
    public int num_entities;
    public int maxclients;

    public int framenum;
    public int time;
    public int previousTime;

    public int startTime;
    public boolean restarted;
    public boolean editmode = false;

    public int numConnectedClients; // not hooked up
}
