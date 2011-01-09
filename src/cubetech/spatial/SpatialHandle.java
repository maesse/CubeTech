/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.spatial;

/**
 *
 * @author mads
 */
public class SpatialHandle {
    static final int MAX_CELL_HANDLES = 10;
    private int[] cellHash = new int[MAX_CELL_HANDLES];
    private int[] Index = new int[MAX_CELL_HANDLES];
    private int offset = 0;
    private Object object;    

    int xmincell;
    int xmaxcell;
    int ymincell;
    int ymaxcell;

    SpatialHandle(Object object) {
        this.object = object;
    }

    
    public void Reset() {
        offset = 0;
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
        if(offset >= MAX_CELL_HANDLES){
            System.err.println("SpatialHandle.Insert() overflow");
            return;
        }

        this.cellHash[offset] = cell.hashCode();
        Index[offset] = index;
        offset++;
    }

    public int GetCellIndex(Cell cell) {
        int hashcode = cell.hashCode();
        for (int i= 0; i < offset; i++) {
            if(this.cellHash[i] == hashcode)
                return getIndex(i);
        }
        return -1;
    }

    public void Remove(Cell cell) {
        int hashcode = cell.hashCode();
        int i = 0;
        for (; i < offset; i++) {
            if(this.cellHash[i] == hashcode)
                break;
        }
        
        if(i == offset) {
            System.err.println("SpatialHandle.Remove(): Already removed?");
            return; // Already removed?
        }

        // Easy case where index is the tail
        if(i == offset-1)
        {
            offset--;
            return;
        }

        // Swap tail with the index
        cellHash[i] = cellHash[offset-1];
        Index[i] = Index[offset-1];
        offset--;
    }

    public void CellIndexChanged(Cell cell, int newIndex) {

        int hashcode = cell.hashCode();
        for (int i= 0; i < offset; i++) {
            if(this.cellHash[i] == hashcode) {
                System.out.println("Changing cell[" + i + "] from " + Index[i] + " to " + newIndex);
                Index[i] = newIndex;
                return;
            }
        }
        
        
    }

    public Object getObject() {
        return object;
    }


    public int getIndex(int index) {
        return Index[index];
    }

    public int getCell(int index) {
        return cellHash[index];
    }

    public int getCount() {
        return offset;
    }

    public static SpatialHandle GetNew(Object object) {
        return new SpatialHandle(object);
    }


}
