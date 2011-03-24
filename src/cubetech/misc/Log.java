package cubetech.misc;

import java.io.PrintStream;
import javax.swing.event.EventListenerList;

/**
 *
 * @author mads
 */
public class Log {

    static protected EventListenerList listenerList = new EventListenerList();
    private static PrintStream stdout = null;
    private static boolean hasInit = false;

    public static void AddLogListener(LogEventListener listener) {
        listenerList.add(LogEventListener.class, listener);
    }

    public static void RemoveLogListener(LogEventListener listener) {
        listenerList.remove(LogEventListener.class, listener);
    }

    private static void FireLogEvent(String log) {
        Object[] listeners = listenerList.getListenerList();
        for (int i= 0; i < listeners.length; i++) {
            if(listeners[i] == LogEventListener.class) {
                ((LogEventListener)listeners[i+1]).HandleLogLine(log);
            }
        }
    }

    public static void Init() {
        if(hasInit)
            return;
        hasInit = true;

        // Save console output stream
        stdout = System.out;
        LoggingOutputStream los = new LoggingOutputStream();
        System.setOut(new PrintStream(los, true));
    }

    public static void Log(String str) {
        stdout.println(str);
        FireLogEvent(str);
    }
}
