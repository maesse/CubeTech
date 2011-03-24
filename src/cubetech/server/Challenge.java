/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.server;

import java.net.InetSocketAddress;

/**
 *
 * @author mads
 */
public class Challenge {
    public InetSocketAddress addr;
    public int challenge;
    public int clientChallenge; // challenge number coming from the client
    public int pingTime; // time the challenge response was sent to client
    public int firstTime; // time the adr was first used, for authorize timeout checks
    public int time;
    public boolean wasRefused;
    public boolean connected;
}
