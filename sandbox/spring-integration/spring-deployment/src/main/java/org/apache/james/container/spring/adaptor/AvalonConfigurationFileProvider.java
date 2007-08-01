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
package org.apache.james.container.spring.adaptor;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.phoenix.tools.configuration.ConfigurationBuilder;
import org.xml.sax.InputSource;

/**
 * loads the well-known classic James configuration file
 *
  * TODO make this thing be based on Resource class and inject resource.getInputStream() into InputSource 
 */
public class AvalonConfigurationFileProvider implements ConfigurationProvider {

    private String absoluteFilePath;

    public void setConfigurationPath(String absoluteFilePath) {
        this.absoluteFilePath = absoluteFilePath;
    }


    public Configuration getConfiguration() {
        InputSource inputSource = new InputSource(absoluteFilePath);
        try
        {
            Configuration configuration = ConfigurationBuilder.build(inputSource, null, null);
            return configuration;
        }
        catch( final Exception e )
        {
//            getLogger().error( message, e );
            throw new RuntimeException("failed loading configuration ", e);
        }

    }
}
