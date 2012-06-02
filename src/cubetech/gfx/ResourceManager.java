package cubetech.gfx;

import java.util.Queue;
import java.util.LinkedList;
import cubetech.misc.Callback;
import org.lwjgl.opengl.EXTFramebufferSRGB;
import cubetech.gfx.Resource.ResourceType;
import java.util.EnumSet;
import cubetech.misc.Profiler.SecTag;
import cubetech.misc.Profiler;
import java.util.Iterator;
import cubetech.iqm.IQMLoader;
import cubetech.iqm.IQMModel;
import cubetech.common.*;
import cubetech.misc.ClassPath;
import cubetech.misc.Profiler.Sec;
import java.util.HashMap;
import cubetech.misc.Ref;
import java.util.AbstractMap;
import java.nio.channels.FileChannel;
import cubetech.net.NetBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;
import java.awt.Image;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import javax.swing.ImageIcon;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

/**
 * Requires cvars
 * @author mads
 */
public final class ResourceManager {
    private static String[] matList = null;
    private static final int MAX_LOAD_PER_UPDATE = 1;
    // Loaded resources
    private HashMap<String, Resource> Ressources = new HashMap<String, Resource>();
    
    // Resources queued for loading
    private Queue<Resource> unloadedRessources = new LinkedList<Resource>();
    
    private TextureLoader textureLoader = new TextureLoader();
    
    // Config
    static CVar devpath;
    static CVar tex_srgb;
    static CVar tex_verbose;

    public ResourceManager() {
        // tmp
        devpath = Ref.cvars.Get("devpath",
                "C:\\Users\\mads\\Documents\\NetBeansProjects\\CubeTech\\src\\cubetech\\",
                EnumSet.of(CVarFlags.ARCHIVE));
        
        tex_srgb = Ref.cvars.Get("tex_srgb", "1", EnumSet.of(CVarFlags.ARCHIVE));
        tex_verbose = Ref.cvars.Get("tex_verbose", "0", EnumSet.of(CVarFlags.ARCHIVE));
        Ref.commands.AddCommand("tex_reload", cmd_reloadtex);
    }

    // fix
    public void Cleanup() {
        cleanupModels();
    }

    public void cleanupModels() {
        Iterator<Resource> it = Ressources.values().iterator();
        while(it.hasNext()) {
            Resource res = it.next();
            if(res.Type == Resource.ResourceType.MODEL) {
                if(res.Data != null) ((IQMModel)res.Data).destroy();
                res.Data = null;
                it.remove();
            }
        }
    }

    public void Update() {
        if(Ref.glRef == null || !Ref.glRef.isInitalized()) return; // OpenGL not ready
        SecTag s = Profiler.EnterSection(Sec.TEXMAN);
        
        // Check cvar modification
        if(tex_srgb.modified) {
            if(!tex_srgb.isTrue()) {
                glDisable(EXTFramebufferSRGB.GL_FRAMEBUFFER_SRGB_EXT);
            } else {
                glEnable(EXTFramebufferSRGB.GL_FRAMEBUFFER_SRGB_EXT);
            }
            if(tex_srgb.modificationCount > 1) {
                cmd_reloadtex.RunCommand(null);
            }
            tex_srgb.modified = false;
        }

        // Load queued textures
        int nLoaded = 0;
        while(!unloadedRessources.isEmpty() && nLoaded < MAX_LOAD_PER_UPDATE) {
            Resource res = unloadedRessources.poll();
            if(res.loaded) {
                // has been loaded already
                continue;
            }
            
            try {
                textureLoader.getTexture(res);
                res.loaded = true;
            } catch (Exception ex) {
                Common.Log(ex);
            }

            nLoaded++;
        }

        s.ExitSection();
    }

    public IQMModel loadModel(String modelName) {
        if(modelName == null) return null;
        String filename = Helper.stripPath(modelName).toLowerCase();
        if(filename == null || filename.isEmpty()) return null; // no go
        if(Ressources.containsKey(filename)) return (IQMModel)Ressources.get(filename).Data; // cached

        String orgName = modelName;
        modelName = devpath.sValue + modelName;
        // load
        IQMModel model;
        try {
            // Try to load it directly
            model = IQMLoader.LoadModel(orgName);
        } catch (IOException ex) {
            try {
                Common.Log("Falling back to dev-path for " + orgName);
                // Fallback to hard-coded dev path
                model = IQMLoader.LoadModel(modelName);
            } catch(IOException ex2) {
                Common.Log("Couldn't load model " + modelName + ": " + Common.getExceptionString(ex2));
                return null;
            }
        }

        // Cache it
        Resource res = new Resource(filename, Resource.ResourceType.MODEL, 0);
        res.Data = model;
        res.loaded = true;
        Ressources.put(filename, res);


        return model;
    }

