package cubetech.ui;

import cubetech.CGame.AnimationEditor;
import cubetech.common.Common;
import cubetech.gfx.Animation;
import cubetech.gfx.CubeMaterial;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.KeyFrame;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.input.MouseEvent;
import cubetech.misc.Ref;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class CTimeline extends CContainer {
    CPanel emptyPanel = null;
    CubeMaterial tinyarrow = null;
    CubeTexture keyframeTex = null;
    AnimationEditor animEd = null;
    
    float zoom = 500f; // Spread one second over this many pixels
    float linespacing = 10; // lines pr second
    float xoffset = 10;
    
    private int mouseDown = -1;
    int currentFrame = 0;
    ArrayList<Integer> keyFrameTimes = new ArrayList<Integer>();

    public CTimeline(float height, AnimationEditor ed) {
        super(new FlowLayout(true, false, true));
        this.animEd = ed;
        setResizeToChildren(CContainer.Direction.HORIZONTAL);
        emptyPanel = new CPanel(new Vector2f(Ref.glRef.GetResolution().x + 200, height));
        addComponent(emptyPanel);
        doLayout();
        try {
            keyframeTex = Ref.ResMan.LoadTexture("data/anim_keyframe.png");
            tinyarrow = CubeMaterial.Load("data/tinyarrow.mat", true);
        } catch (Exception ex) {
            Common.Log("Missing material: data/tinyarrow.mat");
        }
    }

    private void MouseSelectFrame(int time) {
        animEd.model.AnimateToTime(time);
    }

    private void updateKeyFrameList() {
        Animation currAnim = animEd.model.GetCurrentAnimation();
        if(!currAnim.dirty)
            return;

        KeyFrame frame = currAnim.GetRootFrame();
        keyFrameTimes.clear();
        while(frame != null) {
            keyFrameTimes.add(frame.time);
            frame = frame.next;
        }
        currAnim.dirty = false;
    }
   
    @Override
    public void MouseEvent(MouseEvent evt) {
        if(evt.Pressed)
            mouseDown = evt.Button;
        else if(evt.Button >= 0)
            mouseDown = -1;
        if(!isMouseEnter())
            mouseDown = -1;

        if(mouseDown == 1) {
            // select position on timeline
            currentFrame = (int)((evt.Position.x - xoffset) * (1000f/zoom));
            if(currentFrame < 0)
                currentFrame = 0;
            MouseSelectFrame(currentFrame);
        }

        if(evt.WheelDelta != 0 && Ref.Input.IsKeyPressed(Keyboard.KEY_LSHIFT)) {
            zoom += 100 * evt.WheelDelta;
            if(zoom < 100)
                zoom = 100;
            if(zoom > 5000)
                zoom = 5000;
        }
    }

    @Override
    public void RenderImplementation(Vector2f pos) {
        updateKeyFrameList();
        if(mouseDown < 0) {
            currentFrame = animEd.model.GetCurrentAnimation().getTime();
        }
        // Add in a x offset
        pos.x += xoffset;

        Sprite spr = null;
        Vector2f size = getSize();
        float height = Ref.glRef.GetResolution().y;
        Vector2f src = new Vector2f(pos.x+1, height - pos.y -10);
        Vector2f dst = new Vector2f(pos.x+1, height - (pos.y+size.y-15));
        Vector2f dst2 = new Vector2f(pos.x+1, height - (pos.y+size.y-5));

        float spacing = zoom/linespacing;

        int nLines = (int) (size.x / spacing);
        for (int i= 0; i < nLines; i++) {
            spr = Ref.SpriteMan.GetSprite(Type.HUD);


            if(i % (linespacing/2) == 0) {
                // Hitting the second mark.
                float thickness = i % linespacing == 0?2f:1f;
                spr.setLine(src, dst, thickness);
                Vector2f textDst = new Vector2f(dst);
                textDst.y = pos.y+size.y-Ref.textMan.GetCharHeight()*0.4f;
                int count = (int) (1000 * i / linespacing);
                Ref.textMan.AddText(textDst, ""+count, Align.CENTER, Type.HUD,0.5f);
            } else {
                spr.setLine(src, dst2, 1f);
                spr.SetColor(170, 170, 170, 255);
            }
//            spr.SetDepth(-8);
            src.x += spacing;
            dst.x += spacing;
            dst2.x += spacing;
        }

        // Render keyframe markers
        for (Integer keyOffset : keyFrameTimes) {
            spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.Set(pos.x + keyOffset * (zoom/1000f), height - pos.y - size.y/2f, 8f, keyframeTex);
        }

        // Render current frame marker
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(pos.x + currentFrame * (zoom/1000f) - 10, height - pos.y  - 11), new Vector2f(20,10),
                tinyarrow.getTexture(), tinyarrow.getTextureOffset(), tinyarrow.getTextureSize());

        if(mouseDown == 1) {
            Vector2f mousepos = new Vector2f(pos.x + currentFrame * (zoom/1000f) + 10, pos.y  );
//            mousepos.x *= Ref.Input.playerInput.MousePos.x;
//            mousepos.y *= (1 -Ref.Input.playerInput.MousePos.y );
            Ref.textMan.AddText(mousepos, ""+currentFrame, Align.LEFT, Type.HUD);
        }
    }

    @Override
    public void setParent(CContainer parent) {
        this.parent = parent;
        setSize(new Vector2f(0, parent.getInternalSize().y));
        doLayout();
    }

    @Override
    public void onMouseExit() {
        mouseDown = -1;
    }
}
