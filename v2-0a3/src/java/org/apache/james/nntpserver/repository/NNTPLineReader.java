/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;



/**
 * Read and translates client data.
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
public interface NNTPLineReader {
    /** 
     * reads a line of data.
     * @return null indicates end of data
     */
    String readLine();
}
