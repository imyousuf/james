/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.testing;

import junit.framework.TestCase;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

/**
 * Base Test class. Can run a set of tests.
 */
public class BaseTest extends TestCase implements Configurable {

    private TestMethod[] testMethod;
    private boolean showStat;

    public BaseTest(String name) {
        super(name);
    }

    public void configure(Configuration configuration) 
        throws ConfigurationException 
    {
        // list of test methods
        Configuration testseq = configuration.getChild("testsequence");
        showStat = testseq.getAttributeAsBoolean("showStat");
        Configuration[] testconf = testseq.getChildren("testmethod");
        testMethod = new TestMethod[testconf.length];
        for ( int i = 0 ; i < testMethod.length ; i++ )
            testMethod[i] = new TestMethod(testconf[i].getValue());
    }

    public final int countTestCases() {
        return testMethod.length;
    }

    protected final void runTest() throws Throwable {
        for ( int i = 0 ; i < testMethod.length ; i++ ) {
            testMethod[i].invoke(this);
            if ( showStat )
                System.out.println("stat: "+getName()+", "+testMethod[i]);
        }
    }
}
