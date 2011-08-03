package cubetech.Game;

import cubetech.Block;
import cubetech.collision.CubeChunk;
import cubetech.collision.CubeMap;
import cubetech.common.CS;
import cubetech.common.Commands;
import cubetech.common.Common;
import cubetech.common.Common.ErrorCode;
import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.common.ICommand;
import cubetech.common.IDieMethod;
import cubetech.common.Info;
import cubetech.common.MeansOfDeath;
import cubetech.common.Move;
import cubetech.common.Move.MoveType;
import cubetech.common.MoveQuery;
import cubetech.common.PlayerState;
import cubetech.common.items.IItem;
import cubetech.common.items.Weapon;
import cubetech.common.items.WeaponItem;
import cubetech.entities.EntityFlags;
import cubetech.entities.EntityType;
import cubetech.entities.Event;
import cubetech.input.PlayerInput;
import cubetech.misc.Ref;
import cubetech.server.SvFlags;
import cubetech.spatial.SectorQuery;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;


/**
 * this structure is cleared on each ClientSpawn(),
 * except for 'client->pers' and 'client->sess'
 * @author mads
 */
public class GameClient extends Gentity {
    public PlayerState ps = new PlayerState();  // communicated by server to clients

    // the rest of the structure is private to game
    public ClientPersistant pers = new ClientPersistant();
    public int clientIndex = -1;
    public int lastCmdTime;

    // timers
    public int inactivityTime; // for kicking afk players
    public int respawnTime;
    private int timeResidual;

    private GameClient thisIsSilly = this;

    public boolean noclip;
    public boolean godmode;

    void updatePlayerCubes() {
        // Get a list of chunks surrounding the player
        long[] lookups = Ref.cm.cubemap.getVisibleChunks(ps.origin, CubeMap.DEFAULT_GROW_DIST);
        
        for (int i= 0; i < lookups.length; i++) {
            long index = lookups[i];
            CubeChunk chunk = Ref.cm.cubemap.chunks.get(index);

            if(chunk == null) continue;
            
            int version = chunk.version; // server version
            ChunkEntry clEntry = pers.chunkVersions.get(index);

            if(clEntry == null)
            {
                // full transmit of this chunk
                sendFullChunk(index);
            } else if(clEntry.lastSent < version) {
                // partial transmit
                sendPartialChunk(index, clEntry.lastSent);
                // TODO: resend after some time?
            }
            
        }
        ps.pers = pers;
    }

    void sendFullChunk(long index) {
        CubeChunk chunk = Ref.cm.cubemap.chunks.get(index);

        // Create data
        ByteBuffer buf = chunk.createByteBuffer();

        // Queue up
        pers.queueChunkData(buf);

        // Register last sent
        ChunkEntry en = pers.chunkVersions.get(index);
        if(en == null) {
            pers.chunkVersions.put(index, new ChunkEntry(chunk.version));
        } else {
            en.lastSent = chunk.version;
        }
    }

    void sendPartialChunk(long index, int start) {
        CubeChunk chunk = Ref.cm.cubemap.chunks.get(index);
        ChunkEntry en = pers.chunkVersions.get(index);
        
        // Check if we have the needed history data
        if(chunk.version - start >= CubeChunk.NUM_VERSION) {
            // nope -- fallback to full send
            sendFullChunk(index);
            return;
        }

        // Create delta from given start offset
        ByteBuffer buf = chunk.createByteBuffer(start);

        // Queue up
        pers.queueChunkData(buf);

        // register in client history
        if(en == null) {
            pers.chunkVersions.put(index, new ChunkEntry(chunk.version));
        } else {
            en.lastSent = chunk.version;
        }
    }

