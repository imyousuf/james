/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.testing;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
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
 * </pre>
 */
public class ProtocolSimulator {

    private static final String COMMENT_TAG = "#";
    private static final String CLIENT_TAG = "C: ";
    private static final String SERVER_TAG = "S: ";
    private static final int CLIENT_TAG_LEN = CLIENT_TAG.length();
    private static final int SERVER_TAG_LEN = SERVER_TAG.length();
    private static final Perl5Util perl = new Perl5Util();

    /** 
     * Start the protocol simulator . <br>
     * Usage: ProtocolSimulator 'host' 'port' 'template'
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Usage: ProtocolSimulator <host> <port> <template>");
        String host = args[0];
        int port = new Integer(args[1]).intValue();
        File template = new File(args[2]);
        System.out.println("Testing against "+host+":"+port+
                           " using template: "+template.getAbsolutePath());

        // read socket. Ensure that lines written end with CRLF. 
        Socket sock = new Socket(host,port);
        InputStream inp  = sock.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inp));
        PrintWriter writer = new PrintWriter(new BufferedOutputStream(sock.getOutputStream())) {
                public void println() {
                    try {
                        out.write("\r\n");
                        out.flush();
                    } catch(IOException ex) {
                        throw new RuntimeException(ex.toString());
                    }
                }
            };
        
        
        BufferedReader templateReader = new BufferedReader(new FileReader(template));
        try {
            Line[] line = readTemplate(templateReader,reader,writer);
            for ( int i = 0 ; i < line.length ; i++ ) {
                try {
                    line[i].process();
                } catch(Throwable t) {
                    System.out.println("Failed on line: "+i);
                    t.printStackTrace();
                    break;
                }
            }
        } finally {
            // try your be to cleanup.
            try { templateReader.close(); } catch(Throwable t) { }
            try { sock.close(); } catch(Throwable t) { }
        }
    }

    /** represents a single line of protocol interaction */
    private static interface Line {
        /** do something with the line. Either send or validate */
        public void process() throws IOException;
    }


    /** read template and convert into protocol interaction line
     * elements */
    private static Line[] readTemplate(BufferedReader templateReader,
                                       final BufferedReader reader,
                                       final PrintWriter writer) throws Exception 
    {
        List list = new ArrayList();
        while ( true ) {
            final String templateLine = templateReader.readLine();
            if ( templateLine == null )
                break;
            // ignore empty lines.
            if ( templateLine.trim().length() == 0 )
                continue;
            if ( templateLine.startsWith(COMMENT_TAG) )
                continue;
            if ( templateLine.startsWith(CLIENT_TAG) ) {
                list.add(new Line() {
                        // just send the client data
                        public void process() {
                            writer.println(templateLine.substring(CLIENT_TAG_LEN));
                        }
                    });
            } else if ( templateLine.startsWith(SERVER_TAG) ) {
                list.add(new Line() {
                        // read server line and validate
                        public void process() throws IOException {
                            String line = reader.readLine();
                            String pattern = "m/"+templateLine.substring(SERVER_TAG_LEN)+"/";
                            System.out.println(pattern+":"+line);
                            if ( line == null || !perl.match(pattern,line) )
                                throw new IOException
                                    ("Protocol Failure: got=["+line+"] expected=["+templateLine+"]");
                        }
                    });
            } else
                throw new Exception("Invalid template line: "+templateLine);
        }
        return (Line[])list.toArray(new Line[0]);
    }
}
