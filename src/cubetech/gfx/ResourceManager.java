package cubetech.gfx;

import cubetech.common.Common;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import cubetech.misc.ClassPath;
import java.util.Collection;
import java.util.Enumeration;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.HashMap;
import cubetech.misc.Ref;
import java.util.AbstractMap;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import cubetech.net.NetBuffer;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;
import org.lwjgl.BufferUtils;
import java.awt.image.ComponentColorModel;
import java.awt.image.ColorModel;
import java.awt.color.ColorSpace;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import javax.swing.ImageIcon;

import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @author mads
 */
public final class ResourceManager {
    HashMap<String, Resource> Ressources = new HashMap<String, Resource>();
    ArrayList<String> unloaded = new ArrayList<String>();
    private ColorModel glAlphaColorModel;
    private ColorModel glColorModel;
    int nUnloadedTextures = 0; // numbers of textures that needs to be loaded

    // Used for quering new opengl textures ids
    private IntBuffer textureIDBuffer = BufferUtils.createIntBuffer(1);
    private CubeTexture whiteTexture = null;

    public ResourceManager() {
        // Setup color models needed for loading textures
        glAlphaColorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                            new int[] {8,8,8,8},true,false,
                                            ComponentColorModel.TRANSLUCENT,DataBuffer.TYPE_BYTE);
        glColorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                            new int[] {8,8,8,0},false,false,
                                            ComponentColorModel.OPAQUE,DataBuffer.TYPE_BYTE);
        whiteTexture = LoadTexture("data/white.png");
    }

    public void SetWhiteTexture() {
        whiteTexture.Bind();
    }

    public CubeTexture getWhiteTexture() {
        return whiteTexture;
    }

    public void Cleanup() {

    }

    // TODO: Implement delay between updates?
    private static final int MAX_LOAD_PER_UPDATE = 1;
    public void Update() {
        if(nUnloadedTextures == 0)
            return; // nothing to load

        if(Ref.glRef == null || !Ref.glRef.isInitalized())
            return; // OpenGL not ready

        String[] completedLoading = new String[MAX_LOAD_PER_UPDATE];
        int nLoaded = 0;
        // continue until limit is hit or we run out of textures to load
        for(int i=0; i<unloaded.size() && nLoaded < MAX_LOAD_PER_UPDATE; i++) {
            String name = unloaded.get(i);
            CubeTexture tex = (CubeTexture)Ressources.get(name).Data;
            if(tex.loaded) {
                // Derp?
                Common.LogDebug("CubeTexture marked unloaded thinks he's loaded?");
            } else {
                try {
                    getTexture(name, GL_TEXTURE_2D, GL_RGBA, GL_LINEAR, GL_LINEAR, tex);
                } catch (IOException ex) {
                    Common.Log("Cannot load texture: File not found: " + name);
                }
            }
            completedLoading[nLoaded++] = name;
            tex.loaded = true;
        }

        // If anything was loaded, remove it from the "to-load" queue
        for (int i= 0; i < nLoaded; i++) {
            unloaded.remove(completedLoading[i]);
        }
    }


    public CubeTexture LoadTexture(String filename) {
        // Strip off path, so we can check if it is already loaded
        int idx = filename.lastIndexOf('.');
        if(idx <= 0) {
            System.err.println("LoadResource: Filename invalid, extension required (eg .png): " + filename);
            return null;
        }

        // Check if it is cached
        if(Ressources.containsKey(filename.toLowerCase())) {
            return (CubeTexture)Ressources.get(filename.toLowerCase()).Data;
        }

        // Try to load
        String name = filename.substring(idx); // get filetype
        if(name.equalsIgnoreCase(".png") || name.equalsIgnoreCase(".jpg")) { // Load png
            CubeTexture tex = null;
            if(Ref.glRef == null || !Ref.glRef.isInitalized()) {
                // OpenGL not ready, do defered loading
                // TODO: Check if file exists before doing this
//                System.out.println("Cannot load texture: OpenGL not initialized. Deferring");
                nUnloadedTextures++;
                tex = getUnloadedTexture(filename);
                unloaded.add(filename);
            } else { // else try regular loading
                try {
                    tex = getTexture(filename);
                    tex.loaded = true; // If this line runs, there was not exception -- so we should be in the clear
                } catch (IOException ex) { // file not found
                    Common.Log("Cannot load texture: File not found: " + filename);
                    // Fallback to blank texture -- will never load as there isn't a point
                    tex = getUnloadedTexture(filename);
                }
            }

            // Cache resource
            Resource res = new Resource();
            res.Data = tex;
            res.Type = Resource.ResourceType.TEXTURE;
            res.Name = filename;

            Ressources.put(filename.toLowerCase(), res);
            return tex;
        }

        Common.Log("ResMan.LoadTexture: I don't understand this format: " + name);
        return null;
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

    private BufferedImage loadImage(String path) throws IOException {
//        if(path.startsWith("cubetech"))

        URL url = getClassLoader().getResource((path.startsWith("cubetech")?"":"cubetech/")+path);

        Image img = null;
        if(url != null)
            img = new ImageIcon(url).getImage();
        else {
            try {
                img = new ImageIcon(path).getImage();

            } catch (Exception ex) {
                throw new IOException("Cannot find: " + path);
            }
        }

        // FIX: Don't load RGB as ARGB
        BufferedImage bufferedImage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics g = bufferedImage.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        
        return bufferedImage;
    }

    private int createTextureID() {
      glGenTextures(textureIDBuffer);
      return textureIDBuffer.get(0);
    }

    // Doesn't load the texture, but returns a cubetexture that
    // may be loaded later automagically (if the file exists)
    private CubeTexture getUnloadedTexture(String resourceName) {
        CubeTexture tex = new CubeTexture(GL_TEXTURE_2D, -1, resourceName);
        return tex;
    }

        /**
     * Load a texture
     *
     * @param resourceName The location of the resource to load
     * @return The loaded texture
     * @throws IOException Indicates a failure to access the resource
     */
    private CubeTexture getTexture(String resourceName) throws IOException {

        CubeTexture tex = getTexture(resourceName,
                         GL_TEXTURE_2D, // target
                         GL_RGBA,     // dst pixel format
                         GL_LINEAR, // min filter (unused)
                         GL_LINEAR, null);

        return tex;
    }

    /**
     * Load a texture into OpenGL from a image reference on
     * disk.
     *
     * @param resourceName The location of the resource to load
     * @param target The GL target to load the texture against
     * @param dstPixelFormat The pixel format of the screen
     * @param minFilter The minimising filter
     * @param magFilter The magnification filter
     * @return The loaded texture
     * @throws IOException Indicates a failure to access the resource
     */
    private CubeTexture getTexture(String resourceName,
                              int target,
                              int dstPixelFormat,
                              int minFilter,
                              int magFilter, CubeTexture texture) throws IOException {
        int srcPixelFormat;

        // create the texture ID for this texture
        int textureID = createTextureID();
        if(texture == null)
            texture = new CubeTexture(target,textureID,resourceName);
        else {
            // Set the id
            texture.name = resourceName;
            texture.SetID(textureID);
        }

        // bind this texture
        glBindTexture(target, textureID);

        BufferedImage bufferedImage = loadImage(resourceName);
        texture.Width = bufferedImage.getWidth();
        texture.Height = bufferedImage.getHeight();

//        System.out.println(resourceName + ": " + texture.Width + "x" + texture.Height);

        if (bufferedImage.getColorModel().hasAlpha()) {
            srcPixelFormat = GL_RGBA;
        } else {
            srcPixelFormat = GL_RGB;
        }

        // convert that image into a byte buffer of texture data
        ByteBuffer textureBuffer = convertImageData(bufferedImage,texture);

        if (target == GL_TEXTURE_2D) {
            glTexParameteri(target, GL_TEXTURE_MIN_FILTER, minFilter);
            glTexParameteri(target, GL_TEXTURE_MAG_FILTER, magFilter);
        }

        // produce a texture from the byte buffer
        glTexImage2D(target,
                      0,
                      dstPixelFormat,
                      get2Fold(bufferedImage.getWidth()),
                      get2Fold(bufferedImage.getHeight()),
                      0,
                      srcPixelFormat,
                      GL_UNSIGNED_BYTE,
                      textureBuffer );

        return texture;
    }

    /**
     * Get the closest greater power of 2 to the fold number
     *
     * @param fold The target number
     * @return The power of 2
     */
    public static int get2Fold(int fold) {
        int ret = 2;
        while (ret < fold) {
            ret *= 2;
        }
        return ret;
    }

    /**
     * Convert the buffered image to a texture
     *
     * @param bufferedImage The image to convert to a texture
     * @param texture The texture to store the data into
     * @return A buffer containing the data
     */
    private ByteBuffer convertImageData(BufferedImage bufferedImage,CubeTexture texture) {
        ByteBuffer imageBuffer;
        WritableRaster raster;
        BufferedImage texImage;

        int texWidth = 2;
        int texHeight = 2;

        // find the closest power of 2 for the width and height
        // of the produced texture
        while (texWidth < bufferedImage.getWidth()) {
            texWidth *= 2;
        }
        while (texHeight < bufferedImage.getHeight()) {
            texHeight *= 2;
        }

        texture.Height = texHeight;
        texture.Width = texWidth;

        // create a raster that can be used by OpenGL as a source
        // for a texture
        if (bufferedImage.getColorModel().hasAlpha()) {
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,texWidth,texHeight,4,null);
            texImage = new BufferedImage(glAlphaColorModel,raster,false,new Hashtable());
        } else {
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,texWidth,texHeight,3,null);
            texImage = new BufferedImage(glColorModel,raster,false,new Hashtable());
        }

        // copy the source image into the produced image
        Graphics2D g = (Graphics2D)texImage.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(new Color(0f,0f,0f,0f));
        g.fillRect(0,0,texWidth,texHeight);
        g.drawImage(bufferedImage,0,texHeight,texWidth,-texHeight,null);

        // build a byte buffer from the temporary image
        // that be used by OpenGL to produce a texture.
        byte[] data = ((DataBufferByte) texImage.getRaster().getDataBuffer()).getData();

        imageBuffer = ByteBuffer.allocateDirect(data.length);
        imageBuffer.order(ByteOrder.nativeOrder());
        imageBuffer.put(data, 0, data.length);
        imageBuffer.flip();

        return imageBuffer;
    }

    public static AbstractMap.SimpleEntry<ByteBuffer, Integer> OpenFileAsByteBuffer(String file, boolean direct) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(file);
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
        URL url = getClassLoader().getResource("cubetech/"+path);
        File file = null;
        InputStream stream = null;
        if(url == null) {
            url = getClassLoader().getResource(path);

            if(url == null) {
                // Try looking on the filesystem
                file = new File(path);
                if(file == null || !file.canRead()) return null;
                else {

                    return file;

                }
            }
        }
        return new File(url.getFile());
    }

    // Figures out what materials we have
    private static String[] matList = null;
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
                    for (int i= 0; i < strs.length; i++) {
                        data[i] = strs[i];
                    }
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

    // Tries to open a stream to the given path. Checks the JAR first, then the FS
    public static AbstractMap.SimpleEntry<NetBuffer, Integer> OpenFileAsNetBuffer(String path, boolean direct) throws IOException {
        // Look for the resource
        URL url = getClassLoader().getResource("cubetech/"+path);
        File file = null;
        if(url == null) {
            url = getClassLoader().getResource(path);
            if(url == null) {
                // Try looking on the filesystem
                file = new File(path);
                if(file == null || !file.canRead()) throw new FileNotFoundException("Cannot find file: " + path);
                else {

                    AbstractMap.SimpleEntry<ByteBuffer, Integer> result = OpenFileAsByteBuffer(file.getPath(), direct);
                    return new AbstractMap.SimpleEntry<NetBuffer, Integer>(NetBuffer.CreateCustom(result.getKey()), result.getValue());

                }
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

    
}
