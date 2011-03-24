package cubetech.CGame;

import cubetech.misc.Ref;

/**
 * Keeps references to loaded media ressources needed by the cgame
 * @author mads
 */
public class Media {
    // Sounds
    public int s_footStep;
    public int s_itemRespawn;

    public void Load() {
        s_footStep = Ref.soundMan.AddWavSound("data/footsteps.wav");
        s_itemRespawn = Ref.soundMan.AddWavSound("data/weapondrop1.wav");
    }

}
