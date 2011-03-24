package cubetech.ui;

import cubetech.Game.Gentity;
import cubetech.client.ServerInfo;
import cubetech.common.IThinkMethod;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.input.MouseEvent;
import cubetech.misc.Ref;
import java.net.InetSocketAddress;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class ServerListItemUI extends CContainer {
    CLabel name;
    CLabel map;
    CLabel clients;
    CLabel ping;
    InetSocketAddress adr;
    ServerListUI parent = null;
    ServerInfo srvInfo = null;
    boolean highlight = false;

    public void setHighlightFrame() {
        highlight = true;
    }

    public ServerListItemUI(ServerInfo srvInfo, ServerListUI parent) {
        super(new ILayoutManager() {
            public void layoutComponents(CContainer container) {
                float mapOffset = 0.6f;
                float clientsOffset = 0.8f;
                float pingOffset = 0.90f;
                ServerListItemUI ui = (ServerListItemUI)container;

                Vector2f contSize = container.getInternalSize();

                ui.name.setSize(new Vector2f(mapOffset * contSize.x, contSize.y));
                ui.map.setSize(new Vector2f((clientsOffset - mapOffset) * contSize.x, contSize.y));
                ui.clients.setSize(new Vector2f((pingOffset - clientsOffset) * contSize.x, contSize.y));
                ui.ping.setSize(new Vector2f((1f-pingOffset) * contSize.x, contSize.y));
                ui.map.setPosition(new Vector2f(mapOffset * contSize.x, 0));
                ui.clients.setPosition(new Vector2f(clientsOffset * contSize.x, 0));
                ui.ping.setPosition(new Vector2f(pingOffset * contSize.x, 0));
            }
        });
        this.srvInfo = srvInfo;
        srvInfo.onUpdate = new IThinkMethod() {
            public void think(Gentity ent) {
                updateValues();
            }
        };
        this.parent = parent;
        adr = srvInfo.adr;
        name = new CLabel(srvInfo.hostname, Align.LEFT, 1);
        map = new CLabel(cleanMapname(srvInfo.mapname), Align.LEFT, 1);
        clients = new CLabel(srvInfo.nClients + "/" + srvInfo.maxClients, Align.LEFT, 1);
        ping = new CLabel(cleanPing(srvInfo.ping), Align.RIGHT, 1);
        setSize2(new Vector2f(0, Ref.textMan.GetCharHeight()));
        addComponent(name);
        addComponent(map);
        addComponent(clients);
        addComponent(ping);
    }

    private void updateValues() {
        name.setText(srvInfo.hostname);
        map.setText(cleanMapname(srvInfo.mapname));
        clients.setText(srvInfo.nClients + "/" + srvInfo.maxClients);
        ping.setText(cleanPing(srvInfo.ping));
    }

    private String cleanPing(int ping) {
        if(ping <= 0)
            return "N/A";
        return ""+ping;
    }

    private String cleanMapname(String map) {
        if(map.startsWith("data/"))
            return map.replace("data/", "");
        return map;
    }

    @Override
    public void MouseEvent(MouseEvent evt) {
        if(containsPoint(evt.Position)) {
            if(!isMouseEnter())
                MouseEnter();
//            System.out.println("evt: " + evt.Position);

            if(evt.Button == 0 && evt.Pressed) {
                parent.setHighlighted(this);
            }

            // Offset position for children
            Vector2f relativePosition = new Vector2f(evt.Position);
            relativePosition.x -= getInternalPosition().x;
            relativePosition.y -= getInternalPosition().y;
            for (int i= 0; i < getComponentCount(); i++) {
                CComponent comp = getComponent(i);
                boolean hit = comp.containsPoint(relativePosition);
                if(hit != comp.isMouseEnter()) {
                    if(hit)
                        comp.MouseEnter();
                    else
                        comp.MouseExit();
                }
                if(hit) {
                    MouseEvent evt2 = new MouseEvent(evt.Button, evt.Pressed, evt.WheelDelta, evt.Delta, relativePosition);
                    comp.MouseEvent(evt2);

                }

            }
        } else if(isMouseEnter()) {
            MouseExit();
        }
    }

    @Override
    public void Render(Vector2f parentPosition) {
        Vector2f renderpos = new Vector2f(parentPosition);

        renderpos.x += getPosition().x + getMargin().x;
        renderpos.y += getPosition().y + getMargin().y;

        if(isMouseEnter() || highlight) {
            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.Set(new Vector2f(renderpos.x, Ref.glRef.GetResolution().y - (renderpos.y + getInternalSize().y)), getInternalSize(), null, null, null);
            spr.SetColor(255, 255, 255, highlight?isMouseEnter()?80:50:80);
        }
        highlight = false;

        renderpos.x += getInternalMargin().x;
        renderpos.y += getInternalMargin().y;

        // Render all the children
        for (int i= 0; i < getComponentCount(); i++) {
            getComponent(i).Render(renderpos);
        }
    }
}
