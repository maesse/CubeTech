package cubetech.Game;

import cubetech.collision.CollisionResult;
import cubetech.collision.CubeChunk;
import cubetech.collision.CubeMap;
import cubetech.common.*;
import cubetech.common.Common.ErrorCode;
import cubetech.common.Move.MoveType;
import cubetech.common.items.IItem;
import cubetech.common.items.Weapon;
import cubetech.common.items.WeaponItem;
import cubetech.common.items.WeaponState;
import cubetech.entities.EntityFlags;
import cubetech.entities.EntityType;
import cubetech.entities.Event;
import cubetech.entities.Vehicle;
import cubetech.input.PlayerInput;
import cubetech.iqm.IQMAnim;
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

    public int extraWeaponTime;

    // timers
    public int inactivityTime; // for kicking afk players
    public int respawnTime;
    private int timeResidual;

    private GameClient thisIsSilly = this;

    public boolean noclip;
    public boolean godmode;

    private static long[] lookupCache = null;

    ScoreList scores = new ScoreList();

    // This is set if currently in a vehicle
    Vehicle vehicle = null;

    void updatePlayerCubes() {
        if(r.svFlags.contains(SvFlags.BOT)) return;
        // Get a list of chunks surrounding the player
        lookupCache = Ref.cm.cubemap.getVisibleChunks(ps.origin, CubeMap.DEFAULT_GROW_DISTXY, CubeMap.DEFAULT_GROW_DISTZ, lookupCache);
        int nFullUpdates = 0;
        
        for (int i= 0; i < lookupCache.length; i++) {
            long index = lookupCache[i];
            CubeChunk chunk = (CubeChunk)Ref.cm.cubemap.chunks.get(index);

            if(chunk == null) continue;
            
            int version = chunk.version; // server version
            ChunkEntry clEntry = (ChunkEntry)pers.chunkVersions.get(index);

            if(clEntry == null)
            {
                // full transmit of this chunk
                sendFullChunk(index);
                nFullUpdates++;
            } else if(clEntry.lastSent < version) {
                // partial transmit
                sendPartialChunk(index, clEntry.lastSent);
                // TODO: resend after some time?
            }
        }
        ps.pers = pers;
    }

    void sendFullChunk(long index) {
        CubeChunk chunk = (CubeChunk)Ref.cm.cubemap.chunks.get(index);

        // Create data
        ByteBuffer buf = chunk.createByteBuffer();

        // Queue up
        pers.queueChunkData(buf);

        // Register last sent
        ChunkEntry en = (ChunkEntry)pers.chunkVersions.get(index);
        if(en == null) {
            pers.chunkVersions.put(index, new ChunkEntry(chunk.version));
        } else {
            en.lastSent = chunk.version;
        }
    }

    void sendPartialChunk(long index, int start) {
        CubeChunk chunk = (CubeChunk)Ref.cm.cubemap.chunks.get(index);
        ChunkEntry en = (ChunkEntry)pers.chunkVersions.get(index);
        
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
                case CHANGE_WEAPON:
                case NO_AMMO:
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
        String scoreCommand = scores.createUpdate(Ref.game.level.clients);
        if(scoreCommand == null) return;
        SendServerCommand(String.format("scores %s", scoreCommand));
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
        else if(cmd.equals("give")) {
            cmd_Give.RunCommand(tokens);
        } else if(cmd.equals("noclip")) {
            cmd_NoClip.RunCommand(tokens);
        } else if(cmd.equals("kill")) {
            cmd_Kill.RunCommand(tokens);
        } else if(cmd.equals("god")) {
            cmd_God.RunCommand(tokens);
        } else if(cmd.equals("dropweapon")) {
            tossWeapon(ps.weapon);
        }  else if(cmd.equals("use")) {
            playerUse();
        } else
            SendServerCommand("print \"unknown command " + cmd + "\"");
    }
    
    private ICommand cmd_Give = new ICommand() {
        public void RunCommand(String[] args) {
            if(!Ref.game.CheatsOk(thisIsSilly))
                return;

            String name = Commands.Args(args).toLowerCase().trim();

            if(name.equals("health")) {
                setHealth(ps.stats.MaxHealth);
                return;
            }

            // Get a specific item for the player
            IItem item = Ref.common.items.findItemByClassname(name);
            if(item != null) {
                Gentity ent = Ref.game.Spawn();
                ent.s.origin.set(r.currentOrigin);
                ent.classname = item.getClassName();
                Ref.game.spawnItem(ent, item);
                Ref.common.items.FinishSpawningItem.think(ent);
                Ref.common.items.TouchItem.touch(ent, thisIsSilly);
                if(ent.inuse) ent.Free();
            } else if(name.equalsIgnoreCase("car")) {
                Vehicle ent = (Vehicle) Ref.game.Spawn(Vehicle.class);
                
                // Get an x/y direciton vector
                Vector3f forward = getForwardVector();
                forward.z = 0f;
                float len = Helper.Normalize(forward);
                if(len == 0) forward.x = 1f;

                Helper.VectorMA(r.currentOrigin, 200, forward, ent.r.currentOrigin);
                ent.s.pos.type = Trajectory.STATIONARY;
                ent.s.pos.base.set(ent.r.currentOrigin);
                ent.initVehicle();
                ent.Link();
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
        Vector3f spawnPoint = selectSpawnPoint();
        if(spawnPoint != null) {
            SetOrigin(new Vector3f(spawnPoint));
            ps.origin = new Vector3f(spawnPoint);
        }
        
        pers.cmd = Ref.server.GetUserCommand(index);
        SetViewAngles(new Vector3f(90, 180, 0));
        Link();
        respawnTime = Ref.game.level.time;

        // fire the targets of the spawn point
//        if(spawnPoint != null)
//            spawnPoint.UseTargets(this);

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
        Ref.game.level.physics.PlayerSpawn(ps);
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
        if(pers.connected != ClientPersistant.ClientConnected.CONNECTED) return;

        PlayerInput cmd = pers.cmd;
        // sanity check the command time to prevent speedup cheating
        if(cmd.serverTime > Ref.game.level.time + 200)
            cmd.serverTime = Ref.game.level.time + 200;
        if(cmd.serverTime < Ref.game.level.time - 1000)
            cmd.serverTime = Ref.game.level.time -1000;
        int msec = cmd.serverTime - ps.commandTime;
        if(msec < 1) return;
        if(msec > 200) msec = 200;

        // prethink
//        playerUse();

        int oldEventSequence = ps.eventSequence;

        // Prepare for move
        MoveQuery move = new MoveQuery(Ref.server);
        move.ps = ps;
        move.cmd = cmd;
        move.tracemask = Content.MASK_PLAYERSOLID;

        if(vehicle == null) {
            // Do the move
            ps.moveType = MoveType.NORMAL;
            if(noclip) ps.moveType = MoveType.NOCLIP;
            else if(isDead()) ps.moveType = MoveType.DEAD;
            Move.Move(move);
        } else {
            vehicle.processMove(move);
        }

        System.arraycopy(cmd.buttons, 0, ps.oldButtons, 0, ps.oldButtons.length);

        // save results of pmove
        if(ps.entityEventSequence != oldEventSequence) eventTime = Ref.game.level.time;

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
        if(!noclip) touchTriggers();
            

        // NOTE: now copy the exact origin over otherwise clients can be snapped into solid
        if(!Helper.Equals(r.currentOrigin, ps.origin)) {
            int test = 2;
        }
        r.currentOrigin.set(ps.origin);

        // save results of triggers and client events
        if(ps.eventSequence != oldEventSequence)
            eventTime = Ref.game.level.time;

        if(!r.svFlags.contains(SvFlags.BOT)) {
            updatePlayerCubes();
            String update = scores.createUpdate(Ref.game.level.clients);
            if(update != null) {
                SendServerCommand("scores " + update);
            }
        }
        

        // Check for respawning
        if(isDead()) {
            if(cmd.Mouse1 && Ref.game.level.time > respawnTime) {
                respawn();
            }
            return;
        }
        
        Ref.game.level.physics.PlayerUpdatePosition(ps);

        ClientTimerActions(msec);

        // Check for death
        if(ps.origin.z < Ref.game.g_killheight.iValue) {
            Die();
        }
    }

    private void playerUse() {
        if(isDead()) return;
        if(vehicle != null) {
            // Send to vehicle if in one
            vehicle.use.use(vehicle, this, this);
        } else {
            // Try to locate an entity
            Gentity useEntity = findUseEntity();
            if(useEntity != null && useEntity.use != null) {
                useEntity.use.use(useEntity, this, this);
            }
        }
        Gentity sound = Ref.game.TempEntity(ps.origin, Event.GENERAL_SOUND.ordinal());
        int soundIndex = Ref.server.registerSound("data/sounds/ammoclick.wav");
        sound.s.evtParams = soundIndex;
    }

    // Trace out a line in the look direction and try to grab a useable entity
    private Gentity findUseEntity() {
        int mask = Content.SOLID |Content.TRIGGER | Content.PHYSICS;

        Vector3f eye = new Vector3f(ps.origin);
        eye.z += ps.viewheight;

        Vector3f end = getForwardVector();
        Helper.VectorMA(eye, 200f, end, end);

        CollisionResult res = Ref.server.Trace(eye, end, null, null, mask, clientIndex);
        if(res.hit && res.entitynum != Common.ENTITYNUM_NONE) {
            return Ref.game.g_entities[res.entitynum];
        }
        return null;
    }

    private static Vector3f touchRange  = new Vector3f(40,40,52);
    private void touchTriggers() {
        if(ps.stats.Health <= 0) return;
        Vector3f mins = Vector3f.sub(ps.origin, touchRange, null);
        Vector3f maxs = Vector3f.add(ps.origin, touchRange, null);

        SectorQuery q = Ref.server.EntitiesInBox(mins, maxs);

        // can't use ent->absmin, because that has a one unit pad
        Vector3f.add(ps.origin, r.mins, mins);
        Vector3f.add(ps.origin, r.maxs, maxs);

        for (Integer entNum : q.List) {
            Gentity hit = Ref.game.g_entities[entNum];
            if(hit.touch == null) continue;

            if((hit.r.contents & Content.TRIGGER) == 0) continue;

            // use seperate code for determining if an item is picked up
            // so you don't have to actually contact its bounding box
            if(hit.s.eType == EntityType.ITEM) {
                if(!Ref.common.items.playerTouchesItem(ps, hit.s, Ref.game.level.time)) {
                    continue;
                }
            } else {
                if(!Ref.server.EntityContact(mins, maxs, hit.shEnt)) {
                    continue;
                }
            }

            hit.touch.touch(hit, this);
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

            tossClientItems();

            self.s.weapon = Weapon.NONE;
            self.r.contents = Content.CORPSE;
            respawnTime = Ref.game.level.time + 1000;

            ps.animation = ((ps.animation & 128) ^ 128) | Animations.DIE.ordinal();

            Ref.game.AddEvent(self, Event.DIED, killer);
            
            

            Ref.server.LinkEntity(self.shEnt);
            Ref.game.level.physics.PlayerDie(ps);
        }


    };

    private void tossClientItems() {
        Weapon[] weaponList = ps.stats.getWeapons();
        for (Weapon w : weaponList) {
            tossWeapon(w);
        }
    }

    private void tossWeapon(Weapon w) {
        if(!ps.stats.hasWeapon(w)) return;
        if(w == Weapon.NONE) return;
        WeaponItem wi = Ref.common.items.getWeapon(w);
        Ref.common.items.dropItem(this, wi, 0, ps.stats.getAmmo(w));
        ps.stats.removeWeapon(w);
        ps.weapon = ps.stats.getWeaponNearest(w, false);
        ps.weaponState = WeaponState.RAISING;
        ps.weaponTime = Ref.common.items.getWeapon(w).getRaiseTime();
    }

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
        extraWeaponTime -= msec;
        if(extraWeaponTime < 0) extraWeaponTime = 0;
        while(timeResidual >= 1000) {
            timeResidual -= 1000;

            Ref.cm.cubemap.growFromPosition(ps.origin, CubeMap.DEFAULT_GROW_DISTXY, CubeMap.DEFAULT_GROW_DISTZ);

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

    public void respawn() {
        // TODO: Spawn effect and maybe bodyque
        ClientSpawn();
        
        ps.maptime = 0;
    }

    public String Client_Connect(int id, boolean firsttime, boolean isBot) {
        Clear();

        if(id != s.ClientNum)
            Ref.common.Error(ErrorCode.DROP, "Client_Connect: YOU DERPED UP");
        
        clientIndex = id;
        pers.connected = ClientPersistant.ClientConnected.CONNECTING;

        if(isBot) {
            r.svFlags.add(SvFlags.BOT);
            inuse = true;
            
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
        Ref.game.level.physics.PlayerLeave(ps);
    }

    private Vector3f selectSpawnPoint() {
        Gentity spot = null;
        ArrayList<Gentity> spots = new ArrayList<Gentity>();

        while((spot = Ref.game.Find(spot, null, "info_player_spawn")) != null) {
            spots.add(spot);
        }

        if(spots.isEmpty()) {
            Common.LogDebug("Warning: No spawn points found.");
            return new Vector3f(0, 0, 0);
        } else {
            spot = spots.get(Ref.rnd.nextInt(spots.size()));
        }

        boolean spotOk = false;
        int i = 0;
        Vector3f origin = new Vector3f(spot.s.origin);
        while(i < 10 && !spotOk) {
            CollisionResult res = Ref.server.Trace(origin, origin, Game.PlayerMins, Game.PlayerMaxs, -1, spot.s.ClientNum);
            if(res.frac == 1f && !res.startsolid) {
                spotOk = true;
                break;
            }
            origin.z += Game.PlayerMaxs.z - Game.PlayerMins.z;
            i++;
        }

        if(!spotOk) {
            // Cant find a non-solid spawn
            origin.set(spot.s.origin);
        }

        return origin;
    }

    public Vector3f getForwardVector() {
        Vector3f fw = new Vector3f();
        Helper.AngleVectors(ps.viewangles, fw, null, null);
        return fw;
    }

    public void leaveVehicle() {
        if(vehicle == null) return;

        // Remove self from vehicle
        ps.vehicle = 0;
        vehicle.setPassenger(null);
        ps.origin.set(vehicle.r.currentOrigin);
        ps.origin.z += 200;
        Link();
        this.vehicle = null;
        
        ps.eFlags &= ~EntityFlags.NODRAW;
        ps.moveType = MoveType.NORMAL;
        if(ps.stats.Health > 0) {
            Ref.game.level.physics.PlayerSpawn(ps);
        }
        
    }

    public boolean getInVehicle(Vehicle vehicle) {
        if(vehicle.getPassenger() != null) return false;

        ps.eFlags |= EntityFlags.NODRAW;
        ps.velocity.set(0, 0, 0);
        ps.moveType = MoveType.NOCLIP;
        ps.vehicle = vehicle.s.ClientNum;
        Ref.game.level.physics.PlayerDie(ps);

        ps.ducking = false;
        ps.ducked = false;
        ps.viewheight = (int) Game.PlayerViewHeight;
        ps.ducktime = 0;
        ps.jumpTime = 0;

        vehicle.setPassenger(this);
        this.vehicle = vehicle;
        
        
        // todo: setParent(vehicle)

        return true;
    }

    
}
