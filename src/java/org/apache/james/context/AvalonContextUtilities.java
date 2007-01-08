/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

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
    private AvalonContextUtilities() {}
}
