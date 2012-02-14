package cubetech.server;

import cubetech.Game.ClientPersistant;
import cubetech.Game.GameClient;
import cubetech.client.CLSnapshot;
import cubetech.common.*;
import cubetech.entities.*;
import cubetech.input.PlayerInput;
import cubetech.misc.Ref;
import cubetech.net.*;
import cubetech.server.ServerRun.ServerState;
import java.nio.BufferUnderflowException;
import java.util.*;
import org.lwjgl.util.vector.Vector3f;

/**
 * client_t
 * @author mads
 */
public class SvClient {
    

    public SharedEntity gentity;
    public String name;
    public int id;
    
    // Splitscreen info tracking
    int owner = -1;
    int[] localClients = new int[] {-1,-1,-1};

    public ClientState state = ClientState.FREE;
    public String userinfo; // name, etc..
    public PlayerInput lastUserCmd;

    // All kinds of crap
    public String[] reliableCommands = new String[64];
    public int reliableSequence;
    public int reliableAcknowledge;
    public int reliableSent;
    public int messageAcknowledge;
    public int gamestateMessageNum;
    public int lastClientCommand;
    public String lastClientCommandString = "";
    public int deltaMessage;
    public int lastPacketTime;
    public int lastConnectTime;
    public int nextSnapshotTime;
    public boolean rateDelayed;
    public int timeoutCount;
    public ClientSnapshot[] frames = new ClientSnapshot[32];
    public int ping;
    public int rate;
    public int snapshotMsec;
    public NetChan netchan;
    public int oldServerTime;
    public boolean[] csUpdated = new boolean[1025];

    private SvDownload download = new SvDownload(this);

    // Buffer up packets if netchan is clogged with fragmented stuff
    Queue<NetBuffer> netchan_queue = new LinkedList<NetBuffer>();
    
    void clone(SvClient newcl) {
        newcl.gentity = gentity;
        newcl.name = name;
        newcl.id = id;
        newcl.owner = owner;
        newcl.state = state;
        newcl.userinfo = userinfo; // name, etc..
        newcl.reliableSequence = reliableSequence;
        newcl.reliableAcknowledge = reliableAcknowledge;
        newcl.reliableSent = reliableSent;
        newcl.messageAcknowledge = messageAcknowledge;
        newcl.gamestateMessageNum = gamestateMessageNum;
        newcl.lastClientCommand = lastClientCommand;
        newcl.lastClientCommandString = lastClientCommandString;
        newcl.deltaMessage = deltaMessage;
        newcl.lastPacketTime = lastPacketTime;
        newcl.lastConnectTime = lastConnectTime;
        newcl.nextSnapshotTime = nextSnapshotTime;
        newcl.rateDelayed = rateDelayed;
        newcl.timeoutCount = timeoutCount;
        newcl.ping = ping;
        newcl.rate = rate;
        newcl.snapshotMsec = snapshotMsec;
        newcl.netchan = netchan;
        newcl.oldServerTime = oldServerTime;
        newcl.lastUserCmd = lastUserCmd.Clone();
        System.arraycopy(frames, 0, newcl.frames, 0, frames.length);
        System.arraycopy(localClients, 0, newcl.localClients, 0, localClients.length);
        System.arraycopy(reliableCommands, 0, newcl.reliableCommands, 0, reliableCommands.length);
        System.arraycopy(csUpdated, 0, newcl.csUpdated, 0, csUpdated.length);
    }
    
    public SvClient() {
        for (int i= 0; i < frames.length; i++) {
            frames[i] = new ClientSnapshot();
        }
    }
    
    private void UserMove(NetBuffer buf, boolean deltaCompressed) {
        if(deltaCompressed) deltaMessage = messageAcknowledge;
        else deltaMessage = -1;
        
        

        int cmdCount = buf.ReadByte();
        if(cmdCount < 1) {
            Common.LogDebug(name + ": UserMove cmdCount < 1");
            return;
        }

        if(cmdCount > 32) {
            Common.LogDebug(name + ": UserMove cmdCount > 32");
            return;
        }

        PlayerInput[] cmds = new PlayerInput[cmdCount];
        PlayerInput oldcmd = new PlayerInput();
        for (int i= 0; i < cmdCount; i++) {
            try {
                cmds[i] = PlayerInput.ReadDeltaUserCmd(buf, oldcmd);
                oldcmd = cmds[i];
            } catch (BufferUnderflowException ex) {
                int test = 2;
            }
            
        }

        // save time for ping calculation
        frames[messageAcknowledge & 31].messageAcked = Ref.server.time;

        // if this is the first usercmd we have received
        // this gamestate, put the client into the world
        if(state == ClientState.PRIMED) {
            ClientEnterWorld(cmds[cmds.length-1]);
        }

        if(state != ClientState.ACTIVE) {
            deltaMessage = -1;
            return;
        }

        // usually, the first couple commands will be duplicates
        // of ones we have previously received, but the servertimes
        // in the commands will cause them to be immediately discarded
        for (int i= 0; i < cmdCount; i++) {
            // if this is a cmd from before a map_restart ignore it
            if(cmds[i].serverTime > cmds[cmdCount -1].serverTime)
                continue;

            // don't execute if this is an old cmd which is already executed
            // these old cmds are included when cl_packetdup > 0
            if(cmds[i].serverTime <= lastUserCmd.serverTime) {
                continue;
            }

            ClientThink(cmds[i]);
        }
    }

