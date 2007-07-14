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

package org.apache.james.imapserver.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.mail.internet.MimeMessage;

public class MessageSet {
    
    private final long from;
    private final long to;
    private boolean useUid=false;
    private long[] uids;
    private MimeMessage[] msgs;
    private ArrayList selectedMessageNumbers;

    public MessageSet(MimeMessage[] msgs,long[] uids,long from,long to) {
        this(msgs,from,to);
        this.uids=uids;
        this.useUid=true;
    }
    public MessageSet(MimeMessage[] msgs,long[] uids,long number) {
        this(msgs,number);
        this.uids=uids;
        this.useUid=true;
    }
    
    public MessageSet(MimeMessage[] msgs,long from,long to) {
        this(from,to);
        this.msgs=msgs;
    }
    public MessageSet(MimeMessage[] msgs,long number) {
        this(number);
        this.msgs=msgs;
    }
    public MessageSet(long from,long to) {
        this.from=from;
        this.to=to;
    }
    public MessageSet(long number) {
        this.from=-1;
        this.to=number;
    }
    
    public MessageSet(MimeMessage[] msgs, long from, long to, int[] numbers, long[] uids, boolean useUid) {
        if (useUid) {
            from=uids[(int)from-1];
            to=uids[(int)to-1];
        }
        this.msgs=msgs;
        this.from=from;
        this.to=to;
        this.uids=uids;
        this.useUid=useUid;
        selectedMessageNumbers=new ArrayList(numbers.length);
        for (int i = 0; i < numbers.length; i++) {
            selectedMessageNumbers.add(new Integer(numbers[i]));
        }

    }
    public String toString() {
        String result="";
        if (from>0)  {
            result += from +":";
        }
        if (to>0) {
            result += to;
        } else {
            result += "*";
        }
        return result;
    }
    
    public boolean isUid() {
        return useUid;
    }
    
    public List getSelectedMessageNumbers() {
        if (selectedMessageNumbers==null) {
            selectedMessageNumbers=new ArrayList();
            if (isUid()) {
                final long from;
                if (this.from>0) {
                    from=this.from;
                } else {
                    from=this.to;
                }
                final long to;
                if (this.to>0) {
                    to=this.to;
                } else {
                    to=Long.MAX_VALUE;
                }
                for (int i=0; i< msgs.length; i++) {
                    if (uids[i]>=from && uids[i]<=to)  {
                        selectedMessageNumbers.add(new Integer((int)i+1));
                    }
                }

            } else {
                final long from;
                if (this.from > 0) {
                    from = this.from;
                } else {
                    from = this.to;
                }

                final long to;
                if (this.to > 0) {
                    if (this.to > msgs.length) {
                        to = msgs.length;
                    } else {
                        to = this.to;
                    }
                } else {
                    to = msgs.length;
                }

                for (long i = from; i <= to; i++) {
                    selectedMessageNumbers.add(new Integer((int)i));
                }
            }
        }
        return selectedMessageNumbers;
    }
    public MimeMessage getMessage(int no) {
        return msgs[no-1];
    }
    public long getUid(int no) {
        return uids[no-1];
    }

}
