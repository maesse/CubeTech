/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.state;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Graphics;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.TextManager.Align;
import cubetech.misc.Ref;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class IntroState implements IGameState {
    String Name = "intro";
    long startTime;
    CubeParticle[] particles = new CubeParticle[150];
    CubeTexture tex;

    public class CubeParticle {
        public Vector2f Position;
        public Vector2f FinalPos;

        public CubeParticle(Vector2f pos, Vector2f finalPos) {
            this.Position = pos;
            this.FinalPos = finalPos;
        }
    }
    
    public void Enter() {
        startTime = Sys.getTime()/(Sys.getTimerResolution()/1000L);
        int prLine = (int)((float)particles.length/15f);
        
        // Bottom
        Vector2f fPos = new Vector2f(0.2f*Graphics.getWidth(),0.2f*Graphics.getHeight());
        float xprParticle = 0.5f/(float)(prLine*2);
        xprParticle *= Graphics.getWidth();
        for (int i= 0; i < prLine*2; i++) {
            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
            Vector2f finalPos = new Vector2f(fPos.x+xprParticle*(float)i, fPos.y);
            particles[i] = new CubeParticle(pos, finalPos);
        }
        // Left
        fPos = new Vector2f(0.2f*Graphics.getWidth(),0.2f*Graphics.getHeight());
        xprParticle = 0.5f/(float)(prLine*2);
        xprParticle *= Graphics.getWidth();
        for (int i= 0; i < prLine*2; i++) {
            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
            Vector2f finalPos = new Vector2f(fPos.x, fPos.y+xprParticle*(float)i);
            particles[prLine*2+i] = new CubeParticle(pos, finalPos);
        }
        // Right
        fPos = new Vector2f(0.7f*Graphics.getWidth(),0.2f*Graphics.getHeight());
        xprParticle = 0.5f/(float)(prLine*2);
        xprParticle *= Graphics.getWidth();
        for (int i= 0; i < prLine*2; i++) {
            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
            Vector2f finalPos = new Vector2f(fPos.x, fPos.y+xprParticle*(float)i);
            particles[prLine*4+i] = new CubeParticle(pos, finalPos);
        }

        // Top
        fPos = new Vector2f(0.2f*Graphics.getWidth(),0.7f*Graphics.getHeight());
        xprParticle = 0.5f/(float)(prLine*2);
        xprParticle *= Graphics.getWidth();
        for (int i= 0; i < prLine*2; i++) {
            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
            Vector2f finalPos = new Vector2f(fPos.x+xprParticle*(float)i, fPos.y);
            particles[prLine*6+i] = new CubeParticle(pos, finalPos);
        }

        // Top 2
        fPos = new Vector2f(0.35f*Graphics.getWidth(),0.85f*Graphics.getHeight());
        xprParticle = 0.5f/(float)(prLine*2);
        xprParticle *= Graphics.getWidth();
        for (int i= 0; i < prLine*2; i++) {
            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
            Vector2f finalPos = new Vector2f(fPos.x+xprParticle*(float)i, fPos.y);
            particles[prLine*8+i] = new CubeParticle(pos, finalPos);
        }
        // Right 2
        fPos = new Vector2f(0.85f*Graphics.getWidth(),0.35f*Graphics.getHeight());
        xprParticle = 0.5f/(float)(prLine*2);
        xprParticle *= Graphics.getWidth();
        for (int i= 0; i < prLine*2; i++) {
            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
            Vector2f finalPos = new Vector2f(fPos.x, fPos.y+xprParticle*(float)i);
            particles[prLine*10+i] = new CubeParticle(pos, finalPos);
        }
        // Slash1
        fPos = new Vector2f(0.2f*Graphics.getWidth(),0.7f*Graphics.getHeight());
        xprParticle = 0.5f/(float)(prLine*2);
        xprParticle *= Graphics.getWidth();
        for (int i= 0; i < prLine/2+1; i++) {
            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
            Vector2f finalPos = new Vector2f(fPos.x+xprParticle*(float)i, fPos.y+xprParticle*(float)i);
            particles[prLine*12+i] = new CubeParticle(pos, finalPos);
        }
        // Slash2
        fPos = new Vector2f(0.7f*Graphics.getWidth(),0.7f*Graphics.getHeight());
        xprParticle = 0.5f/(float)(prLine*2);
        xprParticle *= Graphics.getWidth();
        for (int i= 0; i < prLine/2+2; i++) {
            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
            Vector2f finalPos = new Vector2f(fPos.x+xprParticle*(float)i, fPos.y+xprParticle*(float)i);
            particles[prLine*12+i+(prLine/2)+1] = new CubeParticle(pos, finalPos);
        }
        // Slash3
        fPos = new Vector2f(0.7f*Graphics.getWidth(),0.2f*Graphics.getHeight());
        xprParticle = 0.5f/(float)(prLine*2);
        xprParticle *= Graphics.getWidth();
        for (int i= 0; i < prLine/2+1; i++) {
            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
            Vector2f finalPos = new Vector2f(fPos.x+xprParticle*(float)i, fPos.y+xprParticle*(float)i);
            particles[prLine*13+i+3] = new CubeParticle(pos, finalPos);
        }

        tex = Ref.ResMan.LoadTexture("data/particle.png");
    }

    public void Exit() {
        
    }

    public void RunFrame(int msec) {
        long time = Sys.getTime()/(Sys.getTimerResolution()/1000L);

        // Play intro for 3 secs
        if(time > startTime + 6000 || Ref.Input.IsKeyPressed(Keyboard.KEY_ESCAPE)) {
            try {
                // State change
                Ref.StateMan.SetState("menu");
            } catch (Exception ex) {
                Logger.getLogger(IntroState.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(-1);
            }
            return;
        }

        // 0-2000 ms = process 0-1f, 2000-3000ms = process 1f
        float process = (time - startTime);
        float actual = process;
        if(process > 2000)
            process = 2000;
        process /= 2000f;
        Vector4f color = new Vector4f(1,1,1,process);
        if(actual > 4000) {
            color.w = (4500-actual)/500f;
            if(actual > 4500)
                color.w = 0;
        }
        // Update and render all particles
        for (int i= 0; i < particles.length; i++) {
            CubeParticle p = particles[i];
            if(p == null)
                continue;
            
                Vector2f pos = new Vector2f(p.FinalPos.x * process + p.Position.x * (1f-process), p.FinalPos.y * process + p.Position.y * (1f-process));


            Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
            spr.setFromCenter(pos, 15f,tex);
            spr.setColor(color);
            //spr.Color = color;
        }

        // Reset this to msec
        process = (time - startTime);
        
//        if(process > 2000)
//            process = 2000;
        // Show text after 1500 msec
        if(process > 1500) {
            if(process > 2000) { // Fully faded in after 2000 msec
//                if(actual > 4500)
//                    Ref.textMan.AddText(new Vector2f(0.5f, 0.1f), "CubeTech.", Align.CENTER,  new Vector4f(1,1,1,(5000f-actual)/500f));
//                else
                    Ref.textMan.AddText(new Vector2f(0.5f*Graphics.getWidth(), 0.1f*Graphics.getHeight()), "CubeTech.", Align.CENTER, SpriteManager.Type.HUD);
            
            }
//            else // Handle fading from 1500-2000
//                Ref.textMan.AddText(new Vector2f(0.5f, 0.1f), "CubeTech.", Align.CENTER, new Vector4f(1, 1,1,1f-(float)(2000-process)/500f));
        }
    }


    public String GetName() {
        return Name;
    }
    
}
