package cubetech.misc;

import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.gfx.ResourceManager;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.SoundStore;

public class SoundManager {
  private int bufferIndex;
  private SoundStore store = null;
  private HashMap<String, Integer> Knownsounds = new HashMap<String, Integer>();
  private HashMap<Integer, Audio> SoundMap = new HashMap<Integer, Audio>();
  private ArrayList<String> playlist = new ArrayList<String>();
  private Audio currentSong = null;
  private float currentSongPosition = 0; //
  private int currentSongIndex = -1; // current song index in playlist
  private int silentTime = 0; // counts down before starting a new track
  private boolean isMusicOn = false;

  private float EffectVolume = 0.2f;
  private float MusicVolume = 0.5f;

  public boolean playmusic = false;

  private HashMap<Integer, Vector4f> entityPositions = new HashMap<Integer, Vector4f>();
  Vector2f lastOrigin = new Vector2f();
  
  CVar volume = Ref.cvars.Get("volume", "0.5", EnumSet.of(CVarFlags.TEMP));

  // 3d distance model
  CVar s_minDistance = Ref.cvars.Get("s_minDistance", "30", EnumSet.of(CVarFlags.CHEAT));
  CVar s_falloff = Ref.cvars.Get("s_falloff", "0.4", EnumSet.of(CVarFlags.CHEAT));
  // doppler settings
  CVar s_dopplerfactor = Ref.cvars.Get("s_dopplerfactor", "1.0", EnumSet.of(CVarFlags.CHEAT));
  CVar s_dopplerspeed = Ref.cvars.Get("s_dopplerspeed", "500", EnumSet.of(CVarFlags.CHEAT)); // speed of sound

  // Handles delay between songs and pumps the audiosystem,
  // necessary when using streamed reading
  public void Update(int msec) {
      if(volume.modified) {
         volume.modified = false;
         setEffectVolume(volume.fValue);
      }

      if(s_minDistance.modified) {
          store.setMinDistance(s_minDistance.fValue);
          s_minDistance.modified = false;
      }

      if(s_falloff.modified) {
          store.setRollOff(s_falloff.fValue);
          s_falloff.modified = false;
      }

      if(s_dopplerfactor.modified) {
          s_dopplerfactor.modified = false;
          store.setDopplerFactor(s_dopplerfactor.fValue);
      }

      if(s_dopplerspeed.modified) {
          s_dopplerspeed.modified = false;
          store.setDopplerSpeed(s_dopplerspeed.fValue);
      }
      
      store.poll(msec);
    if(silentTime > 0) {
        silentTime -= msec;
        if(silentTime <= 0) {
            StartQueuedMusic();
        }
    } else if(playmusic && currentSong != null && (!currentSong.isPlaying() || !store.isMusicPlaying())) {
        PlayNextMusic();
    }
  }

  public void SetEntityPosition(int entityNum, Vector2f position, Vector2f velocity) {
      Vector4f pos = entityPositions.get(entityNum);
      if(pos != null)
          pos.set(position.x, position.y, velocity.x, velocity.y);
      else {
          entityPositions.put(entityNum, new Vector4f(position.x, position.y, velocity.x, velocity.y));
      }
  }

  
  public void Respatialize(Vector2f origin, Vector2f velocity) {
      if(Helper.Equals(origin, lastOrigin))
          return;
      lastOrigin.set(origin);
      store.UpdateListener(origin.x, origin.y, velocity.x, velocity.y);
  }

  // Setup the next song and set a delay before starting it
  public void PlayNextMusic() {
      // Increment playlist index
      currentSongIndex++;
      if(currentSongIndex >= playlist.size()) {
          currentSongIndex = 0;
          ShuffleMusicList();
      }
      if(currentSong != null && (currentSong.isPlaying() || store.isMusicPlaying())) {
          // Something is already playing, stop it.
          currentSong.stop();
      }
      // Load the track
      currentSong = PlayOGGMusic(playlist.get(currentSongIndex), 0f, false, false);

      // Wait one second before playing it
      silentTime = 1000;
  }

  

  

  // Shuffle the songlist
  public void ShuffleMusicList() {
      String[] songlist = new String[] {"data/HarmonicMan.ogg"};

//      ,"data/spacey.ogg",
//      "data/coolsong.ogg", "data/coolsong.ogg","data/coolsong_2.ogg",
//      "data/drumnbass.ogg", "data/funkypatrol.ogg", "data/goa.ogg",
//      "data/noobstef.ogg","data/Beat.ogg","data/FixedHate.ogg",
//      "data/FreakyNation.ogg","data/HarmonicMan.ogg","data/Hellwiggah.ogg",
//      "data/Nicelyd.ogg","data/Niceness.ogg","data/Nyeste.ogg",
//      "data/TheDualist.ogg","data/Youneverknow.ogg"

      // Shuffle 100 times
      for (int i= 0; i < 5; i++) {
          int from = Ref.rnd.nextInt(songlist.length);
          int to = Ref.rnd.nextInt(songlist.length);

          String temp = songlist[from];
          songlist[from] = songlist[to];
          songlist[to] = temp;
      }

      // Add to playlist
      playlist.clear();
      playlist.addAll(Arrays.asList(songlist));
      
  }

