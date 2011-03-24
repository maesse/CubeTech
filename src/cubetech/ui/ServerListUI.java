package cubetech.ui;

import cubetech.client.ServerInfo;
import cubetech.common.Commands.ExecType;
import cubetech.common.IThinkMethod;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.input.MouseEvent;
import cubetech.misc.Button;
import cubetech.misc.Ref;
import cubetech.ui.CContainer.Direction;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

/**
 * Server browser UI
 * @author mads
 */
public class ServerListUI implements IMenu {
    CButton bBack;
    CButton bRefresh;
    CButton bLan;
    CButton bInternet;
   // CButton bFavorites;
    CButton bConnect;
//    CButton bAddServer;

    CContainer topCont;
    CScrollPane centerScroll;
    CContainer centerCont;
    CContainer botCont;

    int lastRefreshTime = 0;

    public void GotMouseEvent(MouseEvent evt) {
        // From 0-1 to 0-resolution space
        Vector2f absCoords = new Vector2f(Ref.glRef.GetResolution());
        absCoords.x *= evt.Position.x;
        absCoords.y *= 1-evt.Position.y;

        evt.Position = absCoords;

        topCont.MouseEvent(evt);
        botCont.MouseEvent(evt);
        centerScroll.MouseEvent(evt);
    }



    public enum ServerSource {
        INTERNET,
        LAN,
        FAVORITES
    }

    Vector2f buttonSize = new Vector2f(0.2f, 0.07f);
    ServerSource source = ServerSource.LAN;
    int lastServerCount = 0;
    ServerListItemUI hightlighted = null;



    ArrayList<ServerListItemUI> currentList = new ArrayList<ServerListItemUI>();

    public void setHighlighted(ServerListItemUI item) {
        hightlighted = item;
    }

    public ServerListUI() {
        CubeTexture buttonBg = Ref.ResMan.LoadTexture("data/menubutton.png");
        
        topCont = new CContainer(new FlowLayout(true, true, false));
        topCont.setSize2(new Vector2f(Ref.glRef.GetResolution().x, 0));
        
        bInternet = new CButton("Internet", buttonBg, Align.CENTER.LEFT, 1.5f, new ButtonEvent() {
            public void buttonPressed(CComponent button, MouseEvent evt) {
                SetSource(ServerSource.INTERNET);
            }
        });
        bLan = new CButton("LAN", buttonBg, Align.CENTER.LEFT, 1.5f, new ButtonEvent() {
            public void buttonPressed(CComponent button, MouseEvent evt) {
                SetSource(ServerSource.LAN);
            }
        });
//        bFavorites = new CButton("Favorites", buttonBg, Align.CENTER.LEFT, 1.5f, new ButtonEvent() {
//            public void buttonPressed(CComponent button, MouseEvent evt) {
//                SetSource(ServerSource.FAVORITES);
//            }
//        });

        topCont.addComponent(bInternet);
        topCont.addComponent(bLan);
       // topCont.addComponent(bFavorites);
//        topCont.addComponent(new CLabel("Server Browser", Align.CENTER, 1.5f));
        topCont.doLayout();

        // now do bottom container
        botCont = new CContainer(new FlowLayout(true, true, true));
//        botCont.setSize2(new Vector2f(Ref.glRef.GetResolution().x, 0));
        
        bBack = new CButton("Back", buttonBg, Align.CENTER.LEFT, 1, new ButtonEvent() {
            public void buttonPressed(CComponent button, MouseEvent evt) {
                Ref.ui.SetActiveMenu(UI.MENU.MAINMENU);
            }
        });
//        bAddServer = new CButton("Add Server", buttonBg, Align.CENTER.LEFT, 1, new ButtonEvent() {
//            public void buttonPressed(CComponent button, MouseEvent evt) {
//
//            }
//        });
        bRefresh = new CButton("Refresh", buttonBg, Align.CENTER.LEFT, 1, new ButtonEvent() {
            public void buttonPressed(CComponent button, MouseEvent evt) {
                SetSource(source);
            }
        });
        bConnect = new CButton("Connect", buttonBg, Align.CENTER.LEFT, 1, new ButtonEvent() {
            public void buttonPressed(CComponent button, MouseEvent evt) {
                if(hightlighted == null)
                    return;

                String host = hightlighted.adr.getAddress().getHostAddress()+":"+hightlighted.adr.getPort();
                Ref.commands.ExecuteText(ExecType.NOW, "connect " + host);
            }
        });
        bConnect.setMargin(30, 8, 8, 8);
        
        botCont.addComponent(bBack);
//        botCont.addComponent(bAddServer);
        botCont.addComponent(bRefresh);
        botCont.addComponent(bConnect);
        botCont.doLayout();

        botCont.setPosition(new Vector2f(0, Ref.glRef.GetResolution().y - botCont.getSize().y));
        SetSource(ServerSource.LAN);
//        centerScroll = new CScrollPane(true, false);
//        centerCont = new CContainer(new FlowLayout(false, false, true));
//        centerCont.setResizeToChildren(false);
//        centerScroll.addComponent(centerCont);
//        centerScroll.setSize2(new Vector2f(Ref.glRef.GetResolution().x, Ref.glRef.GetResolution().y-topCont.getSize().y - botCont.getSize().y));
//        centerScroll.setPosition(new Vector2f(0, topCont.getSize().y));
        
    }

