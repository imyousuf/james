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

package org.apache.james.server.jpa;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import org.apache.jackrabbit.util.Text;
import org.apache.james.api.user.User;

@Entity(name="User")
public class JPAUser implements User {

    /** 
     * Static salt for hashing password.
     * Modifying this value will render all passwords unrecognizable.
     */
    public static final String SALT = "JPAUsersRepository";
    
    /**
     * Hashes salted password.
     * @param username not null
     * @param password not null
     * @return not null
     */
    public static String hashPassword(String username, String password) {
        // Combine dynamic and static salt
        final String hashedSaltedPassword = Text.md5(Text.md5(username + password) + SALT);
        return hashedSaltedPassword;
    }
    
    /** Prevents concurrent modification */
    @SuppressWarnings("unused")
    @Version
    private int version;
    
    /** Key by user name */
    @Id
    private String name;
    /** Hashed password */
    @Basic
    private String password;
    
    protected JPAUser() {}
    
    public JPAUser(final String userName, String password) {
        super();
        this.name = userName;
        this.password = hashPassword(userName, password);
    }

    public String getUserName() {
        return name;
    }
    
    /**
     * Gets salted, hashed password.
     * @return the hashedSaltedPassword
     */
    public final String getHashedSaltedPassword() {
        return password;
    }

    public boolean setPassword(String newPass) {
        final boolean result;
        if (newPass == null) {
            result = false;
        } else {
            password = hashPassword(name, newPass);
            result = true;
        }
        return result;
    }

    public boolean verifyPassword(String pass) {
        final boolean result;
        if (pass == null) {
            result = password == null;
        } else if (password == null) {
            result = false;
        } else {
            result = password.equals(hashPassword(name, pass));
        }
        return result;
    }
    
    
}
