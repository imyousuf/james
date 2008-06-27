/*****************************************************************************
  UsersTownRepository
*****************************************************************************/

package org.apache.james.userrepository;

import org.apache.avalon.blocks.*;
import org.apache.avalon.*;
import org.apache.avalon.utils.*;
import java.util.*;
import java.io.*;
import com.workingdogs.town.*;

/**
 * Implementation of a Repository to store users in database.
 * @version 1.0.0, 10/01/2000
 * @author  Ivan Seskar, Upside Technologies <seskar@winlab.rutgers.edu>
 */
public class UsersTownRepository implements UsersRepository, Configurable {

    private String name;
    private String type;
    private String model;

    private String destination;
    private String prefix;
    private String repositoryName;

    private String conndefinition;
    private String tableName;

    //  System defined logger funtion
    private ComponentManager comp;
    private Logger logger;

    // Constructor - empty
    public UsersTownRepository() {
    }

    // Methods from interface Repository
    public void setAttributes(String name, String destination, String type, String model) {
        this.name = name;
        this.model = model;
        this.type = type;

        this.destination = destination;
        int slash = destination.indexOf("//");
        prefix = destination.substring(0, slash + 2);
        repositoryName = destination.substring(slash + 2);
    }


    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
        // Store logger
        this.logger = (Logger) comp.getComponent(Interfaces.LOGGER);
    }

    public void setConfiguration(Configuration conf) {
        conndefinition = conf.getConfiguration("conn").getValue();
        tableName = conf.getConfiguration("table").getValue("Users");
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getModel() {
        return model;
    }

    public String getChildDestination(String childName) {
        return prefix + repositoryName + "/" + childName;
    }

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
                logger.log("User "+strUserName+" already exists.",
                "UserManager", logger.WARNING);  // old Avalon logger format
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
                logger.log("User "+strUserName+" could not be found while fetching password.",
                "UserManager", logger.WARNING);
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
                logger.log("User: " + strUserName + " does not exist.  Cannot delete", "UserManager", logger.WARNING);
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
                logger.log("User "+strUserName+" doesn't exist", "UserManager", logger.WARNING);
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

    public Enumeration list() {
        Vector list = new Vector();

        try {
            TableDataSet users = new TableDataSet(ConnDefinition.getInstance(conndefinition), tableName);
            for (int i = 0; i < users.size(); i++) {
                list.add(users.getRecord(i).getAsString("username"));
            }
        } catch (Exception e) {
            logger.log("Problem listing mailboxes. " + e ,"UserManager", logger.ERROR);
            e.printStackTrace();
            throw new RuntimeException("Exception while listing users: " + e.getMessage());
        }
        return list.elements();
    }

}
