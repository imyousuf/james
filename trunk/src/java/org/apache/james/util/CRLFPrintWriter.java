/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */

package org.apache.james.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Lines always end with CRLF. Most protocols require this.
 * Don't want to depend on java and OS to do the right thing.
 *
 * Note: method signatures and description exactly matches base
 * class. No point in repeating javadocs. @see base class for method
 * signatures
 */
public class CRLFPrintWriter extends PrintWriter {
    /** base class has autoFlush variable but it is not exposed */
    private boolean m_autoFlush;

    public CRLFPrintWriter (Writer out) {
        this(out,false);
    }

    public CRLFPrintWriter(Writer out,boolean autoFlush) {
        super(out,autoFlush);
        m_autoFlush = autoFlush;
    }

    public CRLFPrintWriter(OutputStream out) {
        this(out,false);
    }

    public CRLFPrintWriter(OutputStream out, boolean autoFlush) {
        super(out,autoFlush);
        m_autoFlush = autoFlush;
    }

    public void println() {
        try {
            out.write("\r\n");
            if ( m_autoFlush )
                out.flush();
        } catch(IOException ex) {
            throw new RuntimeException(ex.toString());
        }
    }
}