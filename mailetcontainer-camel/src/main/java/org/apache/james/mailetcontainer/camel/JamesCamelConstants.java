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
package org.apache.james.mailetcontainer.camel;

/**
 * Constants used in Camel components
 * 
 *
 */
public interface JamesCamelConstants {

    /**
     * Header which holds the next delivery time in milliseconds as long value
     */
    public final static String JAMES_NEXT_DELIVERY = "JAMES_NEXT_DELIVERY";

    /**
     * Property which should be set to true if the mail should get sent again
     */
    public final static String JAMES_RETRY_DELIVERY = "JAMES_RETRY_DELIVERY";


    public final static String JAMES_MAIL_STATE = "JAMES_MAIL_STATE";
}
