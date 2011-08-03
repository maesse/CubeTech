package cubetech.spatial;

import java.util.Collection;
import org.lwjgl.util.vector.Vector3f;

/**
 * Represents a result of a worldSector.AreaEntities() query.
 * Contains a list of results, where the results are entity indices
 * @author mads
 */
public class SectorQuery {
    public Vector3f mins, maxs; // Bounds of the query
    public Collection<Integer> List;

    public SectorQuery(Vector3f mins, Vector3f maxs, Collection<Integer> list) {
        this.mins = new Vector3f(mins);
        this.maxs = new Vector3f(maxs);
        this.List = list;
    }
}
