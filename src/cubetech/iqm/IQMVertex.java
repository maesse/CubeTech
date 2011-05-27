/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.iqm;

/**
 *
 * @author mads
 */
public class IQMVertex {
    float[] position = new float[3], texcoord= new float[2], normal= new float[3], tangent= new float[4];
    char[] blendindices = new char[4], blendweights= new char[4], color= new char[4];
}
