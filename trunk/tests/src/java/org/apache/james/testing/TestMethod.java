/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.testing;

import java.lang.reflect.Method;

class TestMethod {
    // stats
    private int timeTaken;
    private int attempts;
    private int success;
    
    final String name;
    TestMethod(String name) {
        this.name = name;
    }
    
    // invoke test
    void invoke(Object obj) throws Exception {
        Method m = obj.getClass().getMethod(name,new Class[0]);
        attempts++;
        long st = System.currentTimeMillis();
        try {
            m.invoke(obj,new Object[0]);
            success++;
        } finally {
            timeTaken = (int)(System.currentTimeMillis() - st);
        }
    }
    // print report.
    public String toString() {
        return name+", "+timeTaken+", "+success+", "+attempts;
    }
}