    /*
    ================
    ClientEvents

    Events will be passed on to the clients for presentation,
    but any server game effects are handled here
    ================
    */
    void HandleEvents(int oldEventSequence) {
        if(oldEventSequence < ps.eventSequence - Common.MAX_PS_EVENTS)
            oldEventSequence = ps.eventSequence - Common.MAX_PS_EVENTS;

        for(int i = oldEventSequence; i < ps.eventSequence; i++) {
            Event event = ps.events[i & (Common.MAX_PS_EVENTS-1)];

            switch(event) {
                case FOOTSTEP:
                case STEP:
                case DIED:
                case HIT_WALL:
                case GOAL:
                case JUMP:
                    break;

                case FIRE_WEAPON:
                    WeaponItem.get(ps.weapon).fireWeapon(this);
                    break;
                case FIRE_WEAPON_ALT:
                    WeaponItem.get(ps.weapon).fireAltWeapon(this);
                    break;

                default:
                    Common.LogDebug("Unhandled GameClient EV event: " + event);
                    break;
            }
            
        }
    }

    public void SetViewAngles(Vector3f angle) {
        int cmdAngle = Helper.Angle2Short(angle.x);
        ps.delta_angles[0] = cmdAngle - pers.cmd.angles[0];
        ps.delta_angles[0] = -16000;
        cmdAngle = Helper.Angle2Short(angle.y);
        ps.delta_angles[1] = cmdAngle - pers.cmd.angles[1];
        cmdAngle = Helper.Angle2Short(angle.z);
        ps.delta_angles[2] = cmdAngle - pers.cmd.angles[2];
        Helper.VectorCopy(angle, s.Angles);
        Helper.VectorCopy(angle, ps.viewangles);
    }

    /**
     * Updates the scoreboard for this client
     */
    public void ScoreboardMessage() {
        StringBuilder str = new StringBuilder();
        int nClients = 0;
        for (int i= 0; i < Ref.game.level.clients.length; i++) {
            GameClient cl = Ref.game.level.clients[i];
            if(cl == null)
                continue;
            if(cl.pers.connected == ClientPersistant.ClientConnected.DISCONNECTED)
                continue;

            int ping = -1;
            if(cl.pers.connected == ClientPersistant.ClientConnected.CONNECTED)
                ping = cl.ps.ping;

            nClients++;
            str.append(String.format(" %d %d %d", i, ping, (Ref.game.level.time - cl.pers.JoinTime)/60000));
        }

        SendServerCommand(String.format("scores %d %s", nClients, str));
    }

    public void SendServerCommand(String str) {
        Ref.server.GameSendServerCommand(s.ClientNum, str);
    }

    // Handle incomming commands from the client
    public void Client_Command(String[] tokens) {
        String cmd = tokens[0].toLowerCase();
        if(cmd.equals("say"))
            cmd_Say(tokens, false);
        else if(cmd.equals("score"))
            cmd_Score();
        else if(cmd.equals("block")) {
            cmd_Block(tokens);
        } else if(cmd.equals("give")) {
            cmd_Give.RunCommand(tokens);
        } else if(cmd.equals("noclip")) {
            cmd_NoClip.RunCommand(tokens);
        } else if(cmd.equals("kill")) {
            cmd_Kill.RunCommand(tokens);
        } else if(cmd.equals("god")) {
            cmd_God.RunCommand(tokens);
        } else
            SendServerCommand("print \"unknown command " + cmd + "\"");
    }
    
    private ICommand cmd_Give = new ICommand() {
        public void RunCommand(String[] args) {
            if(!Ref.game.CheatsOk(thisIsSilly))
                return;

            String name = Commands.Args(args).toLowerCase().trim();
            boolean giveall = name.equals("all");

            if(giveall || name.equals("health")) {
                setHealth(ps.stats.MaxHealth);
                if(!giveall) return;
            }

            // Get a specific item for the player
            if(!giveall) {
                IItem item = Ref.common.items.findItemByClassname(name);
                if(item == null)
                    return;

                Gentity ent = Ref.game.Spawn();
                ent.s.origin.set(r.currentOrigin);
                ent.classname = item.getClassName();
                Ref.game.spawnItem(ent, item);
                Ref.common.items.FinishSpawningItem.think(ent);
                Ref.common.items.TouchItem.touch(ent, thisIsSilly);
                if(ent.inuse)
                    ent.Free();
            }
        }
    };
    
