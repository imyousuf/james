/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport;

import org.apache.mail.*;
import java.util.*;
import org.apache.avalon.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public abstract class AbstractMatcher implements Matcher {

    private MailetContext context;

    public abstract void init(String condition);

    public abstract Collection match(Mail mail);

    public MailetContext getContext() {
        return context;
    }

    public void setMailetContext(MailetContext context) {
        this.context = context;
    }

}

