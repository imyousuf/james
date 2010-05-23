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
package org.apache.james.container.spring;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * The JamesClassLoader uses the provided system classloader
 * and adds all jars found in the external library directory.
 * 
 * Use "-Djava.system.class.loader=org.apache.james.container.spring.JamesClassLoader"
 * when launching james for this class loader to be invoked.
 */
public class JamesClassLoader extends URLClassLoader {

    /**
     * The ClassLoader that will be used by James application.
     * 
     * The class is loaded using the default system class loader 
     * defines this constructor with a single parameter is used as 
     * the delegation parent.
     * 
     * @param classLoader
     */
    public JamesClassLoader(ClassLoader classLoader) {
        super(new URL[0], classLoader);
        String[] jars = new File(JamesServerApplicationContext.getResourceLoader().getExternalLibraryDirectory()).list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        if (jars != null) {
            for (int i=0; i < jars.length; i++) {
                File file = new File(JamesServerApplicationContext.getResourceLoader().getExternalLibraryDirectory() + jars[i]);
                try {
                    super.addURL(file.toURI().toURL());
                } catch (MalformedURLException e) {
                	// At that time, we can not yet use non jvm classes for logging. Simply log on console. Should never come here...
                	System.out.println("Got an unexpected exception while building the urls for the james class loader for file " + file.getAbsolutePath());
                }
            }
        }
    }

}
