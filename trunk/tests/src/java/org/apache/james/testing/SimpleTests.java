/**
 * AllTests.java
 *
 * Copyright (C) 06-Jan-2003 The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 *
 */package org.apache.james.testing;
import junit.framework.Test;import junit.framework.TestSuite;/** * * $Id: SimpleTests.java,v 1.2 2003/01/27 02:15:31 serge Exp $ */
public class SimpleTests {
    public static void main(String[] args) {        junit.textui.TestRunner.run(SimpleTests.class);    }
    public static Test suite() {        TestSuite suite = new TestSuite("Test for org.apache.james.testing");
        // $JUnit-BEGIN$        suite.addTest(new TestSuite(DeliveryTests.class));        // $JUnit-END$
        return suite;
    }
}