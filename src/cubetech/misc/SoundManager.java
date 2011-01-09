package cubetech.misc;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.SoundStore;

public class SoundManager {
  private int bufferIndex;
  SoundStore store = null;
  private HashMap<String, Integer> Knownsounds = new HashMap<String, Integer>();
  private HashMap<Integer, Audio> SoundMap = new HashMap<Integer, Audio>();
  private ArrayList<String> playlist = new ArrayList<String>();
  private Audio currentSong = null;
  private float currentSongPosition = 0;
  private int currentSongIndex = -1;
  private int silentTime = 0;
  boolean bgmusicon = false;

  private float EffectVolume = 0.2f;
  private float MusicVolume = 0.2f;

  public void Update(int msec) {
      store.poll(msec);
    if(silentTime > 0) {
        silentTime -= msec;
        if(silentTime <= 0) {
            PlayNextMusic();
        }
    } else if(currentSong != null && (!currentSong.isPlaying() || !store.isMusicPlaying())) {
        SetNextSong();
    }
  }

  void PlayNextMusic() {
      currentSong.playAsMusic(1.0f, 1.0f, false);
  }

  void SetNextSong() {
      currentSongIndex++;
      if(currentSongIndex >= playlist.size()) {
          currentSongIndex = 0;
          ShuffleSongs();
      }
      currentSong = PlayOGG(playlist.get(currentSongIndex), 0f, false, false);
      
      silentTime = 1000;
  }

  void ShuffleSongs() {
      String[] songlist = new String[] {"data/intro.ogg","data/spacey.ogg",
      "data/coolsong.ogg", "data/coolsong.ogg","data/coolsong_2.ogg",
      "data/drumnbass.ogg", "data/funkypatrol.ogg", "data/goa.ogg",
      "data/noobstef.ogg","data/Beat.ogg","data/FixedHate.ogg",
      "data/FreakyNation.ogg","data/HarmonicMan.ogg","data/Hellwiggah.ogg",
      "data/Nicelyd.ogg","data/Niceness.ogg","data/Nyeste.ogg",
      "data/TheDualist.ogg","data/Youneverknow.ogg"};

//      for (int i= 0; i < songlist.length; i++) {
//            try {
//                store.getOgg("cubetech/"+songlist[i]);
//            } catch (IOException ex) {
//                Logger.getLogger(SoundManager.class.getName()).log(Level.SEVERE, null, ex);
//            }
//      }

      // Shuffle 100 times
      for (int i= 0; i < 100; i++) {
          int from = Ref.rnd.nextInt(songlist.length-1);
          int to = Ref.rnd.nextInt(songlist.length-1);

          String temp = songlist[from];
          songlist[from] = songlist[to];
          songlist[to] = temp;
      }

      // Add to playlist
      playlist.clear();
      playlist.addAll(Arrays.asList(songlist));
      
  }

  public void PlayBackgroundMusic(boolean value) {
      if(bgmusicon == value)
          return;

      if(playlist.isEmpty()) {
          ShuffleSongs();
      }

      if(!value) {
          if(currentSong != null && currentSong.isPlaying()) {
              currentSongPosition = currentSong.getPosition();
              currentSong.stop();
          }
      } else {
          // Set next song
          if(currentSongPosition == 0f || currentSong == null)
            SetNextSong();
          //else {
         //     currentSong.playAsMusic(1.0f, 1.0f, false);
              //currentSong.setPosition(currentSongPosition);
              //currentSong = PlayOGG(playlist.get(currentSongIndex), currentSongPosition, false);
         // }
      }
      bgmusicon = value;

//        if(store.isMusicPlaying()) {
//          Audio song =  playlist.get(currentSong);
//          if(!song.isPlaying())
//              return;
//          song.stop();
//        }
//      } else {
//         if(!store.isMusicPlaying()) {
//          Audio song =  playlist.get(currentSong);
//          if(!song.isPlaying())
//              song.playAsMusic(1.0f, 1.0f, false);
//        }
//      }
  }

  /**
   * Plays a sound effect
   * @param buffer Buffer index to play gotten from addSound
   */
  public void playEffect(Integer buffer, float volume) {
      SoundMap.get(buffer).playAsSoundEffect(1.0f, volume, false);
  }


  /**
   * Initializes the SoundManager
   *
   * @param channels Number of channels to create
   */
    public void initialize(int channels) {
//        try {
            store = SoundStore.get();
            store.init();
            //store.setDeferredLoading(true);
            
            store.setSoundsOn(true);
            store.setSoundVolume(EffectVolume);
            store.setMusicVolume(MusicVolume);

            store.setMaxSources(channels);


            
//            URL url = SoundManager.class.getClassLoader().getResource("cubetech/" + "data/coolsong.ogg");
//            Audio newfile = store.getOgg(url.openStream());
//            playlist.add(newfile);
//
//            url = SoundManager.class.getClassLoader().getResource("cubetech/" + "data/coolsong_2.ogg");
//            newfile = store.getOgg(url.openStream());
//            playlist.add(newfile);
            
//        } catch (IOException ex) {
//            Logger.getLogger(SoundManager.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

  /**
   * Adds a sound to the Sound Managers pool
   *
   * @param path Path to file to load
   * @return index into SoundManagers buffer list
   */
    public int addSound(String path) {
        // Check cache
        
        Integer handle = Knownsounds.get(path);
        if(handle != null)
          return (int)handle;
      
//        // load wave data from buffer
        URL url = SoundManager.class.getClassLoader().getResource("cubetech/"+path);
        Audio newfile = null;
        try {
            
            newfile = store.getWAV(url.openStream());
            //newfile = AudioLoader.getAudio("WAV", url.openStream());
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

    public Audio PlayOGG(String name, float startTime, boolean loop, boolean startPlaying) {
        if(!store.soundWorks())
            return null;
        Audio newsound = null;
        Integer handle = Knownsounds.get(name);
        
        if(handle != null)
            newsound = SoundMap.get(handle);

        if(newsound == null) {
             URL url = SoundManager.class.getClassLoader().getResource("cubetech/"+name);
             if(url == null) {
                 System.out.println("PlayOGG: Could not find file: " + name);
                 return null;
             }

            try {


                newsound = store.getOggStream(url);
                //newsound = store.getOgg(new BufferedInputStream(url.openStream()));
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
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(SoundManager.class.getName()).log(Level.SEVERE, null, ex);
//        }
        
        //currentMusic = newsound;
        if(startPlaying)
            newsound.playAsMusic(1.0f, 1.0f, loop);
        //if(startTime != 0)
        //newsound.setPosition(startTime);
        return newsound;
    }

    public float getEffectVolume() {
        return EffectVolume;
    }

    public void setEffectVolume(float EffectVolume) {
        this.EffectVolume = EffectVolume;
        store.setSoundVolume(EffectVolume);
    }

    public float getMusicVolume() {
        return MusicVolume;
    }

    public void setMusicVolume(float MusicVolume) {
        this.MusicVolume = MusicVolume;
        store.setMusicVolume(MusicVolume);
    }
}
