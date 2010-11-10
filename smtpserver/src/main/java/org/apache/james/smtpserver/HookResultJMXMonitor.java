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
package org.apache.james.smtpserver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.hook.HookResult;
import org.apache.james.protocols.smtp.hook.HookResultHook;

/**
 * {@link HookResultHook} implementation which will register a {@link HookStatsMBean} under JMX for every Hook it processed 
 *
 */
public class HookResultJMXMonitor implements HookResultHook {

    private Map<String, HookStats> hookStats = new HashMap<String, HookStats>();
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.smtp.hook.HookResultHook#onHookResult(org.apache.james.protocols.smtp.SMTPSession, org.apache.james.protocols.smtp.hook.HookResult, java.lang.Object)
     */
    public HookResult onHookResult(SMTPSession session, HookResult result,
            Object hook) {
        String hookName = hook.getClass().getName();
        try {
            HookStats stats;
            synchronized (hookStats) {
                stats = hookStats.get(hookName);
                if (stats == null) {
                    stats = new HookStats(hookName);
                    hookStats.put(hookName, stats);
                }
            }
           
            stats.increment(result.getResult());
        } catch (Exception e) {
            session.getLogger().error(
                    "Unable to register HookStats for hook " + hookName, e);
        }

        return result;
    }


    @PreDestroy
    public void dispose() {
        synchronized (hookStats) {
            Iterator<HookStats> stats = hookStats.values().iterator();
            while(stats.hasNext()) {
                stats.next().dispose();
            }
            hookStats.clear();
        }
    }
}
