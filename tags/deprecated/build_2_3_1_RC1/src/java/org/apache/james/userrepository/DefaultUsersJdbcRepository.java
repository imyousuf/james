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

import org.apache.james.services.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * A Jdbc-backed UserRepository which handles User instances
 * of the <CODE>DefaultUser</CODE> class.
 * Although this repository can handle subclasses of DefaultUser,
 * like <CODE>DefaultJamesUser</CODE>, only properties from
 * the DefaultUser class are persisted.
 * 
 * TODO Please note that default configuration uses JamesUsersJdbcRepository
 * instead of this class. So we could also delete this implementation.
 * 
 */
public class DefaultUsersJdbcRepository extends AbstractJdbcUsersRepository
{
    /**
     * Reads properties for a User from an open ResultSet.
     * 
     * @param rsUsers A ResultSet with a User record in the current row.
     * @return A User instance
     * @throws SQLException
     *                   if an exception occurs reading from the ResultSet
     */
    protected User readUserFromResultSet(ResultSet rsUsers) throws SQLException 
    {
        // Get the username, and build a DefaultUser with it.
        String username = rsUsers.getString(1);
        String passwordHash = rsUsers.getString(2);
        String passwordAlg = rsUsers.getString(3);
        DefaultUser user = new DefaultUser(username, passwordHash, passwordAlg);
        return user;
    }

    /**
     * Set parameters of a PreparedStatement object with 
     * property values from a User instance.
     * 
     * @param user       a User instance, which should be an implementation class which
     *                   is handled by this Repostory implementation.
     * @param userInsert a PreparedStatement initialised with SQL taken from the "insert" SQL definition.
     * @throws SQLException
     *                   if an exception occurs while setting parameter values.
     */
    protected void setUserForInsertStatement(User user, 
                                             PreparedStatement userInsert) 
        throws SQLException 
    {
        DefaultUser defUser = (DefaultUser)user;
        userInsert.setString(1, defUser.getUserName());
        userInsert.setString(2, defUser.getHashAlgorithm());
        userInsert.setString(3, defUser.getHashedPassword());
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
        throws SQLException 
    {
        DefaultUser defUser = (DefaultUser)user;
        userUpdate.setString(3, defUser.getUserName());
        userUpdate.setString(1, defUser.getHashAlgorithm());
        userUpdate.setString(2, defUser.getHashedPassword());
    }
    
    /**
     * @see org.apache.james.services.UsersRepository#addUser(java.lang.String, java.lang.String)
     */
    public boolean addUser(String username, String password)  {
        User newbie = new DefaultUser(username, "SHA");
        newbie.setPassword(password);
        return addUser(newbie);
    }


}

