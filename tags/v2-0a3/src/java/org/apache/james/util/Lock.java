/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.util;

import java.util.Hashtable;

/**
 * @author Federico Barbieri <fede@apache.org>
 */
public class Lock {
    private Hashtable locks = new Hashtable();

    public boolean isLocked(final Object key) {
        return (locks.get(key) != null);
    }

    public boolean canI(final Object key) {
        Object o = locks.get( key );

        if (null == o || o == this.getCallerId()) {
            return true;
        }

        return false;
    }

    public boolean lock(final Object key) {
        Object theLock;

        synchronized(this) {
            theLock = locks.get(key);

            if (null == theLock) {
                locks.put(key, getCallerId());
                return true;
            } else if (getCallerId() == theLock) {
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean unlock(final Object key) {
        Object theLock;
        synchronized (this) {
            theLock = locks.get(key);

            if (null == theLock) {
                return true;
            } else if (getCallerId() == theLock) {
                locks.remove(key);
                return true;
            } else {
                return false;
            }
        }
    }

    private Object getCallerId() {
        return Thread.currentThread();
    }
}
