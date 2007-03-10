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

package org.apache.james.mailboxmanager.util;

public class UidToKeyConverterImpl extends AbstractLogFactoryAware implements
        UidToKeyConverter {

    public static final String PREFIX = "JAMES-UID-KEY-";

    private long uidValidity;

    public void setUidValidity(long uidValidity) {
        this.uidValidity = uidValidity;
    }

    public String toKey(long uid) {
        return PREFIX + "(" + uid + ";" + uidValidity + ")";
    }

    public Long toUid(String key) {
        if (key != null) {
            if (key.startsWith(PREFIX + "(")) {
                key = key.substring(PREFIX.length() + 1);
                if (key.endsWith(")")) {
                    key = key.substring(0, key.length() - 1);
                    int pos = key.indexOf(';');
                    if (pos > 0 && pos < key.length() - 1) {
                        try {
                            long theUid = Long.parseLong(key.substring(0, pos));
                            long theUidValidity = Long.parseLong(key
                                    .substring(pos + 1));
                            if (theUidValidity == uidValidity) {
                                return new Long(theUid);
                            } else {
                                getLog().debug(
                                        "toUid: uidValidity mismatch. Expected: "
                                                + uidValidity + " was "
                                                + theUidValidity);
                            }
                        } catch (NumberFormatException e) {
                            getLog().debug("toUid", e);
                        }
                    } else {
                        getLog().debug(
                                "toUid: failed. pos=" + pos + " length="
                                        + key.length());
                    }
                } else {
                    getLog().debug("toUid: failed. did not end with ')'");
                }
            } else {
                getLog().debug("toUid: failed. did not start with PREFIX");
            }
        } else {
            getLog().debug("toUid: failed. key is null");
        }
        return null;
    }

}
