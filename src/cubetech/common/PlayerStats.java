package cubetech.common;

import cubetech.net.NetBuffer;

/**
 *
 * @author mads
 */
public class PlayerStats {
    public int Health;
    public int MaxHealth = 100; // not networked..

    // Serialize
    public void WriteDelta(NetBuffer msg, PlayerStats old) {
        msg.WriteDelta(old.Health, Health);
    }

    // Deserialize
    public void ReadDelta(NetBuffer msg, PlayerStats old) {
        Health = msg.ReadDeltaInt(old.Health);
    }

    @Override
    public PlayerStats clone() {
        PlayerStats ps = new PlayerStats();
        ps.Health = Health;
        return ps;
    }
}
