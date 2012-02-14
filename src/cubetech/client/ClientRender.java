package cubetech.client;

import cubetech.CGame.Render;
import cubetech.CGame.ViewParams;
import cubetech.Game.Gentity;
import cubetech.common.Common;
import cubetech.common.ICommand;
import cubetech.common.IThinkMethod;
import cubetech.gfx.GLRef;
import cubetech.gfx.GLState;
import cubetech.gfx.IRenderCallback;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.input.Input;
import cubetech.misc.BMPWriter;
import cubetech.misc.FasterZip;
import cubetech.misc.PngEncoder;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Profiler.SecTag;
import cubetech.misc.Ref;
import cubetech.ui.UI;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.vector.Vector2f;
import org.omg.PortableInterceptor.ACTIVE;

/**
 *
 * @author Mads
 */
public class ClientRender {
    int lastFpsUpdateTime = 0;
    int nFrames = 0;
    public boolean screenshot;
    public int currentFPS;
    
    public ClientRender() {
        Ref.commands.AddCommand("screenshot", cmd_screenshot);
    }
    
    private void BeginFrame() {
       GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
       Ref.SpriteMan.Reset();
       Ref.render.newFrame();
    }
    
    private void renderView(final ViewParams view) {
        view.apply();
//        GL11.glViewport(0, 0, 1280, 800);
        if(Ref.glRef.shadowMan.isEnabled() && !Ref.glRef.deferred.isEnabled() && 
                view != null && view.lights.size() > 0) {
            Ref.glRef.shadowMan.renderShadowsForLight(view.lights.get(0), view, new IRenderCallback() {
                public void render(ViewParams view) {
                    Ref.render.renderAll(view, Render.RF_SHADOWPASS);
                }
            });
        }

        if(!view.renderList.list.isEmpty()) {
            Ref.glRef.deferred.startDeferred(view);
            Ref.render.renderAll(view, 0);
            Ref.glRef.deferred.stopDeferred();
            Ref.glRef.deferred.finalizeShading();
        }
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        Ref.glRef.matrixBuffer.clear();
        view.ProjectionMatrix.store(Ref.glRef.matrixBuffer);
        Ref.glRef.matrixBuffer.flip();
        GL11.glLoadMatrix(Ref.glRef.matrixBuffer);
        Ref.glRef.matrixBuffer.clear();


        //GLState.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        view.viewMatrix.store(Ref.glRef.matrixBuffer);
        Ref.glRef.matrixBuffer.flip();
        GL11.glLoadMatrix(Ref.glRef.matrixBuffer);
        Ref.glRef.matrixBuffer.clear();

        //Ref.glRef.deferred.startPostDeferred();

        Ref.render.renderAll(view, Render.RF_POSTDEFERRED);
        if(!view.renderList.list.isEmpty()) {
            Ref.glRef.deferred.stopPostDeferred();
        }
        
        
    }

