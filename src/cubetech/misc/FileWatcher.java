/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import cubetech.common.IThinkMethod;
import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author mads
 */
public class FileWatcher {
    private static TimerTask task;
    private static ArrayList<FileWatcher> watchers = new ArrayList<FileWatcher>();
    private static Timer timer;

    private long timestamp;
    private File file;
    private IThinkMethod think;

    public FileWatcher(File file, IThinkMethod onchange) {
        this.file = file;
        this.timestamp = file.lastModified();
        this.think = onchange;
        
        if(task == null) {
            initTask();
        }

        watchers.add(this);
    }

    private static void initTask() {
        task = new TimerTask() {
            @Override
            public void run() {
                for (FileWatcher fileWatcher : watchers) {
                    fileWatcher.run();
                }
            }
        };

        timer = new Timer("FileWatcher");
        timer.scheduleAtFixedRate(task, 1000,1000);
    }

    public final void run() {
        long timestamp = file.lastModified();
        if(this.timestamp != timestamp) {
            this.timestamp = timestamp;
            think.think(null);
        }
    }


}
