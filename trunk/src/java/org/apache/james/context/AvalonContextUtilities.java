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

package org.apache.james.context;

import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;

import java.io.File;
import java.io.IOException;

/**
 * This class is essentially a set of static functions for
 * extracting information from the Avalon context.  This class
 * should never be instantiated.  Each function takes the context
 * as a first argument.
 */
public class AvalonContextUtilities {

    /**
     * The file URL prefix
     */
    private static String filePrefix = "file://";

    /**
     * The file URL prefix length
     */
    private static int filePrefixLength = filePrefix.length();

    /**
     * Gets the file or directory described by the argument file URL.
     *
     * @param context the Avalon context
     * @param fileURL an appropriately formatted URL describing a file on
     *                the filesystem on which the server is running.  The
     *                URL is evaluated as a location relative to the
     *                application home, unless it begins with a slash.  In
     *                the latter case the file location is evaluated relative
     *                to the underlying file system root.
     *
     * @throws IllegalArgumentException if the arguments are null or the file
     *                                  URL is not appropriately formatted.
     * @throws ContextException if the underlying context generates a
     *                          ContextException, if the application home is
     *                          not correct, or if an IOException is generated
     *                          while accessing the file.
     */
    public static File getFile(Context context, String fileURL)
            throws Exception {
        if ((context == null) || (fileURL == null)) {
            throw new IllegalArgumentException("The getFile method doesn't allow null arguments.");
        }
        String internalFileURL = fileURL.trim();
        if (!(internalFileURL.startsWith(filePrefix))) {
            throw new IllegalArgumentException("The fileURL argument to getFile doesn't start with the required file prefix - "  + filePrefix);
        }

        String fileName = fileURL.substring(filePrefixLength);
        if (!(fileName.startsWith("/"))) {
            String baseDirectory = "";
            try {
                File applicationHome =
                    (File)context.get(AvalonContextConstants.APPLICATION_HOME);
                baseDirectory = applicationHome.toString();
            } catch (ContextException ce) {
                throw new ContextException("Encountered exception when resolving application home in Avalon context.", ce);
            } catch (ClassCastException cce) {
                throw new ContextException("Application home object stored in Avalon context was not of type java.io.File.", cce);
            }
            StringBuffer fileNameBuffer =
                new StringBuffer(128)
                    .append(baseDirectory)
                    .append(File.separator)
                            .append(fileName);
            fileName = fileNameBuffer.toString();
        }
        try {
            File returnValue = (new File(fileName)).getCanonicalFile();
            return returnValue;
        } catch (IOException ioe) {
            throw new ContextException("Encountered an unexpected exception while retrieving file.", ioe);
        }
    }

    /**
     * Private constructor to ensure that instances of this class aren't
     * instantiated.
     */
    private AvalonContextUtilities() {};
}
