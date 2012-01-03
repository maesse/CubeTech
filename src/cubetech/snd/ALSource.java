/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.snd;

import cubetech.common.Helper;
import cubetech.misc.Ref;
import cubetech.common.Common;
import org.lwjgl.util.vector.Vector3f;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.*;
/**
 *
 * @author mads
 */
public class ALSource {
    int source; // openal source index
    SoundHandle sfx; // current sound effect in use

    int lastTimeUsed;
    Priority priority;
    int entity; // owner entity (-1 if none)
    SoundChannel channel; // associated channel (-1 if none)

    boolean	isActive;		// Is this source currently in use?
    boolean	isPlaying;		// Is this source currently playing, or stopped?
    boolean	isLocked;		// This is locked (un-allocatable)
    boolean	isLooping;		// Is this a looping effect (attached to an entity)
    boolean	isTracking;		// Is this object tracking its owner

    float		curGain;		// gain employed if source is within maxdistance.
    float		scaleGain;		// Last gain value for this source. 0 if muted.

    float		lastTimePos;		// On stopped loops, the last position in the buffer
    int		lastSampleTime;		// Time when this was stopped
    Vector3f		loopSpeakerPos = new Vector3f();		// Origin of the loop speaker
    boolean	local;			// Is this local (relative to the cam)
    public enum Priority {
        AMBIENT, // amb sound effects
        ENTITY,  // entity sound effects
        ONESHOT, // one-shot sounds
        LOCAL,   // local sounds
        STREAM   // streaming sounds (music, etc)
    }

    static int activeSources = 0;
    // Playback sources
    static ALSource[] sourceList;
    static boolean sourcesInitialized;

    static void initialize(int maxSources) {
        int srcCount = 0;
        sourceList = new ALSource[maxSources];
        for (int i= 0; i < maxSources; i++) {
            int srcIndex = alGenSources();
            if(alGetError() != AL_NO_ERROR)
                break;
            sourceList[i] = new ALSource();
            sourceList[i].source = srcIndex;
            srcCount++;
        }
        if(srcCount < maxSources) {
            ALSource[] fittedArray = new ALSource[srcCount];
            System.arraycopy(sourceList, 0, fittedArray, 0, srcCount);
            sourceList = fittedArray;
        }
        sourcesInitialized = true;
        Common.Log("[Sound] Allocated " + srcCount + " sources.");
    }

    static int alloc(Priority prio, int entnum, SoundChannel chan) {
        if(!sourcesInitialized) return -1;
        int empty = -1;
        for (int i= 0; i < sourceList.length; i++) {
            ALSource curSource = sourceList[i];

            if(curSource.isLocked) continue;

            if(!curSource.isActive) {
                empty = i;
                break;
            }
        }

        if(empty >= 0) {
            sourceList[empty].kill();
            sourceList[empty].isActive = true;
            activeSources++;
        }

        return empty;
    }

    static void setup(int srcHandle, SoundHandle sfx, ALSource.Priority prio,
            int entity, SoundChannel channel, boolean local) {
        get(srcHandle).setup(sfx, prio, entity, channel, local);
    }

    static void play(int srcHandle) {
        if(!sourcesInitialized) return;
        int src = sourceList[srcHandle].source;
        alSourcePlay(src);
        sourceList[srcHandle].isPlaying = true;
    }

    static ALSource get(int srcHandle) {
        if(!sourcesInitialized) return null;
        if(srcHandle < 0 || srcHandle >= sourceList.length) {
            Ref.common.Error(Common.ErrorCode.DROP, "ALSource.get(): Invalid source index " + srcHandle);
        }
        return sourceList[srcHandle];
    }

