/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.server;

import cubetech.client.CLSnapshot;
import cubetech.common.CS;
import cubetech.common.Commands;
import cubetech.common.Common;
import cubetech.common.Info;
import cubetech.common.PlayerState;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.entities.SharedEntity;
import cubetech.entities.SvEntity;
import cubetech.gfx.ResourceManager;
import cubetech.input.PlayerInput;
import cubetech.misc.Ref;
import cubetech.net.CLC;
import cubetech.net.NetBuffer;
import cubetech.net.NetChan;
import cubetech.net.SVC;
import cubetech.server.ServerRun.ServerState;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.util.vector.Vector2f;

/**
 * client_t
 * @author mads
 */
public class SvClient {
    private static final int MAX_DOWNLOAD_WINDOW = 8;
    private static final int MAX_DOWNLOAD_BLKSIZE = 1024*2;


    public enum ClientState {
        FREE, // can be reused for a new connection
        ZOMBIE, // client has been disconnected, but don't reuse
        	// connection for a couple seconds
        CONNECTED, // has been assigned to a client, but no gamestate yet
        PRIMED, // gamestate has been sent, but client hasn't sent a usercmd
        ACTIVE // client is fully in game
    }

    public ClientState state = ClientState.FREE;
    public String userinfo; // name, etc..

    public String[] reliableCommands = new String[64];
    public int reliableSequence;
    public int reliableAcknowledge;
    public int reliableSent;
    public int messageAcknowledge;

    public int gamestateMessageNum;

    public PlayerInput lastUserCmd;
    public int lastMessageNum;
    public int lastClientCommand;
    public String lastClientCommandString = "";
    public SharedEntity gentity;
    public String name;

    public int deltaMessage;
//    public int nextReliableTime;
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
    public int id;

    // Downloading
    String downloadName = null;
    NetBuffer download = null;
    int downloadSize; // total size
    int downloadCount; // bytes sent
    int downloadClientBlock; // last block we sent to the client, awaiting ack
    int	downloadCurrentBlock;	// current block number
    int	downloadXmitBlock;	// last block we xmited
    byte[][] downloadBlocks = new byte[MAX_DOWNLOAD_WINDOW][];	// the buffers for the download blocks
    int[] downloadBlockSize = new int[MAX_DOWNLOAD_WINDOW];
    boolean downloadEOF;		// We have sent the EOF block
    int	downloadSendTime;	// time we last got an ack from the client

    // Buffer up packets if netchan is clogged with fragmented stuff
    Queue<NetBuffer> netchan_queue = new LinkedList<NetBuffer>();

    public SvClient() {
        for (int i= 0; i < frames.length; i++) {
            frames[i] = new ClientSnapshot();
        }
    }
    

