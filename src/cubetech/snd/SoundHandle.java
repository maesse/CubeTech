/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.snd;

/**
 *
 * @author mads
 */
public class SoundHandle {
    private Integer handle;
    public Integer getHandle() {
        return handle;
    }

    SoundHandle(Integer handle) {
        this.handle = handle;

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SoundHandle other = (SoundHandle) obj;
        if (this.handle != other.handle && (this.handle == null || !this.handle.equals(other.handle))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + (this.handle != null ? this.handle.hashCode() : 0);
        return hash;
    }
}
