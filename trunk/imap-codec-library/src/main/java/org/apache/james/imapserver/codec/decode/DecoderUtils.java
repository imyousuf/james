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

package org.apache.james.imapserver.codec.decode;

import javax.mail.Flags;

import org.apache.james.api.imap.message.MessageFlags;

/**
 * Utility procedures.
 */
public final class DecoderUtils {

    public static void setFlag( final String flagString, final Flags flags )  {
        if ( flagString.equalsIgnoreCase( MessageFlags.ANSWERED ) ) {
            flags.add(Flags.Flag.ANSWERED);
        }
        else if ( flagString.equalsIgnoreCase( MessageFlags.DELETED ) ) {
            flags.add(Flags.Flag.DELETED);
        }
        else if ( flagString.equalsIgnoreCase( MessageFlags.DRAFT ) ) {
            flags.add(Flags.Flag.DRAFT);
        }
        else if ( flagString.equalsIgnoreCase( MessageFlags.FLAGGED ) ) {
            flags.add(Flags.Flag.FLAGGED);
        }
        else if ( flagString.equalsIgnoreCase( MessageFlags.SEEN ) ) {
            flags.add(Flags.Flag.SEEN);
        } else {
            if ( flagString.equalsIgnoreCase( MessageFlags.RECENT) ) {
                // RFC3501 specifically excludes /Recent 
                // The /Recent flag should be set automatically by the server
            } else {
                // RFC3501 allows novel flags
                flags.add(flagString);
            }
        }
    }
}