    public void ClientThink(PlayerInput cmd) {
        lastUserCmd = cmd;
        if(state != ClientState.ACTIVE)
            return; // may have been kicked since last usercmd

        Ref.game.Client_Think(id);
    }

    private void ClientEnterWorld(PlayerInput cmd) {
        Common.LogDebug(name + ": Going from PRIMED to ACTIVE");
        state = ClientState.ACTIVE;

        // resend all configstrings using the cs commands since these are
        // no longer sent when the client is CS_PRIMED
        UpdateConfigStrings();

        // set up the entity for the client
        gentity = Ref.server.sv.gentities[id];
//        System.out.println("cliEnter: " + id);
        gentity.s.number = id;
        
        // Copy over localclients
        System.arraycopy(localClients, 0, gentity.r.localClients, 0, 3);

        deltaMessage = -1;
        nextSnapshotTime = Ref.server.time; // generate one immediatly
        lastUserCmd = cmd;

        Ref.game.Client_Begin(id);
    }

    void UpdatePing() {
        if(state != ClientState.ACTIVE || gentity == null) {
            ping = -1;
            return;
        }

        float total = 0;
        int count = 0;
        for (int i= 0; i < 32; i++) {
            if(frames[i].messageAcked <= 0)
                continue;
            int delta = frames[i].messageAcked - frames[i].messageSent;
            count++;
            total += delta;
        }

        if(count <= 0)
            ping = -1;
        else {
            ping = (int) (total / count);
            if(ping > 9999)
                ping = 9999;
        }

        Ref.server.GameClientNum(id).ping = ping;
    }

    void CheckTimeout(int droppoint, int zombiepoint) {
        if(owner != -1) return; // got owner
        if(state == ClientState.ZOMBIE && lastPacketTime < zombiepoint) {
            // using the client id cause the cl->name is empty at this point
            Common.LogDebug("Going from ZOMBIE to FREE for client");
            state = ClientState.FREE; // can now be reused
            return;
        }
        if(state.ordinal() >= ClientState.CONNECTED.ordinal() && lastPacketTime < droppoint) {
            // wait several frames so a debugger session doesn't
            // cause a timeout
            if(++timeoutCount > 5) {
                DropClient("timed out");
                state = ClientState.FREE;
            }
        } else {
            timeoutCount = 0;
        }
    }

    public void SendClientMessage() {
        if(state == ClientState.FREE || netchan.source == null)
            return; // not connected

        if(Ref.server.time < nextSnapshotTime) {
            return; // not time yet
        }

        if(netchan.unsentFragments) {
            nextSnapshotTime = Ref.server.time + RateMsec(netchan.unsentLenght - netchan.unsentFragmentStart);
            TransmitNextFragment();
            return;
        }

        // generate and send a new message
        SendClientSnapshot();
    }

    public void SendClientSnapshot() {
        // send with owner
        if(owner != -1) return;
        
        BuildClientSnapshot();

        // bots need to have their snapshots build, but
	// the query them directly without needing to be sent
        if(gentity != null && gentity.r.svFlags.contains(SvFlags.BOT)) return;

        NetBuffer msg = NetBuffer.GetNetBuffer(false, true);
        // let the client know which reliable clientCommands we have received
        msg.Write(lastClientCommand);
        // (re)send any reliable server commands
        UpdateServerCommandsToClient(msg);

        // send over all the relevant entityState_t
        // and the playerState_t
        WriteSnapshotToClient(msg);

        if(!download.isDownloading()) {
            sendCubeFile();
        }

        download.writeDownloadToClient(msg);

        

        SendMessageToClient(msg);


    }
    
    private void DoneDownload() {
        sendCubeFile();
//        System.out.println("download done");
//        SendClientGameState();
    }