    static void update() {
        if(!sourcesInitialized) return;
        for (int i= 0; i < sourceList.length; i++) {
            ALSource src = sourceList[i];

            if(src.isLocked) continue;
            if(!src.isActive) continue;

            if(ALSoundManager.volume.modified || ALSoundManager.s_alGain.modified)
                src.curGain = ALSoundManager.s_alGain.fValue * ALSoundManager.volume.fValue;
            if(ALSoundManager.s_alRolloff.modified)
                alSourcef(src.source, AL_ROLLOFF_FACTOR, ALSoundManager.s_alRolloff.fValue);
            if(ALSoundManager.s_alMinDistance.modified)
                alSourcef(src.source, AL_REFERENCE_DISTANCE, ALSoundManager.s_alMinDistance.fValue);
            
            

            if(src.isLooping) {
                SEntity sent = SEntity.get(src.entity);
                alSourcef(src.source, AL_PITCH, sent.pitch);
                src.curGain = ALSoundManager.s_alGain.fValue * ALSoundManager.volume.fValue * sent.volume;
                // If a looping effect hasn't been touched this frame, pause or kill it
                if(sent.loopAddedThisFrame) {
                    // The sound has changed without an intervening removal
                    if(src.isActive && !sent.startLoopingSound
                            && src.sfx != sent.loopSfx) {
                        src.newLoopMaster(true);

                        src.isPlaying = false;
                        alSourceStop(src.source);
                        alSourcei(src.source, AL_BUFFER, 0);
                        sent.startLoopingSound = false;
                    }

                    // The sound hasn't been started yet
                    if(sent.startLoopingSound) {

                        setup(i, sent.loopSfx, sent.loopPriority, src.entity, null, src.local);
                        src.isLooping = true;
                        ALBuffer.sfxList2.get(sent.loopSfx).loopCnt++;
                        sent.startLoopingSound = false;
                    }
                    
                    ALBuffer sfx = ALBuffer.sfxList2.get(src.sfx);

                    src.scaleGain(src.loopSpeakerPos);
                    if(src.scaleGain == 0) {
                        if(src.isPlaying) {
                            // Sound is mute, stop playback until we are in range again
                            src.newLoopMaster(false);
                            alSourceStop(i);
                            src.isPlaying = false;
                        } else if(sfx.loopActiveCnt == 0 && sfx.masterLoopSrc < 0) {
                            sfx.masterLoopSrc = i;
                        }
                        continue;
                    }

                    if(!src.isPlaying) {
                        if(src.priority == ALSource.Priority.AMBIENT) {
                            // If there are other ambient looping sources with the same sound,
			    // make sure the sound of these sources are in sync.
                            if(sfx.loopCnt != 0) {
                                // we already have a master loop playing, get buffer position.
                                ALSoundManager.clearError(false);
                                int offset = alGetSourcei(sourceList[sfx.masterLoopSrc].source, AL_SAMPLE_OFFSET);
                                int error = alGetError();
                                if(error != AL_NO_ERROR) {
                                    if(error != AL_INVALID_ENUM) {
                                        Common.Log("Warning: Cannot get sample offset from source " + i + ", error: " + error);
                                    }
                                } else {
                                    alSourcei(src.source, AL_SAMPLE_OFFSET, offset);
                                }
                            } else if(sfx.loopCnt != 0 && sfx.masterLoopSrc >= 0) {
                                ALSource master = sourceList[sfx.masterLoopSrc];

                                // This loop sound used to be played, but all sources are stopped. Use last sample position/time
				// to calculate offset so the player thinks the sources continued playing while they were inaudible.
                                if(master.lastTimePos >= 0) {
                                    float secofs = master.lastTimePos + (Ref.common.Milliseconds() - master.lastSampleTime) / 1000f;
                                    secofs = secofs % ((float)sfx.totalsize / sfx.samplerate);
                                    alSourcef(src.source, AL_SEC_OFFSET, secofs);
                                    
                                }

                                // I be the master now
                                sfx.masterLoopSrc = i;
                            } else {
                                sfx.masterLoopSrc = i;
                            }
                        } else if(src.lastTimePos >= 0) {
                            // For unsynced loops (SRCPRI_ENTITY) just carry on playing as if the sound was never stopped
                            float secofs = src.lastTimePos + (Ref.common.Milliseconds() - src.lastTimePos) / 1000f;
                            secofs = secofs % ((float)sfx.totalsize / sfx.samplerate);
                            alSourcef(src.source, AL_SEC_OFFSET, secofs);
                        }

                        sfx.loopActiveCnt++;

                        alSourcei(src.source, AL_LOOPING, AL_TRUE);
                        ALSource.play(i);
                    }

                    // Update locality
                    if(src.local) {
                        alSourcei(src.source, AL_SOURCE_RELATIVE, AL_TRUE);
                        alSourcef(src.source, AL_ROLLOFF_FACTOR, 0f);
                    } else {
                        alSourcei(src.source, AL_SOURCE_RELATIVE, AL_FALSE);
                        alSourcef(src.source, AL_ROLLOFF_FACTOR, ALSoundManager.s_alRolloff.fValue);
                    }
                } else if(src.priority == Priority.AMBIENT) {
                    if(src.isPlaying) {
                        src.newLoopMaster(false);
                        alSourceStop(src.source);
                        src.isPlaying = false;
                    }
                } else {
                    src.kill();
                }

                continue;
                // is-looping end
            }

            // Check if it's done, and flag it
            int state = alGetSourcei(src.source, AL_SOURCE_STATE);
            if(state == AL_STOPPED) {
                src.isPlaying = false;
                src.kill();
                continue;
            }

            // Query relativity of source, don't move if it's true
            state = alGetSourcei(src.source, AL_SOURCE_RELATIVE);

            // See if it needs to be moved
            if(src.isTracking && state == 0) {
                Vector3f org = SEntity.get(src.entity).origin;
                src.setPosition(org);
                src.scaleGain(org);
            }

        }
    }

