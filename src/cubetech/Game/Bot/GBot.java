package cubetech.Game.Bot;

import cubetech.Game.GameClient;
import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.common.ICommand;
import cubetech.common.Info;
import cubetech.input.PlayerInput;
import cubetech.misc.Ref;
import cubetech.server.SvClient;
import cubetech.server.SvFlags;
import java.util.HashMap;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class GBot {
    static HashMap<Integer, Bot> bots = new HashMap<Integer, Bot>();

    static ICommand cmd_addbot = new ICommand() {
        public void RunCommand(String[] args) {
            if(args.length < 2) {
                Common.Log("usage: addbot <botname>");
                return;
            }

            String name = args[1];
            addBot(name);
        }
    };

    public static void init() {
        Ref.commands.AddCommand("addbot", cmd_addbot);
        bots.clear();
    }

    static void addBot(String name) {
        String userinfo = "";
        userinfo = Info.SetValueForKey(userinfo, "name", name);
        userinfo = Info.SetValueForKey(userinfo, "rate", "25000");
        userinfo = Info.SetValueForKey(userinfo, "cl_updaterate", "20");
        userinfo = Info.SetValueForKey(userinfo, "model", "cubeguyTextured");

        // have the server allocate a client slot
        int clientNum = Ref.server.allocateBotClient();
        if(clientNum == -1) {
            Common.Log("Unable to add bot. All slots in use");
            return;
        }        
        
        SvClient client = Ref.server.getClient(clientNum);
        client.SetUserInfo(userinfo);
        
        client.ClientThink(new PlayerInput());

        GameClient bot = (GameClient) Ref.game.g_entities[clientNum];
        bot.r.svFlags.add(SvFlags.BOT);
        bot.inuse = true;
        // have it connect to the game as a normal client
        String error = bot.Client_Connect(clientNum, true, true);
        if(error != null) {
            Common.Log("Bot couldn't connect: " + error);
            Ref.server.freeBotClient(client);
            return;
        }
        client.UserInfoChanged(true);
        bot.Begin();
        
        Bot botInstance = new Bot(bot);
        bots.put(bot.clientIndex, botInstance);
    }

    public static void removeBot(int clientIndex) {
        Bot removed = bots.remove(clientIndex);
        if(removed == null) {
            Common.Log("[GBot] Warning: Couldn't find bot for removal");
        }
    }

    public static void runBotFrame(int time, GameClient gameClient) {
        Bot bot = bots.get(gameClient.clientIndex);
        PlayerInput cmd = bot.runFrame();
        cmd.serverTime = time;
        Ref.server.getClient(gameClient.clientIndex).ClientThink(cmd);

        
        
        
    }
}
