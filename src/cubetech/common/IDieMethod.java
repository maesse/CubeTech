/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

import cubetech.Game.Gentity;

/**
 *
 * @author mads
 */
public interface IDieMethod {
    void die(Gentity self, Gentity inflictor, Gentity attacker, int dmg);
}
