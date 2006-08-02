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

package org.apache.james.smtpserver;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The SMTPResponse which should be returned to the client socked
 */
public class SMTPResponse {
    private Collection resp = new ArrayList();

    private int code;

    /**
     * Init a new SMTPResponse
     * 
     * @param code
     *                The SMTP code
     * @param response
     *                The SMTP response
     * @throws InvalidArgumentException Get thrown if the given response is null
     */
    public SMTPResponse(int code, String response) {
	if (response == null)
	    throw new IllegalArgumentException("Invalid response String: "
		    + response);

	this.code = code;
	resp.add(response);
    }

    /**
     * Store the responseString which should be returned to the client
     * socked
     * 
     * @param response The RepsponseString which should be returned to the
     *                client socked
      * @throws InvalidArgumentException Get thrown if the given response is null
     */
    public void setSMTPResponse(String response) {
	if (response == null)
	    throw new IllegalArgumentException("Invalid response String: "
		    + response);

	resp.clear();
	resp.add(response);
    }

    /**
     * Add a response string
     * 
     * @param response The response string
     * @throws InvalidArgumentException Get thrown if the given response is null
     */
    public void addSMTPResponse(String response) {
	if (response == null)
	    throw new IllegalArgumentException("Invalid response String: "
		    + response);

	resp.add(response);
    }

    /**
     * Set the SMTP code
     * 
     * @param code
     *                The code
     */
    public void setSMTPCode(int code) {
	this.code = code;
    }

    /**
     * Get the SMTP code
     * 
     * @return code The SMTP code
     */
    public int getSMTPCode() {
	return code;
    }

    /**
     * Get the SMTPResponse which should be returend to the client socked
     * 
     * @return response The responseString which should be returned to the
     *         client socked
     */
    public Collection getSMTPResponse() {
	return resp;
    }

    /**
     * Set the SMTPCode and SMTPResponse with a raw response String
     * 
     * @param response The raw response string
     * @throws IllegalArgumentException if the raw String is not valid
     */
    public void setRawSMTPResponse(String response) {
	String[] parts = response.split(" ");
	if (parts.length > 1) {
	    setSMTPCode(Integer.parseInt(parts[0]));
	    setSMTPResponse(response.substring(parts[0].length() + 1));
	    try {

	    } catch (NumberFormatException e) {
		throw new IllegalArgumentException("Invalid SMTPCode: "
			+ parts[0]);
	    }
	} else {
	    throw new IllegalArgumentException("Invalid raw SMTPResponse: "
		    + response);
	}
    }
}
