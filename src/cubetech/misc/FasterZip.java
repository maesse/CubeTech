package cubetech.misc;

import cubetech.common.Common;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;

/**
 *
 * @author mads
 */
public class FasterZip extends Thread {
    private class ZipJob {
        byte[] data;
        String filename;
    }
    
    private FastZipOutputStream zipper = null;
    private final List<ZipJob> jobqueue = Collections.synchronizedList(new ArrayList<ZipJob>());
    private boolean doexit = false;
    private boolean flipflop = true;
    
    public FasterZip(OutputStream out) {
        zipper = new FastZipOutputStream(out);
    }

    public int getJobCount() {
        return jobqueue.size();
    }

    @Override
    public void run() {
        while(!doexit || !jobqueue.isEmpty()) {
            if(jobqueue.isEmpty()) {
                // Wait for job
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    Common.Log(Common.getExceptionString(ex));
                }
            } else {
                // Grab job
                ZipJob job = null;
                synchronized(jobqueue) {
                    job = jobqueue.remove(0);
                }
                
                // Write zip entry
                ZipEntry entry = new ZipEntry(job.filename);
                entry.setMethod(flipflop?ZipEntry.DEFLATED:ZipEntry.STORED);
                if(!flipflop) {
                    entry.setSize(job.data.length);
                    entry.setCrc(0);
                }
                flipflop = !flipflop;
                try {
                    zipper.putNextEntry(entry);
                    zipper.write(job.data);
                    zipper.closeEntry();
                } catch (IOException ex) {
                    Common.Log(Common.getExceptionString(ex));
                    // todo: stop playback
                }
            }
        }
        
        try {
            zipper.close();
        } catch (IOException ex) {
            Common.Log(Common.getExceptionString(ex));
        }
        Common.Log("FasterZip Thread Finished");
    }

    public void enqueueFile(byte[] indata, String inname) {
        ZipJob job = new ZipJob();
        job.data = indata;
        job.filename = inname;
        jobqueue.add(job);
        synchronized(this) {
            notify();
        }
    }

    public void close() {
        doexit = true;
    }
}
