package cubetech.snd;

import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public interface ISoundManager {
    public void SetEntityPosition(int entityNum, Vector3f position, Vector3f velocity);
    public void Respatialize(int entity_num, Vector3f origin, Vector3f velocity, Vector3f[] axis);
    public void addLoopingSound(int entityNum, Vector3f origin, Vector3f velocity, SoundHandle sfx);
    public void clearLoopingSounds(boolean killall);
    public void stopLoopingSound(int entityNum);
    public void startSound(Vector3f origin, int entityNum, SoundHandle buffer, SoundChannel chan, float volume);
    public SoundHandle AddWavSound(String path);
    public void initialize(int channels);
    public void Update(int msec);
    public float getEffectVolume();
    public void setEffectVolume(float EffectVolume);
    
}
