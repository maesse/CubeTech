///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package cubetech.state;
//
//import cubetech.gfx.CubeTexture;
//import cubetech.gfx.Sprite;
//import cubetech.gfx.SpriteManager;
//import cubetech.gfx.SpriteManager.Type;
//import cubetech.gfx.TextManager.Align;
//import cubetech.misc.Ref;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import org.lwjgl.Sys;
//import org.lwjgl.input.Keyboard;
//import org.lwjgl.util.Color;
//import org.lwjgl.util.vector.Vector2f;
//import org.lwjgl.util.vector.Vector4f;
//import org.newdawn.slick.openal.Audio;
//
///**
// *
// * @author mads
// */
//public class IntroState implements IGameState {
//    String Name = "intro";
//    long startTime;
//    CubeParticle[] particles = new CubeParticle[150];
//    CubeTexture tex;
//    Audio introSound = null;
//
//    public class CubeParticle {
//        public Vector2f Position;
//        public Vector2f FinalPos;
//
//        public CubeParticle(Vector2f pos, Vector2f finalPos) {
//            this.Position = pos;
//            this.FinalPos = finalPos;
//        }
//    }
//
//    public void Enter() {
//        startTime = Sys.getTime()/(Sys.getTimerResolution()/1000L);
//        int prLine = (int)((float)particles.length/15f);
//
//        // Bottom
//        Vector2f fPos = new Vector2f(0.2f,0.2f);
//        float xprParticle = 0.5f/(float)(prLine*2);
//        for (int i= 0; i < prLine*2; i++) {
//            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
//            Vector2f finalPos = new Vector2f(fPos.x+xprParticle*(float)i, fPos.y);
//            particles[i] = new CubeParticle(pos, finalPos);
//        }
//        // Left
//        fPos = new Vector2f(0.2f,0.2f);
//        xprParticle = 0.5f/(float)(prLine*2);
//        for (int i= 0; i < prLine*2; i++) {
//            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
//            Vector2f finalPos = new Vector2f(fPos.x, fPos.y+xprParticle*(float)i);
//            particles[prLine*2+i] = new CubeParticle(pos, finalPos);
//        }
//        // Right
//        fPos = new Vector2f(0.7f,0.2f);
//        xprParticle = 0.5f/(float)(prLine*2);
//        for (int i= 0; i < prLine*2; i++) {
//            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
//            Vector2f finalPos = new Vector2f(fPos.x, fPos.y+xprParticle*(float)i);
//            particles[prLine*4+i] = new CubeParticle(pos, finalPos);
//        }
//
//        // Top
//        fPos = new Vector2f(0.2f,0.7f);
//        xprParticle = 0.5f/(float)(prLine*2);
//        for (int i= 0; i < prLine*2; i++) {
//            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
//            Vector2f finalPos = new Vector2f(fPos.x+xprParticle*(float)i, fPos.y);
//            particles[prLine*6+i] = new CubeParticle(pos, finalPos);
//        }
//
//        // Top 2
//        fPos = new Vector2f(0.35f,0.85f);
//        xprParticle = 0.5f/(float)(prLine*2);
//        for (int i= 0; i < prLine*2; i++) {
//            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
//            Vector2f finalPos = new Vector2f(fPos.x+xprParticle*(float)i, fPos.y);
//            particles[prLine*8+i] = new CubeParticle(pos, finalPos);
//        }
//        // Right 2
//        fPos = new Vector2f(0.85f,0.35f);
//        xprParticle = 0.5f/(float)(prLine*2);
//        for (int i= 0; i < prLine*2; i++) {
//            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
//            Vector2f finalPos = new Vector2f(fPos.x, fPos.y+xprParticle*(float)i);
//            particles[prLine*10+i] = new CubeParticle(pos, finalPos);
//        }
//        // Slash1
//        fPos = new Vector2f(0.2f,0.7f);
//        xprParticle = 0.5f/(float)(prLine*2);
//        for (int i= 0; i < prLine/2+1; i++) {
//            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
//            Vector2f finalPos = new Vector2f(fPos.x+xprParticle*(float)i, fPos.y+xprParticle*(float)i);
//            particles[prLine*12+i] = new CubeParticle(pos, finalPos);
//        }
//        // Slash2
//        fPos = new Vector2f(0.7f,0.7f);
//        xprParticle = 0.5f/(float)(prLine*2);
//        for (int i= 0; i < prLine/2+2; i++) {
//            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
//            Vector2f finalPos = new Vector2f(fPos.x+xprParticle*(float)i, fPos.y+xprParticle*(float)i);
//            particles[prLine*12+i+(prLine/2)+1] = new CubeParticle(pos, finalPos);
//        }
//        // Slash3
//        fPos = new Vector2f(0.7f,0.2f);
//        xprParticle = 0.5f/(float)(prLine*2);
//        for (int i= 0; i < prLine/2+1; i++) {
//            Vector2f pos = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
//            Vector2f finalPos = new Vector2f(fPos.x+xprParticle*(float)i, fPos.y+xprParticle*(float)i);
//            particles[prLine*13+i+3] = new CubeParticle(pos, finalPos);
//        }
//
//        tex = Ref.ResMan.LoadTexture("data/particle.png");
//        introSound = Ref.soundMan.PlayOGGMusic("data/cubetech.ogg", 0f, false, true);
//        //Ref.soundMan.playEffect(Ref.soundMan.addSound("data/explosion.wav"), 1.0f);
//    }
//
//    public void Exit() {
//        introSound.stop();
//        Ref.soundMan.PlayBackgroundMusic(true);
//    }
//
//    public void RunFrame(int msec) {
//
//        long time = Sys.getTime()/(Sys.getTimerResolution()/1000L);
//
//        // Play intro for 3 secs
//        if(time > startTime + 8000 || Ref.Input.IsKeyPressed(Keyboard.KEY_ESCAPE)) {
//            try {
//                // State change
//                Ref.StateMan.SetState("menu");
//            } catch (Exception ex) {
//                Logger.getLogger(IntroState.class.getName()).log(Level.SEVERE, null, ex);
//                System.exit(-1);
//            }
//            return;
//        }
//
//        // 0-2000 ms = process 0-1f, 2000-3000ms = process 1f
//        float process = (time - startTime);
//        float actual = process;
//        if(process > 4500)
//            process = 4500;
//        process /= 4500f;
//        Color color = new Color(255,255,255,(int)(process*255));
//        if(actual > 6400) {
//            color.setAlpha((int)((7000-actual)*255/500f));
//            if(actual > 7000)
//                color.setAlpha(0);
//        }
//        // Update and render all particles
//        for (int i= 0; i < particles.length; i++) {
//            CubeParticle p = particles[i];
//            if(p == null)
//                continue;
//
//                Vector2f pos = new Vector2f(p.FinalPos.x * process + p.Position.x * (1f-process), p.FinalPos.y * process + p.Position.y * (1f-process));
//
//
//            Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
//            spr.Set(pos, 0.02f,tex);
//            spr.SetColor(color);
//            //spr.Color = color;
//        }
//
//        // Reset this to msec
//        process = (time - startTime);
//
////        if(process > 2000)
////            process = 2000;
//        // Show text after 1500 msec
//        if(process > 3000) {
//            if(process > 4000) { // Fully faded in after 2000 msec
//                if(actual > 6500)
//                    Ref.textMan.AddText(new Vector2f(0.5f, 0.1f), "CubeTech.", Align.CENTER, new Color(255,255,255,(int)(((7000f-actual)/500f)*255)),null, Type.HUD,1);
//                else
//                    Ref.textMan.AddText(new Vector2f(0.5f, 0.1f), "CubeTech.", Align.CENTER, Type.HUD);
//
//            }
//            else // Handle fading from 1500-2000
//                Ref.textMan.AddText(new Vector2f(0.5f, 0.1f), "CubeTech.", Align.CENTER, new Color(255, 255,255,(int)((1f-(float)(4000-process)/1000f)*255)),null, Type.HUD,1);
//        }
//    }
//
//
//    public String GetName() {
//        return Name;
//    }
//
//}
