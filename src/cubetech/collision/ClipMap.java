package cubetech.collision;

import cubetech.Game.PhysicsSystem;
import cubetech.gfx.ResourceManager;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Contains the currently loaded level. Handles all collision with it
 * @author mads
 */
public class ClipMap {
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
        IChunkGenerator gen = new PerlinChunkGenerator();
        //IChunkGenerator gen = new ChunkGenerator();
        gen.setSeed(seed);
        cubemap = new CubeMap(gen, 4, 4, 2);
    }

    public void ClearCubeMap() {
        // Maybe save old map?
        if(cubemap != null)cubemap.destroy();
        cubemap = null;
        
    }

    public void EmptyCubeMap() {
        // Create an empty cubemap. Multiplayer clients dont load the map directly, but get it
        // in chunks over the network.
        cubemap = new CubeMap();
    }

    public void loadMap(String filename) throws IOException {
        if(filename == null || filename.isEmpty()) return;
        ByteBuffer data =  ResourceManager.OpenFileAsByteBuffer("maps\\" + filename + ".map", false).getKey();

        IChunkGenerator gen = new PerlinChunkGenerator();
        //gen.setSeed(seed);
        cubemap = new CubeMap();
        CubeMap.unserialize(data, cubemap.chunks);
        cubemap.chunkGen = gen;
        

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
