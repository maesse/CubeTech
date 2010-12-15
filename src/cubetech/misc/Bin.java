/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

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
        System.out.println("Bin: Insert");
        Data[Offset] = object;
        return Offset++;
    }

    public void GetData(SpatialQuery query) {
        query.Insert(Data, Offset);
    }

    public Cell getCell() {
        return cell;
    }

    public void Remove(int index) {
        System.out.println("Bin: remove");
        if(index < 0 || index >= BIN_SIZE)
        {
            System.err.println("Bin.Remove() Index invalid: " + index);
            return;
        }

        // Removing tail
        if(index == Offset-1) {
            Offset--;
            return;
        }

        // Put tail in index position and decrement size
        Data[index] = Data[Offset-1];
        Offset--;
    }
}
