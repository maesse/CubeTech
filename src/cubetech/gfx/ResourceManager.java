/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

import cubetech.gfx.CubeTexture;
import java.nio.IntBuffer;
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
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.swing.ImageIcon;

import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @author mads
 */
public class ResourceManager {
    ArrayList<Resource> Ressources = new ArrayList<Resource>();

    private ColorModel glAlphaColorModel;
    private ColorModel glColorModel;

    private IntBuffer textureIDBuffer = BufferUtils.createIntBuffer(1);

    public ResourceManager() {
        glAlphaColorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                            new int[] {8,8,8,8},
                                           true,
                                            false,
                                            ComponentColorModel.TRANSLUCENT,
                                            DataBuffer.TYPE_BYTE);

        glColorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                            new int[] {8,8,8,0},
                                           false,
                                          false,
                                            ComponentColorModel.OPAQUE,
                                           DataBuffer.TYPE_BYTE);
    }
   

    public Resource LoadResource(String filename) {
        // Strip off path, so we can check if it is already loaded
        int idx = filename.lastIndexOf('.');
        if(idx <= 0) {
            System.err.println("LoadResource: Filename is invalid: " + filename);
            return null;
        }
        String name = filename.substring(idx);

        // Check if it is cached
        for(int i=0; i<Ressources.size(); i++) {
            if(Ressources.get(i).Name.equals(filename))
                return Ressources.get(i);
        }

        // Alse try to load
        if(name.equalsIgnoreCase(".png")) {
            // Image
            try {
                CubeTexture tex = getTexture(filename);
                Resource res = new Resource();
                res.Data = tex;
                res.Type = Resource.ResourceType.TEXTURE;
                res.Name = filename;

                Ressources.add(res);
                return res;
            } catch(IOException e) {

            }
        }

        // Didn't find anything
        System.err.println("Couldn't find resource: " + filename);
        return null;
    }

    private BufferedImage loadImage(String path) throws IOException {
        URL url = ResourceManager.class.getClassLoader().getResource("cubetech/"+path);
        if(url == null) {
            throw new IOException("Cannot find: " + path);
        }
        Image img = new ImageIcon(url).getImage();
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

        /**
     * Load a texture
     *
     * @param resourceName The location of the resource to load
     * @return The loaded texture
     * @throws IOException Indicates a failure to access the resource
     */
    private CubeTexture getTexture(String resourceName) throws IOException {
        //CubeTexture tex = table.get(resourceName);

        //if (tex != null) {
        //    return tex;
        //}

        CubeTexture tex = getTexture(resourceName,
                         GL_TEXTURE_2D, // target
                         GL_RGBA,     // dst pixel format
                         GL_LINEAR, // min filter (unused)
                         GL_LINEAR);

        //table.put(resourceName,tex);

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
    public CubeTexture getTexture(String resourceName,
                              int target,
                              int dstPixelFormat,
                              int minFilter,
                              int magFilter) throws IOException {
        int srcPixelFormat;

        // create the texture ID for this texture
        int textureID = createTextureID();
        CubeTexture texture = new CubeTexture(target,textureID);

        // bind this texture
        glBindTexture(target, textureID);

        BufferedImage bufferedImage = loadImage(resourceName);
        texture.Width = bufferedImage.getWidth();
        texture.Height = bufferedImage.getHeight();
        

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
    private static int get2Fold(int fold) {
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
        Graphics g = texImage.getGraphics();
        g.setColor(new Color(0f,0f,0f,0f));
        g.fillRect(0,0,texWidth,texHeight);
        g.drawImage(bufferedImage,0,0,null);

        // build a byte buffer from the temporary image
        // that be used by OpenGL to produce a texture.
        byte[] data = ((DataBufferByte) texImage.getRaster().getDataBuffer()).getData();

        imageBuffer = ByteBuffer.allocateDirect(data.length);
        imageBuffer.order(ByteOrder.nativeOrder());
        imageBuffer.put(data, 0, data.length);
        imageBuffer.flip();

        return imageBuffer;
    }

    public void Cleanup() {
        
    }
}
