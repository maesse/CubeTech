package cubetech.gfx;

import cubetech.misc.Ref;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import cubetech.gfx.SpriteManager.Type;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.AbstractMap.SimpleEntry;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class TextManager {
    // Colors    
    public static final int COLOR_WHITE = 0;
    public static final int COLOR_BLACK = 1;
    public static final int COLOR_RED = 2;
    public static final int COLOR_GREEN = 3;
    public static final int COLOR_BLUE = 4;
    public static final int COLOR_YELLOW = 5;

    public static final Color color_black = new Color(13, 13, 13, 255);
    public static final Color color_white = new Color(255, 255, 255, 255);
    public static final Color color_red = new Color(230, 0, 3, 255);
    public static final Color color_green = new Color(79, 172, 36, 255);
    public static final Color color_blue = new Color(0, 70, 155, 255);
    public static final Color color_yellow = new Color(255, 237, 0, 255);
    public static final Color color_orange = new Color(235, 98, 26, 255);

    // Text render queue
    private Queue<TextQueue> textque = new LinkedList<TextQueue>();
    private boolean initialized = false;   
    private ArrayList<Font> fonts = new ArrayList<Font>();
    public Font defaultFont = null;
    
    public TextManager()  {
        
    }

    public void Init() throws IOException {
        if(initialized) return;
        initialized = true;
        
        // Load fonts
        fonts.add(Font.loadOld("cubetech/data/textures/ui/MediumFont.csv", "data/textures/ui/mediumfont.png"));
        defaultFont = fonts.get(0);
    }
    
    // Adds text, uses default color (white) and maxsize is screen bounds
    public Vector2f AddText(Vector2f pos, String text, Align align, Type type) {
        return AddText(pos, text, align, null, null, type, 1f);
    }

    // Adds text, uses default color (white) and maxsize is screen bounds
    public Vector2f AddText(Vector2f pos, String text, Align align, Type type, float scale) {
        return AddText(pos, text, align, null, null, type, scale);
    }
    
    // Adds text, uses default color (white) and maxsize is screen bounds
    public Vector2f AddText(Vector2f pos, String text, Align align, Type type, float scale, int layer) {
        return AddText(pos, text, align, null, null, type, scale, layer);
    }

    // Splits the text so it fits nicely in the given maxsize (doesn't cap height)
    public Vector2f AddText(Vector2f pos, String text, Align align, Color color, Vector2f maxSize, Type type, float scale) {
        return AddText(pos, text, align, color, maxSize, type, scale, 0);
    }

    // Splits the text so it fits nicely in the given maxsize (doesn't cap height)
    public Vector2f AddText(Vector2f pos, String text, Align align, Color color, Vector2f maxSize, Type type, float scale, int layer) {
        if(maxSize == null)
            maxSize = Ref.glRef.GetResolution();
        if(color == null)
            color = GetColor(-1);

        if(type == Type.GAME && scale < 0f)
            scale *= -Ref.cvars.Find("cg_fov").fValue;
        
        Font font = defaultFont;
        
        textque.add(new TextQueue(font, pos, text, align, color, maxSize, type, scale, layer)); // Queue up for rendering
        popRenderQueue(); // Process immediatly, this will create a lot of sprites

        // Calculate width and height now
        return font.GetStringSize(text, maxSize,null, scale, type);
    }

    // Empties the text-queue.. 
    public void finishRenderQueue() {
        while(!textque.isEmpty()) {
            TextQueue que = textque.poll();
            que.PrintText();
        }
    }

    // Renders the top item in the text queue
    public void popRenderQueue() {
        TextQueue que = textque.poll();
        if(que != null) que.PrintText();
    }

    public static String RemoveDiatricalMarks(String str) {
        return Normalizer.normalize(str, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+","");
    }
    
    public static Color GetColor(int colornumber) {
        switch(colornumber) {
            case COLOR_BLACK:
                return color_black;
            case COLOR_WHITE:
            default:
                return color_white;
            case COLOR_RED:
                return color_red;
            case COLOR_GREEN:
                return color_green;
            case COLOR_BLUE:
                return color_blue;
            case COLOR_YELLOW:
                return color_yellow;
        }
    }

    public int GetCharHeight() {
        return defaultFont.GetCharHeight();
    }

    public Vector2f GetStringSize(String text, Vector2f maxsize, Collection<SimpleEntry<Integer,Integer>> nlIndices, float scale, Type type) {
        return defaultFont.GetStringSize(text, maxsize, nlIndices, scale, type);
    }

    
    public class TextQueue {
        public Font font;
        public Vector2f Position = new Vector2f();
        public String Text;
        public Align Align;
        public Color color;
        public Vector2f MaxSize;
        public Type type;
        public float scale;
        public int layer;

        public TextQueue(Font font, Vector2f pos, String text, Align align, Color color, Vector2f maxSize, Type type, float scale, int layer) {
            this.font = font;
            Position.set(pos);
            Text = text;
            Align = align;
            this.color = color;
            if(maxSize != null) MaxSize = new Vector2f(maxSize);
            this.type = type;
            this.scale = scale;
            this.layer = layer;
        }

        private void PrintText() {
            font.PrintText(this);
        }
    }

    public enum Align {
        LEFT,
        CENTER,
        RIGHT
    }

}
