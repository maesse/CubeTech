package cubetech.snd;

import cubetech.common.Common;
import cubetech.misc.Ref;
import cubetech.snd.ALSource.Priority;
import org.lwjgl.util.vector.Vector3f;

/**
 * Sound Entity modeled off quakes implementation
 * @author mads
 */
public class SEntity {
    static final SEntity[] sentities = new SEntity[Common.MAX_GENTITIES];
    static {
        for(int i=0; i<sentities.length;i++) {
            sentities[i] = new SEntity(i);
        }
    }

    static void setEntityPosition(int entityNum, Vector3f position, Vector3f velocity) {
        if(entityNum < 0 || entityNum >= sentities.length) {
            Ref.common.Error(Common.ErrorCode.DROP, "SetEntityPosition: Bad entitynum " + entityNum);
        }
        SEntity ent = sentities[entityNum];
        ent.origin.set(position);
        ent.velocity.set(velocity);
    }

    static SEntity get(int entnum) {
        if(entnum < 0 || entnum >= sentities.length) {
            Ref.common.Error(Common.ErrorCode.DROP, "SEntity.get(): Index out of bounds: " + entnum);
        }
        return sentities[entnum];
    }
    
    Vector3f origin = new Vector3f();
    Vector3f velocity = new Vector3f();

    // Loop stuff
    boolean srcAllocated; // if a source has been allocated to this entity
    int srcIndex;
    boolean loopAddedThisFrame;
    ALSource.Priority loopPriority;
    SoundHandle loopSfx;
    boolean startLoopingSound;
    private int handle;

    private SEntity(int handle) {
        this.handle = handle;
    }

    void loop(Priority priority, SoundHandle sfx, Vector3f origin, Vector3f velocity) {
        ALSource curSource;
        int src;
        // Do we need to allocate a new source for this entity
        if(!srcAllocated) {
            // Try to get a channel
            src = ALSource.alloc(priority, handle, null);
            if(src == -1) {
                Common.Log("Warning: Failed to allocate source for loop sfx %d on entity %d", sfx.getHandle(), handle);
                return;
            }
            curSource = ALSource.get(src);
            startLoopingSound = true;
            curSource.lastTimePos = -1;
            curSource.lastSampleTime = Ref.common.Milliseconds();
        } else {
            src = srcIndex;
            curSource = ALSource.get(src);
        }

        srcAllocated = true;
        srcIndex = src;

        loopPriority = priority;
        loopSfx = sfx;

        // If this is not set then the looping sound is stopped.
        loopAddedThisFrame = true;

        // UGH
	// These lines should be called via S_AL_SrcSetup, but we
	// can't call that yet as it buffers sfxes that may change
	// with subsequent calls to S_AL_SrcLoop
        curSource.entity = handle;
        curSource.isLooping = true;

        if(ALSoundManager.listenerEntity == handle) {
            curSource.local = true;
            curSource.setPosition(null);
            curSource.setVelocity(null);
        } else {
            curSource.local = false;
            Vector3f sorigin = origin;
            if(sorigin == null) {
                sorigin = this.origin;
            }

            curSource.loopSpeakerPos.set(sorigin);

            curSource.setPosition(sorigin);
            curSource.setVelocity(velocity);
        }
    }
}
