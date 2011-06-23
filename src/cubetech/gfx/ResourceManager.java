package cubetech.gfx;

import java.util.Hashtable;
import cubetech.Helper;
import java.util.Iterator;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.EXTTextureSRGB;
import org.lwjgl.opengl.GL14;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import cubetech.misc.ClassPath;
import java.io.InputStream;
import java.util.HashMap;
import cubetech.misc.Ref;
import java.util.AbstractMap;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
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
import org.lwjgl.opengl.EXTTextureArray;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;

/**
 *
 * @author mads
 */
public final class ResourceManager {

    
    HashMap<String, Resource> Ressources = new HashMap<String, Resource>();
    ArrayList<Resource> unloadedRessources = new ArrayList<Resource>();
//    ArrayList<String> unloaded = new ArrayList<String>();
    private ColorModel glAlphaColorModel;
    private ColorModel glColorModel;
    int nUnloadedTextures = 0; // numbers of textures that needs to be loaded

    public boolean autoSrgb = true;

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

    // TODO: Implement delay between updates?
    private static final int MAX_LOAD_PER_UPDATE = 1;
    public void Update() {
        if(unloadedRessources.isEmpty()) return; // nothing to load

        int nLoaded = 0;
        Iterator<Resource> it = unloadedRessources.iterator();
        // continue until limit is hit or we run out of textures to load
        while(it.hasNext() && nLoaded < MAX_LOAD_PER_UPDATE) {
            Resource res = it.next();
            if(res.loaded) { // has been loaded
                it.remove();
                continue;
            }
            
            try {
                getTexture(res);
                res.loaded = true;
            } catch (IOException ex) {
                System.out.println("Cannot load texture: File not found: " + res.Name);
            }

            it.remove();
            nLoaded++;
        }
    }


    public CubeTexture LoadTexture(String filename) {
        return LoadTexture(filename, false);
    }

