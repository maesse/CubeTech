package cubetech.gfx;


import cubetech.common.Commands;
import cubetech.common.Common;
import cubetech.misc.Ref;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;



/**
 *
 * @author mads
 */
public class CubeMaterial {
    public enum Filtering {
        POINT,
        LINEAR
    }
    
    private CubeTexture texture = null;
    private String textureName = "None";
    private String name = "None"; // filename of this material
    private String fullname = "None";
    private Color color = new Color(255,255,255,255);
    private int translucent = 2; // 0=off, 1=multiply, 2=add
    private boolean ignorez = false;
    private Filtering filter = Filtering.LINEAR;
    private Vector2f textureOffset = new Vector2f(0, 0);
    private Vector2f textureSize = new Vector2f(1, 1);
    private int animFrames = 1;
    private int currFrame = 0;
    private int framedelay = 200; // in milliseconds
    private int lastFrameTime = 0;

    private boolean isLoaded = false; // true if cubetexture has been loaded

    public CubeMaterial() {
        textureName = "None";
    }

    public CubeMaterial(CubeTexture tex) {
        texture = tex;
        textureName = "Temp";
        isLoaded = true;
    }

    // Saves the material in a human readable format
    public void Save(String path) throws IOException {

        if(animFrames <= 0)
            animFrames = 1;

        // Assemble the material into a netbuffer
        StringBuilder str = new StringBuilder();
        str.append(String.format("texture \"%s\"\n", stripPath(textureName)));
        str.append(String.format("translucent \"%d\"\n", translucent));
        str.append(String.format("ignorez \"%d\"\n", ignorez?1:0));
        str.append(String.format("filter \"%d\"\n", filter==Filtering.POINT?0:1));
        str.append(String.format("color \"%d,%d,%d,%d\"\n", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()));
        str.append(String.format("texoffset \"%f:%f\"\n", textureOffset.x, textureOffset.y));
        str.append(String.format("texsize \"%f:%f\"\n", textureSize.x, textureSize.y));
        str.append(String.format("anim \"%d\"\n", animFrames));
        str.append(String.format("animdelay \"%d\"\n", framedelay));

        ResourceManager.SaveStringToFile(path, str.toString());
        String copySrc = textureName;
        String copyDest = getPath(path) + stripPath(textureName);
        // Bam, if madness
        if(!ResourceManager.FileExists(copyDest)) {
            if(ResourceManager.FileExists(copySrc)) {
                if(!copySrc.equals(copyDest)) {
                    Common.Log("Copying texture " + copySrc + " to " + copyDest);
                    BufferedInputStream bis = ResourceManager.OpenFileAsInputStream(copySrc);
                    ResourceManager.SaveInputStreamToFile(bis, copyDest);
                    bis.close();
                }
            } else {
                throw new IOException("Couldn't find the texture for copy. The material has been saved anyways.");
            }
        }

        fullname = path;
        name = CubeMaterial.stripPath(path);
        
    }

