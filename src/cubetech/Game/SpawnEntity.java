package cubetech.Game;

import cubetech.Block;
import cubetech.common.Common;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class SpawnEntity {
    public String className;
    public Vector2f origin = new Vector2f();

    private boolean spawned = false;
    private Gentity ent = null;

    private Block editBlock = null;


    public SpawnEntity(String className, Vector2f position) {
        if(className == null || className.isEmpty())
            Ref.common.Error(Common.ErrorCode.FATAL, "SpawnEntity(): className == null");
        this.className = className;
        origin.set(position);
        editBlock = new Block(-1, new Vector2f(position.x - 6, position.y - 6), new Vector2f(12,12), false);
        editBlock.setLayer(-20); // make sure entities are the first to be selected in the editor
        editBlock.spawnEntity = this;
        if(className.equalsIgnoreCase("item_boots"))
            editBlock.Material.setTexture(Ref.common.items.findItemByClassname(className).icon);
    }

    public Block getBlock() {
        return editBlock;
    }

    public boolean isSpawned() {
        return spawned && ent != null;
    }

    public Gentity getGEntity() {
        return ent;
    }

    public void Spawn() {
        if(spawned) {
            Common.LogDebug("SpawnEntity.Spawn(): WARNING Is already spawned.");
        }
        origin.set(editBlock.GetCenter());
        ent = Ref.game.Spawn();
        ent.s.origin.set(origin);
        ent.classname = className;
        ent.s.pos.base.set(ent.s.origin);
        ent.r.currentOrigin.set(ent.s.origin);

        if(!Ref.game.callSpawn(ent))
            ent.Free();

        spawned = true;
    }

    public void Unspawn() {
        if(!spawned) {
            Common.LogDebug("SpawnEntity.Unspawn(): Error, is not spawned.");
            return;
        }

        ent.Free();
        spawned = false;
        ent = null;
    }
}