    public CubeTexture LoadTexture(String filename, boolean cubemap)  {
        // Check if it is cached
        if(Ressources.containsKey(filename.toLowerCase())) {
            return (CubeTexture)Ressources.get(filename.toLowerCase()).Data;
        }
        
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
                System.out.println("ResMan.LoadTexture: I don't understand this format: " + name);
                return null;
            }
        }
        
        // Going to load it..
        CubeTexture tex = null;
        // Create unloaded resource
        Resource res = new Resource(filename, Resource.ResourceType.TEXTURE, cubemap?GL_TEXTURE_CUBE_MAP:GL_TEXTURE_2D);
        if(!Graphics.isInitialized()) {
            // OpenGL not ready, do defered loading
            tex = getUnloadedTexture(res);
            unloadedRessources.add(res);
        } else { // else try regular loading
            try {
                tex = getTexture(res);
            } catch (IOException ex) { // it's a no go
                System.out.println("Cannot load texture: File not found: " + filename + "(" + ex.getLocalizedMessage() + ")");
                tex = getUnloadedTexture(res);// Fallback to blank texture -- will never load as there isn't a point
            }
            res.loaded = true;
        }

        // Cache resource
        Ressources.put(filename.toLowerCase(), res);
        return tex;
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
        if(!FileExists(path)) throw new IOException("Cannot find: " + path);
        
        URL url = getClassLoader().getResource((path.startsWith("cubetech")?"":"cubetech/")+path);

        Image img = null;
        if(url != null)
            img = new ImageIcon(url).getImage();
        if(img == null) {
            try {
                img = new ImageIcon(path).getImage();

            } catch (Exception ex) {
                throw new IOException("Cannot find: " + path);
            }
        }
        

        // FIX: Don't load RGB as ARGB
        try {
            BufferedImage bufferedImage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics g = bufferedImage.getGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();

            return bufferedImage;
        } catch(IllegalArgumentException ex) {
            throw new IOException("Failed to load texture.", ex);
        }
    }

    private int createTextureID() {
      glGenTextures(textureIDBuffer);
      return textureIDBuffer.get(0);
    }

    // Doesn't load the texture, but returns a cubetexture that
    // may be loaded later automagically (if the file exists)
    private CubeTexture getUnloadedTexture(Resource res) {
        CubeTexture tex = new CubeTexture(res.target, -1, res.Name);
        res.Data = tex;
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
//                         GL_RGBA,     // dst pixel format
                         autoSrgb?EXTTextureSRGB.GL_SRGB_ALPHA_EXT:GL_RGBA,
                         GL_LINEAR, // min filter (unused)
                         GL_LINEAR, null);

        return tex;
    }

    private CubeTexture getTexture(Resource res) throws IOException
    {
        int dstFormat = autoSrgb?EXTTextureSRGB.GL_SRGB_ALPHA_EXT:GL_RGBA;
        if(res.target == GL_TEXTURE_CUBE_MAP) dstFormat = GL_RGB; // don't need alpha for cubemaps
        int minFilter = res.target == GL_TEXTURE_CUBE_MAP?GL_LINEAR:GL_LINEAR_MIPMAP_LINEAR;
        CubeTexture tex = getTexture(res.Name, res.target, dstFormat, minFilter, GL_LINEAR, (CubeTexture)res.Data);
        res.Data = tex;
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
    private CubeTexture getTexture(String resourceName, int target,
                              int dstPixelFormat,
                              int minFilter, int magFilter,
                              CubeTexture texture) throws IOException {
        // create the texture ID for this texture
        int textureID = createTextureID();

        // Create the CubeTexture
        if(texture == null)
            texture = new CubeTexture(target,textureID,resourceName);
        else {
            // Set the id
            texture.name = resourceName;
            texture.SetID(textureID);
        }

        // bind this texture
        glBindTexture(target, textureID);
        // Set filter
        if (target == GL_TEXTURE_2D || target == GL_TEXTURE_CUBE_MAP) {
            glTexParameteri(target, GL_TEXTURE_MIN_FILTER, minFilter);
            glTexParameteri(target, GL_TEXTURE_MAG_FILTER, magFilter);
            texture.minfilter = minFilter;
            texture.magfilter = magFilter;
            if(minFilter == GL_LINEAR_MIPMAP_LINEAR || minFilter == GL_LINEAR_MIPMAP_NEAREST) {
                glTexParameteri(target, GL_GENERATE_MIPMAP, GL_TRUE);
            }
        }

        

        if(target == GL_TEXTURE_CUBE_MAP) {
            // convert that image into a byte buffer of texture data
            ByteBuffer buf = convertImageData(loadImage(resourceName+"_ft.png"),texture, false);
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X,0,dstPixelFormat,
                          texture.Width,
                          texture.Height,
                          0,GL_RGBA,GL_UNSIGNED_BYTE,
                           buf );
            glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X,0,dstPixelFormat,
                          texture.Width,
                          texture.Height,
                          0,GL_RGBA,GL_UNSIGNED_BYTE,
                           convertImageData(loadImage(resourceName+"_bk.png"),texture, false) );
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y,0,dstPixelFormat,
                          texture.Width,
                          texture.Height,
                          0,GL_RGBA,GL_UNSIGNED_BYTE,
                           convertImageData(loadImage(resourceName+"_rt.png"),texture, false) );
            glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y,0,dstPixelFormat,
                          texture.Width,
                          texture.Height,
                          0,GL_RGBA,GL_UNSIGNED_BYTE,
                           convertImageData(loadImage(resourceName+"_lf.png"),texture, false) );
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z,0,dstPixelFormat,
                          texture.Width,
                          texture.Height,
                          0,GL_RGBA,GL_UNSIGNED_BYTE,
                           convertImageData(loadImage(resourceName+"_up.png"),texture, false) );
            glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z,0,dstPixelFormat,
                          texture.Width,
                          texture.Height,
                          0,GL_RGBA,GL_UNSIGNED_BYTE,
                           convertImageData(loadImage(resourceName+"_dn.png"),texture, false) );
        } else {
            // load image data
            BufferedImage bufferedImage = null;
            boolean flipY = true;
            {
                bufferedImage = loadImage(resourceName);
            }
            texture.Width = bufferedImage.getWidth();
            texture.Height = bufferedImage.getHeight();

            // convert that image into a byte buffer of texture data
            ByteBuffer textureBuffer = convertImageData(bufferedImage,texture, flipY);
            int srcPixelFormat = bufferedImage.getColorModel().hasAlpha() ? GL_RGBA : GL_RGB;
            // produce a texture from the byte buffer
            glTexImage2D(target,0,dstPixelFormat,
                          Helper.get2Fold(bufferedImage.getWidth()),
                          Helper.get2Fold(bufferedImage.getHeight()),
                          0,srcPixelFormat,GL_UNSIGNED_BYTE,
                          textureBuffer );
        }

        texture.loaded = true;

        return texture;
    }

    

    /**
     * Convert the buffered image to a texture
     *
     * @param bufferedImage The image to convert to a texture
     * @param texture The texture to store the data into
     * @return A buffer containing the data
     */
    private ByteBuffer convertImageData(BufferedImage bufferedImage,CubeTexture texture, boolean flipY) {
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
        int x = 0, y = 0;
        int w = texHeight, h = texHeight;
        if(flipY) {
            y = texHeight;
            h = -texHeight;
        }
        g.drawImage(bufferedImage,x,y,w,h,null);

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

            //if(!Ref.glRef.isApplet()) { FIX
            if(false) {
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
//
//    // Tries to open a stream to the given path. Checks the JAR first, then the FS
//    public static AbstractMap.SimpleEntry<NetBuffer, Integer> OpenFileAsNetBuffer(String path, boolean direct) throws IOException {
//        // Look for the resource
//        URL url = getClassLoader().getResource("cubetech/"+path);
//        File file = null;
//        if(url == null) {
//            url = getClassLoader().getResource(path);
//            if(url == null) {
//                // Try looking on the filesystem
//                file = new File(path);
//                if(file == null || !file.canRead()) throw new FileNotFoundException("Cannot find file: " + path);
//                else {
//
//                    AbstractMap.SimpleEntry<ByteBuffer, Integer> result = OpenFileAsByteBuffer(file.getPath(), direct);
//                    return new AbstractMap.SimpleEntry<NetBuffer, Integer>(NetBuffer.CreateCustom(result.getKey()), result.getValue());
//
//                }
//            }
//        }
//
//        // Open as a stream. Won't know the final filesize before it is all read,
//        // so we'll have to do some extra copying
//        BufferedInputStream bis = new BufferedInputStream(url.openStream());
//
//        int loadChunkSize = 1024*256;
//        byte[] data = new byte[loadChunkSize]; // Load in mb chunks
//        ArrayList<byte[]> extraData = new ArrayList<byte[]>();
//        int nRead, nTotal = 0;
//
//        // Read untill we hit EOF
//        int checksum = 0;
//        while((nRead = bis.read(data)) != -1) {
//            if(nRead == 0)
//                continue;
//            nTotal += nRead;
//            for (int i= 0; i < nRead; i++) {
//                checksum += data[i]; // simple checksum
//            }
//            extraData.add(data);
//            data = new byte[loadChunkSize];
//        }
//
//        // Create the final buffer now that we know the size
//        ByteBuffer buf = null;
//        if(direct)
//            buf = ByteBuffer.allocateDirect(nTotal);
//        else
//            buf = ByteBuffer.allocate(nTotal);
//
//        // Concat the buffers
//        for (int i= 0; i < extraData.size(); i++) {
//            buf.put(extraData.get(i), 0, Math.min(loadChunkSize, nTotal));
//            nTotal -= loadChunkSize;
//        }
//        buf.position(0);
//        return new AbstractMap.SimpleEntry<NetBuffer, Integer>(NetBuffer.CreateCustom(buf), checksum);
//    }

    // ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB

    public int CreateEmptyDepthTexture(int width, int height, int bitdepth, int target) {
        return CreateEmptyDepthTexture(width, height, bitdepth, target, 0);
    }
    public int CreateEmptyDepthTexture(int width, int height, int bitdepth, int target, int nLevels) {
        int textureId = createTextureID();
        glBindTexture(target, textureId);
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(target, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glTexParameteri(target, GL_DEPTH_TEXTURE_MODE, GL_INTENSITY);
        glTexParameteri(target, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_R_TO_TEXTURE);
        glTexParameteri(target, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
        ByteBuffer nullBuffer = null;
        int component = GL_DEPTH_COMPONENT16;
        if(bitdepth == 24) component = GL_DEPTH_COMPONENT24;
        if(bitdepth == 32) component = GL_DEPTH_COMPONENT32;
        if(target == EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT) {
            glTexImage3D(target, 0, component, width, height,nLevels, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, nullBuffer);
        } else {
            glTexImage2D(target, 0, component, width, height, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, nullBuffer);
        }
        
        return textureId;
    }

    public int CreateEmptyTexture(int width, int height, int target, boolean fill) {
        int textureId = createTextureID();
        glBindTexture(target, textureId);
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        if(fill) {
            ByteBuffer buf = ByteBuffer.allocateDirect(width*height*4);
            buf.order(ByteOrder.nativeOrder());
            for (int i= 0; i < width*height; i++) {
                buf.put((byte)125);
                buf.put((byte)0);
                buf.put((byte)255);
                buf.put((byte)255);
            }
            buf.flip();
            glTexImage2D(target, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        } else {
            ByteBuffer nullBuffer = null;
            glTexImage2D(target, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullBuffer);
        }
        
        return textureId;
    }

    
}
