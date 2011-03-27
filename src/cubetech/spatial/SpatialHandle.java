/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.spatial;


import cubetech.Block;
import cubetech.common.Common;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author mads
 */
public class SpatialHandle {
//    static final int MAX_CELL_HANDLES = 8;
    
    private HashMap<Integer, Integer> cellMap = new HashMap<Integer, Integer>(1);
//    private ArrayList<Integer> cellHash = new ArrayList<Integer>(1);
//    private ArrayList<Integer> Index = new ArrayList<Integer>(1);
    //private int[] cellHash = new int[MAX_CELL_HANDLES];
    //private int[] Index = new int[MAX_CELL_HANDLES];
    //private int offset = 0;
    private Object object;

    int xmincell;
    int xmaxcell;
    int ymincell;
    int ymaxcell;

    SpatialHandle(Object object) {
        this.object = object;
    }

    
    public void Reset() {
        //offset = 0;
        object = null;
        xmincell = 0;
        xmaxcell = 0;
        ymincell = 0;
        ymaxcell = 0;
    }

    // Sets the cell bounds
    public void Set(int xmin, int xmax, int ymin, int ymax) {
        this.xmincell = xmin;
        this.xmaxcell = xmax;
        this.ymincell = ymin;
        this.ymaxcell = ymax;
    }

    public void Insert(Cell cell, int index) {
//        if(offset >= MAX_CELL_HANDLES){
//            System.err.println("SpatialHandle.Insert() overflow");
//            return;
//        }
        if(cellMap.containsKey(cell.hashCode())) {
            Common.LogDebug("SpatialHandle.Insert: Cell already contained");
        }
        cellMap.put(cell.hashCode(), index);
//        cellHash.add(cell.hashCode());
//        Index.add(index);
        //this.cellHash[offset] = cell.hashCode();
        //Index[offset] = index;
        //offset++;
    }

    public int GetCellIndex(Cell cell) {
        int hashcode = cell.hashCode();
        Integer value = cellMap.get(hashcode);
        if(value == null)
            return -1;
        return value;
//        //for (int i= 0; i < offset; i++) {
//        for (int i= 0; i < cellHash.size(); i++) {
//            if(this.cellHash.get(i) == hashcode)
//                return getIndex(i);
//        }
//        return -1;
    }

    @Override
    public String toString() {
        return "Handle("+((Block)object).Handle + ")";
    }

    public void Remove(Cell cell) {
        //Common.LogDebug("  -handle(" + ((Block)object).Handle + ").cell:" + cell);
        int hashcode = cell.hashCode();
        if(cellMap.remove(hashcode) == null) {
            System.err.println("SpatialHandle.Remove(): Already removed?");
        }
//        int i = 0;
//        for (; i < offset; i++) {
//            if(this.cellHash[i] == hashcode)
//                break;
//        }
//
//        if(i == offset) {
//            System.err.println("SpatialHandle.Remove(): Already removed?");
//            return; // Already removed?
//        }
//
//        // Easy case where index is the tail
//        if(i == offset-1)
//        {
//            offset--;
//            return;
//        }
//
//        // Swap tail with the index
//        cellHash[i] = cellHash[offset-1];
//        Index[i] = Index[offset-1];
//        offset--;
    }

    public void CellIndexChanged(Cell cell, int newIndex) {

        int hashcode = cell.hashCode();
        Integer value = cellMap.put(hashcode, newIndex);
        if(value == null)
            value = -1;
//        System.out.println("Changing cell from " + value + " to " + newIndex);
        
//        for (int i= 0; i < offset; i++) {
//            if(this.cellHash[i] == hashcode) {
//                System.out.println("Changing cell[" + i + "] from " + Index[i] + " to " + newIndex);
//                Index[i] = newIndex;
//                return;
//            }
//        }
        
        
    }

    public Object getObject() {
        return object;
    }


//    public int getIndex(int index) {
//        return Index[index];
//    }
//
//    public int getCell(int index) {
//        return cellHash[index];
//    }

//    public int getCount() {
//        return offset;
//    }

    public static SpatialHandle GetNew(Object object) {
        return new SpatialHandle(object);
    }


}