    private ICommand cmd_NoClip = new ICommand() {
        public void RunCommand(String[] args) {
            if(!Ref.game.CheatsOk(thisIsSilly))
                return;

            String msg;
            if(noclip)
                msg = "noclip OFF";
            else
                msg = "noclip ON";
            noclip = !noclip;

            SendServerCommand("print \""+msg+"\"");
        }
    };

    private ICommand cmd_God = new ICommand() {
        public void RunCommand(String[] args) {
            if(!Ref.game.CheatsOk(thisIsSilly))
                return;

            godmode = !godmode;
            String msg;
            if(!godmode)
                msg = "godmode OFF";
            else
                msg = "godmode ON";

            SendServerCommand("print \""+msg+"\"");
        }
    };

    // May kill the player
    public void setHealth(int health) {
//        if(isDead()) {
//            Common.LogDebug("GameClient.setHealth: Can't set health while dead");
//            return;
//        }

        ps.stats.Health = health;
        
    }

    public int getHealth() {
        return ps.stats.Health;
    }

    public int getMaxHealth() {
        return ps.stats.MaxHealth;
    }

    private ICommand cmd_Kill = new ICommand() {
        public void RunCommand(String[] args) {
            if(isDead())
                return;

            godmode = false;
            setHealth(-999);
            player_die.die(thisIsSilly, thisIsSilly, thisIsSilly, 10000, meansOfDeath.SUICIDE);
        }
    };


    private void cmd_Score() {
        ScoreboardMessage();
    }

    private void cmd_Say(String[] tokens, boolean b) {
        String text;
        if(b)
            text = Commands.ArgsFrom(tokens, 0);
        else
            text = Commands.ArgsFrom(tokens, 1);

        Ref.game.Say(this, text);
    }

    // Client want's to modify the map
    private void cmd_Block(String[] tokens) {
        if(!Ref.game.level.editmode) {
            // not in edit mode
            return;
        }

        if(ps.moveType != MoveType.EDITMODE) {
            Common.LogDebug("Player not in EDITMODE :S");
            return;
        }

        if(tokens.length < 2)
            return;

        String bCmd = tokens[1];
        if(bCmd.equalsIgnoreCase("add")) {
            Vector3f position = ps.origin;
            Block b = Ref.cm.cm.AddBlock();
            b.SetCentered(new Vector2f(position.x, position.y), new Vector2f(8, 8));
            Ref.game.SendBlock(b);
        }
    }

    private boolean ClientInactivityTimer() {
//        if(cl.lastCmdTime != 0 && level.time - cl.lastCmdTime > 10000) {
//            if(!cl.pers.LocalClient)
//                Ref.server.DropClient(cl, "Lost connection to player");
//            return false;
//        }
        return true;
    }

    public void ClientUserInfoChanged() {
        String info = Ref.server.GetUserInfo(clientIndex);

        // check for local client
        if(Info.ValueForKey(info, "ip").equals("localhost"))
            pers.LocalClient = true;

        // set name
        String oldname = pers.Name;
        pers.Name = Info.ValueForKey(info, "name");

        if(pers.connected == ClientPersistant.ClientConnected.CONNECTED && !oldname.equals(pers.Name)) {
            Ref.server.GameSendServerCommand(-1, String.format("print \"%s changed name to %s\n\"", oldname, pers.Name));
        }

        String model = Info.ValueForKey(info, "model");

        String str = String.format("n\\%s\\model\\%s",pers.Name,model);
        Ref.server.SetConfigString(CS.CS_PLAYERS+clientIndex, str);
    }

    public void Begin() {
        if(r.linked)
            Unlink();

        Init(s.ClientNum);

        // locate ent at a spawn point
        ClientSpawn();

        pers.connected = ClientPersistant.ClientConnected.CONNECTED;
        pers.JoinTime = Ref.game.level.time;
        
        // save eflags around this, because changing teams will
        // cause this to happen with a valid entity, and we
        // want to make sure the teleport bit is set right
        // so the viewpoint doesn't interpolate through the
        // world to the new position
        int entFlags = ps.eFlags;
        ps = new PlayerState();
        ps.eFlags = entFlags;
        ps.clientNum = s.ClientNum;

        Ref.server.LocateGameData(Ref.game.level.sentities, Ref.game.level.num_entities, Ref.game.level.clients);
        Ref.server.GameSendServerCommand(-1, String.format("print \"%s entered the game.\"\n", pers.Name));
        Ref.game.CalculateRanks();
    }

