package cubetech.CGame;

import cubetech.common.Common.ErrorCode;
import cubetech.misc.Ref;

/**
 *
 * @author mads
 */
public class Render {
    private static final int MAX_RENDER_ENTITIES = 1000;
    private RenderEntity[] entities = new RenderEntity[MAX_RENDER_ENTITIES];
    private int next = 0;

    public Render() {
        for (int i= 0; i < MAX_RENDER_ENTITIES; i++) {
            entities[i] = new RenderEntity();
        }
    }

    public void renderAll() {
        for (int i= 0; i < next; i++) {
            RenderEntity ent = entities[i];
            switch(ent.Type) {
                case RenderEntity.TYPE_MODEL:
                    renderModel(ent);
                    break;
                case RenderEntity.TYPE_SPRITE:
                    renderSprite(ent);
                    break;
                default:
                    Ref.common.Error(ErrorCode.FATAL, "Render.renderAll(): unknown type " + ent.Type);
                    break;
            }
        }
    }
    
    private void renderModel(RenderEntity ent) {
        if(ent.model == null) return;

        float frame = ent.oldframe * ent.backlerp + ent.frame * (1f-ent.backlerp);
        ent.model.animate(ent.frame, ent.oldframe, ent.backlerp);
        ent.model.render(ent.origin, ent.axis);
    }

    private void renderSprite(RenderEntity ent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void reset() {
        next = 0;
    }

    public RenderEntity createEntity(REType type) {
        RenderEntity ent = entities[next];
        next++;

        int ent_type = typeToInt(type);
        ent.Type = ent_type;
        return ent;
    }

    private int typeToInt(REType type) {
        switch(type) {
            case MODEL:
                return 0;
            case SPRITE:
                return 1;
            default:
                Ref.common.Error(ErrorCode.FATAL, "Render.typeToInt unknown type " + type);
                return -1;
        }
    }

    
}
