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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.user.User;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LogEnabled;

/**
 * <p>
 * This repository implementation serves as a bridge between Apache James and
 * LDAP. It allows James to authenticate users against an LDAP compliant server
 * such as Apache DS or Microsoft AD. It also enables role/group based access
 * restriction based on LDAP groups.
 * </p>
 * <p>
 * It is intended for organisations that already have a user-authentication and
 * authorisation mechanism in place, and want to leverage this when deploying
 * James. The assumption inherent here is that such organisations would not want
 * to manage user details via James, but will do so externally using whatever
 * mechanism provided by, or built on top off, their LDAP implementation.
 * </p>
 * <p>
 * Based on this assumption, this repository is strictly <b>read-only</b>. As a
 * consequence, user modification, deletion and creation requests will be
 * ignored when using this repository.
 * </p>
 * <p>
 * The following fragment of XML provides an example configuration to enable
 * this repository: </br>
 * 
 * <pre>
 *  &lt;users-store&gt;
 *      &lt;repository name=&quot;LDAPUsers&quot; 
 *      class=&quot;org.apache.james.userrepository.ReadOnlyUsersLDAPRepository&quot; 
 *      ldapHost=&quot;ldap://myldapserver:389&quot;
 *      principal=&quot;uid=ldapUser,ou=system&quot;
 *      credentials=&quot;password&quot;
 *      userBase=&quot;ou=People,o=myorg.com,ou=system&quot;
 *      userIdAttribute=&quot;uid&quot;/&gt;
 *  &lt;/users-store&gt;
 * </pre>
 * 
 * </br>
 * 
 * Its constituent attributes are defined as follows:
 * <ul>
 * <li><b>ldapHost:</b> The URL of the LDAP server to connect to.</li>
 * <li>
 * <b>principal:</b> The name (DN) of the user with which to initially bind to
 * the LDAP server.</li>
 * <li>
 * <b>credentials:</b> The password with which to initially bind to the LDAP
 * server.</li>
 * <li>
 * <b>userBase:</b>The context within which to search for user entities.</li>
 * <li>
 * <b>userIdAttribute:</b>The name of the LDAP attribute which holds user ids.
 * For example &quot;uid&quot; for Apache DS, or &quot;sAMAccountName&quot; for
 * Microsoft Active Directory.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * In order to enable group/role based access restrictions, you can use the
 * &quot;&lt;restriction&gt;&quot; configuration element. An example of this is
 * shown below: <br>
 * 
 * <pre>
 * &lt;restriction
 * 	memberAttribute=&quot;uniqueMember&quot;&gt;
 * 		&lt;group&gt;cn=PermanentStaff,ou=Groups,o=myorg.co.uk,ou=system&lt;/group&gt;
 *        	&lt;group&gt;cn=TemporaryStaff,ou=Groups,o=myorg.co.uk,ou=system&lt;/group&gt;
 * &lt;/restriction&gt;
 *</pre>
 * 
 * <br>
 * Its constituent attributes and elements are defined as follows:
 * <ul>
 * <li>
 * <b>memberAttribute:</b> The LDAP attribute whose values indicate the DNs of
 * the users which belong to the group or role.</li>
 * <li>
 * <b>group:</b> A valid group or role DN. A user is only authenticated
 * (permitted access) if they belong to at least one of the groups listed under
 * the &quot;&lt;restriction&gt;&quot; sections.</li>
 * </ul>
 * </p>
 * 
 * @see SimpleLDAPConnection
 * @see ReadOnlyLDAPUser
 * @see ReadOnlyLDAPGroupRestriction
 * 
 */
public class ReadOnlyUsersLDAPRepository implements UsersRepository, Configurable, LogEnabled {

    /**
     * <p>
     * The URL of the LDAP server against which users are to be authenticated.
     * Note that users are actually authenticated by binding against the LDAP
     * server using the users &quot;dn&quot; and &quot;credentials&quot;.The
     * value of this field is taken from the value of the configuration
     * attribute &quot;ldapHost&quot;.
     * </p>
     */
    private String ldapHost;

