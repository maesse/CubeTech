/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.collision;

import cubetech.gfx.CubeMaterial;

/**
 *
 * @author mads
 */
public class CubeMaterialCacheEntry {
    private int texid;
    private int offsetId;
    private int sizeId;

    public CubeMaterial material;

    public CubeMaterialCacheEntry(int texid, int offset, int size, CubeMaterial cache) {
        this.texid = texid;
        this.offsetId = offset;
        this.sizeId = size;
        this.material = cache;
    }

    public boolean matches(int texid, int offsetid, int sizeid) {
        return (this.texid == texid && offsetId == offsetid && sizeid == sizeId);
    }
}
