package cubetech.gfx;

import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.gfx.TextManager.TextQueue;
import cubetech.misc.Ref;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author Mads
 */
public class Font {
    // Font settings
    CubeTexture fontTex;
    Vector2f offset = new Vector2f(3,1);
    Vector2f charsize = new Vector2f(34,32);
    Vector2f cellcount = new Vector2f(15,15);
    Vector2f[] charSizes = new Vector2f[15*15];
    Letter[] letters;
    int startChar;
    
    private Vector2f ctexOffset = new Vector2f();
    private Vector2f cfinalpos = new Vector2f();
    private Vector2f ctexSize = new Vector2f();
    private Vector2f csize = new Vector2f();
    
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
    
    public int GetCharHeight() {
        return (int)charsize.y ;
    }
    
    void PrintText(TextQueue queue) {
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
                Color newcolor = TextManager.GetColor(Character.getNumericValue(text.charAt(i+1)));
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
    
    
    public static Font loadOld(String fontFile, String texture) throws IOException {
        Font font = new Font();
        // Init textrendering
        font.fontTex = (CubeTexture)Ref.ResMan.LoadTexture(texture);
        for (int i= 0; i < font.charSizes.length; i++) {
            font.charSizes[i] = font.charsize;
        }

        URL url = ResourceManager.getClassLoader().getResource(fontFile);
        if(url == null)
            throw new IOException("Mission font data: MediumFont.csv");

        InputStream stream = url.openStream();
        BufferedReader dis = new BufferedReader(new InputStreamReader(stream));
        String line = null;
        int count = 0;
        while((line = dis.readLine()) != null) {
            if(line.startsWith("Start Char,")) {
                String sc = line.substring("Start Char,".length()).trim();
                font.startChar = Integer.parseInt(sc);
                continue;
            }

            if(line.startsWith("Char ")) {
                String[] lines = line.split(" ");
                if(lines.length < 4)
                    continue;
                int cindex = Integer.parseInt(lines[1]);
                if(cindex < font.startChar || cindex >= font.startChar + ((int)font.cellcount.x * (int)font.cellcount.y))
                    continue; // not valid
                if(!lines[2].equals("Base") || !lines[3].startsWith("Width,"))
                    continue; // not base width

                String leftover = lines[3].substring("Width,".length()).trim();
                int width = Integer.parseInt(leftover);

                cindex -= font.startChar;
                font.charSizes[cindex] = new Vector2f(width, font.charsize.y);
                count++;
            }
        }
        //System.out.println("Read " + count + " letters..");

        font.letters = new Letter[font.charSizes.length];
        for (int i= 0; i < font.letters.length; i++) {
            Vector2f charIndex = new Vector2f(i%(int)font.cellcount.x, i/(int)font.cellcount.x);
            if(charIndex.y >= font.cellcount.y)
                charIndex.y = font.cellcount.y -1;

            charIndex.x *= font.charsize.x;
            charIndex.y *= font.charsize.y;

            charIndex.x += font.offset.x;
            charIndex.y += font.offset.y;

            Vector2f size = new Vector2f();
            Vector2f.add(charIndex, font.charSizes[i], size);

            font.letters[i] = new Letter(charIndex, size);
        }
        return font;
    }
}
