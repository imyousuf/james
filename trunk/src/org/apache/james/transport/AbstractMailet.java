/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport;

import org.apache.avalon.*;
import org.apache.mail.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri   <scoobie@pop.systemy.it>
 * @author  Stefano Mazzocchi   <stefano@apache.org>
 * @author  Pierpaolo Fumagalli <pier@apache.org>
 * @author  Serge Knystautas    <sergek@lokitech.com>
 */
public abstract class AbstractMailet implements Mailet {

    private MailetContext context;

    protected void setMailetContext(MailetContext context) {
        this.context = context;
    }

    public MailetContext getContext() {
        return context;
    }

    public void init() throws Exception {
    }

    public abstract void service(Mail mail) throws Exception;

    public void destroy() {
    }

    public abstract String getMailetInfo();
}


