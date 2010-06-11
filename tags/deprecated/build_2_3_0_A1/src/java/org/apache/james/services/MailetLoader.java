/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
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

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.mailet.Mailet;

import javax.mail.MessagingException;

public interface MailetLoader {

    /**
     * Get a new Mailet with the specified name acting
     * in the specified context.
     *
     * @param matchName the name of the mailet to be loaded
     * @param context the MailetContext to be passed to the new
     *                mailet
     * @throws MessagingException if an error occurs
     */
    public Mailet getMailet(String mailetName, Configuration configuration)
            throws MessagingException;

}
