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

package org.apache.james.user.ldap;

import java.io.Serializable;

import javax.naming.NamingException;

import org.apache.james.user.api.User;

/**
 * <p>
 * Encapsulates the details of a user as taken from 
 * an LDAP compliant directory. Instances of this class
 * are only applicable to the {@link ReadOnlyUsersLDAPRepository}
 * or its subclasses. Consequently it does not permit the mutation 
 * of user details. It is intended purely as an encapsulation 
 * of the user information as held in the LDAP directory, and as a means
 * of authenticating the user against the LDAP server. Consequently 
 * invocations of the contract method {@link User#setPassword(String)}
 * always returns <code>false</code>. 
 * </p>
 * 
 * @see SimpleLDAPConnection
 * @see ReadOnlyUsersLDAPRepository
 * 
 */
public class ReadOnlyLDAPUser implements User, Serializable {
	private static final long serialVersionUID = -6712066073820393235L;
    
    /**
     * <p>
     * The user's identifier or name. This is the value 
     * that is returned by the method {@link User#getUserName()}.
     * It is also from this value that the user's email 
     * address is formed, so for example: if the value of
     * this field is <code>&quot;john.bold&quot;</code>, and the 
     * domain is <code>&quot;myorg.com&quot;</code>, the user's email
     * address will be <code>&quot;john.bold&#64;myorg.com&quot;</code>.
     * </p> 
     */
    private String userName;
    
    
    /**
     * <p>
     * The distinguished name of the user-record in the 
     * LDAP directory.
     * </p> 
     */
    private String userDN;
    
    /**
     * <p>
     * The URL for connecting to the LDAP server from which to 
     * retrieve the user's details.
     * </p> 
     */
    private String ldapURL;    
    
    /**
     * <p>
     * Constructs an instance for the given user-details, 
     * and which will authenticate against the given host.
     * </p> 
     * 
     * @param userName	The user-identifier/name. This is the 
     * value with which the field {@link #userName} will be initialised, 
     * and which will be returned by invoking {@link #getUserName()}.
     * @param userDN	The distinguished (unique-key) of the user details
     * as stored on the LDAP directory.
     * @param ldapURL	The URL of the LDAP server on which the user 
     * details are held. This is also the host against which the 
     * user will be authenticated, when {@link #verifyPassword(String)} 
     * is invoked. 
     */
    public ReadOnlyLDAPUser(String userName, String userDN, String ldapURL) {
		this.userName = userName;
		this.userDN = userDN;
		this.ldapURL = ldapURL;
	}

    /**
     * <p>
     * Fulfils the contract {@link User#getUserName()}. It returns the value
     * of the field {@link #userName}. This is generally the value from 
     * which the user email address is built, by appending the domain name 
     * to it.
     * </p>  
     * 
     * @return The user's identifier or name.
     */
    public String getUserName() {
		return userName;
	}

    /**
     * <p>
     * Implementation of contract {@link User#setPassword(String)}, which is 
     * provided for compliance purposes only. Instances of this type 
     * mirror LDAP data and do not perform any updates to the directory. 
     * Consequently, this method always returns <code>false</code> and does not do 
     * any work.
     * </p>
     * @return <code>False</code>
     */
    public boolean setPassword(String newPass) {
		return false;
	}


    /**
     * <p>
     * Verifies that the password supplied is actually the user's password,
     * by attempting to bind to the LDAP server using the user's username 
     * and the supplied password.
     * </p>
     * @param password	The password to validate.
     * @return <code>True</code> if a connection can successfully be established 
     * to the LDAP host using the user's id and the supplied password, and <code>False</code>
     * otherwise. <b>Please note</b> that if the LDAP server has suffered a crash or failure 
     * in between the initialisation of the user repository and the invocation of this method,  
     * the result will still be <code>false</code>.
     */
    public boolean verifyPassword(String password) {
		boolean result;
		try {
			SimpleLDAPConnection.openLDAPConnection(userDN, password, ldapURL);
			result = true;
		} catch (NamingException exception) {
			result = false;
		}
		return result;
	}

}
