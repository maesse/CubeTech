package cubetech.spatial;

import cubetech.common.Helper;
import cubetech.entities.SharedEntity;
import cubetech.entities.SvEntity;
import java.util.ArrayList;
import java.util.LinkedList;
import org.lwjgl.util.vector.Vector2f;

/**
 * Builds an uniformly subdivided tree with some given bounds
 * The tree contains nodes and leafs. All nodes and leafs can contain
 * entities. Entities are hold in a linked list.
 * @author mads
 */
public final class WorldSector {
    // Variables used for creation
    private static int MaxDepth = 4;
    private static int createNodeCount = 0;
    
    int Axis; // -1 for leafs, else axis index
    
    // Leaf
    LinkedList<SvEntity> entities = new LinkedList<SvEntity>();

    // Node
    float Distance; // where to split on the given axis
    WorldSector[] children = new WorldSector[2]; // child nodes

    // Only allow worldsector to create new
    private WorldSector() {

    }

    public void LinkEntity(SvEntity ent) {
        if(ent.worldSector != null)
            System.out.println("Warning: WorldSector.LinkEntity() entity is already linked");
        ent.worldSector = this;
        entities.add(ent);
    }

    public boolean UnlinkEntity(SvEntity ent) {
        ent.worldSector = null;
        return entities.remove(ent);
    }

    public SectorQuery AreaEntities(Vector2f mins, Vector2f maxs) {
        SectorQuery results = new SectorQuery(mins, maxs, new ArrayList<Integer>());
        AreaEntities_Recursive(results, this);
        return results;
    }

    private static void AreaEntities_Recursive(SectorQuery result, WorldSector node) {
        // Add valid nodes from this worldsector
        for(SvEntity ent : node.entities) {
            SharedEntity sent = ent.GetSharedEntity();

            if(sent.r.absmin.x > result.maxs.x
                || sent.r.absmin.y > result.maxs.y
                || sent.r.absmax.x < result.mins.x
                || sent.r.absmax.y < result.mins.y)
                continue;

            result.List.add(ent.id);
        }

        if(node.Axis == -1)
            return; // leaf node

        // Recurse down both sides
        if(Helper.VectorGet(result.maxs, node.Axis) > node.Distance)
            AreaEntities_Recursive(result, node.children[0]);
        if(Helper.VectorGet(result.mins, node.Axis) < node.Distance)
            AreaEntities_Recursive(result, node.children[1]);
    }

    // Create a new worldsector with the given parameters
    public static WorldSector CreateWorldSector(Vector2f mins, Vector2f maxs, int maxdepth) {
        int nNodes = 0;
        for (int i= 0; i <= maxdepth; i++) nNodes += Math.pow(2, i);
        System.out.println("WorldSector: New worldsector will have " + nNodes + " nodes in total.");

        createNodeCount = 0;
        MaxDepth = maxdepth;
        WorldSector rootNode = CreateWorldSector_Recursive(0, mins, maxs);
        
        // Check if results are as expected
        if(nNodes != createNodeCount) {
            System.out.println(String.format("WorldSector: Initialization fail, expected %d sectords, got %d", nNodes, createNodeCount));
        }
        return rootNode;
    }

    // internal recursive method for splitting and creating child nodes
    private static WorldSector CreateWorldSector_Recursive(int depth, Vector2f mins, Vector2f maxs) {
        WorldSector node = new WorldSector();
        createNodeCount++;
        // Leaf
        if(depth == MaxDepth) {
            node.Axis = -1;
            return node;
        }

        // Figure out what axis to split
        Vector2f size = new Vector2f();
        Vector2f.sub(maxs, mins, size);
        if(size.x > size.y)
            node.Axis = 0;
        else
            node.Axis = 1;

        // Where to split
        node.Distance = 0.5f * (Helper.VectorGet(maxs, node.Axis) + Helper.VectorGet(mins, node.Axis));
        Vector2f mins1 = new Vector2f(mins);
        Vector2f mins2 = new Vector2f(mins);
        Vector2f maxs1 = new Vector2f(maxs);
        Vector2f maxs2 = new Vector2f(maxs);

        Helper.VectorSet(maxs1, node.Axis, node.Distance);
        Helper.VectorSet(mins2, node.Axis, node.Distance);

        node.children[0] = CreateWorldSector_Recursive(depth+1, mins2, maxs2); // maximum side
        node.children[1] = CreateWorldSector_Recursive(depth+1, mins1, maxs1); // minimum side
        return node;
    }

    

    public WorldSector FindCrossingNode(Vector2f absmin, Vector2f absmax) {
        WorldSector node = this;
        while(true) {
            if(node.Axis == -1)
                break;
            if(Helper.VectorGet(absmin, node.Axis) > node.Distance)
                node = node.children[0];
            else if(Helper.VectorGet(absmax, node.Axis) < node.Distance)
                node = node.children[1];
            else
                break; // crosses the node
        }
        return node;
    }
}