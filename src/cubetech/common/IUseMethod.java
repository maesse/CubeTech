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
public interface IUseMethod {
    void use(Gentity self, Gentity other, Gentity activator);
}
