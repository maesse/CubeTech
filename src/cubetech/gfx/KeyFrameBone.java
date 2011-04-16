/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

/**
 *
 * @author mads
 */
public class KeyFrameBone {
    public int boneId = -1; // Unique identifier for bone
    public float angle, lenght; // Local-space angle
    public KeyFrameBone next = null;
}