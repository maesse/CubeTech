package cubetech.state;

import cubetech.CubeText;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.misc.Button;
import cubetech.misc.Ref;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;
import org.newdawn.slick.openal.Audio;

/**
 * Simple menu
 * TODO: Make nice for keyboard users?
 * @author mads
 */
public class MenuState implements IGameState {
    Button newGame;
    Button optionsButton;
    Button scoresButton;
    Button exit;
    Button contGame;
    float soundPosition = 0f;
    Audio bgmusic = null;

    boolean inOptions = false;
    boolean inScoreboard = false;

    // Options menu
    Button optExit;
    CubeTexture volumeBar;
    CubeTexture background;

    String[] scoreNames = new String[10];
    String[] scoreScores = new String[10];
    long lastscoretime = 0;

    CubeText text = new CubeText();

    Vector2f menuStartPos = new Vector2f(0.75f, 0.55f);
    float menuSpacing = 0.08f;
    Vector2f menuSize = new Vector2f(0.2f, 0.07f);

    public void Enter() {

        for (int i = 0; i < 10; i++) {
            scoreNames[i] = "N/A";
            scoreScores[i] = "N/A";
        }

        //Ref.soundMan.PlayBackgroundMusic(false);
        //bgmusic = Ref.soundMan.PlayOGG("data/intro.ogg", soundPosition, true, true);
        if(newGame != null) {
            boolean gameRunning = false;
            if(Ref.world != null) {
                gameRunning = true;
            }

            // If game is running, move menu down a bit and to fit in "continue"
            int index = gameRunning?1:0;
            newGame.Rect.setLocation((int)(menuStartPos.x*1000), (int)((menuStartPos.y-index++*menuSpacing)*1000));
            optionsButton.Rect.setLocation((int)(menuStartPos.x*1000), (int)((menuStartPos.y-index++*menuSpacing)*1000));
            scoresButton.Rect.setLocation((int)(menuStartPos.x*1000), (int)((menuStartPos.y-index++*menuSpacing)*1000));
            exit.Rect.setLocation((int)(menuStartPos.x*1000), (int)((menuStartPos.y-index++*menuSpacing)*1000));
            return;
        }
        CubeTexture buttonBg = (CubeTexture)(Ref.ResMan.LoadResource("data/menubutton.png").Data);
        volumeBar = (CubeTexture)(Ref.ResMan.LoadResource("data/healthbar.png").Data);
        background = (CubeTexture)(Ref.ResMan.LoadResource("data/background.png").Data);
        contGame = new Button("Continue", new Vector2f(menuStartPos.x, menuStartPos.y), menuSize, buttonBg);
        newGame = new Button("New Game", new Vector2f(menuStartPos.x, menuStartPos.y), menuSize, buttonBg);
        optionsButton = new Button("Options", new Vector2f(menuStartPos.x, menuStartPos.y-menuSpacing), menuSize, buttonBg);
        scoresButton = new Button("Scoreboard", new Vector2f(menuStartPos.x, menuStartPos.y-menuSpacing*2f), menuSize, buttonBg);
        exit = new Button("Exit", new Vector2f(menuStartPos.x, menuStartPos.y-menuSpacing*3f), menuSize, buttonBg);

        optExit = new Button("Back", new Vector2f(0.4f, 0.15f), menuSize, buttonBg);

        inOptions = false;
    }

    public void ShowScoeboard() {
        inScoreboard = true;
        ReadScores();
    }