    /**
     * Send chat message to a specific client
     * @param target
     * @param sourceName
     * @param text
     */
    public void SayTo(String sourceName, String text) {
        if(!inuse ||  pers.connected != ClientPersistant.ClientConnected.CONNECTED)
            return;

//        System.out.println("Sending chat to: " + clientIndex);

        Ref.server.GameSendServerCommand(clientIndex, String.format("chat \"%s^0: %s\"\n", sourceName, text));
    }

    private void ClientSpawn() {
        int index = s.ClientNum;
        Common.LogDebug("Spawning : " + pers.Name);
        // toggle the teleport bit so the client knows to not lerp
        // and never clear the voted flag
        int flags = ps.eFlags & EntityFlags.TELEPORT_BIT;
        flags ^= EntityFlags.TELEPORT_BIT;

        // clear everything but the persistant data
        ClientPersistant perss = pers;
        int ping = ps.ping;
        Clear();
        pers = perss;
        ps.ping = ping;
        ps.eFlags = flags;
        inuse = true;
        classname = "player";
        r.mins = new Vector3f(Game.PlayerMins);
        r.maxs = new Vector3f(Game.PlayerMaxs);
        r.contents = Content.BODY;
        die = player_die;
        ClipMask = Content.PLAYERCLIP | Content.BODY;
        ps.clientNum = index;

        setHealth(getMaxHealth());

        // Set spawn
        Gentity spawnPoint = selectSpawnPoint(ps.origin);
        if(spawnPoint != null) {
            SetOrigin(new Vector3f(spawnPoint.s.origin));
            ps.origin = new Vector3f(spawnPoint.s.origin);
        }
        
        pers.cmd = Ref.server.GetUserCommand(index);
        SetViewAngles(new Vector3f(90, 180, 0));
        Link();
        respawnTime = Ref.game.level.time;

        // fire the targets of the spawn point
        if(spawnPoint != null)
            spawnPoint.UseTargets(this);

        // run a client frame to drop exactly to the floor,
        // initialize animations and other things
        ps.commandTime = Ref.game.level.time - 100;
        pers.cmd.serverTime = Ref.game.level.time;
        Client_Think();

        // positively link the client, even if the command times are weird
        ps.ToEntityState(s, false);
        r.currentOrigin.set(ps.origin);
        Link();

        ClientEndFrame();
        ps.ToEntityState(s, false);
        //System.out.println(""+s.ClientNum);
    }

    // Prepares the EntityState structure and sends predictable events to
    // other clients
    public void ClientEndFrame() {
        ps.ToEntityState(s, false);
        ps.SendPendingPredictableEvents();
        if(!ClientInactivityTimer())
            return;
    }

