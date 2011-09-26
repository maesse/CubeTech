package cubetech.snd;

import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.gfx.ResourceManager;
import cubetech.misc.Ref;
import java.io.IOException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.SoundStore;

public class SoundManager implements ISoundManager {
    private static final int MAX_CHANNELS = 96;
    
    private SoundStore store = null;
    private boolean soundOk = false;

    private HashMap<String, SoundHandle> Knownsounds = new HashMap<String, SoundHandle>();
    private HashMap<SoundHandle, Audio> SoundMap = new HashMap<SoundHandle, Audio>();
    private int bufferIndex;
    
    private float EffectVolume = 0.2f;
    private CVar volume = Ref.cvars.Get("volume", "0.5", EnumSet.of(CVarFlags.TEMP));
    private CVar s_minDistance = Ref.cvars.Get("s_minDistance", "30", EnumSet.of(CVarFlags.CHEAT));
    private CVar s_falloff = Ref.cvars.Get("s_falloff", "0.4", EnumSet.of(CVarFlags.CHEAT));
    private CVar s_dopplerfactor = Ref.cvars.Get("s_dopplerfactor", "1.0", EnumSet.of(CVarFlags.CHEAT));
    private CVar s_dopplerspeed = Ref.cvars.Get("s_dopplerspeed", "500", EnumSet.of(CVarFlags.CHEAT)); // speed of sound

    // Sound loop for each entity
    private SoundLoop[] soundLoops = new SoundLoop[Common.MAX_GENTITIES];

    // Available channels
    private ALChannel[] channels = new ALChannel[MAX_CHANNELS];

    // Current listener info
    private Vector3f listener_origin = new Vector3f();
    private Vector3f[] listener_axis;
    private int listener_num;
    private FloatBuffer orgBuffer = BufferUtils.createFloatBuffer(3);
    private FloatBuffer velBuffer = BufferUtils.createFloatBuffer(3);
    private FloatBuffer oriBuffer = BufferUtils.createFloatBuffer(6);

    private void channelSetup() {
        for (int i= 0; i < channels.length; i++) {
            channels[i] = new ALChannel(i);
        }
    }

    private ALChannel channelMalloc() {
        for (int i= 0; i < channels.length; i++) {
            if(channels[i].sfx == null) {
                channels[i].allocTime = Ref.common.Milliseconds();
                return channels[i];
            }
        }
        return null;
    }

    public void SetEntityPosition(int entityNum, Vector3f position, Vector3f velocity) {
        if (!soundOk) {
            return;
        }
        
        soundLoops[entityNum].origin.set(position);
        soundLoops[entityNum].velocity.set(velocity);
    }

    public void Respatialize(int entity_num, Vector3f origin, Vector3f velocity, Vector3f[] axis) {
        if (!soundOk) {
            return;
        }

        listener_origin.set(origin);
        listener_axis = axis;
        listener_num = entity_num;

        if (axis != null) {
            orgBuffer.put(origin.x).put(origin.y).put(origin.z).flip();
            velBuffer.put(velocity.x).put(velocity.y).put(velocity.z).flip();
            oriBuffer.put(axis[0].x).put(axis[0].y).put(axis[0].z).put(axis[2].x).put(axis[2].y).put(axis[2].z).flip();
            store.setListener(orgBuffer, velBuffer, oriBuffer);
        } else {
            store.UpdateListener(origin.x, origin.y, origin.z, velocity.x, velocity.y, velocity.z);
        }
    }

    /**
     * Plays a sound effect
     * @param buffer Buffer index to play gotten from addSound
     */
    public void playEffect(SoundHandle buffer, float volume) {
        if (!soundOk) {
            return;
        }
//        SoundMap.get(buffer).playAsSoundEffect(Ref.common.com_timescale.fValue, volume, false);
    }

    public void addLoopingSound(int entityNum, Vector3f origin, Vector3f velocity, SoundHandle sfx) {
        if(!soundOk) return;

        Audio aud = SoundMap.get(sfx);
        if(aud == null) {
            Common.Log("addLoopingSound: handle out of range %d", sfx.getHandle());
            return;
        }

        SoundLoop loop = soundLoops[entityNum];

        loop.origin.set(origin);
        loop.velocity.set(velocity);
        loop.active = true;
        loop.kill = true;
        //loop.doppler = false;
        //loop.dopplerScale = 1.0f;
        loop.sfx = aud;
        //loop.framenum = Ref.client.framecount;

//        ALChannel ch = channelMalloc();
//        ch.sfx = aud;
//        ch.entNum = entityNum;
//        ch.loop = true;
//        ch.localsound = listener_num == entityNum;
//        ch.dirty = true;
    }

    public void clearLoopingSounds(boolean killall)
    {
        for (int i= 0; i < soundLoops.length; i++) {
            if(soundLoops[i].kill || killall) {
                soundLoops[i].kill = false;
                stopLoopingSound(i);
            }
        }
    }

