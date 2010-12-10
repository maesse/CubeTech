/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

import cubetech.misc.Ref;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;

import java.util.logging.Level;
import java.util.logging.Logger;
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

    Vector4f white = new Vector4f(1,1,1,1);
    Queue<TextQueue> textque = new LinkedList<TextQueue>();

    public class TextQueue {
        public Vector2f Position;
        public String Text;
        public Align Align;
        public Vector4f Color;

        public TextQueue(Vector2f pos, String text, Align align, Vector4f color) {
            Position = pos;
            Text = text;
            Align = align;
            Color = color;
        }
    }

    public enum Align {
        LEFT,
        CENTER,
        RIGHT
    }

    public TextManager() throws IOException {
        // Init textrendering
        fontTex = (CubeTexture)Ref.ResMan.LoadResource("data/mediumfont.png").Data;
        for (int i= 0; i < charSizes.length; i++) {
            charSizes[i] = charsize;
        }

        URL url = TextManager.class.getClassLoader().getResource("cubetech/data/MediumFont.csv");
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
        System.out.println("Read " + count + " letters..");

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

    public void Render() {
        while(!textque.isEmpty()) {
            TextQueue que = textque.poll();
            if(que == null)
                break;
            PrintText(que);
        }
    }

    private void PrintText(TextQueue queue) {
        Vector2f pos = queue.Position;
        Vector4f color = queue.Color;
        String text = queue.Text;
        Align align = queue.Align;

        int width = Ref.loop.mode.getWidth();
       int height = Ref.loop.mode.getHeight();

       int xoffset = (int)(pos.x * width);
       int lineWidth = GetStringWidth(text);
       switch(align) {
           case CENTER:
               xoffset -= lineWidth / 2;
               break;
           case RIGHT:
               xoffset -= lineWidth + 5;
               break;
       }

       int currW = 0;
       int currH = 0;
       byte[] dat = null;
       try {
            dat = text.getBytes("US-ASCII");
       } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TextManager.class.getName()).log(Level.SEVERE, null, ex);
            dat = text.getBytes();
       }

       for (int i = 0; i < dat.length; i++) {
            if(dat[i] == 32) {
                currW += (int)charsize.x*0.35f;
                continue;
            }

            if(dat[i] == '\n') {
                currW = 0;
                currH++;
                continue;
            }

            if(dat[i] == '\r')
                continue;

            if(dat.length > i + 1 && dat[i] == '^' && dat[i+1] >= '0' && dat[i+1] <= '9')
            {
                // FIX: COLOR
                i++;
                continue;
            }

            int letter = dat[i] - startChar;
            if((letter >= letters.length) || letter < 0)
            {
                System.err.println("Unknown letter: " + letter);
                letter = '#'-startChar;
            }

            // Draw
            Vector2f texOffset = new Vector2f(letters[letter].Offset.x, letters[letter].Offset.y);
            texOffset.x /= fontTex.Width;
            texOffset.y /= fontTex.Height;
            Vector2f size = new Vector2f(charsize.x, charsize.y);
            if(charsize.x - charSizes[letter].x - 4 > 0)
                size.x -= 2;
            Vector2f texSize = new Vector2f(size.x, size.y);
            size.x /= width;
            size.y /= height;
            texSize.x /= fontTex.Width;
            texSize.y /= fontTex.Height;
            Vector2f finalpos = new Vector2f((float)(xoffset + currW) / width, pos.y - (currH * charsize.y) / (float)height);

            try {
                Sprite sprite = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
                //sprite.Set(finalpos, 0.1f,fontTex);
                sprite.Set(finalpos, size, fontTex, texOffset, texSize);
                sprite.Color = color;
                //sprite.Set(finalpos, size, fontTex, texOffset, size);

            } catch (Exception ex) {
                Logger.getLogger(TextManager.class.getName()).log(Level.SEVERE, null, ex);
            }


            // Count up chars
            currW += (int)charSizes[letter].x;
            if(currW >= width) {
                currW = 0;
                currH++;
            }
       }
    }

    public void AddText(Vector2f pos, String text, Align align) {
        AddText(pos, text, align, white);
    }

    public void AddText(Vector2f pos, String text, Align align, Vector4f color) {
        textque.add(new TextQueue(pos, text, align, color));
       
    }
    
    public int GetStringWidth(String str) {
        int w = 0;
        byte[] bytes;
        try {
            bytes = str.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TextManager.class.getName()).log(Level.SEVERE, null, ex);
            bytes = str.getBytes();
        }
        for (int i= 0; i < bytes.length; i++) {
            if(bytes[i] == 32) {
                w += (int)charsize.x * 0.35f;
                continue;
            }
            if(bytes.length > i + 1 && bytes[i] == '^' && bytes[i+1] >= '0' && bytes[i+1] <= '9')
            {
                i++;
                continue;
            }
            int c = bytes[i]-startChar;
            if(c<0 ||c >= charSizes.length)
                continue;

            w += (int)charSizes[c].x;
        }
        return w;
    }

}
