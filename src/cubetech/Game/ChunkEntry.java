/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.Game;

/**
 *
 * @author mads
 */
public class ChunkEntry {
        public int clientAck;
        public int lastSent;

        public ChunkEntry(int lastSent) {
            this.lastSent = lastSent;
        }
    }

