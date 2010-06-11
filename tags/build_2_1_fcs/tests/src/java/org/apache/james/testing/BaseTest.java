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
 * Base Test class. Can run a set of method tests.
 */
public class BaseTest extends TestCase implements Configurable {

    /**
     * The method level test cases to be invoked by this test.
     */
    private TestMethod[] testMethod;

    /**
     * Whether the statistics for each test should be printed.
     */
    private boolean showStat;

    /**
     * Sole constructor for the method.
     *
     * @param name the name of the test case.
     */
    public BaseTest(String name) {
        super(name);
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration configuration) 
        throws ConfigurationException {
        // list of test methods
        Configuration testseq = configuration.getChild("testsequence");
        showStat = testseq.getAttributeAsBoolean("showStat", true);
        Configuration[] testconf = testseq.getChildren("testmethod");
        testMethod = new TestMethod[testconf.length];
        for ( int i = 0 ; i < testMethod.length ; i++ ) {
            testMethod[i] = new TestMethod(testconf[i].getValue());
        }
    }

    /**
     * Lists the number of test cases to be invoked by this test
     *
     * @return the number of test cases
     */
    public final int countTestCases() {
        return testMethod.length;
    }

    /**
     * Runs the test, invoking each element of the TestMethod array once.
     * Displays statistics if the showStat configuration param was true.
     */
    protected final void runTest() throws Throwable {
        for ( int i = 0 ; i < testMethod.length ; i++ ) {
            testMethod[i].invoke(this);
            if ( showStat ) {
                System.out.println("stat: "+getName()+", "+testMethod[i]);
            }
        }
    }

    // ------------ helper methods ----------------------

    /** 
     * @param conf Test configuration
     * @param name Child name
     * @return String values of child elements 
     */
    protected String[] getChildrenValues(Configuration conf,String name) throws ConfigurationException {
        Configuration[] childconf = conf.getChildren(name);
        String[] val = new String[childconf.length];
        for ( int i = 0 ; i < childconf.length ; i++ )
            val[i] = childconf[i].getValue();
        return val;
    }
}
