/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.client;

import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.rmi.Naming;
import java.rmi.RemoteException;
import org.apache.james.remotemanager.UserManager;
import org.apache.james.remotemanager.UserManagerAdmin;


/**
 * This class demonstrate how to use the remote user manager.
 * 
 * @author <a href="mailto:buchi@email.com">Gabriel Bucher</a>
 */
public class UserManagerAdminClient {

    private UserManager userManager;


    public UserManagerAdminClient() {
    }

    private void print(String text) {
        System.out.print(text);
    }

    private void println(String text) {
        print(text + "\n");
    }

    
    public void execute(String rmiURL)
            throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String login = null;
        String password = null;
        
        println("JAMES Remote UserManager Administration Tool");
        println("--------------------------------------------\n");
        println("Please enter your login and password");
        print("Login   : ");
        login = in.readLine();
        print("Password: ");
        password = in.readLine();

        UserManagerAdmin uma = (UserManagerAdmin)Naming.lookup(rmiURL);
        while ((userManager = uma.login(login, password)) == null) {
            println("login failed!\n");
            print("Login   : ");
            login = in.readLine();
            print("Password: ");
            password = in.readLine();
        }

        println("Welcome " + login + "! HELP for a list of commands");
        print("> ");
        while (parseCommand(in.readLine())) {
            print("\n> ");
        }
        