    private void BuildClientSnapshot() {
        // bump the counter used to prevent double adding
        Ref.server.sv.snapshotCounter++;

        // this is the frame we are creating
        ClientSnapshot frame = frames[netchan.outgoingSequence & 31];

        // clear everything in this snapshot
        frame.num_entities = 0;

        if(gentity == null || state == ClientState.ZOMBIE) return;

        // grab the current playerState_t
        PlayerState ps = Ref.server.GameClientNum(id);
        ps.Clone(frame.pss[0]);
        frame.numPS = 1;
        frame.lcIndex[0] = 0;
        
        // Add splitscreen clients
        for (int i = 1; i < 4; i++) {
            if(localClients[i-1] == -1) {
                frame.lcIndex[i] = -1;
                continue;
            }
            ps = Ref.server.GameClientNum(localClients[i-1]);
            ps.Clone(frame.pss[i]);
            frame.lcIndex[i] = frame.numPS;
            frame.numPS++;
        }

        // never send client's own entity, because it can
        // be regenerated from the playerstate
        for (int i = 0; i < frame.numPS; i++) {
            int clientnum = frame.pss[i].clientNum;
            if(clientnum < 0 || clientnum >= Common.MAX_GENTITIES)
            {
                System.err.println("bad gEnt!");
            }

            SvEntity svEnt = Ref.server.sv.svEntities[clientnum];
            svEnt.snapshotCounter = Ref.server.sv.snapshotCounter;
        }

        // find the client's viewpoint
        ArrayList<Integer> snapEntNums = new ArrayList<Integer>();

        for (int i = 0; i < frame.numPS; i++) {
            // add all the entities directly visible to the eye, which
            // may include portal entities that merge other viewpoints
            AddEntitiesVisibleFromPoint(frame.pss[i].origin, frame, snapEntNums, false);
        }
        
        

        // if there were portals visible, there may be out of order entities
        // in the list which will need to be resorted for the delta compression
        // to work correctly.  This also catches the error condition
        // of an entity being included twice.
        Collections.sort(snapEntNums);

        // copy the entity states out
        frame.num_entities = 0;
        frame.first_entity = Ref.server.nextSnapshotEntities;
        for (int i= 0; i < snapEntNums.size(); i++) {
            SharedEntity ent = Ref.server.sv.gentities[snapEntNums.get(i)];
            ent.s.Clone(Ref.server.snapshotEntities[Ref.server.nextSnapshotEntities++ % Ref.server.numSnapshotEntities]);
            frame.num_entities++;
        }
    }

    private void AddEntitiesVisibleFromPoint(Vector3f origin, ClientSnapshot frame, ArrayList<Integer> snapEntNums, boolean b) {
        // during an error shutdown message we may need to transmit
        // the shutdown message after the server has shutdown, so
        // specfically check for it
        if(Ref.server.sv.state == ServerState.DEAD)
            return;

        for (int i= 0; i < Ref.server.sv.num_entities; i++) {
            SharedEntity ent = Ref.server.sv.gentities[i];

            // never send entities that aren't linked in
            if(!ent.r.linked)
                continue;

            if(ent.s.number != i) {
                Common.LogDebug("Fixing ent.s number, was: " + ent.s.number + ", should be: " + i);
                ent.s.number = i;
            }

            // entities can be flagged to explicitly not be sent to the client
            if(ent.r.svFlags.contains(SvFlags.NOCLIENT))
                continue;

            if(ent.r.svFlags.contains(SvFlags.SINGLECLIENT)) {
                int j;
                for (j = 0; j < frame.numPS; j++) {
                    if(ent.r.singleClient == frame.pss[j].clientNum) {
                        break;
                    }
                }
                if(j == frame.numPS) continue;
            }

            if(ent.r.svFlags.contains(SvFlags.NOTSINGLECLIENT)) {
                // fix: splitscreen clients will get predictable events
                int j;
                for (j = 0; j < frame.numPS; j++) {
                    if(ent.r.singleClient != frame.pss[j].clientNum) {
                        break;
                    }
                }
                if(j == frame.numPS) continue;
            }

            if(ent.r.svFlags.contains(SvFlags.CLIENTMASK)) {
                int j;
                for (j = 0; j < frame.numPS; j++) {
                    if(frame.pss[j].clientNum >= 32) {
                        Ref.common.Error(Common.ErrorCode.DROP, "ClientMask >= 32");
                    }
                    
                    if((~ent.r.singleClient & (1 << frame.pss[j].clientNum)) != 0) {
                        break;
                    }
                }
                
                if(j != frame.numPS) continue;
            }

            SvEntity svEnt = Ref.server.SvEntityForGentity(ent);

            // don't double add an entity through portals
            if(svEnt.snapshotCounter == Ref.server.sv.snapshotCounter)
                continue;

            // broadcast entities are always sent
            if(ent.r.svFlags.contains(SvFlags.BROADCAST)) {
                if(ent.s.eType >= EntityType.EVENTS) {
                    //Common.LogDebug("Broadcasting event " + Event.values()[ent.s.eType-EntityType.EVENTS].toString());
                }
                AddEntToSnapshot(svEnt, ent, snapEntNums);
                continue;
            }

            // TODO: Cull things that can't be seen

            AddEntToSnapshot(svEnt, ent, snapEntNums);

        }
    }

