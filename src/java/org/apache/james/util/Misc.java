package org.apache.james.util;

/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/


import java.util.Random;

import javax.mail.MessagingException;

import org.apache.mailet.Mail;

// Not sure about the name yet
// TODO: Rename
public class Misc {

    private Misc() {}	
    
    
    /**
     * Create a unique new primary key name for the given MailObject.
     *
     * @param mail the mail to use as the basis for the new mail name
     * @return a new name
     */
    public static String newName(Mail mail,Random random) throws MessagingException {
        String oldName = mail.getName();
        
        // Checking if the original mail name is too long, perhaps because of a
        // loop caused by a configuration error.
        // it could cause a "null pointer exception" in AvalonMailRepository much
        // harder to understand.
        if (oldName.length() > 76) {
            int count = 0;
            int index = 0;
            while ((index = oldName.indexOf('!', index + 1)) >= 0) {
                count++;
            }
            // It looks like a configuration loop. It's better to stop.
            if (count > 7) {
                throw new MessagingException("Unable to create a new message name: too long."
                                             + " Possible loop in config.xml.");
            }
            else {
                oldName = oldName.substring(0, 76);
            }
        }
        
        StringBuffer nameBuffer =
                                 new StringBuffer(64)
                                 .append(oldName)
                                 .append("-!")
                                 .append(random.nextInt(1048576));
        return nameBuffer.toString();
    }
}
