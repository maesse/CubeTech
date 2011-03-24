/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.entities;

import cubetech.common.Common;
import cubetech.misc.Ref;

/**
 *
 * @author mads
 */
public class SharedEntity {
    public SharedEntity() {
        s = new EntityState();
        r = new EntityShared();
        r.s = s;
    }
    public EntityState s; // communicated by server to clients
    public EntityShared r; // shared by both the server system and game

    public SvEntity GetSvEntity() {
        if(s.ClientNum < 0 || s.ClientNum >= Common.MAX_GENTITIES)
            Ref.common.Error(Common.ErrorCode.DROP, "SharedEntity.GetSvEntity: Bad index " + s.ClientNum);
        return Ref.server.sv.svEntities[s.ClientNum];
    }
}