    private void AddEntToSnapshot(SvEntity svEnt, SharedEntity ent, ArrayList<Integer> snapEntNums) {
        // if we have already added this entity to this snapshot, don't add again
        if(svEnt.snapshotCounter == Ref.server.sv.snapshotCounter)
            return;

        svEnt.snapshotCounter = Ref.server.sv.snapshotCounter;

        snapEntNums.add(ent.s.number);
    }

    private void WriteSnapshotToClient(NetBuffer msg) {
        // this is the snapshot we are creating
        ClientSnapshot frame = frames[netchan.outgoingSequence & 31];

        // try to use a previous frame as the source for delta compressing the snapshot
        ClientSnapshot oldframe = null;
        int lastframe;
        if(deltaMessage <= 0 || state != ClientState.ACTIVE) {
            // client is asking for a retransmit
            lastframe = 0;
            oldframe = null;
        }
        else if(netchan.outgoingSequence - deltaMessage >= (32 -2))
        {
            // client hasn't gotten a good message through in a long time
            Common.LogDebug("Delta request from out of date packet");
            lastframe = 0;
            oldframe = null;
        } else {
            // we have a valid snapshot to delta from
            oldframe = frames[deltaMessage & 31];
            lastframe = netchan.outgoingSequence - deltaMessage;
            // the snapshot's entities may still have rolled off the buffer, though
            if(oldframe.first_entity <= Ref.server.nextSnapshotEntities - Ref.server.numSnapshotEntities) {
                Common.LogDebug("Delta request from out of date entities");
                oldframe = null;
                lastframe = 0;
            }
        }

        msg.Write(SVC.OPS_SNAPSHOT);
        // send over the current server time so the client can drift
        // its view of time to try to match
        if(oldServerTime > 0) {
            // The server has not yet got an acknowledgement of the
            // new gamestate from this client, so continue to send it
            // a time as if the server has not restarted. Note from
            // the client's perspective this time is strictly speaking
            // incorrect, but since it'll be busy loading a map at
            // the time it doesn't really matter.
            msg.Write(Ref.server.sv.time + oldServerTime);
        }
        else
            msg.Write(Ref.server.sv.time);

        // what we are delta'ing from
        msg.Write(lastframe);

        // Figure out snapshot flags
        int snapflags = Ref.server.snapFlagServerBit;
        if(rateDelayed) snapflags |= CLSnapshot.SF_DELAYED;
        if(state != ClientState.ACTIVE) snapflags |= CLSnapshot.SF_NOT_ACTIVE;
        if(frame.numPS > 1 || frame.lcIndex[0] != 0) {
            snapflags |= CLSnapshot.SF_MULTIPS;
        }
        msg.Write(snapflags);
        
        if((snapflags & CLSnapshot.SF_MULTIPS) != 0) {
            msg.Write((byte)frame.numPS);
            for (int i = 0; i < 4; i++) {
                msg.Write((byte)frame.lcIndex[i]);
            }
        }

        int psSize = msg.GetBuffer().position();
        
        // delta encode the playerstate
        for (int i = 0; i < 4; i++) {
            if(frame.lcIndex[i] == -1) continue;
            
            if(oldframe != null && oldframe.lcIndex[i] != -1){
                frame.pss[frame.lcIndex[i]].WriteDelta(msg, oldframe.pss[oldframe.lcIndex[i]]);
            } else {
                frame.pss[frame.lcIndex[i]].WriteDelta(msg, null);
            }
        }
        
        int entSize = msg.GetBuffer().position();
        psSize = entSize - psSize;

        // delta encode the entities

        EmitPacketEntities(oldframe, frame, msg);
        entSize = msg.GetBuffer().position() - entSize;
        
    }

