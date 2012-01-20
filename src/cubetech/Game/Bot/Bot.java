package cubetech.Game.Bot;

import cubetech.Game.GameClient;
import cubetech.common.Helper;
import cubetech.input.PlayerInput;
import cubetech.misc.Ref;
import cubetech.server.SvClient;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class Bot {
    GameClient gc;
    BotState state = new BotState();
    
    Bot(GameClient gc) {
        this.gc = gc;
    }

    PlayerInput runFrame() {
        handleConsoleMessages();
        PlayerInput cmd = new PlayerInput();

        // Handle respawn
        if(gc.isDead() && gc.respawnTime + 1000 < Ref.game.level.time)  {
            cmd.Mouse1 = true;
            cmd.Mouse1Diff = true;
        }

        Vector3f lookDest = Ref.game.g_entities[0].r.currentOrigin;
//        lookAt(cmd, lookDest);

        float looklen = Helper.VectorDistance(lookDest, getOrigin());

        if(looklen < 100) {
            //cmd.buttons[2] = true;
        }
        else {
            //cmd.Forward = true;
            //if(((Ref.game.level.time / 1000) & 1) == 1) cmd.Up = true;
        }
        return cmd;
    }

    private void handleConsoleMessages() {
        SvClient cl = Ref.server.getClient(gc.clientIndex);
        cl.lastPacketTime = Ref.server.sv.time;
        if(cl.reliableAcknowledge == cl.reliableSequence) return;

        cl.reliableAcknowledge = cl.reliableSequence;
    }

    private void lookAt(PlayerInput cmd, Vector3f dest) {
        // Create direction vector
        Vector3f lookDir = Vector3f.sub(dest, getOrigin(), null);
        lookDir.normalise();

        // Convert to angles
        Vector3f angles = Helper.VectorToAngles(lookDir, null);

        int derp  = Helper.Angle2Short(angles.x+90);
        if(derp < 150) derp = 150;
        if(derp > 30600) derp = 30600;
        cmd.angles[0] =  derp;
        cmd.angles[1] = -gc.ps.delta_angles[1] + Helper.Angle2Short(angles.y);
        cmd.angles[2] = Helper.Angle2Short(0);
    }

    private Vector3f getOrigin() {
        return gc.r.currentOrigin;
    }
}
