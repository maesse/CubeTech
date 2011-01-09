/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.misc.Ref;
import java.util.HashMap;
import org.lwjgl.util.vector.Vector2f;
import org.openmali.FastMath;

/**
 *
 * @author mads
 */
public class CubeText {
    
    HashMap<Integer, String> charmap = new HashMap<Integer, String>();
    public String drawString = "made by poopatrol        mazing       torybash       kainsta";
    CubeTexture cubes;
    public float startpos = 400;
    public float temppos;
    public int yoffset = 100;
    float SINHEIGHT = 15;
    float blockTimeFrac = 100f;
    float timefrac = 300;
    int time = 0;
    float temptime;

    int colorcycle = 0;
    boolean colorcycleasc = true;
    public CubeText() {
        cubes = (CubeTexture)(Ref.ResMan.LoadResource("data/cubes.png").Data);
        charmap.put(0, "XXXX XXXXX XX X");
        charmap.put(1, "XXXX XXX X XXXX");
        charmap.put(2, "XXXX  X  X  XXX");
        charmap.put(3, "XX X XX XX XXX ");
        charmap.put(4, "XXXX  XXXX  XXX");
        charmap.put(5, "XXXX  XXXX  X  ");
        charmap.put(6, "XXXX  X XX XXXX");
        charmap.put(7, "X XX XXXXX XX X");
        charmap.put(8, "XXX X  X  X XXX");
        charmap.put(9, "  X  X  XX XXXX");
        charmap.put(10, "X XX XXX X XX X");
        charmap.put(11, "X  X  X  X  XXX");
        charmap.put(12, "XXXXXX X XX X XX X XX X X");
        charmap.put(13, "XXXX XX XX XX X");
        charmap.put(14, "XXXX XX XX XXXX");

        charmap.put(15, "XXXX XXXXX  X  ");
        charmap.put(16, "XXXX XX XXXX X ");
            charmap.put(17, "XXXX XXX X XX X");
            charmap.put(18, "XXXX  XXX  XXXX");
        charmap.put(19, "XXX X  X  X  X "); // t
        charmap.put(20, "X XX XX XX XXXX");

        charmap.put(21, "X XX XX XX X X ");
        charmap.put(22, "X X XX X XX X XX X XXXXXX");
        charmap.put(23, "X XX X X X XX X");
        charmap.put(24, "X XX XXXX X  X ");
        charmap.put(25, "XXX  X X X  XXX");
    }

    public void SetString(String str) {
        drawString = str.toLowerCase();
        startpos = 0;
    }

    public void Render(int msec) {
        time += msec;
        startpos -= ((float)msec/1000f)*100f;;
        colorcycle = 0;
        colorcycleasc = true;
        temppos = startpos;
        temptime = time;
        boolean charoutofscreen = false;
        for (int i= 0; i < drawString.length(); i++) {
            char c = drawString.charAt(i);
            
            if(c == ' ' || !Character.isLetter(c)) {
                temppos += DrawChaw(null, true, true);
                temptime += blockTimeFrac*1;
                continue;
            }
            
            String cmap = charmap.get((int)(c - 'a'));
            boolean smallchar = cmap.length() <= 5*3;
            temppos += DrawChaw(cmap, smallchar, false);

            if(colorcycleasc)
            {
                colorcycle++;
                if(colorcycle >= 21) {
                    colorcycle = 20;
                    colorcycleasc = false;
                }
            }
            else {
                colorcycle--;
                if(colorcycle < 0) {
                    colorcycle = 0;
                    colorcycleasc = true;
                }
            }
            if(smallchar)
                temptime += blockTimeFrac*3;
            else
                temptime += blockTimeFrac*5;
        }
        if(temppos < 0)
            startpos = 850;
    }

    Vector2f Hags(float x, float y) {
        return new Vector2f(x/800, y/600);
    }

    int DrawChaw(String map, boolean small, boolean whitespace) {
        if(whitespace) {
            // special case
            return 30;
        }

        Sprite spr;

        if(small) {
            for (int i= 0; i < 5; i++) {
                for (int j= 0; j < 3; j++) {
                    int index = i*3 + j;
                    char c = map.charAt(index);
                    if(c != 'X')
                        continue;
                    int x = index%3;
                    int y = index/3;
                    float bonusHeight = ((FastMath.sin((float)(temptime+x*blockTimeFrac)/timefrac)+1f)/2f);
                    //float bonusHeight2 = ((FastMath.sin((float)(temptime+y*blockTimeFrac)/timefrac)+1f)/2f);
                    float bonusHeight2 = 0f;
                    spr = Ref.SpriteMan.GetSprite(Type.HUD);
                        spr.Set(Hags(temppos + x*14 - y*10 + bonusHeight2*3f, yoffset - y*10 - x*5 + bonusHeight*SINHEIGHT), Hags(24,24), cubes, new Vector2f(colorcycle%5 * (24f/128f), colorcycle/5 * (24f/128f)), new Vector2f(24f/128f, 24f/128f));
                }
            }
        } else {
            for (int i= 0; i < 5; i++) {
                for (int j= 0; j < 5; j++) {
                    int index = i*5 + j;
                    char c = map.charAt(index);
                    if(c != 'X')
                        continue;
                    int x = index%5;
                    int y = index/5;
                    float bonusHeight = ((FastMath.sin((float)(temptime+x*blockTimeFrac)/timefrac)+1f)/2f);
                    //float bonusHeight2 = ((FastMath.sin((float)(temptime+y*blockTimeFrac)/timefrac)+1f)/2f);
                    float bonusHeight2 = 0f;
                    spr = Ref.SpriteMan.GetSprite(Type.HUD);
                        spr.Set(Hags(temppos + x*14 - y*10 + bonusHeight2*3f, yoffset - y*10 - x*5 + bonusHeight*SINHEIGHT), Hags(24,24), cubes, new Vector2f(colorcycle%5 * (24f/128f), colorcycle/5 * (24f/128f)), new Vector2f(24f/128f, 24f/128f));
                }
            }
        }

        

        if(small)
            return 13*3+28;
        else
            return 13*5+28;
    }
}