    public void Client_Think() {
        pers.cmd = Ref.server.GetUserCommand(s.ClientNum);
        lastCmdTime = Ref.game.level.time;

        // don't think if the client is not yet connected (and thus not yet spawned in)
        if(pers.connected != ClientPersistant.ClientConnected.CONNECTED)
            return;

        PlayerInput cmd = pers.cmd;
        // sanity check the command time to prevent speedup cheating
        if(cmd.serverTime > Ref.game.level.time + 200)
            cmd.serverTime = Ref.game.level.time + 200;
        if(cmd.serverTime < Ref.game.level.time - 1000)
            cmd.serverTime = Ref.game.level.time -1000;

        int msec = cmd.serverTime - ps.commandTime;
        if(msec < 1)
            return;
        if(msec > 200)
            msec = 200;

        ps.moveType = MoveType.NORMAL;
        if(noclip)
            ps.moveType = MoveType.NOCLIP;
        else if(Ref.game.level.editmode)
            ps.moveType = MoveType.EDITMODE;
        else if(isDead())
            ps.moveType = MoveType.DEAD;

//        if(ps.moveType == MoveType.NORMAL)
//            ps.applyPull = true;

        int oldEventSequence = ps.eventSequence;

        // Prepare for move
        MoveQuery move = new MoveQuery(Ref.server);
        move.ps = ps;
        move.cmd = cmd;
        move.tracemask = Content.MASK_PLAYERSOLID;

        // Do the move
        Move.Move(move);

        // save results of pmove
        if(ps.entityEventSequence != oldEventSequence)
            eventTime = Ref.game.level.time;

        ps.ToEntityState(s, false);

        ps.SendPendingPredictableEvents();

        // use the snapped origin for linking so it matches client predicted versions
        r.currentOrigin.set(s.pos.base);
        r.mins = new Vector3f(ps.ducked? Game.PlayerDuckedMins: Game.PlayerMins);
        r.maxs = new Vector3f(ps.ducked? Game.PlayerDuckedMaxs:Game.PlayerMaxs);

        // execute client events
        HandleEvents(oldEventSequence);

        // link entity now, after any personal teleporters have been used
        Link();
//        if(!noclip)
//            TouchTriggers();

        // NOTE: now copy the exact origin over otherwise clients can be snapped into solid
        r.currentOrigin.set(ps.origin);

        // save results of triggers and client events
        if(ps.eventSequence != oldEventSequence)
            eventTime = Ref.game.level.time;

        updatePlayerCubes();

        // Check for respawning
        if(isDead()) {
            if(cmd.Mouse1 && Ref.game.level.time > respawnTime) {
                respawn();
            }
            return;
        }

        ClientTimerActions(msec);

        // Check for death
        if(ps.origin.z < Ref.game.g_killheight.iValue) {
            Die();
        }
    }

    

    private IDieMethod player_die = new IDieMethod() {
        public void die(Gentity self, Gentity inflictor, Gentity attacker, int dmg, MeansOfDeath mod) {
            if(ps.moveType == MoveType.DEAD) return;

            ps.moveType = MoveType.DEAD;

            int killer;
            String killerName = "<non-client>";
            if(attacker != null) {
                killer = attacker.s.ClientNum;
                if(attacker.isClient()) {
                    killerName = attacker.getClient().pers.Name;
                }
            } else {
                killer = Common.ENTITYNUM_WORLD;
                killerName = "<world>";
            }

            Common.Log("Kill: %d %d: %s killed %s by %s", killer, self.s.ClientNum, killerName, pers.Name, mod.toString());

            // broadcast the death event to everyone
            Gentity ent = Ref.game.TempEntity(self.r.currentOrigin, Event.ORBITUARY.ordinal());
            ent.s.evtParams = mod.ordinal();
            ent.s.otherEntityNum = self.s.ClientNum;
            ent.s.frame = killer;
            ent.r.svFlags.add(SvFlags.BROADCAST);

            self.s.weapon = Weapon.NONE;
            self.r.contents = Content.CORPSE;
            respawnTime = Ref.game.level.time + 1000;

            Ref.game.AddEvent(self, Event.DIED, killer);

            Ref.server.LinkEntity(self.shEnt);
        }


    };

    public void Die() {
 
        ps.AddPredictableEvent(Event.DIED, 0);
        ps.velocity.set(0,0,0);
        if(!isDead())
            ps.stats.Health = 0;
    }

