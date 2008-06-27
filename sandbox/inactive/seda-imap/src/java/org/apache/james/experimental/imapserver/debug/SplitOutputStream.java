package org.apache.james.experimental.imapserver.debug;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SimpleLog;

public class SplitOutputStream extends FilterOutputStream {

    private OutputStream debugOutputStream;

    StringBuffer logString = new StringBuffer();

    private boolean DEEP_DEBUG = false;

    private Log log;

    public SplitOutputStream(OutputStream out, OutputStream debug) {
        super(out);
        debugOutputStream = debug;
    }

    public void flush() throws IOException {
        super.flush();
        if (debugOutputStream != null) {
            debugOutputStream.flush();
        }
    }

    public void write(int b) throws IOException {
        super.write(b);
        if (DEEP_DEBUG) {
            if (b == 10) {
                getLog().debug(logString);
                logString = new StringBuffer();
            } else if (b != 13) {
                logString.append((char) b);
            }
        }
        if (debugOutputStream != null) {
            debugOutputStream.write(b);
            debugOutputStream.flush();
        }
    }

    public void setLog(Log log) {
        this.log = log;
    }

    protected Log getLog() {
        if (log == null) {
            log = new SimpleLog("SplitOutputStream");
        }
        return log;
    }

}
