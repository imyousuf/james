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

package org.apache.james.imapserver;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.context.Contextualizable;

/**
 * Interface for objects representing an IMAP4rev1 mailbox (folder) with
 * embedded Access Control List.
 *
 * Reference: RFC 2060
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 * @see Mailbox
 * @see ACL
 */
public interface ACLMailbox
    extends ACL, Mailbox, Component, Contextualizable, Initializable, Disposable {

    /**
     * Set the details particular to this Mailbox. Should only be called once,
     * at creation, and not when restored from storage.
     *
     * @param user String email local part of owner of a personal mailbox.
     * @param abName String absolute, ie user-independent, name of mailbox.
     * @param initialAdminUser String email local-part of a user who will be assigned admin rights on this mailbox
     */
    void prepareMailbox( String user, String absName, String initialAdminUser, int uidValidity );

    /**
     * Re-initialises mailbox when restored from storage. Must be called after
     * setConfiguration, setContext, setComponentManager, if they are called,
     * but before any opertional methods are called.
     */
    void reinitialize()
        throws Exception;

    /**
     * Permanently deletes the mailbox.
     */
    void removeMailbox();
}


