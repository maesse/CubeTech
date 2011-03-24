package cubetech.ui;

import cubetech.common.CS;
import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Info;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.input.Input;
import cubetech.input.KeyEvent;
import cubetech.input.KeyEventListener;
import cubetech.input.MouseEvent;
import cubetech.input.MouseEventListener;
import cubetech.misc.Button;
import cubetech.misc.Ref;
import cubetech.net.ConnectState;
import java.util.EnumMap;
import java.util.EnumSet;
import org.lwjgl.util.vector.Vector2f;

/**
 * Handles UI focus.
 * @author mads
 */
public class UI implements KeyEventListener, MouseEventListener {
    private IMenu activeMenu = null; // Menu that is currently showing
    private EnumMap<MENU, IMenu> menuLookup = new EnumMap<MENU, IMenu>(MENU.class); // menu cache
    private int lasttime; // used for calculating delta times
    CVar ui_fullscreen = Ref.cvars.Get("ui_fullscreen", "1", EnumSet.of(CVarFlags.TEMP));

    Vector2f popupMessageSize = null;
    Vector2f popupSize = null;


    private boolean popup;
    private String popupMessage = "";
    private CButton popupButton;

    // Possible menus
    public enum MENU {
        MAINMENU,
        OPTIONS,
        SERVERS
    }

    public UI() {
        Ref.Input.AddKeyEventListener(this, Input.KEYCATCH_UI);
        Ref.Input.AddMouseEventListener(this, Input.KEYCATCH_UI);
    }

    private void DisplayDownloadInfo() {
        if(Ref.client.clc.downloadName == null)
            return;
        
        String dlText = "Downloading:";
        String etaText = "Estimated time left:";
        String xferText = "Transfer rate:";

        Vector2f res = Ref.glRef.GetResolution();
        int lineCount = 0;
        float charHeight = Ref.textMan.GetCharHeight();

        Ref.textMan.AddText(new Vector2f(0.5f * res.x, charHeight * 6), dlText, Align.RIGHT, Type.HUD);
        Ref.textMan.AddText(new Vector2f(0.5f * res.x,charHeight * 7), etaText, Align.RIGHT, Type.HUD);
        Ref.textMan.AddText(new Vector2f(0.5f * res.x,charHeight * 9), xferText, Align.RIGHT, Type.HUD);

        String s = String.format("%s (%d%%)", Ref.client.clc.downloadName, (int)((float)Ref.client.clc.downloadCount * 100f / Ref.client.clc.downloadSize));
        Ref.textMan.AddText(new Vector2f(0.5f * res.x,charHeight * 6), s, Align.LEFT, Type.HUD);

        String dlTotal = GetSize(Ref.client.clc.downloadSize);
        String dlSize = GetSize(Ref.client.clc.downloadCount);
        if(Ref.client.clc.downloadCount < 4096 || Ref.client.clc.downloadTime == 0) {
            Ref.textMan.AddText(new Vector2f(0.5f * res.x,charHeight * 7), "estimating", Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(0.5f * res.x,charHeight * 8), String.format("(%s of %s copied)", dlSize, dlTotal), Align.LEFT, Type.HUD);
        } else {
            int xferRate;
            if((Ref.client.realtime - Ref.client.clc.downloadTime) / 1000 != 0) {
                xferRate = Ref.client.clc.downloadCount / ((Ref.client.realtime - Ref.client.clc.downloadTime) / 1000);
            }else
                xferRate = 0;
            
            // Extrapolate estimated completion time
            if(Ref.client.clc.downloadSize != 0 && xferRate != 0) {
                int n = Ref.client.clc.downloadSize / xferRate; // estimated time for entire d/l in secs
                
                // We do it in K (/1024) because we'd overflow around 4MB
                n = (n - (((Ref.client.clc.downloadCount/1024)*n) / (Ref.client.clc.downloadSize/1024)))*1000;
                String time = GetTime(n);

                Ref.textMan.AddText(new Vector2f(0.5f * res.x,charHeight * 7), time, Align.LEFT, Type.HUD);
                Ref.textMan.AddText(new Vector2f(0.5f * res.x,charHeight * 8), String.format("(%s of %s copied)", dlSize, dlTotal), Align.LEFT, Type.HUD);
            } else {
                Ref.textMan.AddText(new Vector2f(0.5f * res.x,charHeight * 7), "estimating", Align.LEFT, Type.HUD);
                if(Ref.client.clc.downloadSize != 0)
                    Ref.textMan.AddText(new Vector2f(0.5f * res.x,charHeight * 8), String.format("(%s of %s copied)", dlSize, dlTotal), Align.LEFT, Type.HUD);
                else
                    Ref.textMan.AddText(new Vector2f(0.5f * res.x,charHeight * 8), String.format("(%s copied)", dlSize), Align.LEFT, Type.HUD);
            }

            if(xferRate != 0) {
                Ref.textMan.AddText(new Vector2f(0.5f * res.x,charHeight * 9), String.format("%s/Sec", GetSize(xferRate)), Align.LEFT, Type.HUD);
            }

        }


    }

