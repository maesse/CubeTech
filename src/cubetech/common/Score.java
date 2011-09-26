package cubetech.common;

/**
 *
 * @author mads
 */
public class Score {
    public int client;
    public int ping;
    public int time;
    public int score;
    public boolean isDead;
    public boolean leaved = true; // start out not connected

    public Score(int i) {
        client = i;
    }
}
