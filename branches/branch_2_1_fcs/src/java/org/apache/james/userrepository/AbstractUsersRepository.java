/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A partial implementation of a Repository to store users.
 * <p>This implements common functionality found in different UsersRespository 
 * implementations, and makes it easier to create new User repositories.</p>
 *
 * @author Darrell DeBoer <dd@bigdaz.com>
 * @author Charles Benett <charles@benett1.demon.co.uk>
 */
public abstract class AbstractUsersRepository
    extends AbstractLogEnabled
    implements UsersRepository, Component {

    //
    // Core Abstract methods - override these for a functional UserRepository.
    //

    /**
     * Returns a list populated with all of the Users in the repository.
     * @return an <code>Iterator</code> of <code>User</code>s.
     */
    protected abstract Iterator listAllUsers();

    /**
     * Adds a user to the underlying Repository.
     * The user name must not clash with an existing user.
     */
    protected abstract void doAddUser(User user);

    /**
     * Removes a user from the underlying repository.
     * If the user doesn't exist, returns ok.
     */
    protected abstract void doRemoveUser(User user);

    /**
     * Updates a user record to match the supplied User.
     */
    protected abstract void doUpdateUser(User user);

    //
    // Extended protected methods.
    // These provide very basic default implementations, which will work,
    // but may need to be overridden by subclasses for performance reasons.
    //
    /**
     * Produces the complete list of User names, with correct case.
     * @return a <code>List</code> of <code>String</code>s representing
     *         user names.
     */
    protected List listUserNames() {
        Iterator users = listAllUsers();
        List userNames = new LinkedList();
        while ( users.hasNext() ) {
            User user = (User)users.next();
            userNames.add(user.getUserName());
        }

        return userNames;
    }

    /**
     * Gets a user by name, ignoring case if specified.
     * This implementation gets the entire set of users,
     * and scrolls through searching for one matching <code>name</code>.
     *
     * @param name the name of the user being retrieved
     * @param ignoreCase whether the name is regarded as case-insensitive
     *
     * @return the user being retrieved, null if the user doesn't exist
     */
    protected User getUserByName(String name, boolean ignoreCase) {
        // Just iterate through all of the users until we find one matching.
        Iterator users = listAllUsers();
        while ( users.hasNext() ) {
            User user = (User)users.next();
            String username = user.getUserName();
            if (( !ignoreCase && username.equals(name) ) ||
                ( ignoreCase && username.equalsIgnoreCase(name) )) {
                return user;
            }
        }
        // Not found - return null
        return null;
    }

    //
    // UsersRepository interface implementation.
    //
    /**
     * Adds a user to the repository with the specified User object.
     * Users names must be unique-case-insensitive in the repository.
     *
     * @param user the user to be added
     *
     * @return true if succesful, false otherwise
     * @since James 1.2.2
     */
    public boolean addUser(User user) {
        String username = user.getUserName();

        if ( containsCaseInsensitive(username) ) {
            return false;
        }
        
        doAddUser(user);
        return true;
    }

    /**
     * Adds a user to the repository with the specified attributes.  In current
     * implementations, the Object attributes is generally a String password.
     *
     * @param name the name of the user to be added
     * @param attributes the password value as a String
     */
    public void addUser(String name, Object attributes)  {
        if (attributes instanceof String) {
            User newbie = new DefaultUser(name, "SHA");
            newbie.setPassword( (String) attributes );
            addUser(newbie);
        } else {
            throw new RuntimeException("Improper use of deprecated method" 
                                       + " - use addUser(User user)");
        }
    }

    /**
     * Update the repository with the specified user object. A user object
     * with this username must already exist.
     *
     * @param user the user to be updated
     *
     * @return true if successful.
     */
    public boolean updateUser(User user) {
        // Return false if it's not found.
        if ( ! contains(user.getUserName()) ) {
            return false;
        }
        else {
            doUpdateUser(user);
            return true;
        }
    }

    /**
     * Removes a user from the repository
     *
     * @param user the user to be removed
     */
    public void removeUser(String name) {
        User user = getUserByName(name);
        if ( user != null ) {
            doRemoveUser(user);
        }
    }

    /**
     * Gets the attribute for a user.  Not clear on behavior.
     *
     * @deprecated As of James 1.2.2 . Use the {@link #getUserByName(String) getUserByName} method.
     */
    public Object getAttributes(String name) {
        throw new RuntimeException("Improper use of deprecated method - read javadocs");
    }

    /**
     * Get the user object with the specified user name.  Return null if no
     * such user.
     *
     * @param name the name of the user to retrieve
     *
     * @return the user if found, null otherwise
     *
     * @since James 1.2.2
     */
    public User getUserByName(String name) {
        return getUserByName(name, false);
    }

    /**
     * Get the user object with the specified user name. Match user naems on
     * a case insensitive basis.  Return null if no such user.
     *
     * @param name the name of the user to retrieve
     *
     * @return the user if found, null otherwise
     *
     * @since James 1.2.2
     */
    public User getUserByNameCaseInsensitive(String name) {
        return getUserByName(name, true);
    }

    /**
     * Returns the user name of the user matching name on an equalsIgnoreCase
     * basis. Returns null if no match.
     *
     * @param name the name of the user to retrieve
     *
     * @return the correct case sensitive name of the user
     */
    public String getRealName(String name) {
        // Get the user by name, ignoring case, and return the correct name.
        User user = getUserByName(name, true);
        if ( user == null ) {
            return null;
        } else {
            return user.getUserName();
        }
    }

    /**
     * Returns whether or not this user is in the repository
     */
    public boolean contains(String name) {
        User user = getUserByName(name, false);
        return ( user != null );
    }

    /**
     * Returns whether or not this user is in the repository. Names are
     * matched on a case insensitive basis.
     */
    public boolean containsCaseInsensitive(String name) {
        User user = getUserByName( name, true );
        return ( user != null );
    }

    /**
     * Tests a user with the appropriate attributes.  In current implementations,
     * this typically means "check the password" where a String password is passed
     * as the Object attributes.
     *
     * @param name the name of the user to be tested
     * @param attributes the password to be tested
     *
     * @throws UnsupportedOperationException always, as this method should not be used
     *
     * @deprecated As of James 1.2.2, use {@link #test(String, String) test(String name, String password)}
     */
    public boolean test(String name, Object attributes) {
        throw new UnsupportedOperationException("Improper use of deprecated method - read javadocs");
    }

    /**
     * Test if user with name 'name' has password 'password'.
     *
     * @param name the name of the user to be tested
     * @param password the password to be tested
     *
     * @return true if the test is successful, false if the
     *              password is incorrect or the user doesn't
     *              exist
     * @since James 1.2.2
     */
    public boolean test(String name, String password) {
        User user = getUserByName(name, false);
        if ( user == null ) {
            return false;
        } else {
            return user.verifyPassword(password);
        }
    }

    /**
     * Returns a count of the users in the repository.
     *
     * @return the number of users in the repository
     */
    public int countUsers() {
        List usernames = listUserNames();
        return usernames.size();
    }

    /**
     * List users in repository.
     *
     * @return Iterator over a collection of Strings, each being one user in the repository.
     */
    public Iterator list() {
        return listUserNames().iterator();
    }
}
