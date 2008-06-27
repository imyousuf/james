/*****************************************************************************
  UsersTownRepository
*****************************************************************************/

package org.apache.james.userrepository;

import java.io.*;
import java.util.*;

import org.apache.avalon.blocks.*;
import org.apache.avalon.*;
//import org.apache.avalon.utils.*;
import org.apache.log.LogKit;
import org.apache.log.Logger;
import org.apache.james.services.UsersRepository;

import com.workingdogs.town.*;

/**
 * Implementation of a Repository to store users in database.
 * @version 1.0.0, 10/01/2000
 * @author  Ivan Seskar, Upside Technologies <seskar@winlab.rutgers.edu>
 */
public class UsersTownRepository implements UsersRepository, Loggable, Component, Configurable {

    //private String destination;
    //private String repositoryName;

    private String conndefinition;
    private String tableName;

    //  System defined logger funtion
    //private ComponentManager comp;
    private Logger logger;

    // Constructor - empty
    public UsersTownRepository() {
    }

    public void setLogger(final Logger a_Logger) {
	logger = a_Logger;
    }

    public void configure(Configuration conf) throws ConfigurationException {
	//  destination = conf.getChild("destination").getAttribute("URL");
	//  repositoryName = destination.substring(destination.indexOf("//") + 2);
	conndefinition= conf.getChild("conn").getValue();
	tableName = conf.getChild("table").getValue("Users");

    }

 
	
    // Methods from interface Repository

    public synchronized void addUser(String strUserName, Object attributes) {
        try {
            TableDataSet MRUser = new TableDataSet(ConnDefinition.getInstance(conndefinition), tableName);
            MRUser.setWhere("username = '" + strUserName+"'");
            Record user = null;
            if (MRUser.size() == 0) {
                // file://Add new user
                user = MRUser.addRecord();
                user.setValue("username", strUserName);
                user.setValue("password", attributes.toString());
                user.save();
            } else {
                // file://User already exists: reject add
                logger.warn("User "+strUserName+" already exists."); 
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception caught while storing user: " + e);
        }
    }

    public synchronized Object getAttributes(String strUserName) {
        try {
            TableDataSet MRUser = new TableDataSet(ConnDefinition.getInstance(conndefinition), tableName);
            MRUser.setWhere("username = '" + strUserName+"'");
            if (MRUser.size() == 0) {
                logger.warn("User "+strUserName+" could not be found while fetching password.");
                return(null);
            } else {
                Record user = MRUser.getRecord(0);
                return ((Object) user.getAsString("Password"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception while retrieving password: " + e.getMessage());
        }
    }

    public synchronized void removeUser(String strUserName) {
        try {
            TableDataSet MRUser = new TableDataSet(ConnDefinition.getInstance(conndefinition), tableName);
            MRUser.setWhere("username = '" + strUserName + "'");
            if (MRUser.size() == 0) {
                // file://User doesn't exists: reject delete
                logger.warn("User: " + strUserName + " does not exist.  Cannot delete");
            } else {
                Record user = MRUser.getRecord(0);
                user.markToBeDeleted();
                user.save();
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception while deleting user: " + e.getMessage());
        }
    }

    public boolean contains(String strUserName) {
        try {
            TableDataSet MRUser = new TableDataSet(ConnDefinition.getInstance(conndefinition), tableName);
            MRUser.setWhere("username = '" + strUserName + "'");
            if (MRUser.size() > 0) {
                return true;   // User exists
            } else {
                return false;  // User does not exist
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception while retrieving user: " + e.getMessage());
        }
    }

    public boolean test(String strUserName, Object attributes) {
        try {
            TableDataSet MRUser = new TableDataSet(ConnDefinition.getInstance(conndefinition), tableName);
            MRUser.setWhere("username = '" + strUserName + "'");
            if (MRUser.size() > 0) {
                // UserName exists - check if the password is OK
                Record user = MRUser.getRecord(0);
                return(user.getAsString("Password").equals(attributes.toString()));
            } else {
                // file://UserName does not exist
                logger.warn("User "+strUserName+" doesn't exist");
                return(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception caught while testing UserName: " + e.getMessage());
        }
    }

    public int countUsers() {
    try {
        TableDataSet MRUser = new TableDataSet(ConnDefinition.getInstance(conndefinition), tableName);
        int nSize = MRUser.size();
        return (int) nSize;
    } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("Exception caught while testing UserName: " + e.getMessage());
    }
    }

    public Iterator list() {
        List list = new ArrayList();

        try {
            TableDataSet users = new TableDataSet(ConnDefinition.getInstance(conndefinition), tableName);
            for (int i = 0; i < users.size(); i++) {
                list.add(users.getRecord(i).getAsString("username"));
            }
        } catch (Exception e) {
            logger.error("Problem listing mailboxes. " + e );
            e.printStackTrace();
            throw new RuntimeException("Exception while listing users: " + e.getMessage());
        }
        return list.iterator();
    }

}