    private void EmitPacketEntities(ClientSnapshot from, ClientSnapshot to, NetBuffer msg) {
        int fromnumentities = 0;
        if (from != null)
            fromnumentities = from.num_entities;


        EntityState oldent = null, newent = null;
        int oldindex = 0, newindex = 0;
        int newnum, oldnum;
        while (newindex < to.num_entities || oldindex < fromnumentities)
        {
            if (newindex >= to.num_entities)
                newnum = 9999;
            else
            {
                newent = Ref.server.snapshotEntities[(to.first_entity + newindex) % Ref.server.numSnapshotEntities];
                newnum = newent.number;
            }

            if (oldindex >= fromnumentities)
                oldnum = 9999;
            else
            {
                oldent = Ref.server.snapshotEntities[(from.first_entity + oldindex) % Ref.server.numSnapshotEntities];
                oldnum = oldent.number;
            }

            if (newnum == oldnum)
            {
                // delta update from old position
                // because the force parm is qfalse, this will not result
                // in any bytes being emited if the entity has not changed at all
                newent.WriteDeltaEntity(msg, oldent, false);
                oldindex++;
                newindex++;
                continue;
            }

            if (newnum < oldnum)
            {
                // this is a new entity, send it from the baseline
                newent.WriteDeltaEntity(msg, Ref.server.sv.svEntities[newnum].baseline, true);
                newindex++;
                continue;
            }

            if (newnum > oldnum)
            {
                // the old entity isn't present in the new message
                EntityState.WriteDeltaRemoveEntity(msg, oldent);
                oldindex++;
                continue;
            }
        }

        msg.WriteShort(Common.MAX_GENTITIES-1);    // end of packetentities
    }

    private void UpdateConfigStrings() {
        for (int i= 0; i < csUpdated.length; i++) {
            // if the CS hasn't changed since we went to CS_PRIMED, ignore
            if(!csUpdated[i])
                continue;

            // do not always send server info to all clients
            if(i == CS.CS_SERVERINFO && gentity != null && gentity.r.svFlags.contains(SvFlags.NOSERVERINFO))
                continue;

            SendConfigString(i);
            csUpdated[i] = false;
        }
    }

    void SendConfigString(int index) {
        Ref.server.SendServerCommand(this, String.format("cs %d \"%s\"\n", index, Ref.server.sv.configstrings.get(index)));
    }


    public void AddServerCommand(String str) {
        // do not send commands until the gamestate has been sent
        if(state != SvClient.ClientState.PRIMED && state != SvClient.ClientState.ACTIVE) return;
        
        if(owner != -1) {
            // send through owner
            SvClient cl = Ref.server.clients[owner];
            for (int i = 0; i < 3; i++) {
                if(cl.localClients[i] == id) {
                    str = "lc" + i + " " + str;
                    cl.AddServerCommand(str);
                    break;
                }
            }
            return;
        }

        reliableSequence++;

        // if we would be losing an old command that hasn't been acknowledged,
        // we must drop the connection
        // we check == instead of >= so a broadcast print added by SV_DropClient()
        // doesn't cause a recursive drop client
        if(reliableSequence - reliableAcknowledge == 64 +1 && owner == -1) {
            Common.Log("==== pending server commands ====");
            int i;
            for (i=reliableAcknowledge+1; i<= reliableSequence; i++) {
                Common.Log(String.format("cmd %d: %s", i, reliableCommands[i&63]));
            }
            Common.Log(String.format("cmd %d: %s", i, str));
            DropClient("Server command overflow");
            return;
        }
        int index = reliableSequence & 63;
        reliableCommands[index] = str;

    }

    private void SendClientGameState() {
//        System.out.println(name +": Sending gamestate");
//        System.out.println(name +": Going from CONNECTED to PRIMED");
        state = ClientState.PRIMED;

        // when we receive the first packet from the client, we will
        // notice that it is from a different serverid and that the
        // gamestate message was not just sent, forcing a retransmit
        gamestateMessageNum = netchan.outgoingSequence;

        NetBuffer buf = NetBuffer.GetNetBuffer(false, false);
        // NOTE, MRE: all server->client messages now acknowledge
        // let the client know which reliable clientCommands we have received
        buf.Write(lastClientCommand);

        // send any server commands waiting to be sent first.
        // we have to do this cause we send the client->reliableSequence
        // with a gamestate and it sets the clc.serverCommandSequence at
        // the client side
        UpdateServerCommandsToClient(buf);

        // send the gamestate
        buf.Write(SVC.OPS_GAMESTATE);
        buf.Write(reliableSequence);

        // write the configstrings
        for (int i: Ref.server.sv.configstrings.keySet()) {
            buf.Write(SVC.OPS_CONFIGSTRING);
            buf.Write(i);
            buf.Write(Ref.server.sv.configstrings.get(i));
        }

        // write the baselines
        EntityState nullstate = new EntityState();
        for (int i= 0; i < Common.MAX_GENTITIES; i++) {
            EntityState bases = Ref.server.sv.svEntities[i].baseline;
            if(bases == null || bases.number <= 0)
                continue;

            buf.Write(SVC.OPS_BASELINE);
            bases.WriteDeltaEntity(buf, nullstate, true);
        }



        buf.Write(SVC.OPS_EOF);
        buf.Write(id);
        SendMessageToClient(buf);
    }