    private String GetTime(int time) {
        time /= 1000; // ms -> sec

        if(time > 3600)
            return String.format("%d hr %d min", time / 3600, (time % 3600)/60);
        else if(time > 60)
            return String.format("%d min %d sec", time / 60, time%60);
        else
            return String.format("%d sec", time);
    }

    private String GetSize(int bytes) {
        if(bytes > 1024*1024*1024) {
            return String.format("%f.3f GB", bytes / (1024*1024));
        } else if(bytes > 1024*1024) {
            return String.format("%.2f MB", bytes / (1024*1024));
        } else if(bytes > 1024) {
            return String.format("%d KB", bytes / 1024);
        } else
            return String.format("%d bytes", bytes);
    }

    

    

    // Handle key input
    public void KeyPressed(KeyEvent evt) {

    }

    public void GotMouseEvent(MouseEvent evt) {
        if(popup) {
            Vector2f pos = new Vector2f(evt.Position);
            pos.x *= Ref.glRef.GetResolution().x;
            pos.y = Ref.glRef.GetResolution().y * (1f-pos.y);

            boolean hit = popupButton.containsPoint(pos);
            if(hit && !popupButton.isMouseEnter())
                popupButton.MouseEnter();
            else if(!hit && popupButton.isMouseEnter())
                popupButton.MouseExit();


            Vector2f buttonPosition = popupButton.getInternalPosition();
            pos.x -= buttonPosition.x;
            pos.y -= buttonPosition.y;
            Vector2f oldpos = evt.Position;
            evt.Position = pos;

            popupButton.MouseEvent(evt);
            evt.Position = oldpos;
            return;
        }
        activeMenu.GotMouseEvent(evt);
    }

    // Set the menu to display on screen
    public void SetActiveMenu(MENU ui) {
        IMenu menu = GetMenu(ui);
        activeMenu = menu;
        Ref.Input.SetKeyCatcher((Ref.Input.GetKeyCatcher() & (Input.KEYCATCH_CONSOLE | Input.KEYCATCH_CGAME)) | Input.KEYCATCH_UI);
        
        activeMenu.Show();
    }

    // Updates/Renders the active menu
    public void Update(int realtime) {
        // Calculate delta time
        int msec = realtime - lasttime;
        lasttime = realtime;
        if(msec > 200)
            msec = 200;

        if(activeMenu != null)
            activeMenu.Update(msec);
        
        if(popup) {
            RenderPopup();
            popupButton.Render(new Vector2f());
        }

        // Check if an error message should be displayed
        if(Ref.common.errorMessage.modified) {
            Ref.common.errorMessage.modified = false;
            SetPopup(Ref.common.errorMessage.sValue);
        }
    }

