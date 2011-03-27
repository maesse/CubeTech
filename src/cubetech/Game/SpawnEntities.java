package cubetech.Game;

import cubetech.common.Common;
import java.util.ArrayList;

/**
 *
 * @author mads
 */
public class SpawnEntities {
    ArrayList<SpawnEntity> list = new ArrayList<SpawnEntity>();
    
    public SpawnEntities() {
        
    }

    public ArrayList<SpawnEntity> getList() {
        return list;
    }

    public void SpawnAll() {
        for (SpawnEntity spawnEntity : list) {
            spawnEntity.Spawn();
        }
    }

    public void UnspawnAll() {
        for (SpawnEntity spawnEntity : list) {
            spawnEntity.Unspawn();
        }
    }

    public void removeEntity(SpawnEntity ent) {
        if(!list.remove(ent))
            Common.LogDebug("SpawnEntities.RemoveEntity: was not found in list");
    }

    public void AddEntity(SpawnEntity ent) {
        list.add(ent);
    }

    public ArrayList<Gentity> getSpawned() {
        ArrayList<Gentity> spawned = new ArrayList<Gentity>();
        for (SpawnEntity spawnEntity : list) {
            if(spawnEntity.isSpawned())
                spawned.add(spawnEntity.getGEntity());
        }

        return spawned;
    }

    
}