    public void stopLoopingSound(int entityNum) {
        soundLoops[entityNum].active = false;
        soundLoops[entityNum].kill = false;
    }


  
    /**
    ====================
    S_StartSound

    Validates the parms and ques the sound up
    if pos is NULL, the sound will be dynamically sourced from the entity
    Entchannel 0 will never override a playing sound
    ====================
    **/
    public void startSound(Vector3f origin, int entityNum, SoundHandle buffer, SoundChannel chan, float volume) {
        if (!soundOk) {
            return;
        }

        if(origin == null && (entityNum < 0 || entityNum > Common.MAX_GENTITIES)) {
            Ref.common.Error(Common.ErrorCode.DROP, String.format("startSound: bad entitynumber %d", entityNum));
        }

        if(buffer == null) {
            Common.Log("startSound: given a null handle");
            return;
        }

        Audio sfx = SoundMap.get(buffer);

        int time = Ref.common.Milliseconds();
        int allowed = 4;
        if(entityNum == listener_num) {
            allowed = 8;
        }

        int inplay = 0;
        for (int i= 0; i < channels.length; i++) {
            ALChannel ch = channels[i];
            // Avoid soundspam from a single entity
            if(ch.entNum == entityNum && ch.sfx == sfx && ch.source_playing) {
                if(time - ch.allocTime < 50) {
                    return;
                }
                inplay++;
            }
        }

        if(inplay > allowed) {
            return;
        }

        sfx.setLastTimeUsed(time);

        ALChannel ch = channelMalloc();
        int oldest = time;
        int chosen = -1;
        if(ch == null) {
            for (int i= 0; i < channels.length; i++) {
                ch = channels[i];
                if(ch.entNum != listener_num && ch.entNum != entityNum
                        && ch.allocTime < oldest && ch.entChannel != SoundChannel.ANNOUNCER) {
                    oldest = ch.allocTime;
                    chosen = i;
                }
            }
            if(chosen == -1) {
                for (int i= 0; i < channels.length; i++) {
                    ch = channels[i];
                    if(ch.entNum != listener_num 
                            && ch.allocTime < oldest && ch.entChannel != SoundChannel.ANNOUNCER) {
                        oldest = ch.allocTime;
                        chosen = i;
                    }
                }
                if(chosen == -1) {
                    Common.Log("Dropping sound");
                    return;
                }
            }
            ch = channels[chosen];
            ch.allocTime = time;
        }

        

        if(origin != null) {
            ch.origin.set(origin);
            ch.fixed_origin = true;
        } else {
            ch.fixed_origin = false;
        }

        ch.localsound = entityNum == listener_num;
        ch.entChannel = chan;
        ch.entNum = entityNum;
        ch.loop = false;

        ch.sfx = sfx;
        ch.dirty = true;

       
        // Play at relative position
//        SoundMap.get(buffer).playAsSoundEffect(1.0f, volume, false, position.x, position.y, position.z, position.w);
    }

    /**
     * Adds a sound to the Sound Managers pool
     *
     * @param path Path to file to load
     * @return index into SoundManagers buffer list
     */
    public SoundHandle AddWavSound(String path) {
        if (!soundOk) {
            return null;
        }

        // Check cache
        SoundHandle handle = Knownsounds.get(path);
        if (handle != null) {
            return handle;
        }

        URL url = ResourceManager.getClassLoader().getResource("cubetech/" + path);
        Audio newfile = null;
        try {

            newfile = store.getWAV(url.openStream());
        } catch (IOException ex) {
            Logger.getLogger(SoundManager.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        // Cache
        handle = new SoundHandle(bufferIndex);
        Knownsounds.put(path, handle);
        SoundMap.put(handle, newfile);
        bufferIndex++;
        // return index for this sound
        return handle;
    }

    /**
     * Initializes the SoundManager
     *
     * @param channels Number of channels to create
     */
    public void initialize(int channels) {
        store = SoundStore.get();
        store.setSoundsOn(true);
        store.setSoundVolume(EffectVolume);
      

        store.setMaxSources(channels);
        store.init();
        soundOk = store.soundWorks();

        for (int i= 0; i < soundLoops.length; i++) {
            soundLoops[i] = new SoundLoop();
        }
        
        this.channels = new ALChannel[channels];
        channelSetup();
    }

    // Handles delay between songs and pumps the audiosystem,
    // necessary when using streamed reading
    public void Update(int msec) {
        if (!soundOk) {
            return;
        }

        if (volume.modified) {
            volume.modified = false;
            setEffectVolume(volume.fValue);
        }

        if (s_minDistance.modified) {
            store.setMinDistance(s_minDistance.fValue);
            s_minDistance.modified = false;
        }

        if (s_falloff.modified) {
            store.setRollOff(s_falloff.fValue);
            s_falloff.modified = false;
        }

        if (s_dopplerfactor.modified) {
            s_dopplerfactor.modified = false;
            store.setDopplerFactor(s_dopplerfactor.fValue);
        }

        if (s_dopplerspeed.modified) {
            s_dopplerspeed.modified = false;
            store.setDopplerSpeed(s_dopplerspeed.fValue);
        }

        store.updateALSound(channels, soundLoops);
        store.poll(msec);

    }

    public float getEffectVolume() {
        return EffectVolume;
    }

    public void setEffectVolume(float EffectVolume) {
        this.EffectVolume = EffectVolume;
        store.setSoundVolume(EffectVolume);
        store.setMusicVolume(EffectVolume * 0.5f);
    }

  
}
