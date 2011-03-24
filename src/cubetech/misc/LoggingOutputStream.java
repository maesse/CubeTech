/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 *
 * @author mads
 */
public class LoggingOutputStream extends ByteArrayOutputStream {
    String lineSeparator;
    public LoggingOutputStream() {
        lineSeparator = System.getProperty("line.separator");
    }

    @Override
    public void flush() throws IOException {
        String record;
        synchronized(this) {
            super.flush();
            record = this.toString();
            super.reset();

            if (record.length() == 0 || record.equals(lineSeparator)) {
                // avoid empty records
                return;
            }

            Log.Log(record);
        }

    }
}