    private void SendMessageToClient(NetBuffer buf) {
        // Compress netbuffer
        buf.compress();

        int size = buf.GetBuffer().position();
        frames[netchan.outgoingSequence & 31].messageSize = size;
        frames[netchan.outgoingSequence & 31].messageSent = Ref.server.time;
        frames[netchan.outgoingSequence & 31].messageAcked = -1;

        // send the datagram
        Transmit(buf);

        // set nextSnapshotTime based on rate and requested number of updates
        if(netchan.isLocalhost()) {
            nextSnapshotTime = (int) (Ref.server.time + (1000f / Ref.server.sv_fps.iValue * Ref.common.com_timescale.fValue));
            return;
        }

        // normal rate calculation
        int rateMsec = RateMsec(size);
        if(rateMsec < (int)(snapshotMsec * Ref.common.com_timescale.fValue))  {
            // never send more packets than this, no matter what the rate is at
            rateMsec = (int) (snapshotMsec * Ref.common.com_timescale.fValue);
            rateDelayed = false;
        } else
            rateDelayed = true;

        nextSnapshotTime = (int) (Ref.server.time + rateMsec * Ref.common.com_timescale.fValue);

        // don't pile up empty snapshots while connecting
        if(state != ClientState.ACTIVE) {
            // a gigantic connection message may have already put the nextSnapshotTime
            // more than a second away, so don't shorten it
            // do shorten if client is downloading
            if(!download.isDownloading() && nextSnapshotTime < Ref.server.time + 1000 * Ref.common.com_timescale.fValue)
                nextSnapshotTime = (int) (Ref.server.time + 1000 * Ref.common.com_timescale.fValue);
        }

    }

    private int RateMsec(int bytes) {
        if(netchan.isLocalhost())
            return 0;
        int headerSize = 48;
        if(bytes > NetChan.FRAGMENT_SIZE)
            bytes = NetChan.FRAGMENT_SIZE;
        int rateMsec = (int)((float)(bytes + headerSize) * 1000f  / rate * Ref.common.com_timescale.fValue);
        return rateMsec;
    }

    private void Transmit(NetBuffer buf) {
        buf.Write(SVC.OPS_EOF);
        if(netchan.unsentFragments) {
//            System.out.println("Unsent fragments, queued");
            // Store message in new buffer, so it doesn't get run over
            NetBuffer queuedBuf = NetBuffer.CreateCustom(buf.GetBuffer().duplicate());
            // insert it in the queue, the message will be encoded and sent later
            netchan_queue.add(queuedBuf);
            netchan.TransmitNextFragment();
        } else
            netchan.Transmit(buf);
    }

    private void TransmitNextFragment() {
        netchan.TransmitNextFragment();
        if(!netchan.unsentFragments) {
            // the last fragment was transmitted, check wether we have queued messages
            if(netchan_queue.peek() != null) {
//                Common.LogDebug("Popping queued message: %d left", netchan_queue.size());
                netchan.Transmit(netchan_queue.poll());
            }
        }
    }

    private void UpdateServerCommandsToClient(NetBuffer buf) {
        // write any unacknowledged serverCommands
        for (int i = reliableAcknowledge+1; i <= reliableSequence; i++) {
            buf.Write(SVC.OPS_SERVERCOMMAND);
            buf.Write(i);
            buf.Write(reliableCommands[i & 63]);
        }
        reliableSent = reliableSequence;
    }

    // Recieved a packet from the client
    void ExecuteClientMessage(NetBuffer buf) {
        int serverid = buf.ReadInt();
        messageAcknowledge = buf.ReadInt();
        if(messageAcknowledge < 0) {
            // usually only hackers create messages like this
            // it is more annoying for them to let them hanging
            DropClient( "Illegible server message");
            return;
        }

        reliableAcknowledge = buf.ReadInt();
        // NOTE: when the client message is fux0red the acknowledgement numbers
        // can be out of range, this could cause the server to send thousands of server
        // commands which the server thinks are not yet acknowledged in SV_UpdateServerCommandsToClient
        if(reliableAcknowledge < reliableSequence - 64) {
            DropClient("Illegible server message");
            reliableAcknowledge = reliableSequence;
            return;
        }

        // if this is a usercmd from a previous gamestate,
        // ignore it or retransmit the current gamestate
        //
        if(serverid != Ref.server.sv.serverid && !download.isDownloading() && !lastClientCommandString.equals("nextdl")) {
            if(serverid < Ref.server.sv.serverid && serverid >= Ref.server.sv.restartedServerId) {
                // they just haven't caught the map_restart yet
                Common.LogDebug(name + ": Ignoring pre map_restart/outdated client message");
                return;
            }

            // if we can tell that the client has dropped the last
            // gamestate we sent them, resend it
            if(messageAcknowledge > gamestateMessageNum) {
                Common.LogDebug(name + ": Dropped gamestate, resending");
                SendClientGameState();
            }
            return;
        }

        // this client has acknowledged the new gamestate so it's
        // safe to start sending it the real time again
        if(oldServerTime > 0 && serverid == Ref.server.sv.serverid) {
            Common.LogDebug("Acknowledged gamestate");
            oldServerTime = 0;
        }

        // read optional clientCommand strings
        byte cmd;
        do {
            cmd = buf.ReadByte();

            if(cmd == CLC.OPS_EOF)  break;

            if(cmd == CLC.OPS_CLIENTCOMMAND) {
                // check for flood protection
                if(!ClientCommand(buf)) break;
            }
            
            if(cmd == CLC.OPS_MOVE || cmd == CLC.OPS_MOVENODELTA) {
                int clientIndex = buf.ReadByte();
                SvClient cl;
                if(clientIndex == 0) {
                    cl = this;
                } else {
                    if(localClients[clientIndex-1] == -1) continue;
                    cl = Ref.server.clients[localClients[clientIndex-1]];
                }
                
                if(cmd == CLC.OPS_MOVE) {
                    cl.UserMove(buf, true);
                } else {
                    cl.UserMove(buf, false);
                }
            }

            // got disconnect command?
            if(state == ClientState.ZOMBIE) return; 
        } while(true);
    }

