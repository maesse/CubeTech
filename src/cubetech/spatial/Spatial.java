/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.spatial;

import java.util.HashMap;

/**
 *
 * @author mads
 */
public class Spatial {
    HashMap<Cell, Bin> data = new HashMap<Cell, Bin>();
    SpatialQuery query = new SpatialQuery();
    Cell lookupCell = new Cell(0,0);
    
    public Spatial() {
        
    }

    public void Clear() {
        data.clear();
    }

    // An object has moved, so update the handle and map
    public void Update(SpatialHandle handle, float minx, float miny, float maxx, float maxy) {
        // Figure out what cells the box spans now
        int xmincell = Cell.GetMinCell(minx);
        int xmaxcell = Cell.GetMaxCell(maxx);
        int ymincell = Cell.GetMinCell(miny);
        int ymaxcell = Cell.GetMaxCell(maxy);

        int mostMinX = xmincell < handle.xmincell ? xmincell : handle.xmincell;
        int mostMaxX = xmaxcell > handle.xmaxcell ? xmaxcell : handle.xmaxcell;
        int mostMinY = ymincell < handle.ymincell ? ymincell : handle.ymincell;
        int mostMaxY = ymaxcell > handle.ymaxcell ? ymaxcell : handle.ymaxcell;
        
        // Iterate cells
        for (int y= mostMinY; y < mostMaxY; y++) {
            for (int x= mostMinX; x < mostMaxX; x++) {
                boolean innew = x >= xmincell && x < xmaxcell && y >= ymincell && y < ymaxcell;
                boolean inold = x >= handle.xmincell && x < handle.xmaxcell && y >= handle.ymincell && y < handle.ymaxcell;

                System.out.println("innew: " + innew + " - inold: " + inold);

                if(inold && innew)
                    continue; // No change, leave handle for this bin

                if(!inold && !innew)
                    continue; // shouldn't happen, i think

                lookupCell.Set(x, y);
                Bin bin = data.get(lookupCell);
                
                if(inold && !innew) {
                    // remove
                    int binIndex = handle.GetCellIndex(lookupCell);
                    if(binIndex == -1) {
                        System.err.println("Spatial.Update(): Handle says we're already remove from this bin");
                        continue;
                    }
                    // remove from bin
                    bin.Remove(binIndex);
                    // remove from handle
                    handle.Remove(lookupCell);
                } else {
                    
                    if(bin == null)
                    {
                        // create new bin
                        Cell newcell = new Cell(x, y);
                        bin = new Bin(newcell);
                        data.put(bin.getCell(), bin);
                    }
                    // insert
                    handle.Insert(bin.getCell(), bin.Insert(handle.getObject()));
                }
            }
        }
        handle.Set(xmincell, xmaxcell, ymincell, ymaxcell);
    }

    public SpatialQuery Query(float minx, float miny, float maxx, float maxy) {
        query.Reset();
        
        // Figure out what cells the box spans
        int xmincell = Cell.GetMinCell(minx);
        int xmaxcell = Cell.GetMaxCell(maxx);
        int ymincell = Cell.GetMinCell(miny);
        int ymaxcell = Cell.GetMaxCell(maxy);

        // Iterate cells
        for (int y= ymincell; y < ymaxcell; y++) {
            for (int x= xmincell; x < xmaxcell; x++) {
                // Get bin
                lookupCell.Set(x, y);
                Bin bin = data.get(lookupCell);
                
                if(bin == null)
                    continue;


                // Let bin insert the result into query
                bin.GetData(query);
            }
        }

        return query;
    }

    public void Remove(SpatialHandle handle) {
        for (int y= handle.ymincell; y < handle.ymaxcell; y++) {
            for (int x= handle.xmincell; x < handle.xmaxcell; x++) {
                // Get bin
                lookupCell.Set(x, y);
                Bin bin = data.get(lookupCell);

                if(bin == null)
                    continue;

                int binIndex = handle.GetCellIndex(lookupCell);
                if(binIndex == -1) {
                    System.err.println("Spatial.Update(): Handle says we're already remove from this bin");
                    continue;
                }
                // remove from bin
                bin.Remove(binIndex);
                // remove from handle
                handle.Remove(lookupCell);
            }
        }
    }

    public SpatialHandle Create(float minx, float miny, float maxx, float maxy, Object object) {
        // Figure out what cells the box spans
        int xmincell = Cell.GetMinCell(minx);
        int xmaxcell = Cell.GetMaxCell(maxx);
        int ymincell = Cell.GetMinCell(miny);
        int ymaxcell = Cell.GetMaxCell(maxy);

        SpatialHandle handle = SpatialHandle.GetNew(object);
        handle.Set(xmincell, xmaxcell, ymincell, ymaxcell);
        for (int y= ymincell; y < ymaxcell; y++) {
            for (int x= xmincell; x < xmaxcell; x++) {
                // Insert into these bins
                lookupCell.Set(x, y);
                Bin bin = data.get(lookupCell);
                if(bin == null)
                {
                    // Insert new cell
                    Cell newcell = new Cell(x, y);
                    bin = new Bin(newcell);
                    data.put(bin.getCell(), bin);
                }

                handle.Insert(bin.getCell(), bin.Insert(object));
            }
        }

        return handle;
    }

    
}
