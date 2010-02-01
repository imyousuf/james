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

import org.apache.james.api.user.User;
import org.apache.james.impl.user.DefaultUser;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.PostConstruct;

/**
 * A very lightweight UserRepository, which persists a list
 * of user names in a database. Password information is not 
 * persisted.
 * 
 */
public class ListUsersJdbcRepository extends AbstractJdbcUsersRepository
{

    // REMOVE ME!
    @PostConstruct
    @Override
    public void init() throws Exception {
        super.init();
    }
    
    /**
     * @see org.apache.james.impl.jamesuser.AbstractUsersRepository#test(java.lang.String, java.lang.String)
     */
    public boolean test(String name, String password) {
        // list repository does not store passwords so we always return false!
        return false;
    }

    /**
     * @see org.apache.james.userrepository.AbstractJdbcUsersRepository#readUserFromResultSet(java.sql.ResultSet)
     */
    protected User readUserFromResultSet(ResultSet rsUsers) throws SQLException {
        // Get the username, and build a DefaultUser with it.
        String username = rsUsers.getString(1);
        DefaultUser user = new DefaultUser(username, "SHA");
        return user;
    }

    /**
     * @see org.apache.james.userrepository.AbstractJdbcUsersRepository#setUserForInsertStatement(org.apache.james.api.user.User, java.sql.PreparedStatement)
     */
    protected void setUserForInsertStatement(User user, 
                                             PreparedStatement userInsert) 
        throws SQLException {
        userInsert.setString(1, user.getUserName());
    }

    /**
     * Set parameters of a PreparedStatement object with
     * property values from a User instance.
     * 
     * @param user       a User instance, which should be an implementation class which
     *                   is handled by this Repostory implementation.
     * @param userUpdate a PreparedStatement initialised with SQL taken from the "update" SQL definition.
     * @throws SQLException
     *                   if an exception occurs while setting parameter values.
     */
    protected void setUserForUpdateStatement(User user, 
                                             PreparedStatement userUpdate) 
        throws SQLException {
        throw new UnsupportedOperationException("Can't update a List User - " +
                                                "only has a single attribute.");
    }

    /**
     * @see org.apache.james.api.user.UsersRepository#addUser(java.lang.String, java.lang.String)
     */
    public boolean addUser(String username, String password)  {
        User newbie = new DefaultUser(username, "SHA");
        newbie.setPassword(password);
        return addUser(newbie);
    }


}
