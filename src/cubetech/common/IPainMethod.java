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
public interface IPainMethod {
    void pain(Gentity self, Gentity attacker, int dmg);
}