    private void SetSource(ServerSource newsource) {
        source = newsource;
        lastServerCount = 0;
        hightlighted = null;
        currentList.clear();
        centerScroll = new CScrollPane(Direction.VERTICAL);
        centerScroll.setMargin(2, 2, 2, 2);
        centerCont = new CContainer(new FlowLayout(false, false, true));
        centerCont.setResizeToChildren(Direction.VERTICAL);
        centerScroll.setResizeToChildren(Direction.NONE);
        centerScroll.setBackground(Ref.ResMan.getWhiteTexture());
        centerScroll.addComponent(centerCont);
        centerScroll.setSize(new Vector2f(Ref.glRef.GetResolution().x-centerScroll.getMargin().x - centerScroll.getMargin().z, Ref.glRef.GetResolution().y-topCont.getSize().y - botCont.getSize().y-centerScroll.getMargin().y - centerScroll.getMargin().w));
        centerScroll.setPosition(new Vector2f(0, topCont.getSize().y));
        RefreshList();
    }

    // Called each frame.. fills out serverlist as servers become available after a refresh
    private void CheckForNewServers() {
        switch(source) {
            case FAVORITES:
                break;
            case LAN:
                if(lastServerCount < Ref.client.cl_nLocalServers) {
                    // Fill in new servers
                    AddServerInfo(Ref.client.cl_localServers, lastServerCount, Ref.client.cl_nLocalServers-lastServerCount);
                    lastServerCount = Ref.client.cl_nLocalServers;
                }
                break;
            case INTERNET:
                if(lastServerCount < Ref.client.cl_nGlobalServers) {
                    // Fill in new servers
                    AddServerInfo(Ref.client.cl_globalServers, lastServerCount, Ref.client.cl_nGlobalServers-lastServerCount);
                    lastServerCount = Ref.client.cl_nGlobalServers;
                }
                break;
        }
    }

    private void AddServerInfo(ServerInfo[] data, int offset, int size) {
        int added = 0;
        for (int i= 0; i < size; i++) {
            ServerInfo src = data[offset + i];

            if(src == null)
                continue;

            // Check if list already has this server
            boolean alreadyContained = false;
            for (int j= 0; j < currentList.size(); j++) {
                if(src.adr.equals(currentList.get(j).adr)) {
                    alreadyContained = true;
                    break;
                }
            }
            if(alreadyContained)
                continue;

            ServerListItemUI item = new ServerListItemUI(src, this);
            currentList.add(item);
            centerCont.addComponent(item);
            
            //centerCont.addComponent(item);
            centerCont.doLayout();
            added++;
        }

        System.out.println("Added " + added + " server(s) to the list");
    }