    private boolean ClientCommand(NetBuffer buf) {
        int seq = buf.ReadInt();
        String s = buf.ReadString();

        // see if we have already executed it
        if(lastClientCommand >= seq) return true;

        // drop the connection if we have somehow lost commands
        if(seq > lastClientCommand + 1) {
            Common.Log(name + ": Lost clientcommands. Dropping");
            DropClient( "Lost reliable commands");
            return false;
        }

        ExecuteClientCommand(s);
        lastClientCommand = seq;
        lastClientCommandString = s;
        return true; // continue processing
    }

    private void ExecuteClientCommand(String s) {
        String[] tokens = Commands.TokenizeString(s, false);

        // see if it is a server level command
        String cmd = tokens[0];
        
        if(cmd.equalsIgnoreCase("userinfo"))
            UpdateUserInfo(tokens);
        else if(cmd.equalsIgnoreCase("disconnect"))
            DropClient("Disconnected");
        else if(cmd.equalsIgnoreCase("download")) {
            if(tokens.length >= 2) download.BeginDownload(tokens[1]);
        } else if(cmd.equalsIgnoreCase("nextdl"))
            download.nextDownload(tokens);
        else if(cmd.equalsIgnoreCase("stopdl"))
            download.StopDownload();
        else if(cmd.equalsIgnoreCase("donedl"))
            DoneDownload();
        else if(cmd.startsWith("drop") && tokens.length >= 2) {
            boolean out = cmd.startsWith("dropout");
            try {
                int clientIndex = Integer.parseInt(tokens[1])-1;
                
                if(out && localClients[clientIndex] != -1) {
                    GameClient gc = Ref.server.sv.gameClients[localClients[clientIndex]];
                    Ref.server.DropClient(gc, "dropped out");
                } else if(!out && localClients[clientIndex] == -1) {
                    Ref.server.addExtraLocalClient(this, clientIndex+1, userinfo);
                }
            } catch(NumberFormatException ex) {}
        } else if (Ref.server.sv.state == ServerState.GAME)
            Ref.game.Client_Command(id, tokens);
        
    }

    private void sendCubeFile() {
        ClientSnapshot frame = frames[netchan.outgoingSequence & 31];
        ClientPersistant pers = frame.pss[0].pers;
        if(pers == null || pers.isQueueEmpty()) return;
        download.BeginDownload("@cube");
    }

    private void UpdateUserInfo(String[] tokens) {
        userinfo = tokens[1];
        UserInfoChanged(false);
        Ref.game.ClientUserInfoChanged(id);
    }

    /**
     * Set userinfo for this client.
     * Also extracts name
     * @param newinfo
     */
    public void SetUserInfo(String newinfo) {
        userinfo = newinfo;
        name = Info.ValueForKey(newinfo, "name");
    }

