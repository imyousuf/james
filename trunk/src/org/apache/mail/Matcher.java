/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.mail;



/**
 * This interface define the behaviour of the "routing" inside the processor pipe.
 * The match(Mail) method return an array of two Mail object. The [0] is a reference 
 * to the Mail matching condition, the [1] is the one not matching.
 * 
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public interface Matcher {
    
    public void init(String condition);
    
    public Mail[] match(Mail mail);

    public MailetContext getContext();
}
    