    /**
     * <p>
     * The value of this field is taken from the configuration attribute
     * &quot;userIdAttribute&quot;. This is the LDAP attribute type which holds
     * the userId value. Note that this is not the same as the email address
     * attribute.
     * </p>
     */
    private String userIdAttribute;

    /**
     * <p>
     * This is the LDAP context/sub-context within which to search for user
     * entities. The value of this field is taken from the configuration
     * attribute &quot;userBase&quot;.
     * </p>
     */
    private String userBase;

    /**
     * <p>
     * The user with which to initially bind to the LDAP server. The value of
     * this field is taken from the configuration attribute
     * &quot;principal&quot;.
     * </p>
     */
    private String principal;

    /**
     * <p>
     * The password/credentials with which to initially bind to the LDAP server.
     * The value of this field is taken from the configuration attribute
     * &quot;credentials&quot;.
     * </p>
     */
    private String credentials;

    /**
     * <p>
     * Encapsulates the information required to restrict users to LDAP groups or
     * roles. This object is populated from the contents of the configuration
     * element &lt;restriction&gt;.
     * </p>
     */
    private ReadOnlyLDAPGroupRestriction restriction;

    /**
     * <p>
     * The connection handle to the LDAP server. This is the connection that is
     * built from the configuration attributes &quot;ldapHost&quot;,
     * &quot;principal&quot; and &quot;credentials&quot;.
     * </p>
     */
    private SimpleLDAPConnection ldapConnection;

    private Log log;

    /**
     * <p>
     * Extracts the parameters required by the repository instance from the
     * James server configuration data. The fields extracted include
     * {@link #ldapHost}, {@link #userIdAttribute}, {@link #userBase},
     * {@link #principal}, {@link #credentials} and {@link #restriction}.
     * </p>
     * 
     * @param configuration
     *            An encapsulation of the James server configuration data.
     */
    public void configure(HierarchicalConfiguration configuration) throws ConfigurationException {
        ldapHost = configuration.getString("[@ldapHost]");
        principal = configuration.getString("[@principal]");
        credentials = configuration.getString("[@credentials]");
        userBase = configuration.getString("[@userBase]");
        userIdAttribute = configuration.getString("[@userIdAttribute]");

        restriction = new ReadOnlyLDAPGroupRestriction(configuration.configurationAt("restriction"));

    }

    /**
     * <p>
     * Initialises the user-repository instance. It will create a connection to
     * the LDAP host using the supplied configuration.
     * </p>
     * 
     * @throws Exception
     *             If an error occurs authenticating or connecting to the
     *             specified LDAP host.
     */
    @PostConstruct
    public void init() throws Exception {
        StringBuffer logBuffer;
        if (log.isDebugEnabled()) {
            logBuffer = new StringBuffer(128).append(this.getClass().getName()).append(".initialize()");
            log.debug(logBuffer.toString());

            logBuffer = new StringBuffer(256).append("Openning connection to LDAP host: ").append(ldapHost).append(".");
            log.debug(logBuffer.toString());
        }

        ldapConnection = SimpleLDAPConnection.openLDAPConnection(principal, credentials, ldapHost);

        if (log.isDebugEnabled()) {
            logBuffer = new StringBuffer(256).append("Initialization complete. User baseDN=").append(userBase).append(" ; userIdAttribute=" + userIdAttribute).append("\n\tGroup restriction:" + restriction);
            log.debug(logBuffer.toString());
        }
    }

