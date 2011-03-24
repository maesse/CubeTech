package cubetech.server;

import cubetech.common.PlayerState;

/**
 * Snapshot on the serverside
 * @author mads
 */
public class ClientSnapshot {
    public PlayerState ps = new PlayerState();
    public int num_entities;
    public int first_entity; // into the circular sv_packet_entities[]
                            // the entities MUST be in increasing state number
                            // order, otherwise the delta compression will fail

    public int messageSent; // time the message was transmitted
    public int messageAcked; // time the message was acked
    public int messageSize; // used to rate drop packets
}
