/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

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
 *
 * @version This is $Revision: 1.5.4.3 $
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
 