    private void UserMove(NetBuffer buf, boolean deltaCompressed) {
        if(deltaCompressed)
            deltaMessage = messageAcknowledge;
        else
            deltaMessage = -1;

        int cmdCount = buf.ReadInt();
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
            cmds[i] = PlayerInput.ReadDeltaUserCmd(buf, oldcmd);
            oldcmd = cmds[i];
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

    private void ClientThink(PlayerInput cmd) {
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
        gentity.s.ClientNum = id;

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
        // message times may be wrong across a changelevel
        if(lastPacketTime > Ref.server.time)
            lastPacketTime = Ref.server.time;

        if(state == ClientState.ZOMBIE && lastPacketTime < zombiepoint) {
            // using the client id cause the cl->name is empty at this point
            state = ClientState.FREE;
            return;
        }

        if(state != ClientState.FREE && state != ClientState.ZOMBIE && lastPacketTime < droppoint) {
            // wait several frames so a debugger session doesn't
            // cause a timeout
            if(++timeoutCount > 5) {
                DropClient("timed out");
                state = ClientState.FREE;
            }
        } else
            timeoutCount = 0;
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
        BuildClientSnapshot();

        NetBuffer msg = NetBuffer.GetNetBuffer(false);
        msg.allowOverflow = true;
        // let the client know which reliable clientCommands we have received
        msg.Write(lastClientCommand);
        // (re)send any reliable server commands
        UpdateServerCommandsToClient(msg);

        // send over all the relevant entityState_t
        // and the playerState_t
        WriteSnapshotToClient(msg);

        WriteDownloadToClient(msg);

        SendMessageToClient(msg);
    }

    private void BuildClientSnapshot() {
        // bump the counter used to prevent double adding
        Ref.server.sv.snapshotCounter++;

        // this is the frame we are creating
        ClientSnapshot frame = frames[netchan.outgoingSequence & 31];

        // clear everything in this snapshot
        frame.num_entities = 0;

        if(gentity == null || state == ClientState.ZOMBIE)
            return;

        // grab the current playerState_t
        PlayerState ps = Ref.server.GameClientNum(id);
        ps.Clone(frame.ps);

        // never send client's own entity, because it can
        // be regenerated from the playerstate
        int clientnum = frame.ps.clientNum;
        if(clientnum < 0 || clientnum >= Common.MAX_GENTITIES)
        {
            System.err.println("bad gEnt!");
        }

        SvEntity svEnt = Ref.server.sv.svEntities[clientnum];
        svEnt.snapshotCounter = Ref.server.sv.snapshotCounter;

        // find the client's viewpoint
        ArrayList<Integer> snapEntNums = new ArrayList<Integer>();

        // add all the entities directly visible to the eye, which
        // may include portal entities that merge other viewpoints
        AddEntitiesVisibleFromPoint(ps.origin, frame, snapEntNums, false);

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

    private void AddEntitiesVisibleFromPoint(Vector2f origin, ClientSnapshot frame, ArrayList<Integer> snapEntNums, boolean b) {
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

            if(ent.s.ClientNum != i) {
                Common.LogDebug("Fixing ent.s number, was: " + ent.s.ClientNum + ", should be: " + i);
                ent.s.ClientNum = i;
            }

            // entities can be flagged to explicitly not be sent to the client
            if(ent.r.svFlags.contains(SvFlags.NOCLIENT))
                continue;

            if(ent.r.svFlags.contains(SvFlags.SINGLECLIENT))
                if(ent.r.singleClient != frame.ps.clientNum)
                    continue;

            if(ent.r.svFlags.contains(SvFlags.NOTSINGLECLIENT))
                if(ent.r.singleClient == frame.ps.clientNum)
                    continue;

            if(ent.r.svFlags.contains(SvFlags.CLIENTMASK)) {
                if(frame.ps.clientNum >= 32)
                    Common.LogDebug("ClientMask >= 32");
                if((~ent.r.singleClient & (1 << frame.ps.clientNum)) == (1 << frame.ps.clientNum))
                    continue;
            }

            SvEntity svEnt = Ref.server.SvEntityForGentity(ent);

            // don't double add an entity through portals
            if(svEnt.snapshotCounter == Ref.server.sv.snapshotCounter)
                continue;

            // broadcast entities are always sent
            if(ent.r.svFlags.contains(SvFlags.BROADCAST)) {
                if(ent.s.eType >= EntityType.EVENTS) {
                    Common.LogDebug("Broadcasting event");
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

        snapEntNums.add(ent.s.ClientNum);
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
                Common.LogDebug("Delta requit from out of date entities");
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

        int snapflags = Ref.server.snapFlagServerBit;
        if(rateDelayed)
            snapflags |= CLSnapshot.SF_DELAYED;
        if(state != ClientState.ACTIVE)
            snapflags |= CLSnapshot.SF_NOT_ACTIVE;

        msg.Write(snapflags);

        // delta encode the playerstate
        if(oldframe != null)
            frame.ps.WriteDelta(msg, oldframe.ps);
        else
            frame.ps.WriteDelta(msg, null);

        // delta encode the entities
        EmitPacketEntities(oldframe, frame, msg);
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
                newnum = newent.ClientNum;
            }

            if (oldindex >= fromnumentities)
                oldnum = 9999;
            else
            {
                oldent = Ref.server.snapshotEntities[(from.first_entity + oldindex) % Ref.server.numSnapshotEntities];
                oldnum = oldent.ClientNum;
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

        msg.Write(Common.MAX_GENTITIES-1);    // end of packetentities
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
        if(state != SvClient.ClientState.PRIMED && state != SvClient.ClientState.ACTIVE)
            return;

        reliableSequence++;

        // if we would be losing an old command that hasn't been acknowledged,
        // we must drop the connection
        // we check == instead of >= so a broadcast print added by SV_DropClient()
        // doesn't cause a recursive drop client
        if(reliableSequence - reliableAcknowledge == 64 +1) {
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

        NetBuffer buf = NetBuffer.GetNetBuffer(false);
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
            if(bases == null || bases.ClientNum <= 0)
                continue;

            buf.Write(SVC.OPS_BASELINE);
            bases.WriteDeltaEntity(buf, nullstate, true);
        }



        buf.Write(SVC.OPS_EOF);
        buf.Write(id);
        SendMessageToClient(buf);
    }

    private void SendMessageToClient(NetBuffer buf) {
        int size = buf.GetBuffer().position();
        frames[netchan.outgoingSequence & 31].messageSize = size;
        frames[netchan.outgoingSequence & 31].messageSent = Ref.server.time;
        frames[netchan.outgoingSequence & 31].messageAcked = -1;

        // send the datagram
        Transmit(buf);

        // set nextSnapshotTime based on rate and requested number of updates
        int rateMsec = RateMsec(size);
        if(rateMsec < snapshotMsec)  {
            // never send more packets than this, no matter what the rate is at
            rateMsec = snapshotMsec;
            rateDelayed = false;
        } else
            rateDelayed = true;

        nextSnapshotTime = Ref.server.time + rateMsec;

        // don't pile up empty snapshots while connecting
        if(state != ClientState.ACTIVE) {
            // a gigantic connection message may have already put the nextSnapshotTime
            // more than a second away, so don't shorten it
            // do shorten if client is downloading
            if(nextSnapshotTime < Ref.server.time)
                nextSnapshotTime = Ref.server.time;
        }

    }

    private int RateMsec(int bytes) {
        int headerSize = 48;
        int rateMsec = (int)((float)(bytes + headerSize) * 1000f  / rate);
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
//                System.out.println("Popping queued message");
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
        if(serverid != Ref.server.sv.serverid && downloadName == null && !lastClientCommandString.equals("nextdl")) {
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
        int cmd;
        do {
            cmd = buf.ReadInt();

            if(cmd == CLC.OPS_EOF)
                break;

            if(cmd != CLC.OPS_CLIENTCOMMAND)
                break;

            if(!ClientCommand(buf))
                break;  // we couldn't execute it because of the flood protection

            if(state == ClientState.ZOMBIE)
                return; // disconnect command

        } while(true);

        switch(cmd) {
            case CLC.OPS_MOVE:
                UserMove(buf, true);
                break;
            case CLC.OPS_MOVENODELTA:
                UserMove(buf, false);
                break;
            default:
                if(cmd != CLC.OPS_EOF)
                    Common.Log(name + ": Warning: bad command byte");
                break;
        }
    }

    private boolean ClientCommand(NetBuffer buf) {
        int seq = buf.ReadInt();
        String s = buf.ReadString();

        // see if we have already executed it
        if(lastClientCommand >= seq)
            return true;

//        System.out.println(name + ": ClientCmd: " + seq + ":" + s);
        // drop the connection if we have somehow lost commands
        if(seq > lastClientCommand + 1) {
            Common.Log(name + ": Lost clientcommands. Dropping");
            DropClient( "Lost reliable commands");
            return false;
        }

//        // don't allow another command for one second
//        nextReliableTime = Ref.server.time + 1000;

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
        else if(cmd.equalsIgnoreCase("download"))
            BeginDownload(tokens);
        else if(cmd.equalsIgnoreCase("nextdl"))
            NextDownload(tokens);
        else if(cmd.equalsIgnoreCase("stopdl"))
            StopDownload();
        else if(cmd.equalsIgnoreCase("donedl"))
            DoneDownload();
        else if (Ref.server.sv.state == ServerState.GAME)
            Ref.game.Client_Command(id, tokens);
    }

    private void BeginDownload(String[] tokens) {
        // Stop any existing download
        CloseDownload();

        downloadName = tokens[1];
    }

    private void WriteDownloadToClient(NetBuffer msg) {
        if(downloadName == null)
            return;

        String errorMessage = null;

        if(download == null) {
            // Client is requesting the current map
            if(downloadName.equalsIgnoreCase("map")) {

                if(Ref.cvars.Find("mapname").sValue.equalsIgnoreCase("custom") && !Ref.game.level.editmode) {
                    // Server is running custom map, but not editmode, so try to load the cached custom map
                    try {
                        download = ResourceManager.OpenFileAsNetBuffer("custom", false).getKey();
                    } catch (IOException ex) {
                        Logger.getLogger(SvClient.class.getName()).log(Level.SEVERE, null, ex);
                        download = Ref.cm.cm.SerializeMap(); // fallback to generation
                    }
                }
                else // in editmode - pack up current map
                    download = Ref.cm.cm.SerializeMap();
            } else {
                // Asking for regular file
                try {
                    download = ResourceManager.OpenFileAsNetBuffer(downloadName, false).getKey();
                } catch (IOException ex) {
                    errorMessage = "File not found.";
                }
            }
            downloadSize = download.GetBuffer().limit();

            if(downloadSize <= 0 || errorMessage != null) {
                msg.Write(SVC.OPS_DOWNLOAD);
                msg.Write(0); // first chunk
                msg.Write(-1); // error size
                msg.Write(errorMessage); // error msg
                downloadName = null;
                CloseDownload();
                return;
            }

            Common.LogDebug("Beginnign download of " + downloadName);
            downloadCurrentBlock = downloadCount = downloadXmitBlock = downloadClientBlock = 0;
            downloadEOF = false;
        }

        // Perform any reads that we need to
        while(downloadCurrentBlock - downloadClientBlock < MAX_DOWNLOAD_WINDOW
                && downloadSize != downloadCount) {
            int curindex = downloadCurrentBlock % MAX_DOWNLOAD_WINDOW;
            if(downloadBlocks[curindex] == null)
                downloadBlocks[curindex] = new byte[MAX_DOWNLOAD_BLKSIZE];

            int lenght = MAX_DOWNLOAD_BLKSIZE;
            if(download.GetBuffer().remaining() < lenght)
                lenght = download.GetBuffer().remaining();
            downloadBlockSize[curindex] = lenght;
            
            if(lenght > 0) {
                try {
                    download.GetBuffer().get(downloadBlocks[curindex], 0, lenght);
                } catch(BufferUnderflowException e) {
                    Common.LogDebug("unexpected eof");
                    lenght = 0;
                }
            }

            // EOF now
            if(lenght == 0) {
                downloadCount = downloadSize;
                break;
            }

            downloadCount += downloadBlockSize[curindex];
            // load next block
            downloadCurrentBlock++;
        }

        // Check to see if we have eof condition and add the EOF block
        if(downloadCount == downloadSize && !downloadEOF &&
                downloadCurrentBlock - downloadClientBlock < MAX_DOWNLOAD_WINDOW) {
            downloadBlockSize[downloadCurrentBlock % MAX_DOWNLOAD_WINDOW] = 0;
            downloadCurrentBlock++;
            downloadEOF = true;
        }

        // Loop up to window size times based on how many blocks we can fit in the
	// client snapMsec and rate

	// based on the rate, how many bytes can we fit in the snapMsec time of the client
	// normal rate / snapshotMsec calculation
        int blocksPerSnap = (int) (((rate * snapshotMsec) / 1000f + MAX_DOWNLOAD_BLKSIZE) / MAX_DOWNLOAD_BLKSIZE);
        if(blocksPerSnap <= 0)
            blocksPerSnap = 1;

//        System.out.println(""+blocksPerSnap);

        while(blocksPerSnap-- > 0) {
            // Write out the next section of the file, if we have already reached our window,
            // automatically start retransmitting
            if(downloadClientBlock == downloadCurrentBlock)
                return; // nothing to transmit

            if(downloadXmitBlock == downloadCurrentBlock) {
                // We have transmitted the complete window, should we start resending?
                //FIXME:  This uses a hardcoded one second timeout for lost blocks
                //the timeout should be based on client rate somehow
                if(Ref.server.time - downloadSendTime > 1000) {
                    downloadXmitBlock = downloadClientBlock;
                } else
                    return;
            }

            // Send current block
            int curindex = downloadXmitBlock % MAX_DOWNLOAD_WINDOW;
            msg.Write(SVC.OPS_DOWNLOAD);
            msg.Write(downloadXmitBlock);

            // block zero is special, contains file size
            if(downloadXmitBlock == 0) {
                msg.Write(downloadSize);
            }

            msg.Write(downloadBlockSize[curindex]);

            // Write the block
            if(downloadBlockSize[curindex] > 0) {
                msg.Write(downloadBlocks[curindex], 0, downloadBlockSize[curindex]);
            }

//            System.out.println("writing block " + downloadXmitBlock);
            downloadXmitBlock++;
            downloadSendTime = Ref.server.time;
        }


    }

    private void CloseDownload() {
        if(download != null)
            download = null;

        download = null;
        downloadName = null;
    }

    private void NextDownload(String[] tokens) {
        int block = Integer.parseInt(tokens[1]);
        if(block == downloadClientBlock) {

            // Find out if we are done.  A zero-length block indicates EOF
            if(downloadBlockSize[downloadClientBlock % MAX_DOWNLOAD_WINDOW] == 0) {
                Common.LogDebug("File " + downloadName + " complete.");
                CloseDownload();
                return;
            }

            downloadSendTime = Ref.server.time;
            downloadClientBlock++;
            return;
        }

        // We aren't getting an acknowledge for the correct block, drop the client
	// FIXME: this is bad... the client will never parse the disconnect message
	//			because the cgame isn't loaded yet
        DropClient("broken download");
    }

    private void StopDownload() {
        if(downloadName != null)
            Common.LogDebug("Aborting file download: " + downloadName);

        CloseDownload();
    }

    private void DoneDownload() {
//        System.out.println("download done");
//        SendClientGameState();
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
        if(state == ClientState.ZOMBIE)
            return;

        // TODO: see if we already have a challenge for this ip
        Common.Log("Dropping client");

        // tell everyone why they got dropped
        Ref.server.SendServerCommand(null, String.format("print \"%s %s\"\n", name, reason));
        Ref.game.Client_Disconnect(gentity.s.ClientNum);
        Ref.server.SendServerCommand(this, String.format("disconnect \"%s\"\n", reason));

        SetUserInfo("");
        state = ClientState.ZOMBIE;
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
}
