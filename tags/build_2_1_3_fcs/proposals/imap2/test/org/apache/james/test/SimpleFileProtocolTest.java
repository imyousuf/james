/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.james.test;

import java.io.InputStream;

/**
 * A Protocol test which reads the test protocol session from a file. The
 * file read is taken as "<test-name>.test", where <test-name> is the value
 * passed into the constructor.
 * Subclasses of this test can set up {@link #preElements} and {@link #postElements}
 * for extra elements not defined in the protocol session file.
 */
public class SimpleFileProtocolTest
        extends AbstractProtocolTest
{
    private FileProtocolSessionBuilder builder =
            new FileProtocolSessionBuilder();

    /**
     * Sets up a SimpleFileProtocolTest which reads the protocol session from
     * a file of name "<fileName>.test". This file should be available in the classloader
     * in the same location as this test class.
     * @param fileName The name of the file to read protocol elements from.
     */
    public SimpleFileProtocolTest( String fileName )
    {
        super( fileName );
    }

    /**
     * Reads test elements from the protocol session file and adds them to the
     * {@link #testElements} ProtocolSession. Then calls {@link #runSessions}.
     */
    protected void runTest() throws Throwable
    {
        String fileName = getName() + ".test";
        addTestFile( fileName, testElements );

        runSessions();
    }

    /**
     * Finds the protocol session file identified by the test name, and builds
     * protocol elements from it. All elements from the definition file are added
     * to the supplied ProtocolSession.
     * @param fileName The name of the file to read
     * @param session The ProtocolSession to add elements to.
     */ 
    protected void addTestFile( String fileName, ProtocolSession session) throws Exception
    {
        // Need to find local resource.
        InputStream is = this.getClass().getResourceAsStream( fileName );
        if ( is == null ) {
            throw new Exception( "Test Resource '" + fileName + "' not found." );
        }

        builder.addProtocolLinesFromStream( is, session, fileName );
    }
}
