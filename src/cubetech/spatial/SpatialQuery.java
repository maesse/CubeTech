/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.spatial;

/**
 *
 * @author mads
 */
public class SpatialQuery {
    final static int MAXSIZE = 1000;

    Object[][] BinData = new Object[MAXSIZE][Bin.BIN_SIZE];
    int[]    BinSizes = new int[MAXSIZE];
    int Offset = 0;
    int ReadOffset = 0;
    int ReadBinOffset = 0;
    int QueryNum = 0;

//    private SpatialQuery() {
//
//    }

    public void Reset() {
        Offset = 0;
        ReadOffset = 0;
        ReadBinOffset = 0;
        QueryNum++;
    }

    public void Insert(Object[] data, int size) {
        if(Offset >= MAXSIZE) {
            System.err.println("SpatialQuery.Insert() Offset overflow");
            return;
        }
        BinData[Offset] = data;
        BinSizes[Offset] = size;
        Offset++;
    }

    // Might return the same object multiple times
    public Object ReadNext() {
        if(ReadOffset == Offset)
            return null; // Reached end

        // Switch to next bin
        if(ReadBinOffset >= BinSizes[ReadOffset])
        {
            ReadBinOffset = 0;
            ReadOffset++;

            if(ReadOffset == Offset)
                return null; // Reached end
        }

        // Return element
        return BinData[ReadOffset][ReadBinOffset++];
    }

    public int getQueryNum() {
        return QueryNum;
    }

}
