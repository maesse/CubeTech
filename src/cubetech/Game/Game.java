package cubetech.Game;

import cubetech.Game.Bot.GBot;
import cubetech.Block;
import cubetech.collision.CubeChunk;
import cubetech.common.CS;
import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Commands.ExecType;
import cubetech.common.Common;
import cubetech.common.DamageFlag;
import cubetech.common.Helper;
import cubetech.common.ICommand;
import cubetech.common.MeansOfDeath;
import cubetech.common.items.IItem;
import cubetech.entities.EntityType;
import cubetech.entities.Event;
import cubetech.entities.Func_Door;
import cubetech.entities.IEntity;
import cubetech.entities.SharedEntity;
import cubetech.entities.Info_Player_Spawn;
import cubetech.entities.Missiles;
import cubetech.input.PlayerInput;
import cubetech.misc.Ref;
import cubetech.server.SvFlags;
import cubetech.spatial.SectorQuery;
import java.util.EnumSet;
import java.util.HashMap;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class Game {
    public static final Vector3f PlayerMins = new Vector3f(-30,-30,-46);
    public static final Vector3f PlayerMaxs = new Vector3f(30,30,40);
    public static final float PlayerViewHeight = 26; // from center
    public static final Vector3f PlayerDuckedMins = new Vector3f(-30,-30,-46);
    public static final Vector3f PlayerDuckedMaxs = new Vector3f(30,30,0);
    public static final float PlayerDuckedHeight = -7; // from center
    CVar sv_speed;
    CVar sv_gravity;
    CVar sv_jumpmsec;
    CVar sv_jumpvel;
    CVar g_cheats;
    CVar g_gametype;
    CVar g_restarted;
    CVar g_editmode;
    CVar g_maxclients;
    CVar sv_pullacceleration;
    CVar sv_airaccelerate;
    CVar sv_acceleration;
    CVar sv_friction;
    CVar sv_stopspeed;
    CVar sv_stepheight;
    CVar sv_movemode;
    CVar g_killheight;
    CVar g_knockback;

    public SpawnEntities spawnEntities;
    public Gentity[] g_entities;
    GameClient[] g_clients;
    public LevelLocal level;
    public HashMap<String, IEntity> spawns = new HashMap<String, IEntity>();

    public Game() {
        sv_speed = Ref.cvars.Get("sv_speed", "200", EnumSet.of(CVarFlags.SERVER_INFO, CVarFlags.USER_INFO, CVarFlags.ARCHIVE));
        g_cheats = Ref.cvars.Get("g_cheats", "0", EnumSet.of(CVarFlags.NONE));
        g_gametype = Ref.cvars.Get("g_gametype", "0", EnumSet.of(CVarFlags.SERVER_INFO, CVarFlags.LATCH, CVarFlags.USER_INFO));
        g_restarted = Ref.cvars.Get("g_restarted", "0", EnumSet.of(CVarFlags.ROM));
        g_maxclients = Ref.cvars.Get("g_maxclients", "32", EnumSet.of(CVarFlags.SERVER_INFO, CVarFlags.LATCH, CVarFlags.USER_INFO));
        g_editmode = Ref.cvars.Get("g_editmode", "0", EnumSet.of(CVarFlags.SERVER_INFO, CVarFlags.USER_INFO));
        g_killheight = Ref.cvars.Get("g_killheight", "-3000", EnumSet.of(CVarFlags.NONE, CVarFlags.ARCHIVE));

        
        sv_movemode = Ref.cvars.Get("sv_movemode", "1", EnumSet.of(CVarFlags.SERVER_INFO, CVarFlags.USER_INFO));
        sv_gravity = Ref.cvars.Get("sv_gravity", "800", EnumSet.of(CVarFlags.SERVER_INFO, CVarFlags.USER_INFO, CVarFlags.ARCHIVE));
        sv_jumpmsec = Ref.cvars.Get("sv_jumpmsec", "200", EnumSet.of(CVarFlags.SERVER_INFO, CVarFlags.USER_INFO, CVarFlags.ARCHIVE));
        sv_jumpvel = Ref.cvars.Get("sv_jumpvel", "280", EnumSet.of(CVarFlags.SERVER_INFO, CVarFlags.USER_INFO, CVarFlags.ARCHIVE));
        sv_acceleration = Ref.cvars.Get("sv_acceleration", "7", EnumSet.of(CVarFlags.SERVER_INFO, CVarFlags.USER_INFO, CVarFlags.ARCHIVE));
        sv_airaccelerate = Ref.cvars.Get("sv_airaccelerate", "20", EnumSet.of(CVarFlags.SERVER_INFO, CVarFlags.USER_INFO, CVarFlags.ARCHIVE));
        sv_friction = Ref.cvars.Get("sv_friction", "4", EnumSet.of(CVarFlags.SERVER_INFO, CVarFlags.USER_INFO, CVarFlags.ARCHIVE));
        sv_stopspeed = Ref.cvars.Get("sv_stopspeed", "100", EnumSet.of(CVarFlags.SERVER_INFO, CVarFlags.USER_INFO, CVarFlags.ARCHIVE));
        sv_stepheight = Ref.cvars.Get("sv_stepheight", "4", EnumSet.of(CVarFlags.SERVER_INFO, CVarFlags.USER_INFO, CVarFlags.ARCHIVE));
        g_knockback = Ref.cvars.Get("g_knockback", "1400", EnumSet.of(CVarFlags.SERVER_INFO, CVarFlags.USER_INFO, CVarFlags.ARCHIVE));

        // Add entities to spawn list. All entities that's not items should be added here
        IEntity ent = new Info_Player_Spawn();
        addEntityToSpawn(ent);
//        ent = new Func_Door();
//        addEntityToSpawn(ent);
//        addEntityToSpawn(new Info_Player_Goal());
        spawnEntities = new SpawnEntities();
        
//        spawnEntities = Ref.cm.cm.spawnEntities;
        //Ref.commands.AddCommand("addbot", GBot.cmd_addbot);
    }

    private void addEntityToSpawn(IEntity ent) {
        spawns.put(ent.getClassName(), ent);
    }

    public void Init(int leveltime, int randSeed, boolean restart) {
        Common.Log("--- Game init ---");
        g_cheats = Ref.cvars.Get("g_cheats", "0", EnumSet.of(CVarFlags.NONE));


        level = new LevelLocal();
        level.time = leveltime;
        level.startTime = leveltime;

        //InitWorldSession();

        // initialize all clients for this game
        level.maxclients = g_maxclients.iValue;
        
        // initialize all entities for this game
        g_entities = new Gentity[Common.MAX_GENTITIES];
        level.sentities = new SharedEntity[Common.MAX_GENTITIES];
        for (int i= 0; i < Common.MAX_GENTITIES; i++) {
            if(i < level.maxclients) {
                g_entities[i] = new GameClient();
                g_entities[i].s.ClientNum = i;
            }
            else
                g_entities[i] = new Gentity();
            level.sentities[i] = g_entities[i].shEnt;
        }
        level.gentities = g_entities;

        
        g_clients = new GameClient[level.maxclients];
        for (int i= 0; i < level.maxclients; i++) {
            g_clients[i] = (GameClient)g_entities[i];
        }
        level.clients = g_clients;

        // always leave room for the max number of clients,
        // even if they aren't all used, so numbers inside that
        // range are NEVER anything but clients
        level.num_entities = 64;

        WorldSpawn();

        spawnEntities.AddEntity(new SpawnEntity("info_player_spawn", new Vector3f(0,0,CubeChunk.BLOCK_SIZE * CubeChunk.SIZE + 50)));
        spawnEntities.SpawnAll();

        Ref.server.LocateGameData(level.sentities, level.num_entities, level.clients);

        GBot.init();
    }

    private void WorldSpawn() {
        Ref.server.SetConfigString(CS.CS_LEVEL_START_TIME, ""+level.startTime);
        g_entities[Common.ENTITYNUM_WORLD].s.ClientNum = Common.ENTITYNUM_WORLD;
        g_entities[Common.ENTITYNUM_WORLD].classname = "worldspawn";

        Ref.server.SetConfigString(CS.CS_WARMUP, ""+0);
        if(g_restarted.iValue == 1) {
            Ref.cvars.Set2("g_restarted", "0", true);
        }
    }

    public void RunFrame(int time) {
        // if we are waiting for the level to restart, do nothing
        if(level.restarted)
            return;

        level.framenum++;
        level.previousTime = level.time;
        level.time = time;
        int msec = level.time - level.previousTime;

        //int start = Ref.common.Milliseconds();
        for (int i= 0; i < level.num_entities; i++) {
            Gentity ent = g_entities[i];
            if(!ent.inuse)
                continue;

            // clear events that are too old
            if(level.time - ent.eventTime > 300) {
                if(ent.s.evt > 0) {
                    ent.s.evt = 0;
                    if(i < level.maxclients) // If GameClient, clear of its externalevent
                        ((GameClient)ent).ps.externalEvent = 0;
                }
                if(ent.freeAfterEvent) {
                    // tempEntities or dropped items completely go away after their event
                    ent.Free();
                    continue;
                }
                else if(ent.unlinkAfterEvent) {
                    // items that will respawn will hide themselves after their pickup event
                    ent.unlinkAfterEvent = false;
                    ent.Unlink();
                }
            }

            if(ent.freeAfterEvent)
                continue;

            if(!ent.r.linked && ent.neverfree)
                continue;

            if(ent.s.eType == EntityType.MISSILE) {
                Missiles.runMissile(ent);
                continue;
            }

            if(ent.s.eType == EntityType.ITEM || ent.physicsObject) {
                ent.runItem();
                continue;
            }

            if(ent.s.eType == EntityType.MOVER) {
                ent.mover.runMover();
                continue;
            }

            if(i < 64)
            {
                continue;
            }

            ent.runThink();
        }
        
        // perform final fixups on the players
        for (int i= 0; i < level.maxclients; i++) {
            GameClient ent = (GameClient)g_entities[i];
            if(ent.inuse)
                ent.ClientEndFrame();
        }

        Ref.cm.cubemap.update();
    }

    /**
     * Creates a one-off entity that holds an event
     * @param origin position to place the entity
     * @param evt the event type
     * @return a linked temporary Gentity
     */
    public Gentity TempEntity(Vector3f origin, int evt) {
        Gentity e = Spawn();
        e.s.eType = EntityType.EVENTS + evt;
        e.classname = "tempentity";
        e.eventTime = level.time;
        e.freeAfterEvent = true;
        e.SetOrigin(origin);

        e.Link();

        return e;
    }

    /**
     * Finds and returns a free entity
     * @return
     */
    public Gentity Spawn() {
        int i = 0;
        Gentity e = null;
        for(int force = 0; force < 2; force++) {
            // if we go through all entities and can't find one to free,
            // override the normal minimum times before use
            for (i= 64; i < level.num_entities; i++) {
                e = g_entities[i];
                if(e.inuse)
                    continue;

                // the first couple seconds of server time can involve a lot of
                // freeing and allocating, so relax the replacement policy
                if(force == 0 && e.freetime > level.startTime + 2000 && level.time - e.freetime < 1000)
                    continue;

                // A free entity is always clean, so just init a few things
                e.Init(i);
                return e;
            }
            if(i != Common.MAX_GENTITIES)
                break; // Don't go for aggresive force if we aren't using all the entities
        }

        // Sorry, we're full.
        if(i == Common.ENTITYNUM_MAX_NORMAL) {
            for (i = 0;i < Common.MAX_GENTITIES; i++) {
                Common.Log(String.format("%s: %s", i, g_entities[i].classname));
            }
            Common.Log("No free entities.");
        }

        // open up a new slot
        level.num_entities++;

        // let the server system know that there are more entities
        Ref.server.LocateGameData(level.sentities, level.num_entities, level.clients);
        e = g_entities[level.num_entities-1];
        e.Init(level.num_entities-1);
        return e;
    }

    // TODO: Cache scoreboard here, then let clients use the cached scoreboard string
    public void CalculateRanks() {
        for (int i= 0; i < level.clients.length; i++) {
            if(level.clients[i] != null && level.clients[i].pers.connected == ClientPersistant.ClientConnected.CONNECTED)
                level.clients[i].ScoreboardMessage();
        }
    }


    /**
     * Send chat message to all clients
     * @param ent
     * @param text
     */
    public void Say(GameClient ent, String text) {
        for (int i= 0; i < level.maxclients; i++) {
            ((GameClient)g_entities[i]).SayTo(ent.pers.Name, text);
        }
    }

    public void ShutdownGame(boolean b) {
        Common.Log("--- GAME SHUTDOWN ---");
        Ref.cm.ClearCubeMap();
    }

    public void Client_Begin(int i) {
        GameClient ent = (GameClient) g_entities[i];
        ent.Begin();
    }

    // Handle console commands from the client
    public void Client_Command(int id, String[] tokens) {
        g_clients[id].Client_Command(tokens);
    }

    public void ClientUserInfoChanged(int id) {
        GameClient cl = (GameClient)g_entities[id];
        cl.ClientUserInfoChanged();
    }

    // Unlinks and clears a player that has disconnected
    public void Client_Disconnect(int ClientNum) {
        GameClient ent = (GameClient)g_entities[ClientNum];
        ent.Client_Disconnect();
    }

    public String Client_Connect(int id, boolean firsttime, boolean isBot) {
        GameClient ent = (GameClient)g_entities[id];
        return ent.Client_Connect(id, firsttime, isBot);
    }

    public void Client_Think(int id) {
        ((GameClient)g_entities[id]).Client_Think();
    }

    public IEntity findSpawnEntityFromClassName(String classname) {
        IEntity ent = spawns.get(classname);
        return ent;
    }

    // Find the spawn function for the entity and calls it,
    // returns false if not found
    boolean callSpawn(Gentity ent) {
        if(ent.classname == null || ent.classname.isEmpty())
        {
            Common.LogDebug("callSpawn: Null classname");
            return false;
        }

        // check item spawn functions
        IItem item = Ref.common.items.findItemByClassname(ent.classname);
        if(item != null) {
            spawnItem(ent, item);
            return true;
        }

        // check normal spawn functions
        IEntity spawn = spawns.get(ent.classname);
        if(spawn != null) {
            spawn.init(ent);
            return true;
        }

        Common.LogDebug(ent.classname + " doesn't have a spawn function");
        return false;
    }

    /**
     * Sets the clipping size and plants the object on the floor.
     *
     * Items can't be immediately dropped to floor, because they might
     * be on an entity that hasn't spawned yet.
     * @param ent
     * @param item
     */
    void spawnItem(Gentity ent, IItem item) {
        ent.item = item;

        // some movers spawn on the second frame, so delay item
	// spawns until the third frame so they can ride trains
        ent.nextthink = level.time + 100 * 2; // fix 100 = frametime
        ent.think = Ref.common.items.FinishSpawningItem;

        ent.physicsBounce = 0.5f;
    }

    // Adds an event+parm and twiddles the event counter
    public void AddEvent(Gentity ent, Event evt, int evtParms) {
        if(evt == Event.NONE) {
            Common.LogDebug("Zero event added for entity " + ent.s.ClientNum);
            return;
        }

        // clients need to add the event in playerState_t instead of entityState_t
        int bits;
        if(ent.isClient()) {
            GameClient cl = ent.getClient();
            bits = cl.ps.externalEvent & Common.EV_EVENT_BITS;
            bits = (bits + Common.EV_EVENT_BIT1) & Common.EV_EVENT_BITS;
            cl.ps.externalEvent = evt.ordinal() | bits;
            cl.ps.externalEventParam = evtParms;
            cl.ps.externalEventTime = level.time;
        } else {
            bits = ent.s.evt & Common.EV_EVENT_BITS;
            bits = (bits + Common.EV_EVENT_BIT1) & Common.EV_EVENT_BITS;
            ent.s.evt = evt.ordinal() | bits;
            ent.s.evtParams = evtParms;
        }
        ent.eventTime = level.time;
    }

    public Gentity Find(Gentity from, IGentityFilter filter, String match) {
        if(filter == null)
            filter = GentityFilter.CLASSNAME;

        int start = 0;
        if(from != null)
            start = from.s.ClientNum+1;

        for (; start < level.num_entities; start++) {
            from = g_entities[start];
            if(!from.inuse)
                continue;

            if(filter.filter(from, match))
                return from;
        }

        return null;
    }

    public boolean CheatsOk(GameClient gc) {
        if(g_cheats.iValue != 1) {
            gc.SendServerCommand("print \"Cheats are not enabled on this server.\"");
            return false;
        }

        if(gc.isDead()) {
            gc.SendServerCommand("print \"You must be alive to use this command.\"");
            return false;
        }
        return true;
    }

    void respawnAllItems() {
        for (int i= 0; i < level.num_entities; i++) {
            Gentity ent = g_entities[i];
            if(!ent.inuse || ent.item == null)
                continue;

            //if(ent.think == Ref.common.items.FinishSpawningItem) {
                // Finalize spawn
                ent.think.think(ent);
            //}
        }
    }

    public boolean radiusDamage(Vector3f origin, Gentity attacker, int dmg, int radius, Gentity ignore, MeansOfDeath mod) {
        if(radius < 1) radius = 1;

        Vector3f mins = Vector3f.sub(origin, new Vector3f(radius, radius, radius), null);
        Vector3f maxs = Vector3f.add(origin, new Vector3f(radius, radius, radius), null);

        SectorQuery q = Ref.server.EntitiesInBox(mins, maxs);
        Vector3f v = new Vector3f();
        boolean hitClient = false;
        for (Integer integer : q.List) {
            Gentity ent = g_entities[integer];
            if(ent == ignore) continue;

            // find the distance from the edge of the bounding box

            for (int i= 0; i < 3; i++) {
                float org = Helper.VectorGet(origin, i);
                float min = Helper.VectorGet(ent.r.absmin, i);
                float max = Helper.VectorGet(ent.r.absmax, i);
                if(org < min) Helper.VectorSet(v, i, min - org);
                else if(org > max) Helper.VectorSet(v, i, org - max);
                else Helper.VectorSet(v, i, 0);
            }

            float dist = v.length();
            if(dist >= radius) {
                continue;
            }

            float points = dmg * (1f - dist / radius);
            if(ent.isClient()) {
                hitClient = true;
                Vector3f dir = Vector3f.sub(ent.r.currentOrigin, origin, null);
                dir.z += 24;
                damage(ent, null, attacker, dir, origin, (int)points, DamageFlag.RADIUS, mod);
            }
            
            
        }
        return hitClient;
    }

    /**
    ============
    T_Damage

    targ		entity that is being damaged
    inflictor	entity that is causing the damage
    attacker	entity that caused the inflictor to damage targ
            example: targ=monster, inflictor=rocket, attacker=player

    dir			direction of the attack for knockback
    point		point at which the damage is being inflicted, used for headshots
    damage		amount of damage being inflicted
    knockback	force to be applied against targ as a result of the damage

    inflictor, attacker, dir, and point can be NULL for environmental effects

    dflags		these flags are used to control how T_Damage works
            DAMAGE_RADIUS			damage was indirect (from a nearby explosion)
            DAMAGE_NO_ARMOR			armor does not protect from this damage
            DAMAGE_NO_KNOCKBACK		do not affect velocity, just view angles
            DAMAGE_NO_PROTECTION	kills godmode, armor, everything
    ============
    **/
    public void damage(Gentity targ, Gentity inflictor, Gentity attacker, Vector3f dir, Vector3f point, int damage, int dflags, MeansOfDeath mod) {
        if(inflictor == null) {
            inflictor = g_entities[Common.ENTITYNUM_WORLD];
        }

        if(attacker == null) {
            attacker = g_entities[Common.ENTITYNUM_WORLD];
        }

        // shootable doors / buttons don't actually have any health
        if(targ.s.eType == EntityType.MOVER) {
//            if(targ.use != null && targ.mover.state == Mover.POS1) {
//  FIX
//            }
            return;
        }

        GameClient client = targ.getClient();
        if(client != null) {
            if(client.noclip) {
                return;
            }
        }

        if(dir == null || dir.length() == 0) {
            dflags |= DamageFlag.NO_KNOCKBACK;
        } else {
            dir.normalise();
        }

        int knockback = damage;
        if(knockback > 200) {
            knockback = 200;
        }

        // figure momentum add, even if the damage won't be taken
        if(knockback > 0 && client != null && (dflags & DamageFlag.NO_KNOCKBACK) == 0) {
            float mass = 200;

            Vector3f kvel = new Vector3f(dir);
            kvel.scale(g_knockback.fValue * (float)knockback / mass);
            Vector3f.add(client.ps.velocity, kvel, client.ps.velocity);

            // set the timer so that the other client can't cancel
            // out the movement immediately
//            if(client.ps.pm_time == 0) {
//    FIX?
//            }
        }

        // check for completely getting out of the damage
        if((dflags & DamageFlag.NO_PROTECTION) == 0) {
            // friendly fire, etc..
            if(targ.isClient() && targ.getClient().godmode) {
                return;
            }
        }

        // always give half damage if hurting self
	// calculated after knockback, so rocket jumping works
        if(targ == attacker) {
            damage *= 0.5f;
        }

        if(damage < 1) {
            damage = 1;
        }

        int take = damage;
        int save;

        // do it
        if(take > 0) {
            if(client != null) {
                client.ps.stats.Health -= take;
                if(client.ps.stats.Health <= 0) {
                    targ.die.die(targ, inflictor, attacker, take, mod);
                    if(attacker.isClient()) {
                        attacker.getClient().pers.score++;
                    }
                } else if(targ.pain != null) {
                     targ.pain.pain(targ, attacker, take);
                }
            }
        }
    }

    public void runBotFrame(int time) {
        for (GameClient gameClient : g_clients) {
            if(!gameClient.inuse || !gameClient.r.svFlags.contains(SvFlags.BOT)) continue;
            GBot.runBotFrame(time, gameClient);
            
        }
    }

}