    public static String stripPath(String s) {
        s = s.replace('\\', '/');
        int i = s.lastIndexOf('/');
        String ext = null;
        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1);
        }

        if(ext == null || ext.isEmpty())
            return s;
        return ext;
    }

    public static String getPath(String s) {
        s = s.replace('\\', '/');
        int i = s.lastIndexOf('/');
        if(i == s.length()-1)
            return s; // ends with /, so assume directory
        
        String ext = null;
        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(0,i+1);
        }

        if(ext == null || ext.isEmpty())
            return "";
        return ext;
    }

    public static CubeMaterial Load(String path, boolean loadTexture) throws Exception {
        BufferedInputStream bis = ResourceManager.OpenFileAsInputStream(path);
        BufferedReader br = new BufferedReader(new InputStreamReader(bis));
        String line = null;

        CubeMaterial mat = new CubeMaterial();

        // Parse lines
        while((line = br.readLine()) != null) {
            String[] tokens = Commands.TokenizeString(line, false);
            if(tokens.length < 2) {
                Common.Log("CubeMaterial.Load(" + path + ") Ignored line: " + line);
                continue;
            }

            String cmd = tokens[0].toLowerCase();
            if(cmd.equals("texture")) {
                mat.textureName = tokens[1]; // path
            } else if(cmd.equals("translucent"))  { // 0 - 1
                mat.translucent = Integer.parseInt(tokens[1]);
            } else if(cmd.equals("ignorez")) { // 0 - 1
                mat.ignorez = Integer.parseInt(tokens[1]) == 1;
            } else if(cmd.equals("filter")) { // 0 - 2
                mat.filter = Integer.parseInt(tokens[1]) == 0 ? Filtering.POINT : Filtering.LINEAR;
            } else if(cmd.equals("color")) { // d,d,d where d >= 0 && d <= 255
                mat.color = parseColor(tokens[1]);
            } else if(cmd.equals("texoffset")) { // f:f
                mat.textureOffset = parseVector2f(tokens[1]);
            } else if(cmd.equals("texsize")) { // f:f
                mat.textureSize = parseVector2f(tokens[1]);
            } else if(cmd.equals("anim")) { // 1 - n
                mat.animFrames = Integer.parseInt(tokens[1]);
            } else if(cmd.equals("animdelay")) { // 1 - n
                mat.framedelay = Integer.parseInt(tokens[1]);

            } else
                Common.Log("CubeMaterial.Load(" + path + "): Unknown command " + line);
        }

        // validate
        if(mat.textureName.equals("None"))
            throw new Exception("Could not load material: " + path + "\nInvalid data.");
        if(mat.translucent < 0 || mat.translucent > 2) {
            Common.Log("CubeMaterial.Load(" + path + "): Invalid translucency " + mat.translucent);
            mat.translucent = 0;
        }
        if(mat.color.getAlpha() > 255 || mat.color.getGreen() > 255 || mat.color.getBlue() > 255 || mat.color.getRed() > 255) {
            Common.Log("CubeMaterial.Load(" + path + "): Invalid color: " + mat.color);
            mat.color = new Color(255, 255, 255, 255);
        }
        if(mat.textureSize.x == 0f && mat.textureSize.y == 0f) {
            Common.Log("CubeMaterial.Load(" + path + "): Warning: Texturesize is 0");
        }
        // grab path from material
        String p = CubeMaterial.getPath(path);
        String p2 = CubeMaterial.stripPath(mat.textureName);
        if(!ResourceManager.FileExists(mat.textureName) && !ResourceManager.FileExists(p + p2)) {
            Common.Log("CubeMaterial.Load(" + path + "): Warning: Cannot find texture: " + mat.textureName);
            Common.Log("(debug info: p=" + p + ", p2=" + p2 +")");
        }
        if(mat.animFrames <= 0) {
            Common.Log("CubeMaterial.Load(" + path + "): Invalid animation frame count: " + mat.animFrames);
            mat.animFrames = 1;
        }

        mat.fullname = path;
        mat.name = CubeMaterial.stripPath(path);

        // loadTexture true means that this material is loaded for the game
        if(loadTexture) {
            if(ResourceManager.FileExists(p+mat.textureName))
                mat.texture = Ref.ResMan.LoadTexture(p+mat.textureName);
            else
                mat.texture = Ref.ResMan.LoadTexture(mat.textureName);

//            mat.texture = Ref.ResMan.LoadTexture("data/buttons.png");
            
            // init animation texoffset cache array
            mat.animOffsetCache = new Vector2f[mat.animFrames];
            mat.animOffsetCache[0] = mat.textureOffset;
        }
        mat.isLoaded = true;
        return mat;
    }

    public static Vector2f parseVector2f(String token) {
        Vector2f vec = new Vector2f();
        token = token.replace("\"", " ").trim();
        String[] tokens = token.split(":");
        if(tokens.length < 2) {
            Common.Log("CubeMaterial.parseVector2f(): Failed on " + token);
            return vec;
        }

        if(!tokens[0].isEmpty())
            vec.x = Float.parseFloat(tokens[0].replace(',', '.'));
        if(!tokens[1].isEmpty())
            vec.y = Float.parseFloat(tokens[1].replace(',', '.'));
        return vec;
    }

    private static Color parseColor(String token) {
        int r,g,b,a;
        String[] tokens = token.split(",");
        if(tokens.length < 4) {
            Common.Log("CubeMaterial.parseColor(): failed on " + token);
            return new Color(255,255,255,255);
        }
        if(tokens[0].isEmpty())
            r = 0;
        else
            r = Integer.parseInt(tokens[0]);
        if(tokens[1].isEmpty())
            g = 0;
        else
            g = Integer.parseInt(tokens[1]);
        if(tokens[2].isEmpty())
            b = 0;
        else
            b = Integer.parseInt(tokens[2]);
        if(tokens[3].isEmpty())
            a = 0;
        else
            a = Integer.parseInt(tokens[3]);
        return new Color(r,g,b,a);
    }

    // Getter and Setter madness
    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Filtering getFilter() {
        return filter;
    }

    public void setFilter(Filtering filter) {
        this.filter = filter;
    }

    public boolean isIgnorez() {
        return ignorez;
    }

    public void setIgnorez(boolean ignorez) {
        this.ignorez = ignorez;
    }

    public String getTextureName() {
        return textureName;
    }

    public void setTextureName(String textureName) {
        this.textureName = textureName;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return fullname;
    }

    public void setPath(String Name) {
        fullname = Name;
        name = CubeMaterial.stripPath(Name);
    }

    public Vector2f getTextureOffset() {
        if(animFrames <= 1)
            return textureOffset;
        
        handleFrameTime();
        return getTextureOffset(currFrame);
        //return textureOffset;
    }

    private void handleFrameTime() {
        if(lastFrameTime == 0) {
            lastFrameTime = Ref.client.realtime;
            return;
        }
        if(lastFrameTime + framedelay < Ref.client.realtime) {
            lastFrameTime = Ref.client.realtime;
            currFrame++;
        }
    }

    Vector2f[] animOffsetCache = null;
    public Vector2f getTextureOffset(int frame) {
        frame = frame % animFrames; // Wrap at framecount

        // Init cache array
        if(animOffsetCache == null || animOffsetCache.length < animFrames) {
            animOffsetCache = new Vector2f[animFrames];
            animOffsetCache[0] = textureOffset;
        }

        // Build the offset
        if(animOffsetCache[frame] == null)
        {
            Vector2f offset = new Vector2f(textureOffset);
            int w = (int) ((1f - textureOffset.x) * texture.Width);
            int fw = (int) (textureSize.x * texture.Width);
            int framesPrRow = w / fw;
            // Add x offset
            offset.x += textureSize.x * (frame % framesPrRow);
            // divide new x by width to get number of rows
            offset.y -= textureSize.y * (int)(frame / framesPrRow);
            // take modulos of offset vs size, and add base x offset
//            offset.x = (float) (textureOffset.x + (offset.x ) % (1f - textureOffset.x));
//            System.out.println("frame: " + frame + ", " + offset);
            //offset.x = textureOffset.x + offset.x % (1f - textureOffset.x);

            animOffsetCache[frame] = offset;
        }

        return animOffsetCache[frame];
    }

    public void setTextureOffset(Vector2f textureOffset) {
        this.textureOffset = textureOffset;
    }

    public Vector2f getTextureSize() {
        return textureSize;
    }

    public void setTextureSize(Vector2f textureSize) {
        this.textureSize = textureSize;
    }

    public int getTranslucent() {
        return translucent;
    }

    public void setTranslucent(int translucent) {
        this.translucent = translucent;
    }

    public int getAnimCount() {
        return animFrames;
    }

    public void setAnimCount(int value) {
        if(value <= 0)
            value = 1;
        this.animFrames = value;
    }

    public boolean isIsLoaded() {
        return isLoaded;
    }

    public CubeTexture getTexture() {
        return texture;
    }

    public void setTexture(CubeTexture tex) {
        texture = tex;
    }

    public int getFrameCount() {
        return animFrames;
    }

    public int getFramedelay() {
        return framedelay;
    }

    public void setFramedelay(int framedelay) {
        this.framedelay = framedelay;
    }

    
}