    void setPosition(Vector3f v) {
        if(v == null) {
            alSource3f(source, AL_POSITION, 0,0,0);
        } else {
            alSource3f(source, AL_POSITION, v.x, v.y, v.z);
        }
    }

    void setVelocity(Vector3f v) {
        if(v == null) {
            alSource3f(source, AL_VELOCITY, 0,0,0);
        } else {
            alSource3f(source, AL_VELOCITY, v.x, v.y, v.z);
        }
    }

    void setup(SoundHandle sfx, Priority prio,
            int entity, SoundChannel channel, boolean local) {
        ALBuffer buffer = ALBuffer.sfxList2.get(sfx);
        buffer.lastTimeUsed = Ref.common.Milliseconds();
        
        lastTimeUsed = buffer.lastTimeUsed;
        this.sfx = sfx;
        this.priority = prio;
        this.entity = entity;
        this.channel = channel;
        isPlaying = false;
        isLocked = false;
        isLooping = false;
        isTracking = false;
        curGain = ALSoundManager.s_alGain.fValue * ALSoundManager.volume.fValue;
        scaleGain = curGain;
        this.local = local;

        // Set up OpenAL source
        alSourcei(source, AL_BUFFER, buffer.alBuffer);
        alSourcef(source, AL_PITCH, 1);
        gain(curGain);
        alSource3f(source, AL_POSITION, 0, 0, 0);
        alSource3f(source, AL_VELOCITY, 0, 0, 0);
        alSourcei(source, AL_LOOPING, AL_FALSE);
        alSourcef(source, AL_REFERENCE_DISTANCE, ALSoundManager.s_alMinDistance.fValue);

        if(local) {
            alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE);
            alSourcef(source, AL_ROLLOFF_FACTOR, 0);
        } else {
            alSourcei(source, AL_SOURCE_RELATIVE, AL_FALSE);
            alSourcef(source, AL_ROLLOFF_FACTOR, ALSoundManager.s_alRolloff.fValue);
        }
    }

    void gain(float gain) {
        // todo: check mute
        alSourcef(source, AL_GAIN, gain);
    }

    void scaleGain(Vector3f origin) {
        float distance = 0;
        if(!local) distance = Helper.VectorDistance(origin, ALSoundManager.lastListenerOrigin);

        // If we exceed a certain distance, scale the gain linearly until the sound
	// vanishes into nothingness.
        if(!local && (distance -= ALSoundManager.s_alMaxDistance.fValue) > 0) {
            float scaleFactor;

            if(distance >= ALSoundManager.s_alGraceDistance.fValue) scaleFactor = 0f;
            else scaleFactor = 1f - distance / ALSoundManager.s_alGraceDistance.fValue;

            scaleFactor *= curGain;
            if(scaleGain != scaleFactor) {
                scaleGain = scaleFactor;
                gain(scaleGain);
            }
        } else if(scaleGain != curGain) {
            scaleGain = curGain;
            gain(scaleGain);
        }
    }

    void newLoopMaster(boolean isKilled) {
        if(!sourcesInitialized) return;
        ALBuffer curSfx = ALBuffer.sfxList2.get(sfx);

        if(isPlaying) curSfx.loopActiveCnt--;
        if(isKilled) curSfx.loopCnt--;

        if(curSfx.loopCnt != 0) {
            if(priority == Priority.ENTITY) {
                if(!isKilled && isPlaying) {
                    // only sync ambient loops...
                    // It makes more sense to have sounds for weapons/projectiles unsynced
                    saveLoopPos(source);
                }
            } else if(this == sourceList[curSfx.masterLoopSrc]) {
                ALSource curSrc = null;
                int firstInactive = -1;
                // Only if rmSource was the master and if there are still playing loops for
		// this sound will we need to find a new master.
                if(isKilled || curSfx.loopActiveCnt != 0) {
                    for (int i= 0; i < sourceList.length; i++) {
                        curSrc = sourceList[i];
                        if(curSrc.sfx == sfx && curSrc != this && curSrc.isActive &&
                                curSrc.isLooping && curSrc.priority == Priority.AMBIENT) {
                            if(curSrc.isPlaying) {
                                curSfx.masterLoopSrc = i;
                                break;
                            }else if(firstInactive < 0) {
                                firstInactive = i;
                            }
                        }
                    }
                }

                if(curSfx.loopActiveCnt == 0) {
                    if(firstInactive < 0) {
                        if(isKilled)
                        {
                            curSfx.masterLoopSrc = -1;
                            return;
                        } else curSrc = this;
                    } else curSrc = sourceList[firstInactive];

                    if(isPlaying) {
                        // this was the last not stopped source, save last sample position + time
                        curSrc.saveLoopPos(source);
                    } else {
                        // second case: all loops using this sound have stopped due to listener being of of range,
			// and now the inactive master gets deleted. Just move over the soundpos settings to the
			// new master.
                        curSrc.lastTimePos = lastTimePos;
                        curSrc.lastSampleTime = lastSampleTime;
                    }
                }
            }
        } else {
            curSfx.masterLoopSrc = -1;
        }

    }

    void saveLoopPos(int ALSource) {
        ALSoundManager.clearError(false);
        lastTimePos = alGetSourcef(ALSource, AL_SEC_OFFSET);
        int error = alGetError();
        if(error != AL_NO_ERROR) {
            // Old OpenAL implementations don't support AL_SEC_OFFSET
            if(error != AL_INVALID_ENUM) {
                Common.Log("Warning: Could not get the time offset for alSource " + ALSource);
            }
            lastTimePos = -1;
        } else
        {
            lastSampleTime = Ref.common.Milliseconds();
        }
    }

    void kill() {
        // I'm not touching it. Unlock it first.
        if(isLocked) return;

        // Remove the entity association and loop master status
        if(isLooping) {
            isLooping = false;

            if(entity != -1) {
                SEntity ent = SEntity.get(entity);
                ent.srcAllocated = false;
                ent.srcIndex = -1;
                ent.loopAddedThisFrame = false;
                ent.startLoopingSound = false;
            }

            // TODO

            newLoopMaster(true);
        }

        // Stop it if it's playing
        if(isPlaying) {
            alSourceStop(source);
            isPlaying = false;
        }

        // Remove the buffer
        alSourcei(source, AL_BUFFER, 0);

        sfx = null;
        lastTimeUsed = 0;
        priority = ALSource.Priority.AMBIENT;
        entity = -1;
        channel = null;
        if(isActive) {
            isActive = false;
            activeSources--;
        }
        isLocked = false;
        isTracking = false;
        local = false;
    }
}
