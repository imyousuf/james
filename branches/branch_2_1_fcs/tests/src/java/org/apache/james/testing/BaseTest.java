/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
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