    void RenderBlackBackground() {
        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(0.5f, 0.5f), 0.37f);
        spr.SetColor(new Vector4f(0, 0, 0, 0.3f));
    }

    public void Exit() {
        if(bgmusic != null) {
            soundPosition = bgmusic.getPosition();
            bgmusic.stop();
        }
//        newGame = null;
//        exit = null;
    }

    void ReadScores() {
        // Wait 10 secs between refreshes
//        if(lastscoretime + 10000 < Ref.loop.time)
//            return;
        lastscoretime = Ref.loop.time;
        //{
            
        BufferedReader in = null;
        try {
            URL scoreUrl = new URL("http://pd-eastcoast.com/rgj5scores.php");

            InputStream is = scoreUrl.openStream();
            if(is == null) {
                System.err.println("Could not open URL");
            }
            in = new BufferedReader(new InputStreamReader(is));

//            while(!in.ready()) {
//
//            }
            String[] temp = new String[30];
            int index = 0;
            String str = null;
            while((str = in.readLine()) != null) {
                temp[index++] = str;

//                    if(index == 30)
//                        break;
            }

            System.err.println("Got scores");
            if(index == 30) {
                for (int i= 0; i < 10; i++) {
                    scoreNames[i] = temp[i*3];
                    scoreScores[i] = temp[i*3 + 1];
                }
            }


        } catch (Exception ex) {
            Logger.getLogger(MenuState.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
                Logger.getLogger(MenuState.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //}
        
    }

    void HandleScoreboard() {
        Vector2f mousePos = Ref.Input.playerInput.MousePos;
        RenderBlackBackground();
        Ref.textMan.AddText(new Vector2f(0.5f,0.8f), "Scoreboard", Align.CENTER);

        for (int i = 0; i < 10; i++) {
            Ref.textMan.AddText(new Vector2f(0.2f, 0.7f - 0.05f*i), ""+(i+1), Align.CENTER);
            Ref.textMan.AddText(new Vector2f(0.3f, 0.7f - 0.05f*i), scoreNames[i], Align.LEFT);
            Ref.textMan.AddText(new Vector2f(0.8f, 0.7f - 0.05f*i), scoreScores[i], Align.RIGHT);
        }

        // Exit button
        if(optExit.Intersects(mousePos) && Ref.Input.playerInput.Mouse1) {
            // Exit Cubetech
            inScoreboard = false;
        }

        optExit.Render();
    }

    void HandleOptionsMenu() {
        RenderBlackBackground();
        Vector2f mousePos = Ref.Input.playerInput.MousePos;
        
        Ref.textMan.AddText(new Vector2f(0.5f,0.8f), "Options", Align.CENTER);

        // Music volume
        Ref.textMan.AddText(new Vector2f(0.5f,0.72f), "Music Volume", Align.RIGHT);
        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        Vector2f musvolPos = new Vector2f(0.53f, 0.72f);
        Vector2f musvolSiz = new Vector2f(0.2f, 0.05f);
        spr.Set(musvolPos, musvolSiz, null, new Vector2f(0,0), new Vector2f(1,1));
        float alpha = 0.1f;
        if(mousePos.x >= musvolPos.x && mousePos.x <= musvolPos.x + musvolSiz.x) // inside x coords
            if(mousePos.y >= musvolPos.y && mousePos.y <= musvolPos.y + musvolSiz.y) // inside y coords {
            {
                alpha = 0.2f;
                if(Ref.Input.playerInput.Mouse1) {
                    float frac = mousePos.x - musvolPos.x;
                    frac /= musvolSiz.x;

                    if(frac > 1f)
                        frac = 1f;
                    if(frac < 0f)
                        frac = 0f;

                    Ref.soundMan.setMusicVolume(frac);
                }
                
            }
        spr.SetColor(new Vector4f(1, 1, 1, alpha));
        float frac = Ref.soundMan.getMusicVolume();
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(musvolPos, new Vector2f(frac*musvolSiz.x, musvolSiz.y), volumeBar, new Vector2f(1,0), new Vector2f(-1*frac,1));
        spr.SetColor(new Vector4f(1, 1, 1, 0.5f));

        // Effect volume
        Ref.textMan.AddText(new Vector2f(0.5f,0.66f), "Effect Volume", Align.RIGHT);
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        musvolPos = new Vector2f(0.53f, 0.66f);
        musvolSiz = new Vector2f(0.2f, 0.05f);
        spr.Set(musvolPos, musvolSiz, null, new Vector2f(), new Vector2f());
        alpha = 0.1f;
        if(mousePos.x >= musvolPos.x && mousePos.x <= musvolPos.x + musvolSiz.x) // inside x coords
            if(mousePos.y >= musvolPos.y && mousePos.y <= musvolPos.y + musvolSiz.y) // inside y coords {
            {
                alpha = 0.2f;
                alpha = 0.2f;
                if(Ref.Input.playerInput.Mouse1) {
                    frac = mousePos.x - musvolPos.x;
                    frac /= musvolSiz.x;

                    if(frac > 1f)
                        frac = 1f;
                    if(frac < 0f)
                        frac = 0f;

                    Ref.soundMan.setEffectVolume(frac);
                }
            }
        spr.SetColor(new Vector4f(1, 1, 1, alpha));
        frac = Ref.soundMan.getEffectVolume();
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(musvolPos, new Vector2f(frac*musvolSiz.x, musvolSiz.y), volumeBar, new Vector2f(1,0), new Vector2f(-1*frac,1));
        spr.SetColor(new Vector4f(1, 1, 1, 0.5f));

        // Exit button
        if(optExit.Intersects(mousePos) && Ref.Input.playerInput.Mouse1) {
            // Exit Cubetech
            inOptions = false;
        }

        optExit.Render();
    }

    void HandleMainMenu() {
        boolean gameRunning = false;
        if(Ref.world != null) {
            gameRunning = true;
        }
        Vector2f mousePos = Ref.Input.playerInput.MousePos;
        if(gameRunning) {
            if((contGame.Intersects(mousePos) && Ref.Input.playerInput.Mouse1) || (Ref.Input.GetKey(Keyboard.KEY_ESCAPE).Pressed && Ref.Input.GetKey(Keyboard.KEY_ESCAPE).Changed)) {
            try {
                // Start new game
                Ref.StateMan.SetState("hagser");
                return;
            } catch (Exception ex) {
                Logger.getLogger(MenuState.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(-1);
            }
        }
        }
        if(newGame.Intersects(mousePos) && Ref.Input.playerInput.Mouse1) {
            try {
                // Start new game
                if(Ref.world != null) {
                    Ref.world.StartNewEmptyGame();
//                    Ref.world.LoadWorld("map0.map");
                    }
                Ref.StateMan.SetState("hagser");
                return;
            } catch (Exception ex) {
                Logger.getLogger(MenuState.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(-1);
            }
        }
        else if(optionsButton.Intersects(mousePos) && Ref.Input.playerInput.Mouse1) {
            // Exit Cubetech
            inOptions = true;
        }
        else if(scoresButton.Intersects(mousePos) && Ref.Input.playerInput.Mouse1) {
            // Exit Cubetech
            
            inScoreboard = true;
            ReadScores();
        }
        else if(exit.Intersects(mousePos) && Ref.Input.playerInput.Mouse1) {
            // Exit Cubetech
            System.exit(0);
        }
        if(gameRunning)
            contGame.Render();
        newGame.Render();
        optionsButton.Render();
        scoresButton.Render();
        exit.Render();
    }

    public void RunFrame(int msec) {
        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(0.5f, 0.5f), 0.5f, background);
        //Ref.textMan.AddText(new Vector2f(0.5f,0.85f), "Cubetronic", Align.CENTER);
        text.Render(msec);
        if(inOptions)
            HandleOptionsMenu();
        else if(inScoreboard)
            HandleScoreboard();
        else
            HandleMainMenu();

        
    }

    public String GetName() {
        return "menu";
    }

}
