/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.spatial;



/**
 *
 * @author mads
 */
public class Cell {
    public final static int CELLSIZE = 96;
    public final static float CELLSIZEF = CELLSIZE;
    private int x, y;

    public static int GetMinCell(float value) {
        return (int)Math.floor(value/CELLSIZEF) ;
    }

    public static int GetMaxCell(float value) {
        return (int)Math.floor(value/CELLSIZEF)+1;
    }

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void Set(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public int hashCode() {
        //int extra = (y < 0 ? 1 : 0) | (x < 0 ? 2 : 0);
        int neg = y<0?1:0;
        neg |= x<0?2:0;
        long hags = ((x & 0xffff) << 12 ) | (y & 0xfff) | (neg << 28);
        
        return (int)hags;
    }

    @Override
    public String toString() {
        return "Cell[" + x + "," + y + "] (hash: " + hashCode() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return obj.hashCode() == hashCode();
    }
}
