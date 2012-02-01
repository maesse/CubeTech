package cubetech.server;

import cubetech.Game.ClientPersistant;
import cubetech.common.Common;
import cubetech.gfx.ResourceManager;
import cubetech.misc.Ref;
import cubetech.net.NetBuffer;
import cubetech.net.NetChan;
import cubetech.net.SVC;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Server to client file transfers
 * @author Mads
 */
public class SvDownload {
    private static final int MAX_DOWNLOAD_WINDOW = 8;
    private static final int MAX_DOWNLOAD_BLKSIZE = 2400;
    
    private String downloadName = null;
    private NetBuffer download = null;
    private int downloadSize; // total size
    private int downloadCount; // bytes sent
    private int downloadClientBlock; // last block we sent to the client, awaiting ack
    private int	downloadCurrentBlock;	// current block number
    private int	downloadXmitBlock;	// last block we xmited
    private byte[][] downloadBlocks = new byte[MAX_DOWNLOAD_WINDOW][];	// the buffers for the download blocks
    private int[] downloadBlockSize = new int[MAX_DOWNLOAD_WINDOW];
    private boolean downloadEOF;		// We have sent the EOF block
    private int	downloadSendTime;	// time we last got an ack from the client
    private SvClient client;
    
    public SvDownload(SvClient client) {
        this.client = client;
    }
    
    
    public void BeginDownload(String filename) {
        // Stop any existing download
        closeDownload();
        downloadName = filename;
    }

    public void writeDownloadToClient(NetBuffer msg) {
        if(downloadName == null) return;

        String errorMessage = null;

        if(download == null) {
            if(downloadName.equals("@cube")) {
                ClientSnapshot frame = client.frames[client.netchan.outgoingSequence & 31];
                ClientPersistant pers = frame.pss[0].pers;
                if(pers == null) return;


                // we can go about this far without fragmenting the client
                int maxPacketSize = NetChan.FRAGMENT_SIZE-30;

                int leftOver = maxPacketSize - msg.GetBuffer().position();
                //if(leftOver > 1400) leftOver = 1400;
                leftOver *= 2f; // assume 50% compression ratio

                ByteBuffer buf = pers.dequeueChunkData(leftOver);
                if(buf == null) return;
                download = new NetBuffer(buf);
//            }
            // Client is requesting the current map
//            else if(downloadName.equalsIgnoreCase("map")) {

//                if(Ref.cvars.Find("mapname").sValue.equalsIgnoreCase("custom") && !Ref.game.level.editmode) {
//                    // Server is running custom map, but not editmode, so try to load the cached custom map
//                    try {
//                        download = ResourceManager.OpenFileAsNetBuffer("custom", false).getKey();
//                    } catch (IOException ex) {
//                        Logger.getLogger(SvClient.class.getName()).log(Level.SEVERE, null, ex);
//                        download = Ref.cm.cm.SerializeMap(); // fallback to generation
//                    }
//                }
//                else // in editmode - pack up current map
//                    download = Ref.cm.cm.SerializeMap();
            } else {
                // Asking for regular file
                try {
                    download = ResourceManager.OpenFileAsNetBuffer(downloadName, false).getKey();
                } catch (IOException ex) {
                    errorMessage = "File not found.";
                }
            }
            

            if(download == null || download.GetBuffer().limit() <= 0 || errorMessage != null) {
                msg.Write(SVC.OPS_DOWNLOAD);
                msg.Write(0); // first chunk
                msg.Write(-1); // error size
                if(errorMessage == null) errorMessage = "unknown file error";
                msg.Write(errorMessage); // error msg
                downloadName = null;
                closeDownload();
                return;
            }

            downloadSize = download.GetBuffer().limit();

            if(!downloadName.startsWith("@")) Common.LogDebug("[Server] Starting file-upload: " + downloadName);
            
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
        int blocksPerSnap = (int) (((client.rate * client.snapshotMsec) / 1000f + MAX_DOWNLOAD_BLKSIZE) / MAX_DOWNLOAD_BLKSIZE);
        if(blocksPerSnap <= 1)
            blocksPerSnap = 2;

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
                msg.Write(downloadName);
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

    private void closeDownload() {
        if(download != null)
            download = null;

        download = null;
        downloadName = null;
    }

    // client sends back acks from blocks
    public void nextDownload(String[] tokens) {
        int block = Integer.parseInt(tokens[1]);
        if(block == downloadClientBlock) {

            // Find out if we are done.  A zero-length block indicates EOF
            if(downloadBlockSize[downloadClientBlock % MAX_DOWNLOAD_WINDOW] == 0) {
//                Common.LogDebug("File " + downloadName + " complete.");
                closeDownload();
                return;
            }

            downloadSendTime = Ref.server.time;
            downloadClientBlock++;
            return;
        }

        // We aren't getting an acknowledge for the correct block, drop the client
	// FIXME: this is bad... the client will never parse the disconnect message
	//			because the cgame isn't loaded yet
        client.DropClient("broken download");
    }
    
    public boolean isDownloading() {
        return downloadName != null;
    }

    public void StopDownload() {
        if(downloadName != null)
            Common.LogDebug("Aborting file download: " + downloadName);

        closeDownload();
    }

    
}
