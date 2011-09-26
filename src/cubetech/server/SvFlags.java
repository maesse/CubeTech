/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.server;

/**
 *
 * @author mads
 */
public enum SvFlags {
    NONE,
    NOCLIENT, // don't send entity to clients, even if it has effects
    CLIENTMASK,
    BROADCAST, // send to all connected clients
    SINGLECLIENT, // only send to a single client

    NOSERVERINFO, // don't send CS_SERVERINFO updates to this client
    NOTSINGLECLIENT, // send entity to everyone but one client
    BOT,
}
