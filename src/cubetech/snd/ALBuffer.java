package cubetech.snd;

import cubetech.common.Common;
import cubetech.gfx.ResourceManager;
import cubetech.misc.Ref;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import org.lwjgl.util.WaveData;
import static org.lwjgl.openal.AL10.*;
import org.lwjgl.openal.AL;
/**
 *
 * @author mads
 */
public class ALBuffer {
    private static int bufferhandleCount = 0;
    String filename;
    SoundHandle handle;
    int alBuffer;
    boolean inMemory; // Sound is stored in memory
    boolean locked; // Sound is locked (can not be unloaded)
    boolean isDefault; // play default sound
    int lastTimeUsed = Ref.common.Milliseconds();

    int loopCnt;	// number of loops using this sfx
    int	loopActiveCnt;	// number of playing loops using this sfx
    int	masterLoopSrc;	// All other sources looping this buffer are synced to this master src

    static HashMap<String, ALBuffer> sfxList = new HashMap<String, ALBuffer>();
    static HashMap<SoundHandle, ALBuffer> sfxList2 = new HashMap<SoundHandle, ALBuffer>();
    static ALBuffer defaultSound;

    int totalsize;
    int samplerate;
    int format;

    static SoundHandle AddWavSound(String path) {
        // Check if already loaded
        if(sfxList.containsKey(path)) {
            return sfxList.get(path).handle;
        }

        ALBuffer buffer = null;
        try {
            InputStream file = ResourceManager.OpenFileAsInputStream(path);
            WaveData data = WaveData.create(file);
            file.close();
            buffer = new ALBuffer(path, data);
        } catch (IOException ex) {
            log("Couldn't open wav file from " + path);
            log(ex.toString());
            buffer = new ALBuffer(path);
            buffer.alBuffer = defaultSound.alBuffer;
        }

        sfxList.put(path, buffer);
        sfxList2.put(buffer.handle, buffer);
        return buffer.handle;
    }

    private static void log(String str) {
        Common.Log("[Sound] " + str);
    }

    ALBuffer(String file) {
        filename = file;
        isDefault = true;
        handle = new SoundHandle(bufferhandleCount++);
    }

    ALBuffer(String file, WaveData data) {
        filename = file;
        handle = new SoundHandle(bufferhandleCount++);
        alBuffer = alGenBuffers();
        alBufferData(alBuffer, data.format, data.data, data.samplerate);
        samplerate = data.samplerate;
        format = data.format;
        totalsize = data.data.limit();
    }
}
