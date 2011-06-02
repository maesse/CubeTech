package cubetech.net;

/**
 *
 * @author mads
 */
public enum ConnectState {
    UNINITIALIZED,
     DISCONNECTED   , // not talking to a server
     CONNECTING      , // sending request packets to the server
     CHALLENGING     , // sending challenge packets to the server
     CONNECTED       , // netchan_t established, getting gamestate
     LOADING         , // only during cgame initialization, never during main loop
     PRIMED          , // got gamestate, waiting for first frame
     ACTIVE          , // game views should be displayed
}