    /**
     * Remove this client from the server
     * @param reason
     */
    public void DropClient(String reason) {
        if(state == ClientState.ZOMBIE) return;
        
        // Drop fake splitscreen clients
        for (int i = 0; i < 3; i++) {
            if(localClients[i] != -1) {
                Ref.server.clients[localClients[i]].DropClient(reason);
            }
        }
            

        Common.Log("Dropping client %s (reason: %s)", name, reason);

        if(!netchan.isBot) {
            // see if we already have a challenge for this ip
            for (Challenge challenge : Ref.server.challenges) {
                if(challenge.addr == null) continue;
                if(challenge.addr.equals(netchan.addr)) {
                    challenge.clear();
                    break;
                }
            }
        }

        // Kill any download
        download.StopDownload();

        // tell everyone why they got dropped
        Ref.server.SendServerCommand(null, String.format("print \"%s %s\"\n", name, reason));

        // call the prog function for removing a client
        Ref.game.Client_Disconnect(gentity.s.number);

        // add the disconnect command
        if(owner == -1) {
            Ref.server.SendServerCommand(this, String.format("disconnect \"%s\"\n", reason));
        } else {
            // Don't send disconnect for fake clients
            SvClient cl = Ref.server.clients[owner];
            for (int i = 0; i < 3; i++) {
                if(cl.localClients[i] == id) {
                    cl.localClients[i] = -1;
                }
                if(cl.gentity != null && cl.gentity.r.localClients[i] == id) {
                    cl.gentity.r.localClients[i] = -1;
                }
            }
        }

        if(netchan.isBot) {
            // bots shouldn't go zombie, as there's no real net connection.
            Ref.server.freeBotClient(this);
        }

        // nuke user info.. yes, nuke it from orbit.
        SetUserInfo("");

        if(netchan.isBot) {
            // bots shouldn't go zombie, as there's no real net connection.
            state = ClientState.FREE;
        } else {
            state = ClientState.ZOMBIE; // become free in a few seconds
        }
    }

    public void UserInfoChanged(boolean autoFix) {
        // Get name
        name = Info.ValueForKey(userinfo, "name");
        String oldname = name;
        String cleanName = CleanName(name);
        if(!cleanName.equals(name))
            Ref.server.SendServerCommand(null, String.format("print \"%s ^0changed name to %s\"\n", oldname, cleanName));
        if(!cleanName.equals(name))
        {
            // Set new name
            name = cleanName;
            userinfo = Info.SetValueForKey(userinfo, "name", name);
            Ref.game.ClientUserInfoChanged(id);
        }
        
        // Get Rate
        try {
            rate = Integer.parseInt(Info.ValueForKey(userinfo, "rate"));
            if(rate < 1000)
                rate = 1000;
            if(rate > 200000)
                rate = 200000;
        } catch(NumberFormatException e) {
            Common.Log("SvClient: Failed parsing rate");
            rate = 25000;
        }

        // Get updaterate
        try {
            int snapval = Integer.parseInt(Info.ValueForKey(userinfo, "cl_updaterate"));
            if(snapval < 2)
                snapval = 2;
            if(snapval > Ref.cvars.Find("sv_fps").iValue)
                snapval = Ref.cvars.Find("sv_fps").iValue;
            snapshotMsec = (int)(1000f/(float)snapval)-1;
        } catch(NumberFormatException e) {
            Common.Log("SvClient: Failed parsing cl_updaterate");
            snapshotMsec = 50;
        }
    }

    private String CleanName(String name) {
        // Start out by trimming for writespace
        name = name.trim();

        
        int nSpaces = 0;
        StringBuilder target = new StringBuilder(name.length());
        int skip = 0;
        for (int i= 0; i < name.length(); i++) {
            if(skip > 0) {
                skip--;
                continue;
            }
            
            char c = name.charAt(i);

            // Trim out spaces when there's more than 3
            if(c == ' ') {
                if(nSpaces > 2)
                    continue;
                nSpaces++;
            } 
            // Disallow black text
            else if(c == '^' && i + 1 < name.length() && name.charAt(i+1) == '1') {
                skip++;
                continue;
            }

            target.append(c);
            nSpaces = 0;
        }
        // Get clean name
        name = target.toString();

        // Check if empty
        if(name.isEmpty())
            name = "UnknownCube";

        // Check if name in use
        String orgName = name;
        boolean nameFail = false;
        int lastIndex = 0;
        do {
            nameFail = false;
            for (int i= 0; i < Ref.server.clients.length; i++) {
                SvClient cl = Ref.server.clients[i];
                if(cl.id == this.id)
                    continue; // Don't check against self

                if(cl.state == ClientState.FREE || cl.state == ClientState.ZOMBIE)
                    continue;

                if(name.equals(cl.name)) {
                    // will change name to name(1), name(2) or whatever
                    nameFail = true; // Maybe there's more than one player with the same name
                    
                    lastIndex++;
                    name = String.format("%s(%d)", orgName, lastIndex);
                    break;
                }
            }
        } while(nameFail); // keep trying

        return name;
    }

    
    
    public enum ClientState {
        FREE, // can be reused for a new connection
        ZOMBIE, // client has been disconnected, but don't reuse
        	// connection for a couple seconds
        CONNECTED, // has been assigned to a client, but no gamestate yet
        PRIMED, // gamestate has been sent, but client hasn't sent a usercmd
        ACTIVE // client is fully in game
    }
}
