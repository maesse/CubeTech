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
    public final static float CELLSIZEF = 96;
    private int x, y;

    public static int GetMinCell(float value) {
        return (int)Math.floor(value/CELLSIZEF) ;
    }

    public static int GetMaxCell(float value) {
        return (int)Math.ceil(value/CELLSIZEF);
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
        long hags = (x << 16) | (y & 0xffff);
        return (int)hags;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return obj.hashCode() == hashCode();
    }
}