    public boolean isDead() {
        return ps.stats.Health <= 0;
    }


    
    private void ClientTimerActions(int msec) {
        timeResidual += msec;
        while(timeResidual >= 1000) {
            timeResidual -= 1000;

            Ref.cm.cubemap.growFromPosition(ps.origin, CubeMap.DEFAULT_GROW_DIST);

            if(getHealth() > getMaxHealth())
                ps.stats.Health--;

            if(!isDead())
                ps.maptime += 1;

            for (int i= 0; i < ps.powerups.length; i++) {
                if(ps.powerups[i] > 0)
                    ps.powerups[i] -= 1000;
                if(ps.powerups[i] < 0)
                    ps.powerups[i] = 0;
            }
        }
    }

//    private void TouchTriggers() {
//        // dead clients don't activate triggers!
//        if(isDead())
//            return;
//
//        Vector2f range = new Vector2f(40,40);
//        Vector2f mins = new Vector2f(ps.origin);
//        Vector2f.sub(mins, range, mins);
//        Vector2f maxs = new Vector2f(ps.origin);
//        Vector2f.add(maxs, range, maxs);
//
//        SectorQuery query = Ref.server.EntitiesInBox(mins, maxs);
//
//        // can't use ent->absmin, because that has a one unit pad
//        mins.x = ps.origin.x + r.mins.x;
//        mins.y = ps.origin.y + r.mins.y;
//        maxs.x = ps.origin.x + r.maxs.x;
//        maxs.y = ps.origin.y + r.maxs.y;
//
//        for (int index : query.List) {
//            Gentity hit = Ref.game.g_entities[index];
//
//            if(hit.touch == null && touch == null)
//                continue;
//
//            if((hit.r.contents & Content.TRIGGER) == 0)
//                continue;
//
//            // use seperate code for determining if an item is picked up
//            // so you don't have to actually contact its bounding box
//            if(hit.s.eType == EntityType.ITEM) {
//                if(!Ref.common.items.playerTouchesItem(ps, hit.s, Ref.game.level.time))
//                    continue;
//            } else {
//                if(!Ref.server.EntityContact(mins, maxs, hit.shEnt))
//                    continue;
//            }
//
//            if(hit.touch != null)
//                hit.touch.touch(hit, this);
//
//
//        }
//    }

    public void respawn() {
        
        // TODO: Spawn effect and maybe bodyque
        ClientSpawn();
        Ref.game.respawnAllItems();
        
        ps.maptime = 0;
    }

    public String Client_Connect(int id, boolean firsttime) {
        Clear();

        if(id != s.ClientNum)
            Ref.common.Error(ErrorCode.DROP, "Client_Connect: YOU DERPED UP");
        
        clientIndex = id;
        pers.connected = ClientPersistant.ClientConnected.CONNECTING;
        if(firsttime) {
            // InitSessionData
        }
        ClientUserInfoChanged();
        // don't do the "xxx connected" messages if they were caried over from previous level
        if(firsttime) {
            Ref.server.GameSendServerCommand(-1, String.format("print \"%s connected.\"\n", pers.Name));
        }

        Ref.game.CalculateRanks();

        return null;
    }

    @Override
    public void Clear() {
        // The super class is only cleared when Free is called,
        // and players should never be free'd, so don't call it here.
        ////// super.Clear();
        ps.Clear();
        pers = new ClientPersistant();
        lastCmdTime = 0;
        inactivityTime = 0;
        respawnTime = 0;
    }

    void Client_Disconnect() {
        Unlink();
        inuse = false;
        classname = "disconnected";
        pers.connected = ClientPersistant.ClientConnected.DISCONNECTED;

        Ref.server.SetConfigString(CS.CS_PLAYERS+s.ClientNum, null);

        Ref.game.CalculateRanks();
    }

    void PlaceInEditMode() {
        if(isDead())
            ClientSpawn();
        ps.moveType = MoveType.NORMAL;
    }

    void RemoveFromEditMode() {
        ps.moveType = MoveType.EDITMODE;
    }

    private Gentity selectSpawnPoint(Vector3f origin) {
        Gentity spot = null;
        ArrayList<Gentity> spots = new ArrayList<Gentity>();

        while((spot = Ref.game.Find(spot, null, "info_player_spawn")) != null) {
            spots.add(spot);
        }

        Vector2f spawn = null;

        if(spots.isEmpty()) {
            Common.LogDebug("Warning: No spawn points found.");
        } else {
            spot = spots.get(Ref.rnd.nextInt(spots.size()));
        }

        return spot;
    }

    public Vector3f getForwardVector() {
        Vector3f fw = new Vector3f();
        Helper.AngleVectors(ps.viewangles, fw, null, null);
        return fw;
    }
}
