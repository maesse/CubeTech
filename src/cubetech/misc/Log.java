package cubetech.misc;

import cubetech.common.Common;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.EventListenerList;

/**
 *
 * @author mads
 */
public class Log {

    static protected EventListenerList listenerList = new EventListenerList();
    private static PrintStream stdout = null;
    private static boolean hasInit = false;

    private static boolean logToFile = false;
    private static FileWriter fileHandle = null;
    public static String lineSeparator;
    

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

    private static void initLogfile() {
        File file = new File("logs/");
        if(!file.exists() || !file.isDirectory())
            if(!file.mkdir()) {
                Log("Couldn't create log directory. Not logging to file.");
                return;
            }

        // Create a new log file
        SimpleDateFormat derp = new SimpleDateFormat("dd-MM-yy_HH-mm-ss");
        String str = derp.format(Calendar.getInstance().getTime());
        file = new File("logs/" + str + ".txt");
        try {
            FileWriter osw = new FileWriter(file);
            fileHandle = osw;
            logToFile = true;
        } catch (IOException ex) {
            Log(Common.getExceptionString(ex));
            return;
        }
    }

    public static void Init(boolean isApplet) {
        if(hasInit)
            return;
        hasInit = true;

        lineSeparator = System.getProperty("line.separator");

        // Save console output stream
        stdout = System.out;
        LoggingOutputStream los = new LoggingOutputStream();
        System.setOut(new PrintStream(los, true));

        if(!isApplet)
            initLogfile();
    }

    public static void Log(String str) {
        stdout.println(str);
        if(logToFile) {
            try {
                fileHandle.write(str);
                fileHandle.write(lineSeparator);
                fileHandle.flush();
            } catch (IOException ex) {
                Log(Common.getExceptionString(ex));
                logToFile = false;
            }

        }
        FireLogEvent(str);
    }
}
