/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.services;

import org.apache.avalon.services.Store;


/**
 * Interface for an object which provides MailRepositories or SpoolRepositories
 *
 * <p>The select method requires a configuration object with the form:
 *  <br><repository destinationURL="file://path-to-root-dir-for-repository"
 *  <br>            type="MAIL"
 *  <br>            model="SYNCHRONOUS"/>
 *  <br></repository>
 * <p>This configuration, including any included child elements, is used to configure the returned component.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public interface MailStore extends Store {

    // MailRepository getInbox(String user);

}
 