  // Start or stop background music
  public void PlayBackgroundMusic(boolean enable) {
      if(isMusicOn == enable)
          return;

      if(playlist.isEmpty()) {
          ShuffleMusicList();
      }

      if(!enable) {
          if(currentSong != null && currentSong.isPlaying())
              currentSong.stop();
      } else {
          // Set next song
//          if(currentSong == null || !currentSong.isPlaying() || !store.isMusicPlaying())
            PlayNextMusic();
      }
      isMusicOn = enable;
      playmusic = enable;
  }

  // Start the song that is waiting to be played
  private void StartQueuedMusic() {
      if(currentSong == null) {
          isMusicOn = true;
          PlayNextMusic();
          return;
      }
      
      currentSong.playAsMusic(1.0f, 1.0f, false);
  }

  /**
   * Plays a sound effect
   * @param buffer Buffer index to play gotten from addSound
   */
  public void playEffect(Integer buffer, float volume) {
      SoundMap.get(buffer).playAsSoundEffect(1.0f, volume, false);
  }

   public void playEntityEffect(int entityNum, int buffer, float volume) {
       // If sound is comming from the current player, just play it as a local sound
       if(entityNum == Ref.cgame.GetCurrentPlayerEntityNum()) {
           playEffect(buffer, volume);
           return;
       }
       
       Vector4f position = entityPositions.get(entityNum);

       // No position, play at center
       if(position == null) {
           playEffect(buffer, volume);
           return;
       }

       // Play at relative position
       SoundMap.get(buffer).playAsSoundEffect(1.0f, volume, false, position.x, position.y, position.z, position.w);
    }

  /**
   * Initializes the SoundManager
   *
   * @param channels Number of channels to create
   */
    public void initialize(int channels) {
        store = SoundStore.get();
        store.init();
        store.setSoundsOn(true);
        store.setSoundVolume(EffectVolume);
        store.setMusicVolume(MusicVolume);

        store.setMaxSources(channels);
        
    }

  /**
   * Adds a sound to the Sound Managers pool
   *
   * @param path Path to file to load
   * @return index into SoundManagers buffer list
   */
    public int AddWavSound(String path) {
        // Check cache
        
        Integer handle = Knownsounds.get(path);
        if(handle != null)
          return (int)handle;
      
        URL url = ResourceManager.getClassLoader().getResource("cubetech/"+path);
        Audio newfile = null;
        try {
            
            newfile = store.getWAV(url.openStream());
        } catch (IOException ex) {
            Logger.getLogger(SoundManager.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
        // Cache
        handle = bufferIndex;
        Knownsounds.put(path, handle);
        SoundMap.put(handle, newfile);

        // return index for this sound
        return bufferIndex++;
    }

    public Audio PlayOGGMusic(String name, float startTime, boolean loop, boolean startPlaying) {
        if(!store.soundWorks())
            return null;
        Audio newsound = null;
        Integer handle = Knownsounds.get(name);
        
        if(handle != null)
            newsound = SoundMap.get(handle);

        if(newsound == null) {
             URL url = ResourceManager.getClassLoader().getResource("cubetech/"+name);
             if(url == null) {
                 Common.Log("PlayOGG: Could not find file: " + name);
                 return null;
             }

            try {
                newsound = store.getOggStream(url);
                if(newsound == null)
                    return null;
                handle = bufferIndex;
                Knownsounds.put(name, handle);
                SoundMap.put(handle, newsound);

                // return index for this sound
                bufferIndex++;
                
            } catch (IOException ex) {
                Logger.getLogger(SoundManager.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

        if(startPlaying)
            newsound.playAsMusic(1.0f, 1.0f, loop);

        return newsound;
    }

    public float getEffectVolume() {
        return EffectVolume;
    }

    public void setEffectVolume(float EffectVolume) {
        this.EffectVolume = EffectVolume;
        store.setSoundVolume(EffectVolume);
        store.setMusicVolume(EffectVolume * 0.5f);
    }

    public float getMusicVolume() {
        return MusicVolume;
    }

    public void setMusicVolume(float MusicVolume) {
        this.MusicVolume = MusicVolume;
        store.setMusicVolume(MusicVolume);
    }


}
