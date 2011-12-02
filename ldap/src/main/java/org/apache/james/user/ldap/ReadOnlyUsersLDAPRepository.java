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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.lifecycle.api.LogEnabled;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.api.model.User;
import org.slf4j.Logger;

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
 *      userObjectClass=&quot;inetOrgPerson&quot;/&gt;
 *  &lt;/users-store&gt;
 * </pre>
 * 
 * </br>
 * 
 * Its constituent attributes are defined as follows:
 * <ul>
 * <li><b>ldapHost:</b> The URL of the LDAP server to connect to.</li>
 * <li>
 * <b>principal:</b> (optional) The name (DN) of the user with which to initially bind to
 * the LDAP server.</li>
 * <li>
 * <b>credentials:</b> (optional) The password with which to initially bind to the LDAP
 * server.</li>
 * <li>
 * <b>userBase:</b>The context within which to search for user entities.</li>
 * <li>
 * <b>userIdAttribute:</b>The name of the LDAP attribute which holds user ids.
 * For example &quot;uid&quot; for Apache DS, or &quot;sAMAccountName&quot; for
 * Microsoft Active Directory.</li>
 * <li>
 * <b>userObjectClass:</b>The objectClass value for user nodes below the
 * userBase. For example &quot;inetOrgPerson&quot; for Apache DS, or
 * &quot;user&quot; for Microsoft Active Directory.</li>
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
 * </pre>
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
     * The URL of the LDAP server against which users are to be authenticated.
     * Note that users are actually authenticated by binding against the LDAP
     * server using the users &quot;dn&quot; and &quot;credentials&quot;.The
     * value of this field is taken from the value of the configuration
     * attribute &quot;ldapHost&quot;.
     */
    private String ldapHost;

    /**
     * The value of this field is taken from the configuration attribute
     * &quot;userIdAttribute&quot;. This is the LDAP attribute type which holds
     * the userId value. Note that this is not the same as the email address
     * attribute.
     */
    private String userIdAttribute;

    /**
     * The value of this field is taken from the configuration attribute
     * &quot;userObjectClass&quot;. This is the LDAP object class to use in the
     * search filter for user nodes under the userBase value.
     */
    private String userObjectClass;

    /**
     * This is the LDAP context/sub-context within which to search for user
     * entities. The value of this field is taken from the configuration
     * attribute &quot;userBase&quot;.
     */
    private String userBase;

    /**
     * The user with which to initially bind to the LDAP server. The value of
     * this field is taken from the configuration attribute
     * &quot;principal&quot;.
     */
    private String principal;

    /**
     * The password/credentials with which to initially bind to the LDAP server.
     * The value of this field is taken from the configuration attribute
     * &quot;credentials&quot;.
     */
    private String credentials;

    /**
     * Encapsulates the information required to restrict users to LDAP groups or
     * roles. This object is populated from the contents of the configuration
     * element &lt;restriction&gt;.
     */
    private ReadOnlyLDAPGroupRestriction restriction;

    /**
     * The connection handle to the LDAP server. This is the connection that is
     * built from the configuration attributes &quot;ldapHost&quot;,
     * &quot;principal&quot; and &quot;credentials&quot;.
     */
    private SimpleLDAPConnection ldapConnection;

    private Logger log;

    /**
     * Extracts the parameters required by the repository instance from the
     * James server configuration data. The fields extracted include
     * {@link #ldapHost}, {@link #userIdAttribute}, {@link #userBase},
     * {@link #principal}, {@link #credentials} and {@link #restriction}.
     * 
     * @param configuration
     *            An encapsulation of the James server configuration data.
     */
    public void configure(HierarchicalConfiguration configuration) throws ConfigurationException {
        ldapHost = configuration.getString("[@ldapHost]");
        // JAMES-1351 - ReadOnlyUsersLDAPRepository principal and credentials parameters should be optional
        //              Added an empty String as the default
        principal = configuration.getString("[@principal]", "");
        credentials = configuration.getString("[@credentials]", "");
        userBase = configuration.getString("[@userBase]");
        userIdAttribute = configuration.getString("[@userIdAttribute]");
        userObjectClass = configuration.getString("[@userObjectClass]");

        HierarchicalConfiguration restrictionConfig = null;
        // Check if we have a restriction we can use
        // See JAMES-1204
        if (configuration.containsKey("restriction[@memberAttribute]")) {
            restrictionConfig = configuration.configurationAt("restriction");
        }
        restriction = new ReadOnlyLDAPGroupRestriction(restrictionConfig);

    }

    /**
     * Initialises the user-repository instance. It will create a connection to
     * the LDAP host using the supplied configuration.
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
     * Indicates if the user with the specified DN can be found in the group
     * membership map&#45;as encapsulated by the specified parameter map.
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
     * Gets all the user entities taken from the LDAP server, as taken from the
     * search-context given by the value of the attribute {@link #userBase}.
     * 
     * @return A set containing all the relevant users found in the LDAP
     *         directory.
     * @throws NamingException
     *             Propagated from the LDAP communication layer.
     */
    private Set<String> getAllUsersFromLDAP() throws NamingException {
        Set<String> result = new HashSet<String>();

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(new String[] { "distinguishedName" });
        NamingEnumeration<SearchResult> sr = ldapConnection.getLdapContext().search(userBase, "(objectClass=" + userObjectClass + ")", sc);
        while (sr.hasMore()) {
            SearchResult r = sr.next();
            result.add(r.getNameInNamespace());
        }

        return result;
    }

    /**
     * Extract the user attributes for the given collection of userDNs, and
     * encapsulates the user list as a collection of {@link ReadOnlyLDAPUser}s.
     * This method delegates the extraction of a single user's details to the
     * method {@link #buildUser(String)}.
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
     * Given a userDN, this method retrieves the user attributes from the LDAP
     * server, so as to extract the items that are of interest to James.
     * Specifically it extracts the userId, which is extracted from the LDAP
     * attribute whose name is given by the value of the field
     * {@link #userIdAttribute}.
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
      SearchControls sc = new SearchControls();
      sc.setSearchScope(SearchControls.OBJECT_SCOPE);
      sc.setReturningAttributes(new String[] {userIdAttribute});
      sc.setCountLimit(1);

      NamingEnumeration<SearchResult> sr = ldapConnection.getLdapContext().search(userDN, "(objectClass=" + userObjectClass + ")", sc);
      
      if (!sr.hasMore())
          return null;

      Attributes userAttributes = sr.next().getAttributes();
      Attribute userName = userAttributes.get(userIdAttribute);
      
      if (!restriction.isActivated() || userInGroupsMembershipList(userDN, restriction.getGroupMembershipLists(ldapConnection)))
          return new ReadOnlyLDAPUser(userName.get().toString(), userDN, ldapHost);
      
      return null;
    }

    /**
     * @see UsersRepository#contains(java.lang.String)
     */
    public boolean contains(String name) throws UsersRepositoryException {
        if (getUserByName(name) != null) {
            return true;
        }
        return false;
    }

    /*
     * TODO
     * Should this be deprecated? At least the method isn't declared in the interface anymore
     * 
     * @see UsersRepository#containsCaseInsensitive(java.lang.String)
     */
    public boolean containsCaseInsensitive(String name) throws UsersRepositoryException {
        if (getUserByNameCaseInsensitive(name) != null) {
            return true;
        }
        return false;
    }

    /**
     * @see UsersRepository#countUsers()
     */
    public int countUsers() throws UsersRepositoryException {
        try {
            return getValidUsers().size();
        } catch (NamingException e) {
            log.error("Unable to retrieve user count from ldap", e);
            throw new UsersRepositoryException("Unable to retrieve user count from ldap", e);

        }
    }

    /*
     * TODO
     * Should this be deprecated? At least the method isn't declared in the interface anymore
     * 
     * @see UsersRepository#getRealName(java.lang.String)
     */
    public String getRealName(String name) throws UsersRepositoryException {
        User u = getUserByNameCaseInsensitive(name);
        if (u != null) {
            return u.getUserName();
        }

        return null;
    }

    /**
     * @see UsersRepository#getUserByName(java.lang.String)
     */
    public User getUserByName(String name) throws UsersRepositoryException {
      try {
        return buildUser(userIdAttribute + "=" + name + "," + userBase); 
      } catch (NamingException e) {
          log.error("Unable to retrieve user from ldap", e);
          throw new UsersRepositoryException("Unable to retrieve user from ldap", e);
  
      }
    }

    /*
     * TODO
     * Should this be deprecated? At least the method isn't declared in the interface anymore
     * 
     * @see UsersRepository#getUserByNameCaseInsensitive(java.lang.String)
     */
    public User getUserByNameCaseInsensitive(String name) throws UsersRepositoryException {
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
            throw new UsersRepositoryException("Unable to retrieve user from ldap", e);

        }
        return null;
    }

    /**
     * @see UsersRepository#list()
     */
    public Iterator<String> list() throws UsersRepositoryException {
        List<String> result = new ArrayList<String>();
        try {

            Iterator<ReadOnlyLDAPUser> userIt = buildUserCollection(getValidUsers()).iterator();

            while (userIt.hasNext()) {
                result.add(userIt.next().getUserName());
            }
        } catch (NamingException namingException) {
            throw new UsersRepositoryException("Unable to retrieve users list from LDAP due to unknown naming error.", namingException);
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

    /**
     * @see UsersRepository#removeUser(java.lang.String)
     */
    public void removeUser(String name) throws UsersRepositoryException {
        log.warn("This user-repository is read-only. Modifications are not permitted.");
        throw new UsersRepositoryException("This user-repository is read-only. Modifications are not permitted.");

    }

    /**
     * @see UsersRepository#test(java.lang.String, java.lang.String)
     */
    public boolean test(String name, String password) throws UsersRepositoryException {
        User u = getUserByName(name);
        if (u != null) {
            return u.verifyPassword(password);
        }
        return false;
    }

    /**
     * @see UsersRepository#addUser(java.lang.String, java.lang.String)
     */
    public void addUser(String username, String password) throws UsersRepositoryException {
        log.warn("This user-repository is read-only. Modifications are not permitted.");
        throw new UsersRepositoryException("This user-repository is read-only. Modifications are not permitted.");
    }

    /*
     * TODO
     * Should this be deprecated? At least the method isn't declared in the interface anymore
     * 
     * @see UsersRepository#updateUser(org.apache.james.api.user.User)
     */
    public void updateUser(User user) throws UsersRepositoryException {
        log.warn("This user-repository is read-only. Modifications are not permitted.");
        throw new UsersRepositoryException("This user-repository is read-only. Modifications are not permitted.");
    }

    /**
     * @see org.apache.james.lifecycle.api.LogEnabled#setLog(org.slf4j.Logger)
     */
    public void setLog(Logger log) {
        this.log = log;
    }

    /**
     * VirtualHosting not supported
     */
    public boolean supportVirtualHosting() throws UsersRepositoryException {
        return false;
    }

}
