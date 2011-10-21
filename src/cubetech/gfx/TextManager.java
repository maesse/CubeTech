/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

import cubetech.common.Common;
import cubetech.misc.Ref;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import cubetech.gfx.SpriteManager.Type;
import java.text.Normalizer;
import java.text.Normalizer.Form;


import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class TextManager {
    CubeTexture fontTex;
    Vector2f offset = new Vector2f(3,1);
    Vector2f charsize = new Vector2f(34,32);
    Vector2f cellcount = new Vector2f(15,15);

    Vector2f[] charSizes = new Vector2f[15*15];
    Letter[] letters;
    int startChar;

    Queue<TextQueue> textque = new LinkedList<TextQueue>();
    boolean initialized = false;

    // Colors
    
    public static final int COLOR_WHITE = 0;
    public static final int COLOR_BLACK = 1;
    public static final int COLOR_RED = 2;
    public static final int COLOR_GREEN = 3;
    public static final int COLOR_BLUE = 4;
    public static final int COLOR_YELLOW = 5;

    Color color_black = new Color(13, 13, 13, 255);
    Color color_white = new Color(255, 255, 255, 255);
    Color color_red = new Color(230, 0, 3, 255);
    Color color_green = new Color(79, 172, 36, 255);
    Color color_blue = new Color(0, 70, 155, 255);
    Color color_yellow = new Color(255, 237, 0, 255);
    Color color_orange = new Color(235, 98, 26, 255);

    
    
    public class TextQueue {
        public Vector2f Position;
        public String Text;
        public Align Align;
        public Color color;
        public Vector2f MaxSize;
        public Type type;
        public float scale;
        public int layer;

        public TextQueue(Vector2f pos, String text, Align align, Color color, Vector2f maxSize, Type type, float scale, int layer) {
            Position = pos;
            Text = text;
            Align = align;
            this.color = color;
            MaxSize = maxSize;
            this.type = type;
            this.scale = scale;
            this.layer = layer;
        }
    }

    public enum Align {
        LEFT,
        CENTER,
        RIGHT
    }

    public TextManager()  {
        
    }
    
    

    public Color GetColor(int colornumber) {
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

    public void Init() throws IOException {
        if(initialized)
            return;
        initialized = true;
        // Init textrendering
        fontTex = (CubeTexture)Ref.ResMan.LoadTexture("data/textures/ui/mediumfont.png");
        for (int i= 0; i < charSizes.length; i++) {
            charSizes[i] = charsize;
        }

        URL url = ResourceManager.getClassLoader().getResource("cubetech/data/textures/ui/MediumFont.csv");
        if(url == null)
            throw new IOException("Mission font data: MediumFont.csv");

        InputStream stream = url.openStream();
        BufferedReader dis = new BufferedReader(new InputStreamReader(stream));
        String line = null;
        int count = 0;
        while((line = dis.readLine()) != null) {
            if(line.startsWith("Start Char,")) {
                String sc = line.substring("Start Char,".length()).trim();
                startChar = Integer.parseInt(sc);
                continue;
            }

            if(line.startsWith("Char ")) {
                String[] lines = line.split(" ");
                if(lines.length < 4)
                    continue;
                int cindex = Integer.parseInt(lines[1]);
                if(cindex < startChar || cindex >= startChar + ((int)cellcount.x * (int)cellcount.y))
                    continue; // not valid
                if(!lines[2].equals("Base") || !lines[3].startsWith("Width,"))
                    continue; // not base width

                String leftover = lines[3].substring("Width,".length()).trim();
                int width = Integer.parseInt(leftover);

                cindex -= startChar;
                charSizes[cindex] = new Vector2f(width, charsize.y);
                count++;
            }
        }
        //System.out.println("Read " + count + " letters..");

        letters = new Letter[charSizes.length];
        for (int i= 0; i < letters.length; i++) {
            Vector2f charIndex = new Vector2f(i%(int)cellcount.x, i/(int)cellcount.x);
            if(charIndex.y >= cellcount.y)
                charIndex.y = cellcount.y -1;

            charIndex.x *= charsize.x;
            charIndex.y *= charsize.y;

            charIndex.x += offset.x;
            charIndex.y += offset.y;

            Vector2f size = new Vector2f();
            Vector2f.add(charIndex, charSizes[i], size);

            letters[i] = new Letter(charIndex, size);
        }
    }

    public int GetCharHeight() {
        return (int)charsize.y ;
    }


    // Empties the text-queue.. 
    public void Render() {
        while(!textque.isEmpty()) {
            TextQueue que = textque.poll();
            if(que == null)
                break;
            PrintText(que);
        }
    }

    // Renders the top item in the text queue
    public void PopQueue() {
        if(!textque.isEmpty()) {
            TextQueue que = textque.poll();
            if(que == null)
                return;
            PrintText(que);
        }
    }

    private void PrintText(TextQueue queue) {
        Vector2f pos = queue.Position;
        int layer = queue.layer;
        Color color = new Color(queue.color);
        String text = queue.Text;
        Align align = queue.Align;
        Vector2f maxSize = queue.MaxSize;
        Type type = queue.type;
        float scale = queue.scale;

        float width = Ref.glRef.GetResolution().x;
        float height = Ref.glRef.GetResolution().y;

//        float aspect = height/width;
//

//        else
        {
            width = (1f / scale);
            height = (1f / scale);
        }

        if(type == Type.GAME) {
            width *= 6;
            height *= 6f;
//            width *= Ref.cvars.Find("cg_fov").iValue / Ref.glRef.currentMode.getWidth() ;
//            height *= (Ref.cvars.Find("cg_fov").iValue * (Ref.glRef.currentMode.getHeight()/Ref.glRef.currentMode.getWidth())) / Ref.glRef.currentMode.getHeight();
        }
        
//        if(type == Type.GAME) {
//            maxSize = new Vector2f(Ref.glRef.GetResolution());
//
//         maxSize.scale(2f);
//        }

        float xoffset = (pos.x);
        ArrayList<AbstractMap.SimpleEntry<Integer,Integer>> linebreakList = new ArrayList<AbstractMap.SimpleEntry<Integer,Integer>>();
        Vector2f strSize = GetStringSize(text, maxSize, linebreakList, scale, type); // maxsize is relative to position
        switch(align) {
           case CENTER:
               xoffset -= linebreakList.get(0).getValue() / 2;
               break;
           case RIGHT:
               xoffset -= linebreakList.get(0).getValue() + 5;
               break;
        }
        xoffset *=  width;

        float currW = 0;
        int currH = 0;
        
        int nlIndex = 0;
        int nextLinebreak = (linebreakList.size()<(nlIndex))?9999999:linebreakList.get(nlIndex++).getKey();
        for (int i = 0; i < text.length(); i++) {
            // Time for a linebreak
            if(i >= nextLinebreak) {
                currW = 0; // \r
                if(type == Type.HUD)
                    currH--; // \n
                else
                    currH++;
                // Handle alignment for the new line
                switch(align) {
                   case CENTER:
                       xoffset = (pos.x * width) - linebreakList.get(nlIndex).getValue() / 2;
                       break;
                   case RIGHT:
                       xoffset = (pos.x * width) - linebreakList.get(nlIndex).getValue();
                       break;
                }

                // Set mark for next linebreak, if any
                nextLinebreak = (linebreakList.size()<nlIndex)?9999999:linebreakList.get(nlIndex++).getKey();
                continue;
            }

            char c = text.charAt(i);

            if(Character.isWhitespace(c) && c != '\n') { // whitespace
                currW += charsize.x*0.35f;
                continue;
            }

            // ignore newlines, we already know where they are
            if(c == '\r' || c == '\n')
                continue;

            // Handle color
            if(text.length() > i + 1 && c == '^' && Character.isDigit(text.charAt(i+1)))
            {
                Color newcolor = GetColor(Character.getNumericValue(text.charAt(i+1)));
                // Keep alpha
                int a = color.getAlpha();
                color.setColor(newcolor);
                color.setAlpha(a);
                i++;
                continue;
            }

            int derpa = (int)c;
            int letter = derpa - startChar;
            if((letter >= letters.length) || letter < 0)
            {
                //System.err.println("Unknown letter: " + letter);
                letter = '#'-startChar;
            }

            // Draw
            ctexOffset.set(letters[letter].Offset.x, letters[letter].Offset.y);
            ctexOffset.x /= fontTex.Width;
            ctexOffset.y = fontTex.Height - ctexOffset.y - charsize.y;
//            texOffset.y -= charsize.y*4;
            ctexOffset.y /= fontTex.Height;
            csize.set(charsize.x, charsize.y);
            if(charsize.x - charSizes[letter].x - 4 > 0)
                csize.x -= 2;
            ctexSize.set(csize.x, csize.y);
            csize.x /= width;
            csize.y /= height;
            ctexSize.x /= fontTex.Width;
            ctexSize.y /= fontTex.Height;
            float finalY = Ref.glRef.GetResolution().y - (charsize.y* scale) - (pos.y - (currH * charsize.y * scale) / height);
            if(type == Type.GAME)
                finalY = (pos.y - (currH * charsize.y) / (float)height);
            int bonusY = -1;
            if(type == Type.HUD)
                bonusY = 2;
            cfinalpos.set((float)(xoffset + currW) / width , finalY+bonusY);

            Sprite sprite = Ref.SpriteMan.GetSprite(type);
            sprite.Set(cfinalpos, csize, fontTex, ctexOffset, ctexSize);
            sprite.SetColor(color);
            sprite.SetDepth(layer);

            // Count up chars
            currW += (int)charSizes[letter].x;
        }
    }
    private Vector2f ctexOffset = new Vector2f();
    private Vector2f cfinalpos = new Vector2f();
    private Vector2f ctexSize = new Vector2f();
    private Vector2f csize = new Vector2f();

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
//        else if(type == Type.GAME)
//            scale /= 6f;
        
        textque.add(new TextQueue(pos, text, align, color, maxSize, type, scale, layer)); // Queue up for rendering
        PopQueue(); // Process immediatly, this will create a lot of sprites


        // Calculate width and height now
        return GetStringSize(text, maxSize,null, scale, type);
    }

    public static String RemoveDiatricalMarks(String str) {
        return Normalizer.normalize(str, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+","");
    }

    // will fill the newlineIndex with character index where the line should be split
    public Vector2f GetStringSize(String str, Vector2f maxSize,
            Collection<AbstractMap.SimpleEntry<Integer,Integer>> newlineIndexes, float scale, Type type) {
        if(maxSize == null)
            maxSize = Ref.glRef.GetResolution();
        int w = 0;
        int lines = 1;
        // Get the bytes, because that's what the charactermap is using
//        str = RemoveDiatricalMarks(str);
        
        int lastwhitespace = 0;
        int wordWidth = 0;
        Vector2f result = new Vector2f();
        for (int i= 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if(Character.isWhitespace(c) && c != '\n') { // whitespace
                wordWidth = (int)(charsize.x * 0.35f * scale);
                lastwhitespace = i;
                w += wordWidth;
                continue;
            }

            // Don't count colors, as they gets stripped from the string when rendered
            if(str.length() > i + 1 && c == '^' && Character.isDigit(str.charAt(i+1)))
            {
                i++;
                continue;
            }



            if(c == '\r')
                continue;

            if(c == '\n' || c == Character.LINE_SEPARATOR) {
                if(w-wordWidth == 0)
                    wordWidth = 0; // word covers whole line
                
                if(newlineIndexes != null)
                        newlineIndexes.add(new AbstractMap.SimpleEntry<Integer, Integer>(i, w-wordWidth+9));
                result.x = Math.max(result.x, w);
                if(i != str.length()-1)
                    lines++;
                w = 0;
                lastwhitespace = i;
                wordWidth = 0;
            }

            // Check is it's a known character
            // TODO: Shuld still add some to width, if not known
            if(Character.isIdentifierIgnorable(c))
                continue;
            
            int charAscii = (int)c-startChar;
            if(charAscii<0 ||charAscii >= charSizes.length)
                continue;

            // Add in character width
            w += (int)(charSizes[charAscii].x * scale);
            wordWidth += (int)(charSizes[charAscii].x * scale);
            if(w > maxSize.x + 9) {
                if(lastwhitespace != 0) {
                    // Wrap word
                    result.x = Math.max(result.x, w-wordWidth);
                    if(newlineIndexes != null)
                        newlineIndexes.add(new AbstractMap.SimpleEntry<Integer, Integer>(lastwhitespace, w-wordWidth+9));
                    w = wordWidth;
                } else {
                    result.x = Math.max(result.x, w);
                    if(newlineIndexes != null)
                        newlineIndexes.add(new AbstractMap.SimpleEntry<Integer, Integer>(i-1, w+9));
                    w = 0;
                }
                wordWidth = 0;
                if(i != str.length()-1)
                    lines++;
                
                lastwhitespace = 0;
            }
        }
        result.x = Math.max(result.x, w);
        result.x += 9;
        result.y = lines * GetCharHeight() * scale;
        if(newlineIndexes != null)
            newlineIndexes.add(new AbstractMap.SimpleEntry<Integer, Integer>(str.length(), w+4));
        return result;
    }

}
