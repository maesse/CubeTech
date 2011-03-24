/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.entities;

import cubetech.misc.Ref;
import cubetech.spatial.WorldSector;

/**
 *
 * @author mads
 */
public class SvEntity {
    public int id;
    public SvEntity nextEntity;
    public EntityState baseline = new EntityState();
    public int snapshotCounter;
    public WorldSector worldSector = null; // Current sector linked into

    public SvEntity(int id) {
        this.id = id;
    }

    public SharedEntity GetSharedEntity() {
        return Ref.server.sv.gentities[id];
    }
}
