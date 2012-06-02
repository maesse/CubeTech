package cubetech.net;

/**
 *
 * @author Mads
 */
public class NetStats {
    // incomming/outgoing Stats
    public int clAvgBytesIn, clAvgPacketsIn;
    public int clLastBytesIn, clLastBytesOut;
    public int clAvgBytesOut, clAvgPacketsOut;
    
    public int svAvgBytesIn, svAvgPacketsIn;
    public int svAvgBytesOut, svAvgPacketsOut;
    
    protected int tclAvgBytesIn, tclAvgPacketsIn;
    protected int tclAvgBytesOut, tclAvgPacketsOut;
    protected int tsvAvgBytesIn, tsvAvgPacketsIn;
    protected int tsvAvgBytesOut, tsvAvgPacketsOut;
    
    
    protected void updateStats() {
        clAvgPacketsIn = tclAvgPacketsIn;
        clAvgPacketsOut = tclAvgPacketsOut;
        svAvgPacketsIn = tsvAvgPacketsIn;
        svAvgPacketsOut = tsvAvgPacketsOut;
        clAvgBytesIn = tclAvgBytesIn;
        clAvgBytesOut = tclAvgBytesOut;
        svAvgBytesIn = tsvAvgBytesIn;
        svAvgBytesOut = tsvAvgBytesOut;
        tclAvgPacketsIn = 0;
        tclAvgPacketsOut = 0;
        tsvAvgPacketsIn = 0;
        tsvAvgPacketsOut = 0;
        tclAvgBytesIn = 0;
        tclAvgBytesOut = 0;
        tsvAvgBytesIn = 0;
        tsvAvgBytesOut = 0;
    }
}