    public CubeTexture LoadTexture(String filename) {
        return LoadTexture(filename, false);
    }

    public CubeTexture LoadTexture(String filename, boolean cubemap)  {
        // Check if it is cached
        if(Ressources.containsKey(filename)) {
            return (CubeTexture)Ressources.get(filename).Data;
        } else if(Ressources.containsKey(filename.toLowerCase())) {
            return (CubeTexture)Ressources.get(filename.toLowerCase()).Data;
        }
        
        // Validate file extension
        if(!cubemap) {
            // Strip off path, so we can check if it is already loaded
            int idx = filename.lastIndexOf('.');
            if(idx <= 0) {
                System.err.println("LoadResource: Filename invalid, extension required (eg .png): " + filename);
                return null;
            }
            
            // Try to load
            String name = filename.substring(idx); // get filetype
            boolean nameOk = name.equalsIgnoreCase(".png") || name.equalsIgnoreCase(".jpg") || name.equalsIgnoreCase(".tga");
            if(!nameOk) {
                Common.Log("ResMan.LoadTexture: I don't understand this format: " + name);
                return null;
            }
        }
        
        // Going to load it..
        CubeTexture tex = null;
        // Create unloaded resource
        Resource res = new Resource(filename, Resource.ResourceType.TEXTURE, cubemap?GL_TEXTURE_CUBE_MAP:GL_TEXTURE_2D);

        if(Ref.glRef == null || !Ref.glRef.isInitalized()) {
            // OpenGL not ready, do defered loading
            tex = textureLoader.getUnloadedTexture(res);
            unloadedRessources.add(res);
        } else { // else try regular loading
            try {
                tex = textureLoader.getTexture(res);
            } catch (Exception ex) { // it's a no go
                String error = "Cannot load texture: File not found: " + filename;
                if(Ref.common.isDeveloper()) {
                    error = error + "(" + Common.getExceptionString(ex) + ")";
                }
                Common.Log(error);
                tex = textureLoader.getUnloadedTexture(res);// Fallback to blank texture -- will never load as there isn't a point
            }
            res.loaded = true;
        }

        // Cache resource
        Ressources.put(filename.toLowerCase(), res);
        return tex;
    }

    

    public Image loadImage2(String path) throws IOException {
        URL url = getClassLoader().getResource("cubetech/"+path);
        if(url == null) {
            File file = new File(path);
            if(file == null || !file.canRead()) throw new FileNotFoundException("Cannot find file: " + path);
            else {
                return new ImageIcon(file.getPath()).getImage();
            }
        }
        
        Image img = new ImageIcon(url).getImage();
        return img;
    }

    public static AbstractMap.SimpleEntry<ByteBuffer, Integer> OpenFileAsByteBuffer(String file, boolean direct) throws FileNotFoundException, IOException {
        FileInputStream fis = null;
        if(Ref.common != null && Ref.common.isDeveloper()) {
            try {
                fis = new FileInputStream(devpath.sValue+file);
            } catch(Exception ex) {
                fis = new FileInputStream(file);
            }
        } else {
            fis = new FileInputStream(file);
        }
        FileChannel fc = fis.getChannel();
        int lenght = (int)fc.size(); // file size
        byte[] backingArray = new byte[1024*256];
        // Allocatedirect is faster, but doesn't expose any way for
        // fast byte traversal, that we need to checksum calculation
        //ByteBuffer bb = ByteBuffer.allocateDirect(1024*256); // 256k reads
        ByteBuffer bb = ByteBuffer.wrap(backingArray); // 256k reads

        ByteBuffer destBuffer = null; // Whole file goes here
        if(direct)
            destBuffer = ByteBuffer.allocateDirect(lenght);
        else
            destBuffer = ByteBuffer.allocate(lenght);
        destBuffer.order(ByteOrder.nativeOrder());
        int nRead, nTotal = 0;
        int checksum = 0;
        while((nRead = fc.read(bb)) != -1) {
            if(nRead == 0)
                continue; // Waiting for I/O

            // calculate checksum
            for (int i= 0; i < nRead; i++) {
                checksum += backingArray[i];
            }

            // Put into destination buffer
            destBuffer.put(backingArray, 0, nRead);

            // Keep track of size
            nTotal += nRead;
            if(nTotal >= lenght)
                break;

            // Clear bb for new read
            bb.clear();
        }
        // Clear destination buffer for read
        destBuffer.position(0);
        destBuffer.limit(nTotal);

        return new AbstractMap.SimpleEntry<ByteBuffer, Integer>(destBuffer, checksum);
    }