        println("\nGoodbye...");
    }

    private boolean parseCommand(String commandLine) {
        if ((commandLine == null) || (commandLine.equals("")))  {
            return true;
        }
        StringTokenizer commandLineTokenizer = new StringTokenizer(commandLine.trim(), " ");
        int argumentCount = commandLineTokenizer.countTokens();
        if (argumentCount < 1) {
            return true;
        }
        String command = commandLineTokenizer.nextToken();
        String[] arguments = null;
        arguments = new String[argumentCount - 1];
        int i = 0;
        while (commandLineTokenizer.hasMoreTokens()) {
            arguments[i] = commandLineTokenizer.nextToken();
            i++;
        }

        try {
            if (command.equalsIgnoreCase("quit") ||
                    command.equalsIgnoreCase("exit")) {
                return false;
            } else if (command.equalsIgnoreCase("help")) {
                help();
            } else if (command.equalsIgnoreCase("list")) {
                listRepositories(arguments);
            } else if (command.equalsIgnoreCase("select")) {
                selectRepository(arguments);
            } else if (command.equalsIgnoreCase("adduser")) {
                addUser(arguments);
            } else if (command.equalsIgnoreCase("deleteuser")) {
                deleteUser(arguments);
            } else if (command.equalsIgnoreCase("verifyuser")) {
                verifyUser(arguments);
            } else if (command.equalsIgnoreCase("listusers")) {
                listUsers();
            } else if (command.equalsIgnoreCase("countusers")) {
                countUsers();
            } else if (command.equalsIgnoreCase("setpassword")) {
                setPassword(arguments);
            } else if (command.equalsIgnoreCase("setalias")) {
                setAlias(arguments);
            } else if (command.equalsIgnoreCase("unsetalias")) {
                unsetAlias(arguments);
            } else if (command.equalsIgnoreCase("checkalias")) {
                checkAlias(arguments);
            } else if (command.equalsIgnoreCase("setforward")) {
                setForward(arguments);
            } else if (command.equalsIgnoreCase("unsetforward")) {
                unsetForward(arguments);
            } else if (command.equalsIgnoreCase("checkforward")) {
                checkForward(arguments);
            } else {
                println("Unknown command!");
            }
        } catch (RemoteException re) {
            println(">EXCEPTION: " + re.getMessage());
        }
        return true;
    }

    
    private void help() {
        println("Currently implemented commmands:");
        println("help                                  display this help");
        println("list                                  list all repositories");
        println("select [name]                         select repository");
        println("adduser [username] [password]         add a new user");
        println("deleteuser [username]                 delete existing user");
        println("verifyuser [username]                 verify if specified user exist");
        println("listusers                             display existing userss");
        println("countusers                            display number of existing users");
        println("setpassword [username] [newpassword]  set new password for existing user");
        println("setalias [username] [alias]           set alias for existing user");
        println("unsetalias [username] [alias]         unset alias for existing user");
        println("checkalias [username]                 check if alias is set for existing user");
        println("setforward [username] [forward]       set forward for existing user");
        println("unsetforward [username] [forward]     unset forward for existing user");
        println("checkforward [username]               check if forward is set for existing user");
        println("quit|exit                             quit Administration Tool");
    }


    private void listRepositories(String[] args) 
            throws RemoteException {
        ArrayList list = userManager.getRepositoryNames();
        Iterator iterator = list.iterator();
        println("Repositories:");
        while (iterator.hasNext()) {
            println("-> " + (String)iterator.next());
        }                                               
    }

    private void selectRepository(String[] args)
            throws RemoteException {
        if (args.length != 1) {
            println("usage: select [name]");
            return;
        }

        String name = args[0];
        if (userManager.setRepository(name)) {
            println("select new repository [" + name + "]");
        } else {
            println("unknown repository!");
        }
    }

    private void addUser(String[] args)
            throws RemoteException {
        if (args.length != 2) {
            println("usage: adduser [username] [password]");
            return;
        }
        String username = args[0];
        String password = args[1];
        if (userManager.verifyUser(username)) {
            println("User " + username + " already exist!");
        } else {
            if (userManager.addUser(username, password)) {
                println("User " + username + " added.");
            } else {
                println("Error adding user " + username);
            }
        }
    }

    private void deleteUser(String[] args)
            throws RemoteException {
        if (args.length != 1) {
            println("usage: deleteuser [username]");
            return;
        }
        String username = args[0];
        if (userManager.deleteUser(username)) {
            println("User " + username + " deleted.");
        } else {
            println("Failed to delete User " + username);
        }
    }

    private void verifyUser(String[] args)
            throws RemoteException {
        if (args.length != 1) {
            println("usage: verifyuser [username]");
            return;
        }
        String username = args[0];
        if (userManager.verifyUser(username)) {
            println("User " + username + " exist.");
        } else {
            println("User " + username + " does not exist.");
        }
    }

    private void listUsers() 
            throws RemoteException {
        countUsers();
        ArrayList list = userManager.getUserList();
        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            println("User: " + (String)iterator.next());
        }                                               
    }

    private void countUsers() 
            throws RemoteException {
        println("Existing users: " + userManager.getCountUsers());
    }

    private void setPassword(String[] args)
            throws RemoteException {
        if (args.length != 2) {
            println("usage: setpassword [username] [newpassword]");
            return;
        }
        String username = args[0];
        String password = args[1];
        if (userManager.setPassword(username, password)) {
            println("Password for user " + username + " reset");
        } else {
            println("Error resetting password for user " + username);
        }
    }

    private void setAlias(String[] args)
            throws RemoteException {
        if (args.length != 2) {
            println("usage: setalias [username] [alias]");
            return;
        }
        String username = args[0];
        String alias = args[1];
        if (userManager.setAlias(username, alias)) {
            println("Alias for user " + username + " set to: " + alias);
        } else {
            println("Error setting alias " + alias + " for user " + username);
        }
    }

    private void unsetAlias(String[] args)
            throws RemoteException {
        if (args.length != 1) {
            println("usage: unsetalias [username]");
            return;
        }
        String username = args[0];
        if (userManager.unsetAlias(username)) {
            println("Alias for user " + username + " unset!");
        } else {
            println("Error unset alias for user " + username);
        }
    }

    private void checkAlias(String[] args)
            throws RemoteException {
        if (args.length != 1) {
            println("usage: checkalias [username]");
            return;
        }
        String username = args[0];
        String alias = userManager.checkAlias(username);
        if (alias != null) {
            println("Alias for user " + username + " is set to " + alias);
        } else {
            println("No alias is set for user " + username);
        }
    }

    private void setForward(String[] args)
            throws RemoteException {
        if (args.length != 2) {
            println("usage: setforward [username] [forward]");
            return;
        }
        String username = args[0];
        String forward = args[1];
        if (userManager.setForward(username, forward)) {
            println("Forward for user " + username + " set to: " + forward);
        } else {
            println("Error setting forward " + forward + " for user " + username);
        }
    }

    private void unsetForward(String[] args)
            throws RemoteException {
        if (args.length != 1) {
            println("usage: unsetforward [username]");
            return;
        }
        String username = args[0];
        if (userManager.unsetForward(username)) {
            println("Forward for user " + username + " unset!");
        } else {
            println("Error unset forward for user " + username);
        }
    }

    private void checkForward(String[] args)
            throws RemoteException {
        if (args.length != 1) {
            println("usage: checkforward [username]");
            return;
        }
        String username = args[0];
        String forward = userManager.checkForward(username);
        if (forward != null) {
            println("Forward for user " + username + " is set to " + forward);
        } else {
            println("No forward is set for user " + username);
        }
    }




    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                System.out.println("usage: UserManagerAdminClient <rmi-url>");
                return;
            }
            (new UserManagerAdminClient()).execute(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
