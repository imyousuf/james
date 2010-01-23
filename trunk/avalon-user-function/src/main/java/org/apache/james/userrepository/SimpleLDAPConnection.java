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

package org.apache.james.userrepository;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/**
 * <p>
 * A connection handle to an LDAP server which is created by means 
 * of a simple principal and password/credentials 
 * authentication against the LDAP server. 
 * </p>
 * 
 * @author Obi Ezechukwu
 */
public class SimpleLDAPConnection {

	/**
	 * <p>
	 * The distinguished-name/DN of the principal to authenticate
	 * against the LDAP server.
	 * </p>
	 */
	private String principal;
	
	/**
	 * <p>
	 * The credentials with which to authenticate against the LDAP server.
	 * </p>
	 */
	private String credentials;
	
	/**
	 * <p>The URL of the LDAP server.</p>
	 */
	private String ldapURL;
	
	/**
	 * <p>
	 * The root directory context that is visible to the 
	 * authenticated user. 
	 * </p>
	 */
	private DirContext ldapContext;
	
	/**
	 * <p>
	 * Creates an instance with the given login details.
	 * </p>
	 * @param principal		The distinguished-name (DN) of the user
	 * to authenticate against the server.
	 * @param credentials	The credentials with which to authenticate 
	 * the user e.g. the user's password.
	 * @param ldapURL		The URL of the LDAP server against which to 
	 * authenticate the user. 
	 */
	private SimpleLDAPConnection(String principal, String credentials,
			String ldapURL) {
		super();
		this.principal = principal;
		this.credentials = credentials;
		this.ldapURL = ldapURL;
	}	
	
	/**
	 * <p>
	 * Opens a connection to the specified LDAP server using 
	 * the specified user details.  
	 * </p>
	 * @param principal		The distinguished-name (DN) of the user
	 * to authenticate against the server.
	 * @param credentials	The credentials with which to authenticate 
	 * the user e.g. the user's password.
	 * @param ldapURL		The URL of the LDAP server against which to 
	 * authenticate the user. 
	 * @return	A connection to the LDAP host.
	 * @throws NamingException	Propagated from the underlying LDAP connection.
	 */
	public static SimpleLDAPConnection openLDAPConnection(String principal,
			String credentials, String ldapURL) throws NamingException {
		SimpleLDAPConnection result = new SimpleLDAPConnection(principal,
				credentials, ldapURL);
		result.initializeContext();
		return result;
	}

	/**
	 * <p>
	 * Returns the root directory context that is visible to the 
	 * authenticated user.
	 * </p>  
	 * @return	The directory context that is visible to the 
	 * authenticated user. 
	 */
	public DirContext getLdapContext() {
		return ldapContext;
	}
 
	/**
	 * <p>
	 * Internal helper method which creates an LDAP/JNDI context using the 
	 * specified user credentials.  
	 * </p>
	 * @throws NamingException	Propagated from underlying 
	 * LDAP communication API.
	 */
	private void initializeContext() throws NamingException {
		Properties props = new Properties();
		props.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");
		props.put(Context.PROVIDER_URL, ldapURL);

		props.put(Context.SECURITY_AUTHENTICATION, "simple");
		props.put(Context.SECURITY_PRINCIPAL, principal);
		props.put(Context.SECURITY_CREDENTIALS, credentials);

		ldapContext = new InitialDirContext(props);
	}
}
