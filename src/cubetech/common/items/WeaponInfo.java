package cubetech.common.items;

import cubetech.CGame.IMethodCentity;
import cubetech.common.items.IItem;
import cubetech.iqm.IQMModel;

/**
 *
 * @author mads
 */
public class WeaponInfo {
    public IQMModel missileModel;
    public IQMModel viewModel;
    public IMethodCentity missileTrailFunc;
    public String missileSound;
    public float trailTime;
    public float trailRadius;
    public String fireSound;
    public String explodeSound;
}
