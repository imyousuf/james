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

package org.apache.james.security.openpgp;

/**
 * Utility methods.
 *
 */
public class OpenPGPUtils {

    
    /**
     * Converts the given string into an array of bytes
     * without performing platform dependent encoding.
     * Takes only the bottom 8 bits.
     * @param string <code>String</code>, not null
     * @return <code>byte</code> array, not null
     */
    public static final byte[] toBytes(final String string) {
        final char[] characters = string.toCharArray();
        final int length = characters.length;
        final byte[] results = new byte[length];
        for (int i=0;i<length;i++) {
            results[i] = (byte) characters[i];
        }
        return results;
    }
    
    /**
     * Converts an array of 8 bit bytes into a string
     * without performing platform dependent encoding.
     * @param bytes <code>byte</code> array, not null
     * @return <code>String</code> representation
     */
    public static final String toString(final byte[] bytes) {
        final int length = bytes.length;
        final char[] characters = new char[length];
        for (int i=0;i<length;i++) {
            characters[i] = (char) (bytes[i] & 255);
        }
        final String result = new String(characters);
        return result;
    }
}
