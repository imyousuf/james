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

package org.apache.james.testing;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.james.util.InternetPrintWriter;
import org.apache.oro.text.perl.Perl5Util;

/**
 * <pre>
 * Simulates protocol sessions. Connects to a protocol server. Test
 * reads template and sends data or reads data from server and
 * validates against given template file.
 *
 * Template rules are:
 *   - All lines starting with '#' are regarded as comments.
 *   - All template lines start with 'C: ' or 'S: '
 *   - Each template line is treated as a seprate unit.
 *   - 'C: ' indicates client side communication that is communication
 *     from this test to remote server.
 *   - 'S: ' indicates server response.
 *   - Client side communication is obtained from template file and is
 *     sent as is to the server.
 *   - Expected Server side responses are read from template. Expected
 *     Server side response is matched against actual server response
 *     using perl regular expressions.
 *
 * Example POP3 prototol script to test for authentication:
 *   # note \ before '+'. '+' needs to be escaped
 *   S: \+OK.*
 *   C: USER test
 *   S: \+OK.*
 *   C: PASS invalidpwd
 *   S: -ERR.*
 *   C: USER test
 *   S: \+OK.*
 *   C: PASS test
 *   S: \+OK.*
 *   C: QUIT
 *   S: \+OK.*
 * </pre>
 */
public class ProtocolSimulator {

    /**
     * starts test run
     */
    public static void main(String[] args) throws Exception {
        ProtocolSimulatorOptions params = ProtocolSimulatorOptions.getOptions(args);
        if (params == null) {
            return;        }
        new ProtocolSimulator(params).start();
    }

    // -------- constants shared by protocol simulators -----------
    private static final String COMMENT_TAG = "#";
    private static final String CLIENT_TAG = "C: ";
    private static final String SERVER_TAG = "S: ";
    private static final int CLIENT_TAG_LEN = CLIENT_TAG.length();
    private static final int SERVER_TAG_LEN = SERVER_TAG.length();

    private final ProtocolSimulatorOptions params;
    private final Perl5Util perl = new Perl5Util();

    /**
     * @params options Options to control Protocol Simulator run
     */
    public ProtocolSimulator(ProtocolSimulatorOptions options) {
        this.params = options;
    }

    /**
     * start simulation run
     */
    public void start() throws IOException, InterruptedException {
        System.out.println("Testing Against Server " + params.host + ":" + params.port);
        System.out.println("Simulation Template: " + params.template);
        System.out.println("Number Of Workers=" + params.workers + ", Iterations Per Worker=" + params.iterations);

        // create protocol simulation commands and fire simulation.
        Command simulation = getSimulationCmd(params.template);
        runSimulation(simulation);
    }

    // ---- methods to fire off simulation ---------

    /**
     * Starts simulation threads. Blocks till simulation threads are done.
     */
    private void runSimulation(final Command simulation)
            throws InterruptedException {
        Thread[] t = new Thread[params.workers];
        for (int i = 0; i < t.length; i++) {
            final int workerID = i;
            t[i] = new Thread("protocol-simulator-" + workerID) {
                public void run() {
                    try {
                        for (int i = 0; i < params.iterations; i++) {
                            runSingleSimulation(simulation);                        }
                    } catch (Throwable t) {
                        System.out.println("Terminating " + workerID + " Reason=" + t.toString());
                        t.printStackTrace();
                    }
                }
            };
        }
        for (int i = 0; i < t.length; i++) {
            t[i].start();        }
        for (int i = 0; i < t.length; i++) {
            t[i].join();        }
    }

    /**
     * run single simulation. called per thread per interation
     */
    private void runSingleSimulation(Command simulation) throws Throwable {
        Socket sock = null;
        try {
            sock = new Socket(params.host, params.port);
            InputStream inp = sock.getInputStream();
            OutputStream out = sock.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inp));
            PrintWriter writer = new InternetPrintWriter(new BufferedOutputStream(out), true);
            simulation.execute(reader, writer);
        } finally {
            if (sock != null) {
                try {                    sock.close();                } catch (Throwable t) {}            }
        }
    }

    // --------- helper methods --------------

    /**
     * read template and convert into single protocol interaction command
     */
    private Command getSimulationCmd(String template)
            throws IOException {
        BufferedReader templateReader = new BufferedReader(new FileReader(template));
        try {
            return getSimulationCmd(templateReader);
        } finally {
            // try your best to cleanup.
            try {                templateReader.close();            } catch (Throwable t) {}
        }
    }

    /**
     * read template and convert into a single protocol interaction command.
     */
    private Command getSimulationCmd(BufferedReader templateReader)
            throws IOException {
        // convert template lines into Protocol interaction commands.
        final List list = new ArrayList();
        while (true) {
            final String templateLine = templateReader.readLine();
            if (templateLine == null) {
                break;            }
            // ignore empty lines.
            if (templateLine.trim().length() == 0) {
                list.add(new NOOPCmd());            } else if (templateLine.startsWith(COMMENT_TAG)) {
                list.add(new NOOPCmd());            } else if (templateLine.startsWith(CLIENT_TAG)) {
                list.add(new Command() {
                    // just send the client data
                    public void execute(BufferedReader reader, PrintWriter writer)
                            throws Throwable {
                        writer.println(templateLine.substring(CLIENT_TAG_LEN));
                    }
                }                );
            } else if (templateLine.startsWith(SERVER_TAG)) {
                list.add(new Command() {
                    // read server line and validate
                    public void execute(BufferedReader reader, PrintWriter writer)
                            throws Throwable {
                        String line = reader.readLine();
                        String pattern = templateLine.substring(SERVER_TAG_LEN);
                        System.out.println(pattern + ":" + line);
                        pattern = "m/" + pattern + "/";
                        if (line == null || !perl.match(pattern, line)) {
                            throw new IOException
                                    ("Protocol Failure: got=[" + line + "] expected=[" + templateLine + "]");                        }
                    }
                }                );
            } else {
                throw new IOException("Invalid template line: " + templateLine);            }
        }

        // aggregate all commands into a single simulation command
        Command simulation = new Command() {
            public void execute(BufferedReader reader, PrintWriter writer)
                    throws Throwable {
                for (int i = 0; i < list.size(); i++) {
                    Command cmd = (Command) list.get(i);
                    try {
                        cmd.execute(reader, writer);
                    } catch (Throwable t) {
                        System.out.println("Failed on line: " + i);
                        throw t;
                    }
                }
            }
        };
        return simulation;
    }

    // ----- helper abstractions -------

    /**
     * represents a single protocol interaction
     */
    private interface Command {
        /**
         * do something. Either write to or read from and validate
         * @param reader Input from protocol server
         * @param writer Output to protocol Server
         */
        public abstract void execute(BufferedReader reader, PrintWriter writer) throws Throwable;
    }

    /**
     * do nothing
     */
    private static class NOOPCmd implements Command {
        public void execute(BufferedReader reader, PrintWriter writer)
                throws Throwable {}
    }
}
