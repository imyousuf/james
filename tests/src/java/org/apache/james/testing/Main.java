/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

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

        for (int i = 0; i < testconf.length; i++) {
            Configuration conf = testconf[i];
            String clsname = conf.getAttribute("class");
            String name = conf.getAttribute("name");
            int repetition = conf.getAttributeAsInteger("repetition");
            boolean async = conf.getAttributeAsBoolean("async");

            Class clazz = Class.forName(clsname);
            Constructor cstr = clazz.getConstructor(new Class[] { String.class });
            Test test = (Test) cstr.newInstance(new Object[] {name});

            if (test instanceof Configurable) {
                ((Configurable) test).configure(conf);
            }

            if (repetition > 1) {
                test = new RepeatedTest(test, repetition);
            }

            if (async) {
                TestSuite ts = new ActiveTestSuite();

                ts.addTest(test);
                test = ts;
            }
            alltests.addTest(test);
        }
        TestRunner.run(alltests);
    }
}
