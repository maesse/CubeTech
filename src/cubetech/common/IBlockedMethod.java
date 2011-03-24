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
public interface IBlockedMethod {
    void blocked(Gentity self, Gentity other);
}
