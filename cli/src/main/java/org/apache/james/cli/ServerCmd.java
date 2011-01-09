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
package org.apache.james.cli;

import java.io.IOException;
import java.io.PrintStream;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class ServerCmd {
    private static final String HOST_OPT_LONG = "host";
    private static final String HOST_OPT_SHORT = "h";
    private static final String PORT_OPT_LONG = "port";
    private static final String PORT_OPT_SHORT = "p";
    private static final int defaultPort = 9999;
    private static Options options = null;


    static {
        options = new Options();
        Option optHost = new Option(HOST_OPT_SHORT, HOST_OPT_LONG, true, "node hostname or ip address");
        optHost.setRequired(true);
        options.addOption(optHost);
        options.addOption(PORT_OPT_SHORT, PORT_OPT_LONG, true, "remote jmx agent port number");
    }

    /**
     * Prints usage information to stdout.
     */
    private static void printUsage() {
        HelpFormatter hf = new HelpFormatter();
        String header = String.format("%nAvailable commands:%n" + "adduser <username> <password>%n" + "removeuser <username>%n" + "listusers%n" + "adddomain <domainname>%n" + "removedomain <domainname>%n" + "listdomains%n" + "addMapping <address|regex> <user> <domain> <fromaddress|regexstring>%n"
                + "removeMapping <address|regex> <user> <domain> <fromaddress|regexstring>%n" + "listMappings [<user> <domain>]%n");
        String usage = String.format("java %s --host <arg> <command>%n", ServerCmd.class.getName());
        hf.printHelp(usage, "", options, header);
    }

    private void onException(Exception e, PrintStream out) {

        out.println("Error while execute command:");
        out.println(e.getMessage());
    }

    public void print(String[] data, PrintStream out) {
        for (int i = 0; i < data.length; i++) {
            String u = data[i];
            out.println(u);
        }
        out.println();
    }

    public static void main(String[] args) throws IOException, InterruptedException, ParseException {
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException parseExcep) {
            System.err.println(parseExcep);
            printUsage();
            System.exit(1);
        }

        String host = cmd.getOptionValue(HOST_OPT_LONG);
        int port = defaultPort;

        String portNum = cmd.getOptionValue(PORT_OPT_LONG);
        if (portNum != null) {
            try {
                port = Integer.parseInt(portNum);
            } catch (NumberFormatException e) {
                throw new ParseException("Port must be a number");
            }
        }

        ServerProbe probe = null;
        try {
            probe = new ServerProbe(host, port);
        } catch (IOException ioe) {
            System.err.println("Error connecting to remote JMX agent!");
            ioe.printStackTrace();
            System.exit(3);
        }

        if (cmd.getArgs().length < 1) {
            System.err.println("Missing argument for command.");
            printUsage();
            System.exit(1);
        }

        ServerCmd sCmd = new ServerCmd();

        // Execute the requested command.
        String[] arguments = cmd.getArgs();
        String cmdName = arguments[0];
        try {
            if (cmdName.equals("adduser")) {
                if (arguments.length == 2) {
                    probe.addUser(arguments[0], arguments[1]);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (cmdName.equals("removeuser")) {
                if (arguments.length == 1) {
                    probe.removeUser(arguments[0]);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (cmdName.equals("listusers")) {
                if (arguments.length == 0)
                    sCmd.print(probe.listUsers(), System.out);
                else
                    printUsage();
                System.exit(1);
            } else if (cmdName.equals("adddomain")) {
                if (arguments.length == 1) {
                    probe.addDomain(arguments[0]);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (cmdName.equals("removedomain")) {
                if (arguments.length == 1) {
                    probe.removeDomain(arguments[0]);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (cmdName.equals("listdomains")) {
                if (arguments.length == 0)
                    sCmd.print(probe.listDomains(), System.out);
                else
                    printUsage();
                System.exit(1);
            }

            else {
                System.err.println("Unrecognized command: " + cmdName + ".");
                printUsage();
                System.exit(1);
            }
        } catch (Exception e) {
            sCmd.onException(e, System.err);
            System.exit(1);
        }

        System.exit(0);
    }

}
