/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.userrepository;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.Constants;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;

import javax.naming.AuthenticationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.*;

/**
 * Implementation of a Repository to store users.
 *
 * This clas is a dummy for the proposal!
 *
 * @version 1.0.0, 24/04/1999
 * @author  Charles Bennett
 */
public class UsersLDAPRepository
    extends AbstractLogEnabled
    implements UsersRepository, Configurable, Contextualizable, Initializable{

    private ComponentManager comp;

    private Logger logger;
    private String path;
    private String name;
    private String destination;
    private String type;
    private String model;
    private DirContext ctx;

    private String LDAPHost;
    private String rootNodeDN;
    private String rootURL;
    private String serverRDN;
    private String baseNodeDN;
    private String baseURL;
    private String mailAddressAttr;
    private String identAttr;
    private String authType;
    private String principal;
    private String password;
    private String usersDomain;
    private String membersAttr;
    private boolean manageGroupAttr;
    private String groupAttr;
    private boolean managePasswordAttr;
    private String passwordAttr;

    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(Context)
     */
    public void contextualize(Context context)
        throws ContextException {
        Collection serverNames
            = (Collection)context.get(Constants.SERVER_NAMES);
        usersDomain = (String)serverNames.iterator().next();
    }

    /**
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
     */
    public void compose(ComponentManager compMgr) {
        this.comp = comp;
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf)
        throws ConfigurationException {

        LDAPHost = conf.getChild("LDAPServer").getValue();
        rootNodeDN = conf.getChild("LDAPRoot").getValue();
        serverRDN = conf.getChild("ThisServerRDN").getValue();
        mailAddressAttr
            = conf.getChild("MailAddressAttribute").getValue();
        identAttr = conf.getChild("IdentityAttribute").getValue();
        authType = conf.getChild("AuthenticationType").getValue();
        principal = conf.getChild("Principal").getValue();
        password = conf.getChild("Password").getValue();

        membersAttr = conf.getChild("MembersAttribute").getValue();
        manageGroupAttr
            = conf.getChild("ManageGroupAttribute").getValueAsBoolean( false );
        groupAttr = conf.getChild("GroupAttribute").getValue();
        managePasswordAttr = conf.getChild("ManagePasswordAttribute").getValueAsBoolean( false );
        passwordAttr = conf.getChild("PasswordAttribute").getValue();
    }

    public void setServerRoot() {
        StringBuffer serverRootBuffer =
            new StringBuffer(128)
                    .append(serverRDN)
                    .append(", ")
                    .append(rootNodeDN);
        this.setBase(serverRootBuffer.toString());
    }

    public void setBase(String base) {
        baseNodeDN = base;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        //setServerRoot();
        StringBuffer urlBuffer =
            new StringBuffer(128)
                    .append(LDAPHost)
                    .append("/");
        rootURL = urlBuffer.toString() + rootNodeDN;
        baseURL = urlBuffer.toString() + baseNodeDN;

        getLogger().info("Creating initial context from " + baseURL);
        //System.out.println("Creating initial context from " + baseURL);

        Hashtable env = new Hashtable();
        env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(javax.naming.Context.PROVIDER_URL, baseURL);

        try {
            ctx = new InitialDirContext(env); // Could throw a NamingExcpetion
        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        }


        getLogger().info("Initial context initialized from " + baseURL);
    }



    public String getChildDestination(String childName) {

        String destination = null;
        String filter = "cn=" + childName;
        SearchControls ctls = new SearchControls();

        try {

            NamingEnumeration result  = ctx.search("", filter, ctls);

            if (result.hasMore()) {
                StringBuffer destinationBuffer =
                    new StringBuffer(128)
                            .append("cn=")
                            .append(childName)
                            .append(", ")
                            .append(baseNodeDN);
                destination = destinationBuffer.toString();
                getLogger().info("Pre-exisisting LDAP node: " + destination);
            } else {
                Attributes attrs = new BasicAttributes(true);
                Attribute objclass = new BasicAttribute("objectclass");
                objclass.add("top");
                objclass.add("rfc822MailGroup");
                attrs.put(objclass);
                Attribute cname = new BasicAttribute("cn");
                cname.add(childName);
                attrs.put(cname);
                Attribute owner = new BasicAttribute("owner");
                owner.add("JAMES-unassigned");
                attrs.put(owner);

                ctx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, authType);
                ctx.addToEnvironment(javax.naming.Context.SECURITY_PRINCIPAL, principal);
                ctx.addToEnvironment(javax.naming.Context.SECURITY_CREDENTIALS, password);

                ctx.createSubcontext("cn=" + childName, attrs);
                ctx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, "none");

                StringBuffer destinationBuffer =
                    new StringBuffer(128)
                            .append("cn=")
                            .append(childName)
                            .append(", ")
                            .append(baseNodeDN);
                destination = destinationBuffer.toString();
                getLogger().info("Created new LDAP node: " + destination);
            }
        } catch (NamingException e) {
            System.out.println("Problem with child nodes " + e.getMessage());
            e.printStackTrace();
        }

        return destination;
    }

    /**
     * List users in repository.
     *
     * @return Iterator over a collection of Strings, each being one user in the repository.
     */
    public Iterator list() {

        List result = new ArrayList();
        String filter = mailAddressAttr + "=*";
        String[] attrIDs = {membersAttr};

        try {
            Attribute members
                = ctx.getAttributes("", attrIDs).get(membersAttr);
            if (members != null) {
                NamingEnumeration enum = members.getAll();
                while (enum.hasMore()) {
                    result.add((String)enum.next());
                }
            }
        } catch (NamingException e) {
            getLogger().error("Problem listing mailboxes. " + e );

        }
        return result.iterator();
    }

    // Methods from interface UsersRepository --------------------------

    /**
     * Update the repository with the specified user object.  Unsupported for
     * this user repository type.
     *
     * @return false
     */
    public boolean addUser(User user) {
        return false;
    }

    public  User getUserByName(String name) {
        return new DefaultUser("dummy", "dummy");
    }

    public User getUserByNameCaseInsensitive(String name) {
        return getUserByName(name);
    }

    public boolean containsCaseInsensitive(String name) {
        return contains(name);
    }

    // TODO: This is in violation of the contract for the interface.
    //       Should only return null if the user doesn't exist.  Otherwise
    //       this should return a consistent string representation of the name
    public String getRealName(String name) {
        return null;
    }

    public boolean updateUser(User user) {
        return false;
    }

    public boolean test(String name, String password) {
        return false;
    }

    /**
     * Adds userName to the MemberAttribute (specified in conf.xml) of this
     * node.
     * If ManageGroupAttribute (conf.xml) is TRUE then calls addGroupToUser.
     */
    public synchronized void addUser(String userName, Object attributes) {

        String[] attrIDs = {membersAttr};

        // First, add username to mailGroup at baseNode

        try {
            Attribute members = ctx.getAttributes("", attrIDs).get(membersAttr);


            if (members != null && members.contains(userName)) {//user already here
                StringBuffer infoBuffer =
                    new StringBuffer(64)
                            .append("Found ")
                            .append(userName)
                            .append(" already in mailGroup. ");
                getLogger().info(infoBuffer.toString());
                //System.out.println(infoBuffer.toString());

            } else {
                ctx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, authType);
                ctx.addToEnvironment(javax.naming.Context.SECURITY_PRINCIPAL, principal);
                ctx.addToEnvironment(javax.naming.Context.SECURITY_CREDENTIALS, password);

                ModificationItem[] mods = new ModificationItem[1];
                mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(membersAttr, userName));

                ctx.modifyAttributes("", mods);

                ctx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, "none");
                StringBuffer infoBuffer =
                    new StringBuffer(128)
                            .append(userName)
                            .append(" added to mailGroup ")
                            .append(baseNodeDN);
                getLogger().info(infoBuffer.toString());
                //System.out.println(infoBuffer.toString());
            }
        } catch (NamingException e) {
            StringBuffer exceptionBuffer =
                new StringBuffer(256)
                        .append("Problem adding user ")
                        .append(userName)
                        .append(" to: ")
                        .append(baseNodeDN)
                        .append(e);
            getLogger().error(exceptionBuffer.toString());
        }

        // Add attributes to user objects, if necessary

        if (manageGroupAttr) {
            addGroupToUser(userName);
        }

        if (managePasswordAttr) {
            String userPassword = (String) attributes; // Not yet implemented
        }
    }

    private void addGroupToUser(String userName) {
        String[] attrIDs = {membersAttr};

        Hashtable env = new Hashtable();
        env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(javax.naming.Context.PROVIDER_URL, rootURL);

        DirContext rootCtx = null;
        try {
            rootCtx = new InitialDirContext(env);

            String[] returnAttrs = {groupAttr};
            SearchControls ctls = new SearchControls();
            ctls.setReturningAttributes(attrIDs);
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            StringBuffer filterBuffer =
                new StringBuffer(128)
                        .append(mailAddressAttr)
                        .append("=")
                        .append(userName)
                        .append("@")
                        .append(usersDomain);
            String filter = filterBuffer.toString();

            NamingEnumeration enum  = rootCtx.search("", filter, ctls);

            if (enum.hasMore()) { // ie User is in Directory
                SearchResult newSr = (SearchResult)enum.next();
                String userDN = newSr.getName();
                Attribute servers = rootCtx.getAttributes(userDN, returnAttrs).get(groupAttr);


                if (servers != null && servers.contains(baseNodeDN)) {//server already registered for user
                    getLogger().info(baseNodeDN + " already in user's Groups. " );
                    //System.out.println(baseNodeDN + " already in user's Groups. ");

                } else {

                    rootCtx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, authType);
                    rootCtx.addToEnvironment(javax.naming.Context.SECURITY_PRINCIPAL, principal);
                    rootCtx.addToEnvironment(javax.naming.Context.SECURITY_CREDENTIALS, password);

                    rootCtx.modifyAttributes(userDN, DirContext.ADD_ATTRIBUTE, new BasicAttributes(groupAttr, baseNodeDN, true));

                    rootCtx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, "none");
                    getLogger().info(baseNodeDN + " added to user's groups ");
                    //System.out.println(baseNodeDN + " added to users' groups ");

                }

            } else {
                StringBuffer infoBuffer =
                    new StringBuffer(64)
                            .append("User ")
                            .append(userName)
                            .append(" not in directory.");
                getLogger().info(infoBuffer.toString());
                // System.out.println(infoBuffer.toString());

            }
        } catch (NamingException e) {
            getLogger().error("Problem adding group to user " + userName);
            //System.out.println("Problem adding group to user " + userName);
            //System.out.println(e.getMessage());
            //e.printStackTrace();
        } finally {
            closeDirContext(rootCtx);
        }
    }

    public synchronized Object getAttributes(String name) {
        return null;
    }


    public synchronized void removeUser(String userName) {
        String[] attrIDs = {membersAttr};

        try {
            Attribute members = ctx.getAttributes("", attrIDs).get(membersAttr);
            if (members == null) {
                System.out.println("UsersLDAPRepository - Null list attribute.");

            } else  if (!members.contains(userName)) {//user not here
                getLogger().info(userName + " missing from mailGroup. ");
                //System.out.println(userName + " missing from mailGroup. ");

            } else {
                // First, remove username from mailGroup at baseNode

                ctx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, authType);
                ctx.addToEnvironment(javax.naming.Context.SECURITY_PRINCIPAL, principal);
                ctx.addToEnvironment(javax.naming.Context.SECURITY_CREDENTIALS, password);

                ModificationItem[] mods = new ModificationItem[1];
                mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(membersAttr, userName));

                ctx.modifyAttributes("", mods);


                ctx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, "none");
                getLogger().info(userName + " removed from mailGroup. ");
                //System.out.println(userName + " removed from mailGroup. ");
            }
        } catch (NamingException e) {
            StringBuffer exceptionBuffer =
                new StringBuffer(256)
                        .append("Problem removing user ")
                        .append(userName)
                        .append(": ")
                        .append(e);
            getLogger().error(exceptionBuffer.toString());
            //System.out.println("Problem removing user " + userName);
            //System.out.println(e.getMessage());
            //e.printStackTrace();
        }
        if (manageGroupAttr) {
            removeGroupFromUser(userName);
        }

        if (managePasswordAttr) {
            // not yet implemented
        }

    }

    public void removeGroupFromUser(String userName) {

        Hashtable env = new Hashtable();
        env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(javax.naming.Context.PROVIDER_URL, rootURL);


        DirContext rootCtx = null;
        try {
            rootCtx = new InitialDirContext(env);

            // Find directory entry
            String[] returnAttrs = {groupAttr};
            SearchControls ctls = new SearchControls();
            ctls.setReturningAttributes(returnAttrs);
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            StringBuffer filterBuffer =
                new StringBuffer(128)
                        .append(mailAddressAttr)
                        .append("=")
                        .append(userName)
                        .append("@")
                        .append(usersDomain);
            String filter = filterBuffer.toString();

            NamingEnumeration enum  = rootCtx.search("", filter, ctls);

            if (enum.hasMore()) { // ie User is in Directory
                SearchResult newSr = (SearchResult)enum.next();
                String userDN = newSr.getName();

                System.out.println("Found user entry: " + userDN);

                Attribute servers = rootCtx.getAttributes(userDN, returnAttrs).get(groupAttr);
                if (servers == null) { //should not happen
                    getLogger().info("GroupAttribute missing from user: " + userName);
                    // System.out.println("GroupAttribute missing from user: " + userName );

                } else if (!servers.contains(baseNodeDN)) {//server not registered for user
                    getLogger().info(baseNodeDN + " missing from users' Groups. " );
                    //System.out.println(baseNodeDN + " missing from users' Groups. ");

                } else {

                    rootCtx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, authType);
                    rootCtx.addToEnvironment(javax.naming.Context.SECURITY_PRINCIPAL, principal);
                    rootCtx.addToEnvironment(javax.naming.Context.SECURITY_CREDENTIALS, password);

                    ModificationItem[] mods = new ModificationItem[1];
                    mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(groupAttr, baseNodeDN));

                    rootCtx.modifyAttributes(userDN, mods);

                    //rootCtx.modifyAttributes(userDN, DirContext.REPLACE_ATTRIBUTE, changes);

                    rootCtx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, "none");
                    getLogger().info(baseNodeDN + " removed from users' groups " );
                    //System.out.println(baseNodeDN + " removed from users' groups ");

                }

            } else {
                StringBuffer infoBuffer =
                    new StringBuffer(64)
                            .append("User ")
                            .append(userName)
                            .append(" not in directory.");
                getLogger().info(infoBuffer.toString());
                //System.out.println(infoBuffer.toString());

            }
        } catch (NamingException e) {
            StringBuffer exceptionBuffer =
                new StringBuffer(256)
                        .append("Problem removing user ")
                        .append(userName)
                        .append(e);
            getLogger().error(exceptionBuffer.toString());
            //System.out.println("Problem removing user " + userName);
            //System.out.println(e.getMessage());
            //e.printStackTrace();
        } finally {
            closeDirContext(rootCtx);
            rootCtx = null;
        }
    }


    public boolean contains(String name) {
        boolean found = false;
        String[] attrIDs = {membersAttr};

        try {
            Attribute members = ctx.getAttributes("", attrIDs).get(membersAttr);
            if (members != null && members.contains(name)) {
                found = true;
                StringBuffer infoBuffer =
                    new StringBuffer(64)
                            .append("Found ")
                            .append(name)
                            .append(" in mailGroup. ");
                getLogger().info(infoBuffer.toString());
                //System.out.println(infoBuffer.toString());
            }
        } catch (NamingException e) {
            StringBuffer exceptionBuffer =
                new StringBuffer(256)
                        .append("Problem finding user ")
                        .append(name)
                        .append(" : ")
                        .append(e);
            getLogger().error(exceptionBuffer.toString());
            //System.out.println(exceptionBuffer.toString());
        }
        return found;
    }


    public boolean test(String name, Object attributes) {
        boolean result = false;
        boolean foundFlag = false;
        String testPassword = (String) attributes;
        String userDN = null;

        try {
            String[] returnAttrs = {identAttr, passwordAttr};
            SearchControls ctls = new SearchControls();
            ctls.setReturningAttributes(returnAttrs);
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            StringBuffer filterBuffer = 
                new StringBuffer(128)
                        .append(mailAddressAttr)
                        .append("=")
                        .append(name)
                        .append("@")
                        .append(usersDomain);
            String filter = filterBuffer.toString();

            Hashtable env = new Hashtable();
            env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(javax.naming.Context.PROVIDER_URL, rootURL);
            DirContext rootCtx = null;

            try {
                rootCtx = new InitialDirContext(env);
    
                NamingEnumeration enum  = rootCtx.search("", filter, ctls);
                if (enum.hasMore()) { // ie User is in Directory
                    SearchResult sr = (SearchResult)enum.next();
                    String userRDN = sr.getName();
                    StringBuffer userDNBuffer =
                        new StringBuffer(128)
                                .append(userRDN)
                                .append(", ")
                                .append(rootNodeDN);
                    userDN = userDNBuffer.toString();
                    foundFlag = true;
                    //System.out.println("UserDN is : " + userDN);
                }
            } finally {
                closeDirContext(rootCtx);
            }
        } catch (Exception e) {
            StringBuffer exceptionBuffer =
                new StringBuffer(256)
                        .append("Problem finding user ")
                        .append(name)
                        .append(" for password test.")
                        .append(e); 
            getLogger().error(exceptionBuffer.toString());
            //e.getMessage();
            //e.printStackTrace();
        }

        if (foundFlag) { // ie User is in Directory
            Hashtable env2 = new Hashtable();
            env2.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env2.put(javax.naming.Context.PROVIDER_URL, rootURL);
            env2.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
            env2.put(javax.naming.Context.SECURITY_PRINCIPAL, userDN);
            env2.put(javax.naming.Context.SECURITY_CREDENTIALS, testPassword);
            //System.out.println("Creating initial context from " + baseURL);

            DirContext testCtx = null;
            try {
                testCtx = new InitialDirContext(env2);
                result = true;

            } catch (AuthenticationException ae) {
                result = false;
                StringBuffer exceptionBuffer =
                    new StringBuffer(256)
                            .append("Attempt to authenticate with incorrect password for ")
                            .append(name)
                            .append(" : ")
                            .append(ae); 
                getLogger().error(exceptionBuffer.toString());
                //System.out.println(exceptionBuffer.toString());
                //System.out.println(ae.getMessage());
                //ae.printStackTrace();
            } catch (Exception e) {
                StringBuffer exceptionBuffer =
                    new StringBuffer(256)
                            .append("Problem checking password for ")
                            .append(name)
                            .append(" : ")
                            .append(e); 
                getLogger().error(exceptionBuffer.toString());
                //System.out.println(exceptionBuffer.toString());
                //System.out.println(e.getMessage());
                //e.printStackTrace();
            } finally {
                closeDirContext(testCtx);
            }
        }
        return result;

    }

    public int countUsers() {

        String[] attrIDs = {membersAttr};
        int result = -1;

        try {
            Attribute members = ctx.getAttributes("", attrIDs).get(membersAttr);
            if (members != null) {
                result = members.size();
            } else {
                result = 0;
            }
        } catch (NamingException e) {
            getLogger().error("Problem counting users: "  + e);
            //System.out.println("Problem counting users. ");
        }
        return result;
    }

    public String getDomains() {
        return usersDomain;
    }

    /**
     * Disposes of all open directory contexts
     *
     * @throws Exception if an error is encountered during shutdown
     */
    public void dispose() throws Exception {
        closeDirContext(ctx);
        ctx = null;
    }

    private void closeDirContext(DirContext ctx) {
        try {
            if (ctx != null) {
                ctx.close();
            }
        } catch (NamingException ne) {
            getLogger().warn("UsersLDAPRepository: Unexpected exception encountered while closing directory context: " + ne);
        }
    }
}


