package cubetech.collision;

//import cubetech.common.Common;
//import cubetech.common.Common.ErrorCode;
//import cubetech.common.ICommand;
//import cubetech.misc.Ref;
//import cubetech.net.NetBuffer;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.channels.FileChannel;
//import java.util.Random;
//import java.util.logging.Level;
//import java.util.logging.Logger;

/**
 * Contains the currently loaded level. Handles all collision with it
 * @author mads
 */
public class ClipMap {
    public CMap cm; // Currently loaded block map
    public CubeMap cubemap = null; // Currently loaded cube map
   

    public ClipMap() {

//        Ref.commands.AddCommand("savemap", new ICommand() {
//            public void RunCommand(String[] args) {
//                if(args.length != 2)
//                {
//                    Common.Log("usage: savemap <filename>");
//                    return;
//                }
//
//                SaveBlockMap(args[1]);
//            }
//        });

    }

    public void GenerateCubeMap(long seed) {
        cubemap = new CubeMap(seed, 4, 4, 2);
    }

    public void ClearCubeMap() {
        // Maybe save old map?
        cubemap = null;
    }

    public void EmptyCubeMap() {
        // Create an empty cubemap. Multiplayer clients dont load the map directly, but get it
        // in chunks over the network.
        cubemap = new CubeMap();
    }

    

//    public int LoadBlockMap(String name, boolean clientLoad) throws ClipmapException {
//        if(name == null || name.isEmpty())
//        {
//            Ref.common.Error(ErrorCode.DROP, "LoadMap: Null name");
//            return 0;
//        }
//
//        // Server already loaded the map for us
//        if(cm != null && cm.name.equalsIgnoreCase(name) && clientLoad) {
//            return cm.checksum;
//        }
//        if(cm != null)
//            ClearBlockMap();
//
//        // Lets try loading the map
//        cm = new CMap(name);
//
//        return cm.checksum;
//    }
//
//    public void SaveBlockMap(String filename) {
//        if (cm == null) {
//           return;
//        }
//
//        try {
//            ByteBuffer buf = cm.SerializeMap().GetBuffer();
//            FileChannel chan = new FileOutputStream(filename, false).getChannel();
//            chan.write(buf);
//            chan.close();
//            Common.Log("Saved map: " + filename);
//        } catch (IOException ex) {
//            Logger.getLogger(ClipMap.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//    public void LoadBlockMap(NetBuffer mapdata, boolean clientLoad) throws ClipmapException  {
//
//        // Server already loaded the map for us
//        if(cm != null && clientLoad) {
//            return;
//        }
//        if(cm != null)
//            ClearBlockMap();
//
//        // Lets try loading the map
//        cm = new CMap(mapdata);
//    }
//
//    public void ClearBlockMap() {
//        cm = null;
//        Ref.spatial.Clear();
//    }


}
