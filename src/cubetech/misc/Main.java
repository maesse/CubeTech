/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import cubetech.misc.Window;

/**
 *
 * @author mads
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Window wct = new Window();
		if (wct.initialize()) {
			wct.execute();
			wct.destroy();
		}
		System.exit(0);
    }

}
