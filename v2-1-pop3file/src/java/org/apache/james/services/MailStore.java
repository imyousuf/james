/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.services;

import org.apache.avalon.cornerstone.services.store.Store;

/**
 * Interface for an object which provides MailRepositories or SpoolRepositories
 *
 * <p>The select method requires a configuration object with the form:
 *  <br>&lt;repository destinationURL="file://path-to-root-dir-for-repository"
 *  <br>            type="MAIL"&gt;
 *  <br>&lt;/repository&gt;
 * <p>This configuration, including any included child elements, is used to 
 * configure the returned component.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 *
 * @version This is $Revision: 1.5 $
 */
public interface MailStore 
    extends Store {

    /**
     * The component role used by components implementing this service
     */
    String ROLE = "org.apache.james.services.MailStore";

    // MailRepository getInbox(String user);

    /**
     * Convenience method to get the inbound spool repository.
     */
    SpoolRepository getInboundSpool();

}
 
