package cubetech.gfx;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import cubetech.common.Commands;
import cubetech.common.Common;
import cubetech.common.ICommand;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Profiler.SecTag;
import cubetech.misc.Ref;
import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import static org.lwjgl.opengl.ARBPixelBufferObject.*;
import org.lwjgl.opengl.EXTBgra;
import org.lwjgl.opengl.EXTTextureSRGB;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.vector.Vector2f;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallback;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

/**
 *
 * @author Mads
 */
public final class VideoManager implements ICommand {
    // The thread accesses these
    private final static Object lock = new Object();
    private static byte[] imageBuffer = null; // Data from vlc goes in here
    private static boolean dirty = true; // imagebuffer contains fresh data
    private static final BlockingQueue<Command> commandQueue = new ArrayBlockingQueue<Command>(20);
    private static final VideoThread videoThread = new VideoThread();
    private static Dimension dim = null;
    private static boolean isPlaying = false;
    
    private int width, height;
    private CubeTexture texture; // the texture is updated using the pbo
    private int pboid; // imagebuffer is copied to this pbo
    
    
    public enum Type {
        CREATE_PLAYER,PLAY_VIDEO, STOP_VIDEO,DESTROY_PLAYER
    }
    
    public class Command {
        private Type type;
        // potential parameters
        private int width, height;
        private String filename;
    }
    
    public VideoManager(int width, int height) {
        this.width = width;
        this.height = height;
        imageBuffer = new byte[width*height*4];
        
        // Generate texture
        int texid = Ref.ResMan.getTextureLoader().CreateEmptyTexture(width, height, GL_TEXTURE_2D, EXTTextureSRGB.GL_SRGB_ALPHA_EXT, false, null);
        texture = new CubeTexture(GL_TEXTURE_2D, texid, "Video Streaming Texture");
        
        texture.loaded = true;
        
        // Generate PBO for fast texture streaming
        pboid = glGenBuffersARB();
        
        new Thread(videoThread).start();
        createPlayer(width, height);
        
        Ref.commands.AddCommand("video", (ICommand)this);
    }
    
    public void RunCommand(String[] args) {
        if(args.length < 2) return;
        String cmd = args[1];
        if(cmd.equalsIgnoreCase("play")) {
            playVideo(Commands.ArgsFrom(args, 2));
        } else if(cmd.equalsIgnoreCase("stop")) {
            stopVideo();
        }
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public Vector2f getVideoDimensions() {
        Vector2f v = new Vector2f(512,512);
        if(dim != null) {
            v.x = dim.width;
            v.y = dim.height;
        }
        return v;
    }
    
    public void createPlayer(int width, int height) {
        Command cmd = new Command();
        cmd.type = Type.CREATE_PLAYER;
        cmd.width = width;
        cmd.height = height;
        commandQueue.add(cmd);
    }
    
    public void playVideo(String file) {
        Command cmd = new Command();
        cmd.type = Type.PLAY_VIDEO;
        if(file == null || file.isEmpty()) {
            file = "http://www.youtube.com/watch?v=z4uj5NZS7g8";
            //file = "G:\\downloads\\complete\\f-hangover2\\f-hangover2.720.mkv";
        }
        cmd.filename = file;
        commandQueue.add(cmd);
    }
    
    public void stopVideo() {
        Command cmd = new Command();
        cmd.type = Type.STOP_VIDEO;
        commandQueue.add(cmd);
    }
    
    public void updateTexture() {
        if(!dirty) return; // texture is already up to date
        SecTag main = Profiler.EnterSection(Sec.VideoUpdate);
        glBindBufferARB(GL_PIXEL_UNPACK_BUFFER_ARB, pboid);
        //GLRef.checkError();
        glBufferDataARB(GL_PIXEL_UNPACK_BUFFER_ARB, width*height*4, GL_STREAM_DRAW_ARB);
        //GLRef.checkError();
        ByteBuffer buffer = glMapBufferARB(GL_PIXEL_UNPACK_BUFFER_ARB, GL_WRITE_ONLY_ARB, null);
        //GLRef.checkError();
        readImage(buffer);
        glUnmapBufferARB(GL_PIXEL_UNPACK_BUFFER_ARB);
        
        // copy data from pbo to texture
        texture.Bind();
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, EXTBgra.GL_BGRA_EXT, GL_UNSIGNED_BYTE, 0);
        glBindBufferARB(GL_PIXEL_UNPACK_BUFFER_ARB, 0);
        texture.setFiltering(true, GL_LINEAR);
        texture.setFiltering(false, GL_LINEAR);
        texture.Unbind();
        main.ExitSection();
        dirty = false;
    }
    