    public static boolean FileExists(String path) {
        URL url = getClassLoader().getResource("cubetech/"+path);
        File file = null;
        if(url == null) {
            url = getClassLoader().getResource(path);
            if(url == null) {
                // Try looking on the filesystem
                file = new File(path);
                if(file == null || !file.canRead()) return false;
                else return true;
            }
            return true;
        }
        return true;
    }

    public static File OpenFileAsFile(String path) {
        File file = null;
        
        if(Ref.common != null && Ref.common.isDeveloper()) {
            file = new File(devpath.sValue+path);
            if(!file.exists() || file.isDirectory()) file = null;
        }
        
        if(file == null) {
            URL url = getClassLoader().getResource("cubetech/"+path);
            if(url != null) file = new File(url.getFile());
        }
        
        if(file == null) {
            URL url = getClassLoader().getResource(path);
            if(url != null) file = new File(url.getFile());
        }
        
        if(file == null) {
            // Try looking on the filesystem
            file = new File(path);
            if(!file.exists() || file.isDirectory()) file = null;
        }
        return file;
    }

    // Figures out what materials we have
    
    public static String[] getMaterialList() {
        if(matList != null)
            return matList; // cache
        
        try {
            String[] strs = ClassPath.getClasspathFileNamesWithExtension(".mat");

            if(!Ref.glRef.isApplet()) {
                try {
                    File dir = new File("data");
                    String[] files =  dir.list(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            if(name.length() > 4 && name.substring(name.length()-4).equalsIgnoreCase(".mat"))
                                return true;
                            return false;
                        }
                    });

//                    System.out.println("Local filecount: " + files.length);

                    String[] data = new String[strs.length + files.length];
                    System.arraycopy(strs, 0, data, 0, strs.length);
                    for (int i= 0; i < files.length; i++) {
                        data[strs.length + i] = "data/" + files[i];
                    }
                    strs = data;
                    
                } catch(Exception ex) {
                    System.out.println(ex);
                }
            }
            matList = strs;
            return strs;
        } catch (ZipException ex) {
            Logger.getLogger(ResourceManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ResourceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }

    public static BufferedInputStream OpenFileAsInputStream(String path) throws IOException {
        if(!FileExists(path))
            throw new FileNotFoundException("Cannot find file: " + path);

        URL url = getClassLoader().getResource("cubetech/"+path);
        File file = null;
        InputStream stream = null;
        if(url == null) {
            url = getClassLoader().getResource(path);
            
            if(url == null) {
                // Try looking on the filesystem
                file = new File(path);
                if(file == null || !file.canRead()) throw new FileNotFoundException("Cannot find file: " + path);
                else {

                    stream  = new FileInputStream(file);

                }
            }
        }
        if(stream == null)
            stream = url.openStream();
        BufferedInputStream bis = new BufferedInputStream(stream);
        return bis;
    }

    public static void SaveStringToFile(String path, String str) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(path, false));
        writer.write(str);
        writer.flush();
        writer.close();
    }

    public static boolean CreatePath(String string) {
        File path = new File(Helper.getPath(string));
        if(path.isDirectory())
            return true;
        try {
            return path.mkdirs();
        } catch(Exception ex) {}
        return false;
    }

    public static void SaveInputStreamToFile(InputStream str, String dst) throws IOException {
        FileOutputStream fos = new FileOutputStream(dst, false);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while((bytesRead = str.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }
        fos.flush();
        fos.close();
    }

    public static AbstractMap.SimpleEntry<NetBuffer, Integer> OpenFileAsNetBufferDisk(String path, boolean direct) {
        // Try looking on the filesystem
        File file = new File(path);
        if(file == null || !file.canRead()) return null;
        else {

            AbstractMap.SimpleEntry<ByteBuffer, Integer> result;
            try {
                result = OpenFileAsByteBuffer(file.getPath(), direct);
            }catch (IOException ex) {
                return null;
            }
            return new AbstractMap.SimpleEntry<NetBuffer, Integer>(NetBuffer.CreateCustom(result.getKey()), result.getValue());

        }
    }

    // Tries to open a stream to the given path. Checks the JAR first, then the FS
    public static AbstractMap.SimpleEntry<NetBuffer, Integer> OpenFileAsNetBuffer(String path, boolean direct) throws IOException {
        if(!Ref.glRef.isApplet()) {
            CVar dev = Ref.cvars.Find("developer");
            if(dev != null && dev.isTrue()) {
                AbstractMap.SimpleEntry<NetBuffer, Integer> fromDisk =
                        OpenFileAsNetBufferDisk(devpath.sValue+path, direct);
                if(fromDisk != null) return fromDisk;
            }
            AbstractMap.SimpleEntry<NetBuffer, Integer> fromDisk = OpenFileAsNetBufferDisk(path, direct);
            if(fromDisk != null) return fromDisk;
        }

        // Look for the resource
        URL url = getClassLoader().getResource("cubetech/"+path);
        if(url == null) {
            url = getClassLoader().getResource(path);
            if(url == null) {
                AbstractMap.SimpleEntry<NetBuffer, Integer> fromDisk = OpenFileAsNetBufferDisk(path, direct);
                if(fromDisk != null) return fromDisk;
                throw new IOException("File not found: " + path);
            }
        }

        // Open as a stream. Won't know the final filesize before it is all read,
        // so we'll have to do some extra copying
        BufferedInputStream bis = new BufferedInputStream(url.openStream());

        int loadChunkSize = 1024*256;
        byte[] data = new byte[loadChunkSize]; // Load in mb chunks
        ArrayList<byte[]> extraData = new ArrayList<byte[]>();
        int nRead, nTotal = 0;

        // Read untill we hit EOF
        int checksum = 0;
        while((nRead = bis.read(data)) != -1) {
            if(nRead == 0)
                continue;
            nTotal += nRead;
            for (int i= 0; i < nRead; i++) {
                checksum += data[i]; // simple checksum
            }
            extraData.add(data);
            data = new byte[loadChunkSize];
        }

        // Create the final buffer now that we know the size
        ByteBuffer buf = null;
        if(direct)
            buf = ByteBuffer.allocateDirect(nTotal);
        else
            buf = ByteBuffer.allocate(nTotal);

        // Concat the buffers
        for (int i= 0; i < extraData.size(); i++) {
            buf.put(extraData.get(i), 0, Math.min(loadChunkSize, nTotal));
            nTotal -= loadChunkSize;
        }
        buf.position(0);
        return new AbstractMap.SimpleEntry<NetBuffer, Integer>(NetBuffer.CreateCustom(buf), checksum);
    }

    public CubeTexture getNoNormalTexture() {
        return textureLoader.noNormalTexture;
    }
    
    public CubeTexture getNoSpecularTexture() {
        return textureLoader.noSpecularTexture;
    }
    
    public void SetWhiteTexture() {
        textureLoader.whiteTexture.Bind();
    }

    public CubeTexture getWhiteTexture() {
        return textureLoader.whiteTexture;
    }
    
    public static ClassLoader getClassLoader() {
        try {
            // Got permission?
            SecurityManager mngr = System.getSecurityManager();
            if(mngr != null) mngr.checkPermission(new RuntimePermission("getClassLoader"));
            return ResourceManager.class.getClassLoader();
        } catch(SecurityException ex) {
            // We're not allowed :(
            // Do it anyway
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return ResourceManager.class.getClassLoader();
                }
            });
        }
    }

    private ICommand cmd_reloadtex = new ICommand() {
        public void RunCommand(String[] args) {
            Common.Log("Reloading all textures...");
            long startTime = System.nanoTime();
            int count = 0;
            for (Resource resource : Ressources.values()) {
                if(resource.Type != ResourceType.TEXTURE) continue;
                if(!resource.loaded) continue;
                if(resource.target != GL_TEXTURE_2D &&
                        resource.target != GL_TEXTURE_CUBE_MAP) continue;
                try {
                    textureLoader.getTexture(resource);
                    count++;
                } catch (Exception ex) {
                    Common.Log(ex);
                }
            }
            startTime = (System.nanoTime() - startTime);
            Common.Log("Reloaded %d textures (took %.2fms)", count, startTime/1000000f);

        }
    };

    public Callback<File, Void> onFileModified = new Callback<File, Void>() {
        public Void execute(File e, Object tag) {
            if(tag instanceof String == false) return null;
            String filename = (String)tag;
            
            Common.LogDebug("Detected file modification for " + filename);
            
            Resource res = Ressources.get(filename.toLowerCase());
            if(res == null) {
                Common.LogDebug("Can't find texture in cache.. aborting reload");
            } else if(res.Type == ResourceType.TEXTURE && res.loaded && 
                    (res.target == GL_TEXTURE_2D || res.target == GL_TEXTURE_CUBE_MAP)) {

                res.loaded = false;
                unloadedRessources.add(res);
                Common.LogDebug("Queued file for loading");
            } else {
                Common.LogDebug("Won't try to load it.");
            }
            
            return null;
        }

    };

    public TextureLoader getTextureLoader() {
        return textureLoader;
    }
}
