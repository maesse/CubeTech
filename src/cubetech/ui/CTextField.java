package cubetech.ui;

import cubetech.client.MessageField;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.input.MouseEvent;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CTextField extends CComponent {
    private String text = "";
    private ButtonEvent onTextChange = null;
    private MessageField msgField = null;

    public CubeTexture texture = Ref.ResMan.getWhiteTexture();
    public Vector4f color = new Vector4f(40,40,40,200);
    private int charWidth = 25;
    private boolean reqFocus = false;
    

    public CTextField(int charWidth) {
        handleSize(charWidth);
    }

    public CTextField(int charWidth, String text) {
        this.text = text;
        handleSize(charWidth);
    }

    public CTextField(int charWidth, String text, ButtonEvent evt) {
        this.text = text;
        this.onTextChange = evt;
        handleSize(charWidth);
    }

    private void handleSize(int charWidth) {
        this.charWidth = charWidth;
        setSize(new Vector2f(charWidth * 14, Ref.textMan.GetCharHeight() + 4));
    }

    @Override
    public void MouseEvent(MouseEvent evt) {
        if(containsPoint(evt.Position) && evt.Button == 0 && evt.Pressed)
            requestFocus();
    }

    public void requestFocus() {
        if(msgField != null)
            Ref.client.message.SetField(msgField);
        else
            reqFocus = true;
    }

    public void removeFocus() {
        if(msgField == null)
            return;

        String str = msgField.buffer.toString();
        if(!text.equals(str)) {
            text = str;
            if(onTextChange != null)
                onTextChange.buttonPressed(this, null);
        }
        Ref.client.message.SetField(null);
        msgField = null;
    }

    public String getText() {
        if(msgField != null)
            return msgField.buffer.toString();
        return text;
    }

    @Override
    public void Render(Vector2f parentPosition) {
        Vector2f renderposition = getInternalPosition(); // takes margin into account
        renderposition.x += parentPosition.x;
        renderposition.y += parentPosition.y;

        
        // Background
        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);

        spr.Set(new Vector2f(renderposition.x, Ref.glRef.GetResolution().y - renderposition.y - getSize().y +4), getSize(), texture, null, null);

        // Text
        int alpha = (int)color.w;
        if(isMouseEnter() || msgField != null)
            alpha += 80;
        if(alpha > 255)
            alpha = 255;
        spr.SetColor((int)color.x, (int)color.y, (int)color.z, alpha);
        if(msgField == null && reqFocus) {
            msgField = new MessageField(new Vector2f(renderposition.x, Ref.glRef.GetResolution().y - renderposition.y), text);
            msgField.widthInChars = charWidth;
            Ref.client.message.SetField(msgField);
            reqFocus = false;
        }
        if(msgField != null)
            msgField.Render();
        else
            Ref.textMan.AddText(renderposition, text, Align.LEFT, Type.HUD);
    }

}
