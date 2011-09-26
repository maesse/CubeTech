package cubetech.snd;

import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import java.security.PrivilegedAction;
import java.security.AccessController;
import cubetech.common.Common;
import cubetech.misc.Ref;
import java.nio.FloatBuffer;
import java.util.EnumSet;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.*;
import org.lwjgl.openal.AL;
/**
 *
 * @author mads
 */
public class ALSoundManager implements ISoundManager {
    // Listener
    static Vector3f lastListenerOrigin = new Vector3f();
    static int listenerEntity;

    // Bookkeeping
    private static boolean deviceCreated = false;
    static CVar volume = Ref.cvars.Get("volume", "0.5", EnumSet.of(CVarFlags.TEMP));
    static CVar s_dopplerfactor = Ref.cvars.Get("s_dopplerfactor", "1.0", EnumSet.of(CVarFlags.ARCHIVE));
    static CVar s_dopplerspeed = Ref.cvars.Get("s_dopplerspeed", "2200", EnumSet.of(CVarFlags.ARCHIVE)); // speed of sound
    static CVar s_alMinDistance = Ref.cvars.Get("s_alMinDistance", "120", EnumSet.of(CVarFlags.CHEAT));
    static CVar s_alMaxDistance = Ref.cvars.Get("s_alMaxDistance", "2200", EnumSet.of(CVarFlags.CHEAT));
    static CVar s_alGraceDistance = Ref.cvars.Get("s_alGraceDistance", "512", EnumSet.of(CVarFlags.CHEAT));
    static CVar s_alRolloff = Ref.cvars.Get("s_alRolloff", "0.8", EnumSet.of(CVarFlags.CHEAT));
    static CVar s_alGain = Ref.cvars.Get("s_alGain", "1", EnumSet.of(CVarFlags.TEMP));
    private FloatBuffer axisBuffer = BufferUtils.createFloatBuffer(6*3);

    public void initialize(int channels) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try {
                    AL.create();
                    deviceCreated = true;
                    log("Sound device created.");
                } catch (Exception e) {
                    System.err.println("Sound initialisation failure.");
                    System.err.println(e);
                }
                return null;
            }
        });

        if(deviceCreated) {
            // Init sources
            int maxSources = 128;
            ALSource.initialize(maxSources);
            alDistanceModel(AL_INVERSE_DISTANCE_CLAMPED);
            alDopplerFactor(s_dopplerfactor.fValue);
            alDopplerVelocity(s_dopplerspeed.fValue);
        }
    }

    private void log(String str) {
        Common.Log("[Audio] " + str);
    }

    public void SetEntityPosition(int entityNum, Vector3f position, Vector3f velocity) {
        SEntity.setEntityPosition(entityNum, position, velocity);
    }

    public void Respatialize(int entity_num, Vector3f origin, Vector3f velocity, Vector3f[] axis) {
        axisBuffer.clear();
        axisBuffer.put(axis[0].x).put(axis[0].y).put(axis[0].z).put(axis[2].x).put(axis[2].y).put(axis[2].z).flip();
        alListener(AL_ORIENTATION, axisBuffer);
        alListener3f(AL_POSITION, origin.x, origin.y, origin.z);
        alListener3f(AL_VELOCITY, velocity.x, velocity.y, velocity.z);
        lastListenerOrigin.set(origin);
        listenerEntity = entity_num;
    }

    public void addLoopingSound(int entityNum, Vector3f origin, Vector3f velocity, SoundHandle sfx) {
        SEntity.get(entityNum).loop(ALSource.Priority.ENTITY, sfx, origin, velocity);
    }

    public void clearLoopingSounds(boolean killall) {
        for (ALSource src : ALSource.sourceList) {
            if(src.isLooping && src.entity != -1) {
                SEntity.sentities[src.entity].loopAddedThisFrame = false;
            }
        }
    }

    public void stopLoopingSound(int entityNum) {
        SEntity ent = SEntity.get(entityNum);
        if(ent.srcAllocated) {
            ALSource.get(ent.srcIndex).kill();
        }
    }

    public void startSound(Vector3f origin, int entityNum, SoundHandle buffer, SoundChannel chan, float volume) {
        Vector3f sorigin = null;
        if(origin != null) {
            sorigin = origin;
        } else {
            if(entityNum == listenerEntity) {
                startLocalSound(buffer, chan, volume);
                return;
            }
            sorigin = SEntity.get(entityNum).origin;
        }

        int src = ALSource.alloc(ALSource.Priority.ONESHOT, entityNum, chan);
        if(src == -1) return;

        ALSource.setup(src, buffer, ALSource.Priority.ONESHOT, entityNum, chan, false);

        ALSource curSource = ALSource.get(src);
        if(origin == null) curSource.isTracking = true;

        curSource.setPosition(sorigin);
        curSource.scaleGain(sorigin);

        // Start it playing
        ALSource.play(src);
    }

    // non-spatialized sound
    private void startLocalSound(SoundHandle sfx, SoundChannel chan, float volume) {
        // Try to grab a source
        int isrc = ALSource.alloc(ALSource.Priority.LOCAL, -1, chan);
        if(isrc == -1) return;

        // Set up the effect
        ALSource.setup(isrc, sfx, ALSource.Priority.LOCAL, -1, chan, true);

        ALSource src = ALSource.get(isrc);

        src.curGain *= volume;
        alSourcef(src.source, AL_GAIN, src.curGain);

        // Start it playing
        ALSource.play(isrc);
    }

    static void clearError(boolean quiet) {
        int error = alGetError();
        if(quiet) return;
        if(error != AL_NO_ERROR) {
            Common.Log("Warning: unhandled AL error: " + error);
        }
    }

    public void Update(int msec) {
        // Update SFX channels
        ALSource.update();

        if(s_dopplerfactor.modified) {
            alDopplerFactor(s_dopplerfactor.fValue);
            s_dopplerfactor.modified = false;
        }
        if(s_dopplerspeed.modified) {
            alDopplerVelocity(s_dopplerspeed.fValue);
            s_dopplerspeed.modified = false;
        }

        // Clear the modified flags on the other cvars
        s_alGain.modified = false;
        volume.modified = false;
        s_alMinDistance.modified = false;
        s_alRolloff.modified = false;
    }

    public float getEffectVolume() {
        return volume.fValue;
    }

    public void setEffectVolume(float vol) {
        volume.set(""+vol);
    }

    public SoundHandle AddWavSound(String path) {
        return ALBuffer.AddWavSound(path);
    }

}