    private void EndFrame() {
        SecTag s = Profiler.EnterSection(Sec.ENDFRAME);
        nFrames++;
        if(Ref.client.realtime >= lastFpsUpdateTime + 1000) {
            currentFPS = nFrames;
            nFrames = 0;
            lastFpsUpdateTime = Ref.client.realtime;
        }
        
        
        // Render normal sprites
        Ref.SpriteMan.DrawNormal();
        
        // Render all views
        ArrayList<ViewParams> viewList = Ref.render.getViewList();
        for (ViewParams view : viewList) {
            renderView(view);
        }
        
        // Create view for UI and such
        ViewParams uiView = ViewParams.createFullscreenOrtho();
        Ref.Input.getOverlay().render(uiView);
        Ref.render.assignAndClear(uiView);
        renderView(uiView);
        
        
        Ref.render.reset();
        
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(0, (int)Ref.glRef.GetResolution().x, 0, (int)Ref.glRef.GetResolution().y, 1,-1000);

            GLState.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
        
        // Queue HUD renders
        Ref.Console.Render();
        if(Ref.client.cl_showfps.iValue == 1)
            Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x, 0), ""+currentFPS, Align.RIGHT, Type.HUD);
        Ref.SpriteMan.DrawHUD();
        Ref.textMan.finishRenderQueue(); // Draw remaining text - shouldn't be any


        if(screenshot || (Ref.client.demo.isPlaying() && Ref.cvars.Find("cl_demorecord").isTrue())) {
            takeScreenshot();
        }
        s.ExitSection();
        
        updateScreen();
    }

    ICommand cmd_screenshot = new ICommand() {
        public void RunCommand(String[] args) {
            screenshot = true;
        }
    };


    ByteBuffer screenshotBuffer = null;
    private void takeScreenshot() {
        screenshot = false;

        // Are we taking pictures of a demo?
        boolean video = (Ref.client.demo.isPlaying() && Ref.cvars.Find("cl_demorecord").isTrue());

        int width = (int)Ref.glRef.GetResolution().x;
        int height = (int)Ref.glRef.GetResolution().y;

        // Copy backbuffer
        if(screenshotBuffer == null || screenshotBuffer.capacity() < 3*width*height) {
            screenshotBuffer = ByteBuffer.allocateDirect(3*width*height);
        }
        screenshotBuffer.clear();
        GL11.glReadPixels(0, 0, width, height, GL12.GL_BGR, GL11.GL_UNSIGNED_BYTE, screenshotBuffer);

        byte[] encData = null;
        if(video) {
            // Bmp for speed
            BMPWriter enc = new BMPWriter(screenshotBuffer, width, height);
            encData = enc.encodeImage();
            writeVideoFrame(encData);
        } else {
            // Encode as png
            // Change format from bgr to rgb
            screenshotBuffer.position(0);
            for (int i = 0; i < width*height; i++) {
                byte b = screenshotBuffer.get();
                byte g = screenshotBuffer.get();
                byte r = screenshotBuffer.get();
                screenshotBuffer.position(i*3);
                screenshotBuffer.put(r);
                screenshotBuffer.put(g);
                screenshotBuffer.put(b);
            }
            PngEncoder enc = new PngEncoder(screenshotBuffer, false,PngEncoder.FILTER_NONE, 3, width, height);
            encData = enc.pngEncode();
            writeScreenshot(encData);
        }
    }

    private void writeVideoFrame(byte[] bmpdata) {
        
        // Open zip stream
        if(Ref.client.demo.zipStream == null) {
            String directory = "demos\\";

            // Create dest folder
            File folder = new File(directory);
            if(!folder.exists() || !folder.isDirectory()) {
                boolean created = folder.mkdir();
                if(!created) {
                    Common.Log("Can't create destination directory %s", directory);
                    return;
                }
            }
            String filepath = directory + Ref.client.demo.getDemoName() + ".zip";
            try {
                
                Ref.client.demo.zipStream = new FasterZip(new FileOutputStream(filepath));
                Ref.client.demo.zipStream.start();
            } catch (FileNotFoundException ex) {
                Common.Log(Common.getExceptionString(ex));
                // todo: stop playback
            }
        }

        // Write to zipstream
        if(Ref.client.demo.zipStream != null) {
            int number = Ref.client.clc.timeDemoFrames;
            Ref.client.demo.zipStream.enqueueFile(bmpdata, number + ".bmp");
        }

        // resync
        while(Ref.client.demo.zipStream.getJobCount() > 10) {
            try {
                Thread.sleep(2);
            } catch (InterruptedException ex) { }
        }
    }

    private void writeScreenshot(byte[] filedata) {
        // Figure out destination
        String directory = "screenshots\\"; // default screenshot direction

        // Create dest folder
        File folder = new File(directory);
        if(!folder.exists() || !folder.isDirectory()) {
            boolean created = folder.mkdir();
            if(!created) {
                Common.Log("Can't create destination directory %s", directory);
                return;
            }
        }

        // Write it
        int number =  Ref.client.framecount;
        String filename = directory + "\\" + number;
        File f = new File(filename + ".png");
        try {
            boolean created = f.createNewFile();
            if(!created || !f.canWrite()) {
                Common.Log("Couldn't create file %s for screenshot", filename+".png");
                return;
            }
            FileChannel fc = new FileOutputStream(f).getChannel();
            ByteBuffer data = ByteBuffer.wrap(filedata, 0, filedata.length);
            fc.write(data);
            fc.close();
            Common.Log("Saved screenshot as %s.png (%.1fMB)", filename, filedata.length / (1024*1024f));
        } catch (IOException ex) {
            Common.Log(Common.getExceptionString(ex));
        }
    }

    private void updateScreen() {
        SecTag s = Profiler.EnterSection(Sec.RENDERWAIT);
        Display.update();
        s.ExitSection();
    }
    
    public void UpdateScreen() {
        GLRef.checkError();
        BeginFrame();



        if(!Ref.ui.IsFullscreen()) {
            switch(Ref.client.clc.state) {
                case DISCONNECTED:
                    // Force ui up
                    Ref.ui.SetActiveMenu(UI.MENU.MAINMENU);
                    break;
                case CONNECTING:
                case CHALLENGING:
                case CONNECTED:
                    // connecting clients will only show the connection dialog
                    // refresh to update the time
                    Ref.ui.DrawConnectScreen(false);
                    break;
                case LOADING:
                case PRIMED:
                    // draw the game information screen and loading progress
                    Ref.cgame.DrawActiveFrame(Ref.client.cl.serverTime, Ref.client.demo.isPlaying());

                    // also draw the connection information, so it doesn't
                    // flash away too briefly on local or lan games
                    // refresh to update the time
                    Ref.ui.DrawConnectScreen(true);
                    break;
                case ACTIVE:
                    Ref.cgame.DrawActiveFrame(Ref.client.cl.serverTime, Ref.client.demo.isPlaying());
                    
                    break;
            }
        }

        GLRef.checkError();

//        Ref.StateMan.RunFrame((int)frametime);

        if((Ref.Input.GetKeyCatcher() & Input.KEYCATCH_UI) > 0)
            Ref.ui.Update(Ref.client.realtime);

        
        EndFrame();
    }
}
