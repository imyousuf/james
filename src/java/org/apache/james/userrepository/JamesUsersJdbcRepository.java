/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.userrepository;

import org.apache.mailet.MailAddress;
import org.apache.mailet.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A Jdbc-backed UserRepository which handles User instances
 * of the <CODE>DefaultJamesUser</CODE> class, or any superclass.
 *
 */
public class JamesUsersJdbcRepository extends AbstractJdbcUsersRepository
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
        // Get the column values
        String username = rsUsers.getString(1);
        String pwdHash = rsUsers.getString(2);
        String pwdAlgorithm = rsUsers.getString(3);
        boolean useForwarding = rsUsers.getBoolean(4);
        String forwardingDestination = rsUsers.getString(5);
        boolean useAlias = rsUsers.getBoolean(6);
        String alias = rsUsers.getString(7);

        MailAddress forwardAddress = null;
        if ( forwardingDestination != null ) {
            try {
                forwardAddress = new MailAddress(forwardingDestination);
            }
            catch (javax.mail.internet.ParseException pe) {
                StringBuffer exceptionBuffer =
                    new StringBuffer(256)
                        .append("Invalid mail address in database: ")
                        .append(forwardingDestination)
                        .append(", for user ")
                        .append(username)
                        .append(".");
                throw new RuntimeException(exceptionBuffer.toString());
            }
        }

        // Build a DefaultJamesUser with these values, and add to the list.
        DefaultJamesUser user = new DefaultJamesUser(username, pwdHash, pwdAlgorithm);
        user.setForwarding(useForwarding);
        user.setForwardingDestination(forwardAddress);
        user.setAliasing(useAlias);
        user.setAlias(alias);

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
        throws SQLException {
        setUserForStatement(user, userInsert, false);
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
        setUserForStatement(user, userUpdate, true);
    }

    /**
     * Sets the data for the prepared statement to match the information
     * in the user object.
     *
     * @param user the user whose data is to be stored in the PreparedStatement.
     * @param stmt the PreparedStatement to be modified.
     * @param userNameLast whether the user id is the last or the first column
     */
    private void setUserForStatement(User user, PreparedStatement stmt,
                                     boolean userNameLast) throws SQLException {
        // Determine column offsets to use, based on username column pos.
        int nameIndex = 1;
        int colOffset = 1;
        if ( userNameLast ) {
            nameIndex = 7;
            colOffset = 0;
        }

        // Can handle instances of DefaultJamesUser and DefaultUser.
        DefaultJamesUser jamesUser;
        if (user instanceof DefaultJamesUser) {
            jamesUser = (DefaultJamesUser)user;
        }
        else if ( user instanceof DefaultUser ) {
            DefaultUser aUser = (DefaultUser)user;
            jamesUser = new DefaultJamesUser(aUser.getUserName(),
                                             aUser.getHashedPassword(),
                                             aUser.getHashAlgorithm());
        }
        // Can't handle any other implementations.
        else {
            throw new RuntimeException("An unknown implementation of User was " +
                                       "found. This implementation cannot be " +
                                       "persisted to a UsersJDBCRepsitory.");
        }

        // Get the user details to save.
        stmt.setString(nameIndex, jamesUser.getUserName());
        stmt.setString(1 + colOffset, jamesUser.getHashedPassword());
        stmt.setString(2 + colOffset, jamesUser.getHashAlgorithm());
        stmt.setInt(3 + colOffset, (jamesUser.getForwarding() ? 1 : 0));

        MailAddress forwardAddress = jamesUser.getForwardingDestination();
        String forwardDestination = null;
        if ( forwardAddress != null ) {
            forwardDestination = forwardAddress.toString();
        }
        stmt.setString(4 + colOffset, forwardDestination);
        stmt.setInt(5 + colOffset, (jamesUser.getAliasing() ? 1 : 0));
        stmt.setString(6 + colOffset, jamesUser.getAlias());
    }
}
