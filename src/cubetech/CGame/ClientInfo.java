/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;

import cubetech.common.Info;


/**
 *  each client has an associated clientInfo_t
 *  that contains media references necessary to present the
 *  client model and other color coded effects
 *  this is regenerated each time a client's configstring changes,
 *  usually as a result of a userinfo (name, model, etc) change
 * @author mads
 */
public class ClientInfo {
    public boolean		infoValid;

    public String name;
//    public team_t team;

//    public Vector3f color1;
//    public Vector3f color2;

//    public int score;			// updated by score servercmds
////    public int location;		// location index for team mode
//    public int health;			// you only get this info about your teammates
//    public int armor;
//    public int curWeapon;
//
//    public int				handicap;
//    public int				wins, losses;	// in tourney mode

    // when clientinfo is changed, the loading of models/skins/sounds
    // can be deferred until you are dead, to prevent hitches in
    // gameplay
    public String			modelName;

    public static ClientInfo Parse(String s) {
        ClientInfo info = new ClientInfo();
        info.name = Info.ValueForKey(s, "n");
        info.modelName = "data/" + Info.ValueForKey(s, "model") + ".iqm";
        info.infoValid = true;
        return info;
    }
}
