/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.smtpserver;

import java.util.Date;
import org.apache.arch.*;

/**
 * Singleton that provides unique identifier.
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */ 
public class IdProvider implements Component {
    
    private static long count;
    
    private String name;
    
    public IdProvider(String baseName) {
        this.name = baseName;
    }
    
    public String getMessageId() {
        return new String(new Date().getTime() + "." + count++ + "@" + name);
    }
}    

        