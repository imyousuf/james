/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.matchers;

import org.apache.mailet.*;
import java.util.*;

/**
 * @version 1.0.0, 04/12/2000
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class IsSingleRecipient extends GenericMatcher {

    public Collection match(Mail mail) {
        if (mail.getRecipients().size() == 1) {
            return mail.getRecipients();
        } else {
            return null;
        }
    }
}
