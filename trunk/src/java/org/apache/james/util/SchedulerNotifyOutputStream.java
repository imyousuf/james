/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */

package org.apache.james.util;

import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This will reset the scheduler each time a certain amount of data has
 * been transfered.  This allows us to keep the timeout settings low, while
 * not timing out during large data transfers.
 */
public class SchedulerNotifyOutputStream extends OutputStream {
    OutputStream out = null;
    TimeScheduler scheduler = null;
    String triggerName = null;
    int lengthReset = 0;

    int writtenCounter = 0;

    public SchedulerNotifyOutputStream(OutputStream out,
            TimeScheduler scheduler, String triggerName, int lengthReset) {
        this.out = out;
        this.scheduler = scheduler;
        this.triggerName = triggerName;
        this.lengthReset = lengthReset;

        writtenCounter = 0;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        writtenCounter += len;

        if (writtenCounter > lengthReset) {
            writtenCounter -= lengthReset;
            scheduler.resetTrigger(triggerName);
        }
    }

    public void write(int b) throws IOException {
        out.write(b);
        writtenCounter++;

        if (writtenCounter > lengthReset) {
            writtenCounter -= lengthReset;
            scheduler.resetTrigger(triggerName);
        }
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void close() throws IOException {
        out.close();
    }
}
