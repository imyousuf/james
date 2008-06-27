/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */

package org.apache.james.util;

import java.io.*;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;

/**
 * This will reset the scheduler each time a certain amount of data has
 * been transfered.  This allows us to keep the timeout settings low, while
 * not timing out during large data transfers.
 */
public class SchedulerNotifyInputStream extends InputStream {
    InputStream in = null;
    TimeScheduler scheduler = null;
    String triggerName = null;
    int lengthReset = 0;

    int readCounter = 0;

    public SchedulerNotifyInputStream(InputStream in,
            TimeScheduler scheduler, String triggerName, int lengthReset) {
        this.in = in;
        this.scheduler = scheduler;
        this.triggerName = triggerName;
        this.lengthReset = lengthReset;

        readCounter = 0;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int l = in.read(b, off, len);
        readCounter += l;

        if (readCounter > lengthReset) {
            readCounter -= lengthReset;
            scheduler.resetTrigger(triggerName);
        }

        return l;
    }

    public int read() throws IOException {
        int b = in.read();
        readCounter++;

        if (readCounter > lengthReset) {
            readCounter -= lengthReset;
            scheduler.resetTrigger(triggerName);
        }

        return b;
    }

    public void close() throws IOException {
        in.close();
    }
}
