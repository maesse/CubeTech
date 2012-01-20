package cubetech.input;


/**
 *
 * @author mads
 */
public class KeyEvent {
    private Key key;
    public KeyEvent(Key source) {
        key = source;
    }
    
    public Key getSource() {
        return key;
    }
}
