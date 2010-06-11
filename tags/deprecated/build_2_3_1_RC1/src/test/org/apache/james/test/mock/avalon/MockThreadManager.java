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
package org.apache.james.test.mock.avalon;

import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.avalon.excalibur.thread.impl.DefaultThreadPool;
import org.apache.excalibur.thread.ThreadPool;

public class MockThreadManager implements ThreadManager {
    public ThreadPool getThreadPool(String string) throws IllegalArgumentException {
        return getDefaultThreadPool();
    }

    public ThreadPool getDefaultThreadPool() {
        try {
            DefaultThreadPool defaultThreadPool = new DefaultThreadPool(1);
            defaultThreadPool.enableLogging(new MockLogger());
            return defaultThreadPool;
        } catch (Exception e) {
            e.printStackTrace();  
            return null;
        }
    }
}
