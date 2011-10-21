package cubetech.CGame;

import cubetech.gfx.CubeMaterial;
import cubetech.misc.Ref;
import cubetech.snd.SoundHandle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps references to loaded media ressources needed by the cgame
 * @author mads
 */
public class Media {
    // Sounds
    public SoundHandle s_footStep;
    public SoundHandle s_itemRespawn;
    public CubeMaterial t_blood;

    public void Load() {
        s_footStep = Ref.soundMan.AddWavSound("data/sounds/footsteps.wav");
        s_itemRespawn = Ref.soundMan.AddWavSound("data/sounds/weapondrop1.wav");
        try {
            t_blood = CubeMaterial.Load("data/textures/blood_mist.mat", true);
        } catch (Exception ex) {
            Logger.getLogger(Media.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
