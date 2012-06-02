package cubetech.gfx;

import cubetech.gfx.PNGReader.PNGFile;
import java.awt.Color;
import cubetech.common.Common;
import cubetech.gfx.TargaReader.TargaFile;
import cubetech.misc.Ref;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;
import javax.swing.ImageIcon;
import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.opengl.EXTTextureArray;
import org.lwjgl.opengl.EXTTextureSRGB;

import org.lwjgl.util.ReadableColor;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;

/**
 *
 * @author Mads
 */
public class TextureLoader {
    // PNG loading
    private ColorModel glAlphaColorModel;
    private ColorModel glColorModel;
    
    // Used for quering new opengl textures ids
    protected CubeTexture whiteTexture = null;
    protected CubeTexture noSpecularTexture = null;
    protected CubeTexture noNormalTexture = null; // 1 pixel (0,0,1)
    
    public TextureLoader() {
        // Setup color models needed for loading textures
        glAlphaColorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                            new int[] {8,8,8,8},true,false,
                                            ComponentColorModel.TRANSLUCENT,DataBuffer.TYPE_BYTE);
        glColorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                            new int[] {8,8,8,0},false,false,
                                            ComponentColorModel.OPAQUE,DataBuffer.TYPE_BYTE);
        
        // fix
        whiteTexture = new CubeTexture(GL_TEXTURE_2D, -1,"data/textures/white.png");
        whiteTexture.loaded = true;
    }
    
    public void generateTextures() {
        if(whiteTexture.GetID() == -1) {
            int whiteTextureId = CreateEmptyTexture(1, 1, GL_TEXTURE_2D, true, (org.lwjgl.util.Color) ReadableColor.WHITE);
            whiteTexture.SetID(whiteTextureId);
        }
        byte[] data = new byte[] {(byte)127, (byte)127, (byte)255, (byte)255};
        noNormalTexture = CreateTextureFromData(1, 1, GL_TEXTURE_2D, data);
        noNormalTexture.textureSlot = 3;
        noNormalTexture.Bind();
        data = new byte[] {(byte)0, (byte)0, (byte)0, (byte)255};
        noSpecularTexture = CreateTextureFromData(1, 1, GL_TEXTURE_2D, data);
        noSpecularTexture.textureSlot = 4;
        noSpecularTexture.Bind();
    }
    
    private BufferedImage loadImage(String path, float rotationAngle) throws IOException {
//        if(!FileExists(path)) throw new IOException("Cannot find: " + path);
        String filepath = path.replace("/", "\\");
        filepath = filepath.replace("cubetech\\", "");
        
        Image img = null;
        boolean asFile = true;
        if(Ref.common.isDeveloper() && ResourceManager.FileExists(ResourceManager.devpath.sValue + filepath)) {
            try {
                ByteBuffer buffer = ResourceManager.OpenFileAsByteBuffer(ResourceManager.devpath.sValue + filepath, false).getKey();
                byte[] data = buffer.array();
                img = new ImageIcon(data).getImage();
                if(img != null) path = ResourceManager.devpath.sValue + filepath;
            } catch (Exception ex) {}
        }
        
        if(img == null)
        {
            asFile = false;
            URL url = ResourceManager.getClassLoader().getResource((path.startsWith("cubetech")?"":"cubetech/")+path);
            if(url != null) img = new ImageIcon(url).getImage();
        }

        if(img == null) {
            asFile = false;
            try {
                img = new ImageIcon(path).getImage();
            } catch (Exception ex) {
                throw new IOException("Cannot find: " + path);
            }
        }
        

        // FIX: Don't load RGB as ARGB
        try {
            int w = img.getWidth(null), h = img.getHeight(null);
            BufferedImage bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D)bufferedImage.getGraphics();
            if(rotationAngle != 0) {
                AffineTransform transform  = new AffineTransform();
                //transform.setToTranslation(w, 0);
                transform.rotate(Math.toRadians(rotationAngle), w/2f, h/2f);
                g.drawImage(img, transform, null);
            } else {
                g.drawImage(img, 0, 0, null);
            }
            
            
            g.dispose();
            if(ResourceManager.tex_verbose.iValue > 1) Common.LogDebug("Loaded texture " + path + (asFile?" (file)":" (jar)"));
            return bufferedImage;
        } catch(IllegalArgumentException ex) {
            throw new IOException("Failed to load texture.", ex);
        }
    }

    private int createTextureID() {
        return glGenTextures();
    }

    // Doesn't load the texture, but returns a cubetexture that
    // may be loaded later automagically (if the file exists)
    protected CubeTexture getUnloadedTexture(Resource res) {
        CubeTexture tex = new CubeTexture(res.target, -1, res.Name);
        res.Data = tex;
        return tex;
    }

    public boolean isSRGB() {
        if(Ref.glRef == null) return false;
        return Ref.glRef.caps.GL_EXT_texture_sRGB && ResourceManager.tex_srgb.isTrue();
    }

    protected CubeTexture getTexture(Resource res) throws Exception
    {
        boolean useSRGB = isSRGB() && !res.Name.contains("normal"); // normals are linear
        int dstFormat = useSRGB?EXTTextureSRGB.GL_SRGB_ALPHA_EXT:GL_RGBA;
        if(res.target == GL_TEXTURE_CUBE_MAP)  {
            dstFormat = useSRGB?EXTTextureSRGB.GL_SRGB_EXT:GL_RGB;
        } // don't need alpha for cubemaps
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
    protected CubeTexture getTexture(String resourceName, int target,
                              int dstPixelFormat,
                              int minFilter, int magFilter,
                              CubeTexture texture) throws Exception {
        int msec = Ref.common.Milliseconds();
        int textureID = -1;
        
        try {
            if(target == GL_TEXTURE_CUBE_MAP && texture != null && texture.loaded) {
                texture.loaded = false;
                glDeleteTextures(texture.GetID());
                if(ResourceManager.tex_verbose.iValue > 3) Common.LogDebug("Deleted texture (id: %d)", texture.GetID());
            }
            if(texture == null || !texture.loaded) {
                // create the texture ID for this texture
                textureID = createTextureID();
                if(ResourceManager.tex_verbose.iValue > 3) Common.LogDebug("Created texture (id: %d)", textureID);

                // Create the CubeTexture
                if(texture == null)
                    texture = new CubeTexture(target,textureID,resourceName);
                else {
                    // Set the id
                    texture.name = resourceName;
                    texture.SetID(textureID);
                }
            }

            // bind this texture
            glBindTexture(target, texture.GetID());
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
                texture.setWrap(GL_CLAMP_TO_EDGE);
                glTexParameteri(target, GL_TEXTURE_MIN_FILTER, minFilter);
                // convert that image into a byte buffer of texture data
                ByteBuffer buf = convertImageData(loadImage(resourceName+"_ft.png", -90f),texture, false);
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X,0,dstPixelFormat,
                              texture.Width,
                              texture.Height,
                              0,GL_RGBA,GL_UNSIGNED_BYTE,
                               buf );
                glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X,0,dstPixelFormat,
                              texture.Width,
                              texture.Height,
                              0,GL_RGBA,GL_UNSIGNED_BYTE,
                               convertImageData(loadImage(resourceName+"_bk.png", 90f),texture, false) );
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y,0,dstPixelFormat,
                              texture.Width,
                              texture.Height,
                              0,GL_RGBA,GL_UNSIGNED_BYTE,
                               convertImageData(loadImage(resourceName+"_lf.png", 180f),texture, false) );
                glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y,0,dstPixelFormat,
                              texture.Width,
                              texture.Height,
                              0,GL_RGBA,GL_UNSIGNED_BYTE,
                               convertImageData(loadImage(resourceName+"_rt.png", 0f),texture, false) );
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z,0,dstPixelFormat,
                              texture.Width,
                              texture.Height,
                              0,GL_RGBA,GL_UNSIGNED_BYTE,
                               convertImageData(loadImage(resourceName+"_up.png", -90f),texture, false) );
                glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z,0,dstPixelFormat,
                              texture.Width,
                              texture.Height,
                              0,GL_RGBA,GL_UNSIGNED_BYTE,
                               convertImageData(loadImage(resourceName+"_dn.png", -90f),texture, false) );
            } else {
                
                // load image data
                int srcPixelFormat = GL_RGBA;
                ByteBuffer textureBuffer = null;
                if(resourceName.toLowerCase().endsWith(".tga")) {
                    TargaFile tgaFile = TargaReader.getImage(resourceName, true);
                    texture.Width = tgaFile.width;
                    texture.Height = tgaFile.height;
                    textureBuffer = tgaFile.data;
                } else if(resourceName.toLowerCase().endsWith(".png")) {
                    ByteBuffer fileinput = ResourceManager.OpenFileAsByteBuffer(resourceName, true).getKey();
                    PNGFile pngFile = PNGReader.readFile(fileinput);
                    texture.Width = pngFile.width;
                    texture.Height = pngFile.height;
                    textureBuffer = pngFile.data;
                    srcPixelFormat = pngFile.hasAlpha ? GL_RGBA : GL_RGB;
                    //pngFile = null;
                } else {
                    BufferedImage bufferedImage = loadImage(resourceName, 0f);
                    texture.Width = bufferedImage.getWidth();
                    texture.Height = bufferedImage.getHeight();
                    // convert that image into a byte buffer of texture data
                    textureBuffer = convertImageData(bufferedImage,texture, true);
                    srcPixelFormat = bufferedImage.getColorModel().hasAlpha() ? GL_RGBA : GL_RGB;
                }
                
                // produce a texture from the byte buffer
                glTexImage2D(target,0,dstPixelFormat,
                              texture.Width,
                              texture.Height,
                              0,srcPixelFormat,GL_UNSIGNED_BYTE,
                              textureBuffer);
                //textureBuffer = null;
            }
        } catch(IOException ex) {
            if(textureID > 0) {
                glDeleteTextures(textureID);
            }
            throw ex;
        }

        texture.loaded = true;
        
        int msec2 = Ref.common.Milliseconds();
        float totalSec = (msec2 - msec) / 1000f;
        Common.LogDebug("Loaded texture '%s' in %.2f seconds", texture.name, totalSec);


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
        int w = texWidth, h = texHeight;
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
        } else if(target == GL_TEXTURE_CUBE_MAP) {
            for (int i = 0; i < 6; i++) {
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X+i, 0, component, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, nullBuffer);
            }
        } else {
            glTexImage2D(target, 0, component, width, height, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, nullBuffer);
        }
        GLRef.checkError();
        return textureId;
    }
    
    public int CreateEmptyTexture(int width, int height, int target, boolean fill, org.lwjgl.util.Color color) {
        return CreateEmptyTexture(width, height, target, GL_RGBA, fill, color);
    }
    public int CreateEmptyTexture(int width, int height, int target, int dstFormat, boolean fill, org.lwjgl.util.Color color) {
        int textureId = createTextureID();
        glBindTexture(target, textureId);
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        if(fill) {
            if(color == null) {
                color = (org.lwjgl.util.Color) org.lwjgl.util.Color.WHITE;
            }
            ByteBuffer buf = ByteBuffer.allocateDirect(width*height*4);
            buf.order(ByteOrder.nativeOrder());
            for (int i= 0; i < width*height; i++) {
                color.writeRGBA(buf);
            }
            buf.flip();
            glTexImage2D(target, 0, dstFormat, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        } else {
            ByteBuffer nullBuffer = null;
            if(target == GL_TEXTURE_CUBE_MAP) {
                for (int i = 0; i < 6; i++) {
                    glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X+i, 0, ARBTextureFloat.GL_ALPHA32F_ARB, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullBuffer);
                }
            } else {
                glTexImage2D(target, 0, dstFormat, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullBuffer);
            }
        }
        
        return textureId;
    }

    public CubeTexture CreateTextureFromData(int width, int height, int target, byte[] data) {
        int textureId = createTextureID();
        glBindTexture(target, textureId);
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        ByteBuffer buf = ByteBuffer.allocateDirect(width*height*4);
        buf.order(ByteOrder.nativeOrder());
        for (int i= 0; i < width*height; i++) {
            buf.put(data[(i*4+0) % data.length]);
            buf.put(data[(i*4+1) % data.length]);
            buf.put(data[(i*4+2) % data.length]);
            buf.put(data[(i*4+3) % data.length]);
        }
        buf.flip();
        glTexImage2D(target, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        CubeTexture tex = new CubeTexture(target, textureId, "GeneratedTexture");
        tex.loaded = true;
        return tex;
    }
}
