package cubetech.entities;

import cubetech.common.Common;
import cubetech.server.SvFlags;
import java.util.EnumSet;
import org.lwjgl.util.vector.Vector2f;

/**
 * shared by both the server system and game
 
 * @author mads
 */
public class EntityShared {
    public EntityState s;

    public EnumSet<SvFlags> svFlags = EnumSet.of(SvFlags.NONE); // Transmit flags
    public int singleClient; // arg to SvFlags

    public boolean bmodel;  // if false, assume an explicit mins / maxs bounding box
                            // only set by trap_SetBrushModel
    
    public boolean linked; // true if currently linked into the worldsectors
    public int linkcount; // incremented at each link, for stats
    public Vector2f absmax = new Vector2f(); // max in world-space, used for linking
    public Vector2f absmin = new Vector2f(); // derived from mins/maxs and origin + rotation

    // Used by clipMoveToEntities
    public int contents; // CONTENTS_TRIGGER, CONTENTS_SOLID, CONTENTS_BODY, etc
                         // a non-solid entity should set to 0
    
    // when a trace call is made and passEntityNum != ENTITYNUM_NONE,
    // an ent will be excluded from testing if:
    // ent->s.number == passEntityNum	(don't interact with self)
    // ent->s.ownerNum = passEntityNum	(don't interact with your own missiles)
    // entity[ent->s.ownerNum].ownerNum = passEntityNum	(don't interact with other missiles from owner)
    public int ownernum = Common.ENTITYNUM_NONE; // Entity that created this entity

    // currentOrigin will be used for all collision detection and world linking.
    // it will not necessarily be the same as the trajectory evaluation for the current
    // time, because each entity must be moved one at a time after time is advanced
    // to avoid simultanious collision issues
    public Vector2f currentAngles = new Vector2f(); // Also used for Linking
    public Vector2f currentOrigin = new Vector2f(); // Also used for Linking
    public Vector2f maxs = new Vector2f(); // Also used for Linking 
    public Vector2f mins = new Vector2f(); // Also used for Linking 

    // The corresponding Gentity will clear the shared entity, when
    // it is freed.
    public void Clear() {
        linked = false;
        linkcount = 0;
        svFlags = EnumSet.of(SvFlags.NONE);
        singleClient = 0;
        mins = new Vector2f();
        maxs = new Vector2f();
        absmin = new Vector2f();
        absmax = new Vector2f();
        currentAngles = new Vector2f();
        currentOrigin = new Vector2f();
        ownernum = Common.ENTITYNUM_NONE;
    }
}
