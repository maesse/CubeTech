package cubetech.ui;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.input.MouseEvent;
import cubetech.misc.Ref;

import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class CSlider extends CComponent {
    private ButtonEvent onValueChange = null;
    private float min = 0, max = 1;
    private float frac = 0;

    CubeTexture texture = null;

     public CSlider(Vector2f size, ButtonEvent evt) {
        setSize(new Vector2f(size));
        texture = Ref.ResMan.LoadTexture("data/healthbar.png");
    }

    @Override
    public void MouseEvent(MouseEvent evt) {
        if(containsPoint(evt.Position) && Ref.Input.playerInput.Mouse1)
        {
            // Get position on x axis as a fraction
            float pixelOffset = evt.Position.x - getInternalPosition().x;

            float oldFrac = frac;
            
            frac = pixelOffset / getSize().x;
            if(frac < 0)
                frac = 0;
            if(frac > 1)
                frac = 1;

            if(Math.abs(frac-oldFrac) > 0.01f && onValueChange != null)
                onValueChange.buttonPressed(this, evt);
        }
    }

    public void setValueDontFire(float val) {
        val += min;
        float len = max-min;
        val /= len;
        frac = val;
    }

    public float getValue() {
        float len = max-min;
        len *= frac;
        return min + len;
    }

    @Override
    public void Render(Vector2f parentPosition) {
        Vector2f renderposition = getInternalPosition(); // takes margin into account
        renderposition.x += parentPosition.x;
        renderposition.y += parentPosition.y;


        // Background
        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        Vector2f size = getSize();
        spr.Set(new Vector2f(renderposition.x, Ref.glRef.GetResolution().y - renderposition.y - size.y ), size, Ref.ResMan.getWhiteTexture(), new Vector2f(), new Vector2f(-1,1));
        spr.SetColor(40, 40, 40, 127);

        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(renderposition.x, Ref.glRef.GetResolution().y - renderposition.y - size.y ), new Vector2f(size.x*frac, size.y), texture, new Vector2f(), new Vector2f(-frac,1));
        spr.SetColor(255, 255, 255, isMouseEnter()?255:200);
    }

}
