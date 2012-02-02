package cubetech.gfx;

import java.util.ArrayList;
import static org.lwjgl.opengl.ARBVertexShader.*;


/**
 *
 * @author Mads
 */
public class StrideInfo {
    private ArrayList<Item> items = new ArrayList<Item>();
    private int stride;
    
    public StrideInfo(int stride) {
        this.stride = stride;
    }
    
    public void addItem(int paramIndex, int count, int dataType, boolean normalize, int offset) {
        Item item = new Item();
        item.parameter = paramIndex;
        item.count = count;
        item.dataType = dataType;
        item.shouldNormalize = normalize;
        item.offset = offset;
        items.add(item);
    }
    
    public void applyInfo() {
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            glVertexAttribPointerARB(item.parameter, item.count, item.dataType, item.shouldNormalize, stride, item.offset);
        }
    }
    
    public void enableAtribs() {
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            glEnableVertexAttribArrayARB(item.parameter);
        }
    }
    
    public void disableAttribs() {
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            glDisableVertexAttribArrayARB(item.parameter);
        }
    }
    
    private class Item {
        int parameter;
        int count;
        int dataType;
        boolean shouldNormalize;
        int offset;
    }
}
