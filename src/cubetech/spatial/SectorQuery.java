package cubetech.spatial;

import java.util.Collection;
import org.lwjgl.util.vector.Vector2f;

/**
 * Represents a result of a worldSector.AreaEntities() query.
 * Contains a list of results, where the results are entity indices
 * @author mads
 */
public class SectorQuery {
    public Vector2f mins, maxs; // Bounds of the query
    public Collection<Integer> List;

    public SectorQuery(Vector2f mins, Vector2f maxs, Collection<Integer> list) {
        this.mins = new Vector2f(mins);
        this.maxs = new Vector2f(maxs);
        this.List = list;
    }
}