    public void RefreshList() {
        if(Ref.client.realtime - lastRefreshTime < 1000)
            return; // Too early
        lastRefreshTime = Ref.client.realtime;

        switch(source) {
            case INTERNET:
                Ref.commands.ExecuteText(ExecType.NOW, "internetservers");
            case FAVORITES:
                break;
            case LAN:
                Ref.commands.ExecuteText(ExecType.NOW, "localservers");
                break;
        }
    }

    float itemHeight = 0.08f;
    float startHeight = 0.83f;

    private void RenderServerList() {
        
//        CheckMouseEvent();
        
//        float nameOffset = 0.05f;
//        float mapOffset = 0.6f;
//        float clientsOffset = 0.8f;
//        float pingOffset = 0.96f;
//
//        float currSize = 0f;
//        for (int i= 0; i < currentList.size(); i++) {
//            // TODO: Check currsize
//
//            if(hightlighted == i) {
//                // Draw highlight
//                Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
//                spr.Set(new Vector2f(0.02f, startHeight - (i+1) * itemHeight), new Vector2f(0.96f, itemHeight), null, null, null);
//                spr.SetColor(255,255,255,100);
//            }
//
////            ServerInfo info = currentList.get(i);
////            currSize += Ref.textMan.AddText(new Vector2f(nameOffset, 0.01f+ startHeight - (i+1) * itemHeight), info.hostname, Align.LEFT, Type.HUD).y;
////            Ref.textMan.AddText(new Vector2f(mapOffset,0.01f+ startHeight - (i+1) * itemHeight), info.mapname, Align.LEFT, Type.HUD);
////            Ref.textMan.AddText(new Vector2f(clientsOffset,0.01f+ startHeight - (i+1) * itemHeight), info.nClients + "/" + info.maxClients, Align.LEFT, Type.HUD);
////            Ref.textMan.AddText(new Vector2f(pingOffset,0.01f+ startHeight - (i+1) * itemHeight), ""+info.ping, Align.RIGHT, Type.HUD);
//        }

        if(hightlighted != null)
            hightlighted.setHighlightFrame();
        centerScroll.Render(new Vector2f());
    }
    // Checks if a server has been selected with the mouse
//    private void CheckMouseEvent() {
//        if(Ref.Input.playerInput.Mouse1 && Ref.Input.playerInput.Mouse1Diff
//                && Ref.Input.playerInput.MousePos.y >= 0.33f && Ref.Input.playerInput.MousePos.y <= 0.83f) {
//            // Figure out what index is now selected
//            float height = 0.83f - Ref.Input.playerInput.MousePos.y;
//            height /= itemHeight;
//            int index = (int)height;
//            // Check if valid
//            if(index < 0 || index >= currentList.size())
//                index = -1;
//            hightlighted = index;
//        }
//    }

    public void Update(int msec) {
        // Update
//        Vector2f mousePos = Ref.Input.playerInput.MousePos;

        CheckForNewServers();
        
        // Render
        botCont.setPosition(new Vector2f(0, Ref.glRef.GetResolution().y - botCont.getSize().y));
        
//        spr = Ref.SpriteMan.GetSprite(Type.HUD);
//        spr.Set(new Vector2f(0.02f,0.1f), new Vector2f(0.96f, 0.2f), null, null, null);
//        spr.SetColor(255,255,255,20);

        RenderServerList();
//        Vector2f relativePosition = new Vector2f(mousePos);
//        relativePosition.x *= Ref.glRef.GetResolution().x;
//        relativePosition.y *= Ref.glRef.GetResolution().y;
//        centerCont.Render(new Vector2f());
        topCont.Render(new Vector2f());

        botCont.Render(new Vector2f());
//        bBack.Render();
//        bRefresh.Render();
//        bLan.Render();
//        bInternet.Render();
//        bFavorites.Render();
//        if(hightlighted != -1)
//            bConnect.Render();
//        bAddServer.Render();
    }

    public boolean IsFullscreen() {
        return true;
    }

    public void Show() {
       SetSource(source);
    }

}
