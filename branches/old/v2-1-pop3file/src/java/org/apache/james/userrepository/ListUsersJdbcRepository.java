/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.userrepository;

import org.apache.james.services.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A very lightweight UserRepository, which persists a list
 * of user names in a database. Password information is not 
 * persisted.
 * 
 * @author Darrell DeBoer <dd@bigdaz.com>
 */
public class ListUsersJdbcRepository extends AbstractJdbcUsersRepository
{
    /**
     * Reads properties for a User from an open ResultSet.
     * 
     * @param rsUsers A ResultSet with a User record in the current row.
     * @return A User instance
     * @throws SQLException
     *                   if an exception occurs reading from the ResultSet
     */
    protected User readUserFromResultSet(ResultSet rsUsers) throws SQLException {
        // Get the username, and build a DefaultUser with it.
        String username = rsUsers.getString(1);
        DefaultUser user = new DefaultUser(username, "SHA");
        return user;
    }

    /**
     * Set parameters of a PreparedStatement object with 
     * property values from a User instance.
     * @param user       a User instance, which should be an implementation class which
     *                   is handled by this Repostory implementation.
     * @param userInsert a PreparedStatement initialised with SQL taken from the "insert" SQL definition.
     * @throws SQLException
     *                   if an exception occurs while setting parameter values.
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
}