    private void readImage(ByteBuffer dst) {
        synchronized(lock) {
            dst.clear();
            // Read in each line, flipping the y axis
            for (int i = height-1; i >= 0; i--) {
                int offset = width * i * 4;
                dst.put(imageBuffer, offset, width*4);
            }
            
            dst.flip();
        }
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }

    public CubeTexture getTexture() {
        if(dirty) updateTexture();
        return texture;
    }
    
    // Threaded video player
    private static class VideoThread extends MediaPlayerEventAdapter implements Runnable {
        // Static stuff for the library
        private LibVlc libvlc;
        private MediaPlayerFactory factory;
        private DirectMediaPlayer player = null;
        private boolean exit = false;
        
        private RenderCallback renderCallback = new RenderCallback() {
            public void display(Memory data) {
                synchronized(lock) {
                    data.read(0, imageBuffer, 0, imageBuffer.length);
                    dirty = true;
                }
            }
        };
        
        @Override
        public void playing(MediaPlayer mediaPlayer) {
            Common.LogDebug("[VID] Playback started.");
            isPlaying = true;
            dim = player.getVideoDimension();
        }
        
        @Override
        public void error(MediaPlayer mediaPlayer) {
            Common.LogDebug("[VID] Playback error.");
        }
        
        @Override
        public void stopped(MediaPlayer mediaPlayer) {
            Common.LogDebug("[VID] Playback stopped.");
            isPlaying = false;
            dim = null;
        }
        
        @Override
        public void finished(MediaPlayer mediaPlayer) {
            Common.LogDebug("[VID] Playback finished.");
            isPlaying = false;
            dim = null;
        }

        public void run() {
            libvlc = (LibVlc)Native.loadLibrary("C:\\Program Files (x86)\\VideoLAN\\VLC\\"+RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
            factory = new MediaPlayerFactory(libvlc);
            
            while(!exit) {
                Command cmd = null;
                while((cmd = commandQueue.poll()) != null) {
                    switch(cmd.type) {
                        case CREATE_PLAYER:
                            if(player == null) {
                                createPlayer(cmd.width, cmd.height);
                            }
                            break;
                        case DESTROY_PLAYER:
                            destroyPlayer();
                            break;
                        case PLAY_VIDEO:
                            play(cmd.filename);
                            break;
                        case STOP_VIDEO:
                            stop();
                            break;
                        default:
                            System.out.println("Unknown video command " + cmd);
                            break;
                    }
                }
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
            }
        }
        
        private void createPlayer(int width, int height) {
            player = factory.newDirectMediaPlayer(width, height, renderCallback);
            player.setPlaySubItems(true);
            player.addMediaPlayerEventListener((MediaPlayerEventListener)this);
        }
        
        private void destroyPlayer() {
            if(player == null) return;
            player.release();
            player = null;
        }
        
        private void play(String path) {
            if(player == null)  
            {
                Common.Log("Can't play media. Player is not created");
                return;
            }
            player.playMedia(path);
        }
        
        private void stop() {
            if(player == null) {
                Common.Log("Can't stop media. Player is not created");
                return;
            }
            player.stop();
        }

        
    }
}
