package cubetech.common;

import cubetech.Game.ClientPersistant;
import cubetech.Game.GameClient;
import cubetech.misc.Ref;
import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author mads
 */
public class ScoreList {
    public Score[] scores = new Score[64];
    private int lastUpdateTime;

    public ScoreList() {
        for (int i= 0; i < scores.length; i++) {
            scores[i] = new Score(i);
        }
    }

    public Score[] getValidScores(boolean sort) {
        Score[] dest = new Score[scores.length];
        int count = 0;
        for (int i= 0; i < scores.length; i++) {
            if(scores[i].leaved) continue;
            
            dest[count] = scores[i];
            count++;
        }

        if(sort) {
            Arrays.sort(dest, 0, count, scoreComparator);
        }

        Score[] resized = new Score[count];
        System.arraycopy(dest, 0, resized, 0, count);
        return resized;
    }

    private static Comparator<Score> scoreComparator = new Comparator<Score>() {
        public int compare(Score o1, Score o2) {
            if(o1.score == o2.score) return 0;
            if(o1.score > o2.score) return 1;
            return -1;
        }
    };

    public void parseScores(String[] tokens) {
        int count = Integer.parseInt(tokens[1]);
        for (int i= 0; i < count; i++) {
            // parse
            int client = Integer.parseInt(tokens[4 * i + 2]);
            int ping = Integer.parseInt(tokens[4 * i + 3]);
            int score = Integer.parseInt(tokens[4 * i + 4]);
            int flags = Integer.parseInt(tokens[4 * i + 5]);

            boolean isDead = (flags & 1) == 1;
            boolean isLeaved = (flags & 2) == 2;

            Score s = scores[client];
            s.ping = ping;
            s.score = score;
            s.isDead = isDead;
            s.leaved = isLeaved;
        }
    }

    public String createUpdate(GameClient[] clients) {
        int time = Ref.common.frametime;
//        if(time < lastUpdateTime + 200) return null;
        
        
        StringBuilder str = new StringBuilder();
        int nClients = 0;
        for (int i= 0; i < clients.length; i++) {
            GameClient cl = clients[i];
            Score s = scores[i];
            boolean leaved = false;
            if(cl == null)
                continue;
            if(cl.pers.connected == ClientPersistant.ClientConnected.DISCONNECTED) {
                if(s.leaved) continue; // already registered
                leaved = true;
            }

            int ping = -2; // -2 = not connected, -1 is for bots
            if(cl.pers.connected == ClientPersistant.ClientConnected.CONNECTED)
                ping = cl.ps.ping;
            int score = cl.pers.score;
            boolean isDead = cl.isDead();

            // Check if score-entry has changed
            if(s.isDead == isDead && s.score == score && s.ping == ping && s.leaved == leaved) continue; // nope
            if(s.isDead == isDead && s.score == score && s.leaved == leaved && lastUpdateTime + 1000 > time) continue;
            s.isDead = isDead;
            s.score = score;
            s.ping = ping;
            s.leaved = leaved;
            int flags = (isDead?1:0) | (leaved?2:0);
            
            str.append(String.format(" %d %d %d %d", i, ping, score, flags));
            nClients++;
        }

        lastUpdateTime = time;
        if(nClients == 0) return null;

        return nClients + " " + str.toString();
    }
}
