/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.userrepository;

import java.io.*;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.Loggable;
import org.apache.james.Constants;
import org.apache.james.services.UsersRepository;
import org.apache.log.Logger;

/**
 * Implementation of a Repository to store users.
 * @version 1.0.0, 24/04/1999
 * @author  Charles Bennett
 */
public class UsersLDAPRepository
    implements UsersRepository, Loggable, Configurable, Contextualizable, Initializable{

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


    public void setLogger(final Logger a_Logger) {
        logger = a_Logger;
    }

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
            = conf.getChild("ManageGroupAttribute").getValue().equals("TRUE");
        groupAttr = conf.getChild("GroupAttribute").getValue();
        managePasswordAttr = conf.getChild("ManagePasswordAttribute").getValue().equals("TRUE");
        passwordAttr = conf.getChild("PasswordAttribute").getValue();
    }

    public void compose(ComponentManager compMgr) {
        this.comp = comp;
    }

    public void contextualize(Context context) 
        throws ContextException {
        Collection serverNames
            = (Collection)context.get(Constants.SERVER_NAMES);
        usersDomain = (String)serverNames.iterator().next();
    }

    public void setServerRoot() {
        this.setBase(serverRDN +", " + rootNodeDN);
    }

    public void setBase(String base) {
        baseNodeDN = base;
    }

    public void initialize() throws Exception {
        //setServerRoot();
        rootURL = LDAPHost + "/" + rootNodeDN;
        baseURL = LDAPHost + "/" + baseNodeDN;

        logger.info("Creating initial context from " + baseURL);
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


        logger.info("Initial context initialised from " + baseURL);
    }



    public String getChildDestination(String childName) {

        String destination = null;
        String filter = "cn=" + childName;
        SearchControls ctls = new SearchControls();

        try {

            NamingEnumeration result  = ctx.search("", filter, ctls);

            if (result.hasMore()) {
                destination = "cn=" + childName + ", " + baseNodeDN;
                logger.info("Pre-exisisting LDAP node: " + destination);
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

                ctx.createSubcontext("cn="+childName, attrs);
                ctx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, "none");

                destination = "cn=" + childName + ", " + baseNodeDN;
                logger.info("Created new LDAP node: " + destination);
            }
        } catch (NamingException e) {
            System.out.println("Problem with child nodes " + e.getMessage());
            e.printStackTrace();
        }

        return destination;
    }

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
            logger.error("Problem listing mailboxes. " + e );

        }
        return result.iterator();
    }




    // Methods from interface UsersRepository --------------------------

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
                logger.info("Found " + userName + " already in mailGroup. " );
                //System.out.println("Found " + userName + " already in mailGroup. ");

            } else {
                ctx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, authType);
                ctx.addToEnvironment(javax.naming.Context.SECURITY_PRINCIPAL, principal);
                ctx.addToEnvironment(javax.naming.Context.SECURITY_CREDENTIALS, password);

                ModificationItem[] mods = new ModificationItem[1];
                mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(membersAttr, userName));

                ctx.modifyAttributes("", mods);

                ctx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, "none");
                logger.info(userName + " added to mailGroup " + baseNodeDN );
                //System.out.println(userName + " added to mailGroup " + baseNodeDN);
            }
        } catch (NamingException e) {
            logger.error("Problem adding user " + userName + " to: " + baseNodeDN + e);
            //System.out.println("Problem adding user " + userName + " to: " + baseNodeDN);
            //System.out.println(e.getMessage());
            //e.printStackTrace();
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

        try {
            DirContext rootCtx = new InitialDirContext(env);

            String[] returnAttrs = {groupAttr};
            SearchControls ctls = new SearchControls();
            ctls.setReturningAttributes(attrIDs);
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String filter = mailAddressAttr + "=" + userName + "@" + usersDomain;

            NamingEnumeration enum  = rootCtx.search("", filter, ctls);

            if (enum.hasMore()) { // ie User is in Directory
                SearchResult newSr = (SearchResult)enum.next();
                String userDN = newSr.getName();
                Attribute servers = rootCtx.getAttributes(userDN, returnAttrs).get(groupAttr);


                if (servers != null && servers.contains(baseNodeDN)) {//server already registered for user
                    logger.info(baseNodeDN + " already in user's Groups. " );
                    //System.out.println(baseNodeDN + " already in user's Groups. ");

                } else {

                    rootCtx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, authType);
                    rootCtx.addToEnvironment(javax.naming.Context.SECURITY_PRINCIPAL, principal);
                    rootCtx.addToEnvironment(javax.naming.Context.SECURITY_CREDENTIALS, password);

                    rootCtx.modifyAttributes(userDN, DirContext.ADD_ATTRIBUTE, new BasicAttributes(groupAttr, baseNodeDN, true));

                    rootCtx.addToEnvironment(javax.naming.Context.SECURITY_AUTHENTICATION, "none");
                    logger.info(baseNodeDN + " added to user's groups ");
                    //System.out.println(baseNodeDN + " added to users' groups ");

                }

            } else {
                logger.info("User " + userName + " not in Directory.");
                // System.out.println("User " + userName + " not in Directory.");

            }
            rootCtx.close();



        } catch (NamingException e) {
            logger.error("Problem adding group to user " + userName);
            //System.out.println("Problem adding group to user " + userName);
            //System.out.println(e.getMessage());
            //e.printStackTrace();
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
                logger.info(userName + " missing from mailGroup. ");
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
                logger.info(userName + " removed from mailGroup. ");
                //System.out.println(userName + " removed from mailGroup. ");
            }
        } catch (NamingException e) {
            logger.error("Problem removing user " + userName + e);
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


        try {
            DirContext rootCtx = new InitialDirContext(env);

            // Find directory entry
            String[] returnAttrs = {groupAttr};
            SearchControls ctls = new SearchControls();
            ctls.setReturningAttributes(returnAttrs);
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String filter = mailAddressAttr + "=" + userName + "@" + usersDomain;

            NamingEnumeration enum  = rootCtx.search("", filter, ctls);

            if (enum.hasMore()) { // ie User is in Directory
                SearchResult newSr = (SearchResult)enum.next();
                String userDN = newSr.getName();

                System.out.println("Found user entry: " + userDN);

                Attribute servers = rootCtx.getAttributes(userDN, returnAttrs).get(groupAttr);
                if (servers == null) { //should not happen
                    logger.info("GroupAttribute missing from user: " + userName);
                    // System.out.println("GroupAttribute missing from user: " + userName );

                } else if (!servers.contains(baseNodeDN)) {//server not registered for user
                    logger.info(baseNodeDN + " missing from users' Groups. " );
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
                    logger.info(baseNodeDN + " removed from users' groups " );
                    //System.out.println(baseNodeDN + " removed from users' groups ");

                }

            } else {
                logger.info("User " + userName + " not in Directory.");
                //System.out.println("User " + userName + " not in Directory.");

            }
            rootCtx.close();

        } catch (NamingException e) {
            logger.error("Problem removing user " + userName + e);
            //System.out.println("Problem removing user " + userName);
            //System.out.println(e.getMessage());
            //e.printStackTrace();
        }

    }


    public boolean contains(String name) {
        boolean found = false;
        String[] attrIDs = {membersAttr};

        try {
            Attribute members = ctx.getAttributes("", attrIDs).get(membersAttr);
            if (members != null && members.contains(name)) {
                found = true;
                logger.info("Found " + name + " in mailGroup. " );
                //System.out.println("Found " + name + " in mailGroup. ");
            }
        } catch (NamingException e) {
            logger.error("Problem finding user " + name + e);
            //System.out.println("Problem finding user " + name + " : " + e);
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
            String filter = mailAddressAttr + "=" + name + "@" + usersDomain;

            Hashtable env = new Hashtable();
            env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(javax.naming.Context.PROVIDER_URL, rootURL);
            DirContext rootCtx = new InitialDirContext(env);

            NamingEnumeration enum  = rootCtx.search("", filter, ctls);
            if (enum.hasMore()) { // ie User is in Directory
                SearchResult sr = (SearchResult)enum.next();
                String userRDN = sr.getName();
                userDN = userRDN +", " + rootNodeDN;
                foundFlag = true;
                //System.out.println("UserDN is : " + userDN);
            }

            rootCtx.close();
        } catch (Exception e) {
            logger.error("Problem finding user " + name + " for password test." +e);
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

            try {
                DirContext testCtx = new InitialDirContext(env2);
                result = true;
                testCtx.close();

            } catch (AuthenticationException ae) {
                result = false;
                logger.error("Attempt to authenticate with incorrect password for " + name + " : " + ae );
                //System.out.println("Attempt to authenticate with incorrect password for " + name + " : " + ae);
                //System.out.println(ae.getMessage());
                //ae.printStackTrace();
            } catch (Exception e) {
                logger.error("Problem checking password for " + name + " : " + e );
                //System.out.println("Problem checking password for " + name + " : " + e);
                //System.out.println(e.getMessage());
                //e.printStackTrace();
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
            logger.error("Problem counting users: "  + e);
            //System.out.println("Problem counting users. ");
        }
        return result;
    }

    public String getDomains() {
        return usersDomain;
    }

    /**
     * Disposes of all open directory contexts.
     * Based on signature from interface Disposable in new Avalon
     */
    public void dispose() throws Exception {
        ctx.close();
    }

}


