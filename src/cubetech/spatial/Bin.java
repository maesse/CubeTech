/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.spatial;

import cubetech.Block;
import cubetech.common.Common;
import java.util.ArrayList;

/**
 * A Bin containing collidables
 * @author mads
 */
public class Bin {
    public final static int BIN_SIZE = 256;
    
    private int Offset = 0;
    private Object[] Data = new Object[BIN_SIZE];
    private Cell cell; // The cell that points to this bin

    public Bin(Cell cell) {
        this.cell = cell;
    }

    public int Insert(Object object) {
//        System.out.println("Bin: Insert");
        Data[Offset] = object;
        return Offset++;
    }

    public void GetData(SpatialQuery query) {
        if(Offset == 0)
            return;
        query.Insert(Data, Offset);
    }

    public Cell getCell() {
        return cell;
    }

    public void getBlocks(ArrayList<Block> dest) {
        for (int i = 0; i < Offset; i++) {
            dest.add((Block)Data[i]);
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("[");
        for (int i= 0; i < Offset; i++) {
            str.append(((Block)Data[i]).Handle);
            if(i<Offset-1)
                str.append(",");
        }
        str.append("]");
        return String.format("Bin[%s, size:%d, handles: %s]", cell, Offset, str.toString());
    }

    public void Remove(int index) {
//        System.out.println("Bin: remove");
        if(index < 0 || index >= Offset)
        {
            System.err.println("Bin.Remove() Index invalid: " + index);
            return;
        }

        // Removing tail
        if(index == Offset-1) {
//            Common.LogDebug("  -bin: " + toString() + " (tail)");
            Offset--;
            return;
        }

        // Put tail in index position and decrement size

        //Common.LogDebug("  -bin: " + toString());
        Data[index] = Data[Offset-1];
        ((Block)Data[index]).SpatialHandleChanged(cell, index);
        Offset--;
    }
}
