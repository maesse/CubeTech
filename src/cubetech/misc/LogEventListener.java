package cubetech.misc;

import java.util.EventListener;

/**
 *
 * @author mads
 */
public interface LogEventListener extends EventListener {
    public void HandleLogLine(String str);

}