    /**
     * <p>
     * Indicates if the user with the specified DN can be found in the group
     * membership map&#45;as encapsulated by the specified parameter map.
     * </p>
     * 
     * @param userDN
     *            The DN of the user to search for.
     * @param groupMembershipList
     *            A map containing the entire group membership lists for the
     *            configured groups. This is organised as a map of
     * 
     *            <code>&quot;&lt;groupDN&gt;=&lt;[userDN1,userDN2,...,userDNn]&gt;&quot;</code>
     *            pairs. In essence, each <code>groupDN</code> string is
     *            associated to a list of <code>userDNs</code>.
     * @return <code>True</code> if the specified userDN is associated with at
     *         least one group in the parameter map, and <code>False</code>
     *         otherwise.
     */
    private boolean userInGroupsMembershipList(String userDN, Map<String, Collection<String>> groupMembershipList) {
        boolean result = false;

        Collection<Collection<String>> memberLists = groupMembershipList.values();
        Iterator<Collection<String>> memberListsIterator = memberLists.iterator();

        while (memberListsIterator.hasNext() && !result) {
            Collection<String> groupMembers = memberListsIterator.next();
            result = groupMembers.contains(userDN);
        }

        return result;
    }

    /**
     * <p>
     * Gets all the user entities taken from the LDAP server, as taken from the
     * search-context given by the value of the attribute {@link #userBase}.
     * </p>
     * 
     * @return A set containing all the relevant users found in the LDAP
     *         directory.
     * @throws NamingException
     *             Propagated from the LDAP communication layer.
     */
    private Set<String> getAllUsersFromLDAP() throws NamingException {
        Set<String> result = new HashSet<String>();
        NamingEnumeration<?> boundNames = ldapConnection.getLdapContext().list(userBase);

        NameClassPair elementInfo;
        while (boundNames.hasMore()) {
            elementInfo = (NameClassPair) boundNames.next();
            result.add(elementInfo.getNameInNamespace());
        }

        return result;
    }

    /**
     * <p>
     * Extract the user attributes for the given collection of userDNs, and
     * encapsulates the user list as a collection of {@link ReadOnlyLDAPUser}s.
     * This method delegates the extraction of a single user's details to the
     * method {@link #buildUser(String)}.
     * </p>
     * 
     * @param userDNs
     *            The distinguished-names (DNs) of the users whose information
     *            is to be extracted from the LDAP repository.
     * @return A collection of {@link ReadOnlyLDAPUser}s as taken from the LDAP
     *         server.
     * @throws NamingException
     *             Propagated from the underlying LDAP communication layer.
     */
    private Collection<ReadOnlyLDAPUser> buildUserCollection(Collection<String> userDNs) throws NamingException {
        List<ReadOnlyLDAPUser> results = new ArrayList<ReadOnlyLDAPUser>();

        Iterator<String> userDNIterator = userDNs.iterator();

        while (userDNIterator.hasNext()) {
            ReadOnlyLDAPUser user = buildUser(userDNIterator.next());
            results.add(user);
        }

        return results;
    }