    public void SetPopup(String text) {
        if(popupButton == null)
            popupButton = new CButton("Close", null, Align.CENTER, 1.0f, new ButtonEvent() {
                public void buttonPressed(CComponent button, MouseEvent evt) {
                    popup = false;
                }
            });
        popup = true;
        popupMessage = text;
        popupMessageSize = null; // recalculate size on next frame
        Ref.Console.Close();
    }

    
    private void RenderPopup() {
        Vector2f res = Ref.glRef.GetResolution();
//        if(popupSize == null) {
            popupSize = new Vector2f(res);
            popupSize.scale(0.5f);
            popupButton.setPosition(new Vector2f(0.5f * res.x - popupButton.getSize().x /2f, (0.5f*res.y + popupSize.y*0.5f - popupButton.getLayoutSize().y) ));
            //popupButton.SetPosition();
//        }

        if(popupMessageSize == null)
            popupMessageSize = Ref.textMan.GetStringSize(popupMessage, popupSize, null,1, Type.HUD);

        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(0.5f * res.x - popupSize.x/2f, 0.5f * res.y - popupSize.y/2f), popupSize, null, null, null);
        //spr.Set(0.5f, 0.5f, 0.3f);
        spr.SetColor(30,30,30,200);
        
        Ref.textMan.AddText(new Vector2f(0.5f*res.x, 0.5f*res.y +(((popupMessageSize.y-50))/2f)), popupMessage, Align.CENTER, null, popupSize, Type.HUD, 1);
        
    }

    // Custom call for drawing the connect screen
    public void DrawConnectScreen(boolean overlay) {
        Vector2f res = Ref.glRef.GetResolution();

        if(!overlay) {
            // Draw background
            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.Set(new Vector2f(), res, null,null,null);
            spr.SetColor(0, 0, 0, 255);
        }

        

        String info = Ref.client.cl.GameState.get(CS.CS_SERVERINFO);
        if(info != null && info.length() > 0)
            Ref.textMan.AddText(new Vector2f(0.5f * res.x, 30), String.format("Loading %s", Info.ValueForKey(info, "mapname")), Align.CENTER, Type.HUD);

        Ref.textMan.AddText(new Vector2f(0.5f * res.x, 10), String.format("Connecting to %s", Ref.client.servername), Align.CENTER, Type.HUD);

        if(Ref.client.state < ConnectState.CONNECTED && Ref.client.clc.servermessage != null)
            Ref.textMan.AddText(new Vector2f(0.5f * res.x, res.y - res.y *0.2f), String.format("%s", Ref.client.clc.servermessage), Align.CENTER, Type.HUD);

        String s = "";
        switch(Ref.client.state) {
            case ConnectState.CONNECTING:
                s = String.format("Awaiting challenge...%d", Ref.client.clc.ConnectPacketCount);
                break;
            case ConnectState.CHALLENGING:
                s = String.format("Awaiting connection...%d", Ref.client.clc.ConnectPacketCount);
                break;
            case ConnectState.CONNECTED:
                DisplayDownloadInfo();
                s = String.format("Awaiting gamestate...");
                break;
        }
        Ref.textMan.AddText(new Vector2f(0.5f * res.x, res.y - res.y *0.3f), s, Align.CENTER, Type.HUD);
    }

    // Returns true if the active menu requires it
    public boolean IsFullscreen() {
        if(activeMenu != null && (Ref.Input.GetKeyCatcher() & Input.KEYCATCH_UI) == Input.KEYCATCH_UI)
            return activeMenu.IsFullscreen();

        return false;
    }

    // Look up the menu from an enum
    private IMenu GetMenu(MENU find) {
        // Check menu cache
        IMenu menu = menuLookup.get(find);
        if(menu != null)
            return menu;

        // First use, load it
        switch(find) {
            case MAINMENU:
                menu = new MainMenuUI();
                break;
            case OPTIONS:
                menu = new OptionsUI();
                break;
            case SERVERS:
                menu = new ServerListUI();
                break;
            default:
                System.out.println("UI.GetMenu(): No init for " + find);
                return null;
        }
        menuLookup.put(find, menu); // cache it
        return menu;
    }
}
