package cubetech.net;

/**
 *
 * @author mads
 */
public class ConnectState {
    public static final int UNINITIALIZED   = 0;
    public static final int DISCONNECTED    = 1; // not talking to a server
    public static final int CONNECTING      = 2; // sending request packets to the server
    public static final int CHALLENGING     = 3; // sending challenge packets to the server
    public static final int CONNECTED       = 4; // netchan_t established, getting gamestate
    public static final int LOADING         = 5; // only during cgame initialization, never during main loop
    public static final int PRIMED          = 6; // got gamestate, waiting for first frame
    public static final int ACTIVE          = 7; // game views should be displayed
}
