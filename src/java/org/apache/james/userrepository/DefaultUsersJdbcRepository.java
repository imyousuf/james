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
 * A Jdbc-backed UserRepository which handles User instances
 * of the <CODE>DefaultUser</CODE> class.
 * Although this repository can handle subclasses of DefaultUser,
 * like <CODE>DefaultJamesUser</CODE>, only properties from
 * the DefaultUser class are persisted.
 * 
 * @author Darrell DeBoer <dd@bigdaz.com>
 */
public class DefaultUsersJdbcRepository extends AbstractJdbcUsersRepository
{
    protected User readUserFromResultSet(ResultSet rsUsers) throws SQLException 
    {
        // Get the username, and build a DefaultUser with it.
        String username = rsUsers.getString(1);
        String passwordAlg = rsUsers.getString(2);
        String passwordHash = rsUsers.getString(3);
        DefaultUser user = new DefaultUser(username, passwordHash, passwordAlg);
        return user;
    }

    protected void setUserForInsertStatement(User user, 
                                             PreparedStatement userInsert) 
        throws SQLException 
    {
        DefaultUser defUser = (DefaultUser)user;
        userInsert.setString(1, defUser.getUserName());
        userInsert.setString(2, defUser.getHashAlgorithm());
        userInsert.setString(3, defUser.getHashedPassword());
    }

    protected void setUserForUpdateStatement(User user, 
                                             PreparedStatement userUpdate) 
        throws SQLException 
    {
        DefaultUser defUser = (DefaultUser)user;
        userUpdate.setString(3, defUser.getUserName());
        userUpdate.setString(1, defUser.getHashAlgorithm());
        userUpdate.setString(2, defUser.getHashedPassword());
    }
}

