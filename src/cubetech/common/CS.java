/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

/**
 *
 * @author mads
 */
public class CS {
    public final static int CS_SERVERINFO = 0;
    public final static int CS_SYSTEMINFO = 1;
    public final static int CS_MUSIC = 2;
    public final static int CS_MESSAGE = 3;		// from the map worldspawn's message field
    public final static int CS_MOTD = 4;		// g_motd string for server message of the day
    public final static int CS_WARMUP = 5;		// server time when the match will be restarted
    public final static int CS_SCORES1 = 6;
    public final static int CS_SCORES2 = 7;
    public final static int CS_VOTE_TIME = 8;
    public final static int CS_VOTE_STRING = 9;
    public final static int CS_VOTE_YES = 10;
    public final static int CS_VOTE_NO = 11;

    public final static int CS_TEAMVOTE_TIME = 12;
    public final static int CS_TEAMVOTE_STRING = 14;
    public final static int CS_TEAMVOTE_YES = 16;
    public final static int CS_TEAMVOTE_NO = 18;

    public final static int CS_GAME_VERSION = 20;
    public final static int CS_LEVEL_START_TIME = 21;		// so the timer only shows the current level
    public final static int CS_INTERMISSION = 22;		// when 1, fraglimit/timelimit has been hit and intermission will start in a second or two
    public final static int CS_FLAGSTATUS = 23;	// string indicating flag status in CTF
    public final static int CS_SHADERSTATE = 24;
    public final static int CS_BOTINFO = 25;

    public final static int CS_ITEMS = 27;		// string of 0's and 1's that tell which items are present

    public final static int CS_MODELS = 32;
    public final static int CS_SOUNDS = (CS_MODELS + 256);
    public final static int CS_PLAYERS = (CS_SOUNDS + 256);
    public final static int CS_LOCATIONS = (CS_PLAYERS + 64);
    public final static int CS_PARTICLES = (CS_LOCATIONS + 64);

    public final static int CS_MAX = (CS_PARTICLES + 64);
}