    /**
     * <p>
     * Given a userDN, this method retrieves the user attributes from the LDAP
     * server, so as to extract the items that are of interest to James.
     * Specifically it extracts the userId, which is extracted from the LDAP
     * attribute whose name is given by the value of the field
     * {@link #userIdAttribute}.
     * </p>
     * 
     * @param userDN
     *            The distinguished-name of the user whose details are to be
     *            extracted from the LDAP repository.
     * @return A {@link ReadOnlyLDAPUser} instance which is initialized with the
     *         userId of this user and ldap connection information with which
     *         the userDN and attributes were obtained.
     * @throws NamingException
     *             Propagated by the underlying LDAP communication layer.
     */
    private ReadOnlyLDAPUser buildUser(String userDN) throws NamingException {
        ReadOnlyLDAPUser result;

        Attributes userAttributes = ldapConnection.getLdapContext().getAttributes(userDN);
        Attribute userName = userAttributes.get(userIdAttribute);

        result = new ReadOnlyLDAPUser(userName.get().toString(), userDN, ldapHost);

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.user.UsersRepository#contains(java.lang.String)
     */
    public boolean contains(String name) {
        if (getUserByName(name) != null) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.api.user.UsersRepository#containsCaseInsensitive(java
     * .lang.String)
     */
    public boolean containsCaseInsensitive(String name) {
        if (getUserByNameCaseInsensitive(name) != null) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.user.UsersRepository#countUsers()
     */
    public int countUsers() {
        try {
            return getValidUsers().size();
        } catch (NamingException e) {
            log.error("Unable to retrieve user count from ldap", e);
        }
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.api.user.UsersRepository#getRealName(java.lang.String)
     */
    public String getRealName(String name) {
        User u = getUserByNameCaseInsensitive(name);
        if (u != null) {
            return u.getUserName();
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.api.user.UsersRepository#getUserByName(java.lang.String)
     */
    public User getUserByName(String name) {
        try {
            Iterator<ReadOnlyLDAPUser> userIt = buildUserCollection(getValidUsers()).iterator();
            while (userIt.hasNext()) {
                ReadOnlyLDAPUser u = userIt.next();
                if (u.getUserName().equals(name)) {
                    return u;
                }
            }

        } catch (NamingException e) {
            log.error("Unable to retrieve user from ldap", e);
        }
        return null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.api.user.UsersRepository#getUserByNameCaseInsensitive
     * (java.lang.String)
     */
    public User getUserByNameCaseInsensitive(String name) {
        try {
            Iterator<ReadOnlyLDAPUser> userIt = buildUserCollection(getValidUsers()).iterator();
            while (userIt.hasNext()) {
                ReadOnlyLDAPUser u = userIt.next();
                if (u.getUserName().equalsIgnoreCase(name)) {
                    return u;
                }
            }

        } catch (NamingException e) {
            log.error("Unable to retrieve user from ldap", e);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.user.UsersRepository#list()
     */
    public Iterator<String> list() {
        List<String> result = new ArrayList<String>();
        try {

            Iterator<ReadOnlyLDAPUser> userIt = buildUserCollection(getValidUsers()).iterator();

            while (userIt.hasNext()) {
                result.add(userIt.next().getUserName());
            }
        } catch (NamingException namingException) {
            throw new RuntimeException("Unable to retrieve users list from LDAP due to unknown naming error.", namingException);
        }

        return result.iterator();
    }

    private Collection<String> getValidUsers() throws NamingException {
        Set<String> userDNs = getAllUsersFromLDAP();
        Collection<String> validUserDNs;

        if (restriction.isActivated()) {
            Map<String, Collection<String>> groupMembershipList = restriction.getGroupMembershipLists(ldapConnection);
            validUserDNs = new ArrayList<String>();

            Iterator<String> userDNIterator = userDNs.iterator();
            String userDN;
            while (userDNIterator.hasNext()) {
                userDN = userDNIterator.next();
                if (userInGroupsMembershipList(userDN, groupMembershipList))
                    validUserDNs.add(userDN);
            }
        } else {
            validUserDNs = userDNs;
        }
        return validUserDNs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.api.user.UsersRepository#removeUser(java.lang.String)
     */
    public void removeUser(String name) {
        log.warn("This user-repository is read-only. Modifications are not permitted.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.user.UsersRepository#test(java.lang.String,
     * java.lang.String)
     */
    public boolean test(String name, String password) {
        User u = getUserByName(name);
        if (u != null) {
            return u.verifyPassword(password);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.api.user.UsersRepository#addUser(org.apache.james.api
     * .user.User)
     */
    public boolean addUser(User user) {
        log.warn("This user-repository is read-only. Modifications are not permitted.");
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.user.UsersRepository#addUser(java.lang.String,
     * java.lang.Object)
     */
    public void addUser(String name, Object attributes) {
        log.warn("This user-repository is read-only. Modifications are not permitted.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.user.UsersRepository#addUser(java.lang.String,
     * java.lang.String)
     */
    public boolean addUser(String username, String password) {
        log.warn("This user-repository is read-only. Modifications are not permitted.");
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.api.user.UsersRepository#updateUser(org.apache.james
     * .api.user.User)
     */
    public boolean updateUser(User user) {
        log.warn("This user-repository is read-only. Modifications are not permitted.");
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging
     * .Log)
     */
    public void setLog(Log log) {
        this.log = log;
    }

}
