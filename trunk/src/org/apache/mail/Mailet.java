/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.mail;

import org.apache.avalon.*;

/**
 * Draft of a Mailet inteface. The <code>service</code> perform all needed work
 * on the Mail object. Whatever remains at the end of the service is considered
 * to need futher processing and will go to the next Mailet if there is one
 * configured or will go to the error processor if not.
 * Setting a Mail state (setState(String)) to Mail.GOHST or cleaning its recipient
 * list has the same meaning that s no more processing is needed.
 * The service should NEVER add recipients to the Mail passed. Instead a new Mail
 * object should be created and delivered using the transport API.
 * API are provided trought the MailetContext interface.
 *
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri   <scoobie@pop.systemy.it>
 * @author  Stefano Mazzocchi   <stefano@apache.org>
 * @author  Pierpaolo Fumagalli <pier@apache.org>
 * @author  Serge Knystautas    <sergek@lokitech.com>
 */
public interface Mailet extends Service {

    public void service(Mail mail) throws Exception;

    public String getMailetInfo();

    public MailetContext getContext();
}


