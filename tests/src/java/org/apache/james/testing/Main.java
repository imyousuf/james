/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.testing;

import java.io.File;
import java.lang.reflect.Constructor;

import junit.extensions.ActiveTestSuite;
import junit.extensions.RepeatedTest;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;

/**
 * Run tests. 
 * - Reads test configuration file, constructs test suite
 * - initiates tests, reports result.
 */
public class Main {

    /**
     * Main method used to invoke tests.  Sole argument should be
     * a configuration file with test suite configuration.
     *
     * @param args Sole argument should be a configuration file
     *             with test suite configuration
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Usage java org.apache.james.testing.Main <testconfigfile>");

        File testconfFile = new File(args[0]);
        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
        Configuration alltestconf = builder.buildFromFile(testconfFile);
        Configuration[] testconf = alltestconf.getChildren("test");
        TestSuite alltests = new TestSuite();
        for ( int i = 0 ; i < testconf.length ; i++ ) {
            Configuration conf = testconf[i];
            String clsname = conf.getAttribute("class");
            String name = conf.getAttribute("name");
            int repetition = conf.getAttributeAsInteger("repetition");
            boolean async = conf.getAttributeAsBoolean("async");

            Class clazz = Class.forName(clsname);
            Constructor cstr = clazz.getConstructor(new Class[] { String.class });
            Test test = (Test)cstr.newInstance(new Object[] {name});
            if ( test instanceof Configurable ) {
                ((Configurable)test).configure(conf);
            }

            if (repetition > 1) {
                test = new RepeatedTest(test,repetition);
            }

            if ( async ) {
                TestSuite ts = new ActiveTestSuite();
                ts.addTest(test);
                test = ts;
            }
            alltests.addTest(test);
        }
        TestRunner.run(alltests);
    }
}
