/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
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
    extends ACL, Mailbox, Contextualizable, Initializable, Disposable {

    /**
     * Set the details particular to this Mailbox. Should only be called once,
     * at creation, and not when restored from storage.
     *
     * @param user String email local part of owner of a personal mailbox.
     * @param abName String absolute, ie user-independent, name of mailbox.
     * @param initialAdminUser String email local-part of a user who will be assigned admin rights on this mailbox
     */
    void prepareMailbox( String user, String absName, String initialAdminUser );

    /**
     * Re-initialises mailbox when restored from storage. Must be called after
     * setConfiguration, setContext, setComponentManager, if they are called,
     * but before any opertional methods are called.
     */
    void reinitialize()
        throws Exception;
}


