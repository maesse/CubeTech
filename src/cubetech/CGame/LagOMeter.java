package cubetech.CGame;

import cubetech.client.CLSnapshot;
import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.misc.Ref;
import java.util.EnumSet;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class LagOMeter {
    public static final int SAMPLES = 256;
    int[] frameSamples = new int[SAMPLES];
    int frameCount = 0;
    int[] snapshotFlags = new int[SAMPLES];
    int[] snapshotSamples = new int[SAMPLES];
    int snapshotCount = 0;

    CVar cg_lagometer;
    //CVar cg_lagometer_derp;

    public LagOMeter() {
        cg_lagometer = Ref.cvars.Get("cg_lagometer", "0", EnumSet.of(CVarFlags.ARCHIVE));
    }

    // Adds current frame information to interpolate/exterpolate bar
    public void AddFrameInfo() {
        int offset = Ref.cgame.cg.time - Ref.cgame.cg.latestSnapshotTime;
        frameSamples[frameCount % SAMPLES] = offset;
        frameCount++;
    }

    public void AddSnapshotInfo(Snapshot snap) {
        if(snap == null) {
            // dropped
            snapshotSamples[snapshotCount % SAMPLES] = -1;
            snapshotCount++;
            return;
        }
        
        snapshotSamples[snapshotCount % SAMPLES] = snap.ping;
        snapshotFlags[snapshotCount % SAMPLES] = snap.snapFlags;
        snapshotCount++;
    }

    public void Draw() {
        if(cg_lagometer.iValue == 0 && Ref.net.net_graph.iValue <= 1)
            return;

        float width = 250;
        float height = 200;
        float split = 0.5f;

        float maxRange = 300;
        float maxPing = 300;

        float interpHeight = height * split;
        float pingHeight = height * (1f-split);
        float sampleWidth = (float) Math.ceil(width / SAMPLES);

        
        float vScale = interpHeight / maxRange;
        float interpMid = pingHeight + interpHeight * 0.5f;
        float pingMid = pingHeight * 0.5f;
        Vector2f offset = new Vector2f(Ref.glRef.GetResolution());
//        offset.x -= width;
        offset.x -= 10;
        offset.y = interpMid;
        Sprite spr;
        for (int i= 0; i < SAMPLES; i++) {
            int index = (frameCount - 1 - i) & SAMPLES-1;
            float value = frameSamples[index];
            value *= vScale;
            if(value > 0) {
                if(value > maxRange)
                    value = maxRange;
                spr = Ref.SpriteMan.GetSprite(Type.HUD);
                spr.setLine(offset, new Vector2f(offset.x, offset.y - value), sampleWidth);
                spr.SetColor(255, 255, 0, 255);
            } else if(value < 0) {
                value = -value;
                if(value > maxRange)
                    value = maxRange;
                spr = Ref.SpriteMan.GetSprite(Type.HUD);
                spr.setLine(offset, new Vector2f(offset.x, offset.y + value), sampleWidth);
                spr.SetColor(0, 0, 255, 255);
            }
            offset.x -= sampleWidth;
        }
        
        offset = new Vector2f(Ref.glRef.GetResolution());
        offset.x -= 10;
        offset.y = 10;
//        offset.y = pingMid;
        vScale = pingHeight / maxPing;
        for (int i= 0; i < SAMPLES; i++) {
            int index = (snapshotCount - 1 - i) & SAMPLES-1;
            int ping = snapshotSamples[index];
            if(ping > 0) {
                float value = ping * vScale;
                if(value > pingHeight)
                    value = pingHeight;

                spr = Ref.SpriteMan.GetSprite(Type.HUD);
                spr.setLine(offset, new Vector2f(offset.x, offset.y + value), sampleWidth);
                if((snapshotFlags[index] & CLSnapshot.SF_DELAYED) > 0)
                    spr.SetColor(255, 255, 0, 255);
                else
                    spr.SetColor(0, 255, 0, 255);
                
            } else if(ping < 0) {
                // Dropped packet
                spr = Ref.SpriteMan.GetSprite(Type.HUD);
                spr.setLine(offset, new Vector2f(offset.x, offset.y + pingMid), sampleWidth);
                spr.SetColor(255, 0, 0, 255);
            }
            offset.x -= sampleWidth;
        }
    }

}
