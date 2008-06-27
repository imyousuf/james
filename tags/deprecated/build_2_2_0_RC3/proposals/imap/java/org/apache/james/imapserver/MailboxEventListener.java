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

import java.io.Serializable;
import java.util.EventListener;

/**
 * Interface for objects that need to be informed of changes in a Mailbox.
 *
 * <p>Not currently active in this implementaiton
 * 
 * @version 0.1 on 14 Dec 2000
 */
public interface MailboxEventListener
    extends EventListener, Serializable {

    void receiveEvent( MailboxEvent me );
}
