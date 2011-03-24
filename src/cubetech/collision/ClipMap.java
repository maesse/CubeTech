package cubetech.collision;

import cubetech.common.Common.ErrorCode;
import cubetech.common.ICommand;
import cubetech.misc.Ref;
import cubetech.net.NetBuffer;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains the currently loaded level. Handles all collision with it
 * @author mads
 */
public class ClipMap {
    public CMap cm; // Currently loaded map

    public ClipMap() {
        Ref.commands.AddCommand("savemap", new ICommand() {
            public void RunCommand(String[] args) {
                if(args.length != 2)
                {
                    System.out.println("usage: savemap <filename>");
                    return;
                }

                SaveMap(args[1]);
            }
        });
    }

    public int LoadMap(String name, boolean clientLoad) throws ClipmapException {
        if(name == null || name.isEmpty())
        {
            Ref.common.Error(ErrorCode.DROP, "LoadMap: Null name");
            return 0;
        }

        // Server already loaded the map for us
        if(cm != null && cm.name.equalsIgnoreCase(name) && clientLoad) {
            return cm.checksum;
        }
        if(cm != null)
            ClearMap();

        // Lets try loading the map
        cm = new CMap(name);

        return cm.checksum;
    }

    public void SaveMap(String filename) {
        if (cm == null) {
           return;
        }

        try {    
            ByteBuffer buf = cm.SerializeMap().GetBuffer();
            FileChannel chan = new FileOutputStream(filename, false).getChannel();
            chan.write(buf);
            chan.close();
            System.out.println("Saved map: " + filename);
        } catch (IOException ex) {
            Logger.getLogger(ClipMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void LoadMap(NetBuffer mapdata, boolean clientLoad) throws ClipmapException  {

        // Server already loaded the map for us
        if(cm != null && clientLoad) {
            return;
        }
        if(cm != null)
            ClearMap();

        // Lets try loading the map
        cm = new CMap(mapdata);
    }

    public void ClearMap() {
        cm = null;
        Ref.spatial.Clear();
    }


}
