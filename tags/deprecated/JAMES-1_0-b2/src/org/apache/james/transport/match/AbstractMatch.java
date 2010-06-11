/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.match;

import org.apache.mail.Mail;
import java.util.*;
import org.apache.arch.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public abstract class AbstractMatch implements Match {
    
    public abstract Vector match(Mail mail, String condition);
    
    public void setComponentManager(ComponentManager comp) {
    }

    public void setContext(Context context) {
    }
}
    
