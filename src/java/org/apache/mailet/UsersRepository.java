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

package org.apache.mailet;

import java.util.Iterator;


/**
 * Interface for a repository of users. A repository represents a logical
 * grouping of users, typically by common purpose. E.g. the users served by an
 * email server or the members of a mailing list.
 *
 *
 * @version $Revision: 1.4 $
 */
public interface UsersRepository {

    /**
     * The component role used by components implementing this service
     */
   // String ROLE = "org.apache.mailet.UsersRepository";

    String USER = "USER";

    /**
     * Adds a user to the repository with the specified User object.
     *
     * @param user the user to be added
     *
     * @return true if succesful, false otherwise
     * @since James 1.2.2
     */
    boolean addUser(User user);

    /**
     * Adds a user to the repository with the specified attributes.  In current
     * implementations, the Object attributes is generally a String password.
     *
     * @param name the name of the user to be added
     * @param attributes see decription
     */
    void addUser(String name, Object attributes);

    /**
     * Gets the attribute for a user.  Not clear on behavior.
     *
     * @deprecated  Removed As of James 2.1 . Use the {@link #getUserByName(String) getUserByName} method.
     */
    //Object getAttributes(String name);


    /**
     * Get the user object with the specified user name.  Return null if no
     * such user.
     *
     * @param name the name of the user to retrieve
     * @return the user being retrieved, null if the user doesn't exist
     *
     * @since James 1.2.2
     */
    User getUserByName(String name);

    /**
     * Get the user object with the specified user name. Match user naems on
     * a case insensitive basis.  Return null if no such user.
     *
     * @param name the name of the user to retrieve
     * @return the user being retrieved, null if the user doesn't exist
     *
     * @since James 1.2.2
     */
    User getUserByNameCaseInsensitive(String name);

    /**
     * Returns the user name of the user matching name on an equalsIgnoreCase
     * basis. Returns null if no match.
     *
     * @param name the name to case-correct
     * @return the case-correct name of the user, null if the user doesn't exist
     */
    String getRealName(String name);

    /**
     * Update the repository with the specified user object. A user object
     * with this username must already exist.
     *
     * @return true if successful.
     */
    boolean updateUser(User user);

    /**
     * Removes a user from the repository
     *
     * @param name the user to remove from the repository
     */
    void removeUser(String name);

    /**
     * Returns whether or not this user is in the repository
     *
     * @param name the name to check in the repository
     * @return whether the user is in the repository
     */
    boolean contains(String name);

    /**
     * Returns whether or not this user is in the repository. Names are
     * matched on a case insensitive basis.
     *
     * @param name the name to check in the repository
     * @return whether the user is in the repository
     */
    boolean containsCaseInsensitive(String name);


    /**
     * Tests a user with the appropriate attributes.  In current implementations,
     * this typically means "check the password" where a String password is passed
     * as the Object attributes.
     *
     * @deprecated As of James 1.2.2, use {@link #test(String, String) test(String name, String password)}
     */
    boolean test(String name, Object attributes);

    /**
     * Test if user with name 'name' has password 'password'.
     *
     * @param name the name of the user to be tested
     * @param password the password to be tested
     *
     * @return true if the test is successful, false if the user
     *              doesn't exist or if the password is incorrect
     *
     * @since James 1.2.2
     */
    boolean test(String name, String password);

    /**
     * Returns a count of the users in the repository.
     *
     * @return the number of users in the repository
     */
    int countUsers();

    /**
     * List users in repository.
     *
     * @return Iterator over a collection of Strings, each being one user in the repository.
     */
    Iterator list();

}
