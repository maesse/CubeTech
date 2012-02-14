package cubetech.misc;

/**
 *
 * @author Mads
 */
public interface Callback<E, V> {
    public V execute(E e, Object tag);
}
