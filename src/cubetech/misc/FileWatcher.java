package cubetech.misc;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Threaded file watcher
 * @author mads
 */
public final class FileWatcher {
    private static final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                synchronized(lock) {
                    for (FileWatcher fileWatcher : watchers) {
                        fileWatcher.run();
                    }
                }
            }
        };
    private static final Object lock = new Object();
    private static final ArrayList<FileWatcher> watchers = new ArrayList<FileWatcher>();
    private static final Timer timer = new Timer("FileWatcher");
    
    static {
        timer.scheduleAtFixedRate(task, 1000,1000);
    }

    private long timestamp;
    private File file;
    private Callback<File, Void> think;
    private Object tag;

    public FileWatcher(File file, Object tag, Callback<File, Void> onchange) {
        this.file = file;
        this.timestamp = file.lastModified();
        this.think = onchange;
        this.tag = tag;
        
        synchronized(lock) {
            // Check if there already is a watch for this file
            boolean contained = false;
            for (FileWatcher watcher : watchers) {
                if(watcher.file.equals(file)) {
                    contained = true;
                    break;
                }
            }
            if(!contained) {
                watchers.add(this);
            }
        }
    }

    public final void run() {
        long currentTimestamp = file.lastModified();
        if(timestamp != currentTimestamp) {
            timestamp = currentTimestamp;
            think.execute(file, tag);
        }
    }
}
