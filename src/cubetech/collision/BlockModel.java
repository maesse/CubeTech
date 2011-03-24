package cubetech.collision;

import cubetech.Block;
import cubetech.common.Helper;
import cubetech.misc.Ref;
import java.util.ArrayList;
import org.lwjgl.util.vector.Vector2f;

/**
 * Basically a collection of blocks, grouped as one.
 * Can be attached to an entity. If not attached,
 * it will just work as normal world geometry.
 *
 * @author mads
 */
public class BlockModel {
    // min/max in absolute coords
    public Vector2f mins = new Vector2f();
    public Vector2f maxs = new Vector2f();
    
    public Vector2f center = new Vector2f();
    public Vector2f size = new Vector2f();
    
    public ArrayList<Block> blocks = new ArrayList<Block>();
    private boolean moving = false; // true when doing a move

    public int modelIndex = -1;
    public int entityNum = -1;

    public BlockModel(int modelindex) {
        this.modelIndex = modelindex;
    }

    public void addBlock(Block b) {
        blocks.add(b);
        if(b.CustomVal > 1) {
            if(b.CustomVal == modelIndex)
                System.out.println("WARNING: BlockModel.addBlock: Block is already in this model");
            else
            {
                // Remove from old model
                BlockModel bm = Ref.cm.cm.getModel(b.CustomVal);
                bm.removeBlock(b);
            }
        }

        b.CustomVal = modelIndex;
        calculateBounds();
    }

    public void attachEntity(int entNumber) {
        entityNum = entNumber;
    }

    public void removeBlock(Block b) {
        if(b.CustomVal != modelIndex) {
            System.out.println("BlockModel.removeBlock: Block's model is " + b.CustomVal + ", this model is " + modelIndex + ", cannot remove.");
            return;
        }

        blocks.remove(b);
        b.CustomVal = 0;
        calculateBounds();
    }


    private void calculateBounds() {
        mins.set(Integer.MAX_VALUE, Integer.MAX_VALUE);
        maxs.set(Integer.MIN_VALUE, Integer.MIN_VALUE);
        for (Block block : blocks) {
            Helper.AddPointToBounds(new Vector2f(block.GetCenter().x - block.getAbsSize().x * 0.5f, block.GetCenter().y - block.getAbsSize().y * 0.5f), mins, maxs);
            Helper.AddPointToBounds(new Vector2f(block.GetCenter().x + block.getAbsSize().x * 0.5f, block.GetCenter().y + block.getAbsSize().y * 0.5f), mins, maxs);
        }

        Vector2f.sub(maxs, mins, size);
        center.set(size);
//        System.out.println("Size: " + size);
        center.scale(0.5f);
        Vector2f.add(mins, center, center);

        // Change the size of the entity to reflect the change
        if(entityNum > 0)
            Ref.server.SetBrushModel(Ref.server.sv.gentities[entityNum], modelIndex);
    }

    

    // Move the model to this position (centered)
    public void moveTo(Vector2f position) {
        if(moving)
            System.out.println("BlockModel.moveTo: Warning, called recursively");

//        System.out.println(position);

        moving = true;

        // Do the moves
        // position - originToCenter = centerBounds -> position movement vector
        Vector2f deltaMove = new Vector2f();
        Vector2f.sub(position, center, deltaMove);

        if(deltaMove.x != 0f || deltaMove.y != 0f)
        {
            for (Block block : blocks) {
                Vector2f newposition = new Vector2f();
                Vector2f.add(block.getPosition(), deltaMove, newposition);
                block.SetPosition(newposition);
//                block.Material.setTexture(Ref.ResMan.getWhiteTexture());
            }
            center.set(position);
            Vector2f.add(mins, deltaMove, mins);
            Vector2f.add(maxs, deltaMove, maxs);
        }

        moving = false;
    }

    // This block is reporting in that is has been moved
    public void notifyChange(Block b) {
        if(moving)
            return; // We are the ones doing the move, so ignore this notification

        // A block has changed size/position/angle, so update the models bounds
        calculateBounds();
    }

    public boolean intersects(Vector2f point) {
        // Scale to rect ints
        if(point.x >= mins.x && point.x <= maxs.x) // inside x coords
            if(point.y >= mins.y && point.y <= maxs.y ) // inside y coords {
                return true;

        return false;
    }
    
}
