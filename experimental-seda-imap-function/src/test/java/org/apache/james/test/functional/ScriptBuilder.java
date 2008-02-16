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

package org.apache.james.test.functional;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import org.apache.commons.lang.StringUtils;


public class ScriptBuilder {
    
    public static ScriptBuilder open(String host, int port) throws Exception {
        InetSocketAddress address = new InetSocketAddress(host, port);
        SocketChannel socket = SocketChannel.open(address);
        socket.configureBlocking(false);
        Client client = new Client(socket, socket);
        return new ScriptBuilder(client);
    }
    
    private int tagCount = 0;
    
    private boolean peek = false;
    private int messageNumber = 1;
    private String user = "imapuser";
    private String password = "password";
    private String mailbox = "testmailbox";
    private String file = "rfc822.mail";
    private String basedir = "/org/apache/james/test/functional/";
    private boolean createdMailbox = false;
    private final Client client;
    
    
    public ScriptBuilder(final Client client) {
        super();
        this.client = client;
    }
    
    public final boolean isPeek() {
        return peek;
    }

    public final void setPeek(boolean peek) {
        this.peek = peek;
    }

    public final String getBasedir() {
        return basedir;
    }

    public final void setBasedir(String basedir) {
        this.basedir = basedir;
    }

    public final String getFile() {
        return file;
    }

    public final void setFile(String file) {
        this.file = file;
    }
    
    private InputStream openFile() throws Exception {
        InputStream result = this.getClass().getResourceAsStream( basedir + file );
        return new IgnoreHeaderInputStream(result);
    }
    
    public final int getMessageNumber() {
        return messageNumber;
    }

    public final void setMessageNumber(int messageNumber) {
        this.messageNumber = messageNumber;
    }

    public final String getMailbox() {
        return mailbox;
    }

    public final void setMailbox(String mailbox) {
        this.mailbox = mailbox;
    }

    public final String getPassword() {
        return password;
    }

    public final void setPassword(String password) {
        this.password = password;
    }

    public final String getUser() {
        return user;
    }

    public final void setUser(String user) {
        this.user = user;
    }

    public void login() throws Exception {
        command("LOGIN " + user + " " + password);
    }

    private void command(final String command) throws Exception {
        tag();
        write(command);
        lineEnd();
        response();
    }
    
    public void select() throws Exception {
        command("SELECT " + mailbox);
    }
    
    public void create() throws Exception {
        command("CREATE " + mailbox);
        createdMailbox = true;
    }
    
    public void delete() throws Exception {
        if (createdMailbox) {
            command("DELETE " + mailbox);
        }
    }
    
    public void fetchSection(String section) throws Exception {
        final String body;
        if (peek) {
            body = " (BODY.PEEK[";
        } else {
            body = " (BODY[";
        }
        final String command = "FETCH " + messageNumber + body + section + "])";
        command(command);
    }
    

    public void fetchFlags() throws Exception {
        final String command = "FETCH " + messageNumber + " (FLAGS)";
        command(command);
    }
    
    public void append() throws Exception {
        tag();
        write("APPEND " + mailbox);
        write(openFile());
        lineEnd();
        response();
    }

    private void write(InputStream in) throws Exception {
        client.write(in);
    }

    private void response() throws Exception {
        client.readResponse();
    }
    
    private void tag() throws Exception {
        client.lineStart();
        write("A" + ++tagCount + " ");
    }
    
    private void lineEnd() throws Exception {
        client.lineEnd();
    }

    private void write(String phrase) throws Exception {
        client.write(phrase);
    }
     
    public void close() throws Exception {
        client.close();
    }
    
    public void logout() throws Exception {
        command("LOGOUT");
    }
    
    public void quit() throws Exception {
        delete();
        logout();
        close();
    }
    
    public static final class Client {
        
        private static final Charset ASCII = Charset.forName("us-ascii");
        
        private final Out out;
        private final ReadableByteChannel source;
        private final WritableByteChannel sump;
        private ByteBuffer inBuffer = ByteBuffer.allocate(256);
        private final ByteBuffer outBuffer = ByteBuffer.allocate(262144);
        private final ByteBuffer crlf;
        private boolean isLineTagged = false;
        private int continuationBytes = 0;
        
        public Client(ReadableByteChannel source, WritableByteChannel sump) throws Exception {
            super();
            this.source = source;
            this.sump = sump;
            this.out = new Out();
            byte[] crlf = {'\r', '\n'};
            this.crlf = ByteBuffer.wrap(crlf);
            inBuffer.flip();
            readLine();
        }

        public void write(InputStream in) throws Exception {
            outBuffer.clear();
            int next = in.read(); 
            while(next != -1) {
                if (next == '\n') {
                    outBufferNext((byte)'\r');
                    outBufferNext((byte)'\n');
                } else if (next == '\r') {
                    outBufferNext((byte)'\r');
                    outBufferNext((byte)'\n');
                    next = in.read();
                    if (next == '\n') {
                        next = in.read();
                    } else if (next != -1) {
                        outBufferNext((byte) next);
                    }
                } else {
                    outBufferNext((byte) next);
                }
                next = in.read();
            }
            
            writeOutBuffer();
        }
        
        public void outBufferNext(byte next) throws Exception {
            outBuffer.put(next);
        }

        private void writeOutBuffer() throws Exception {
            outBuffer.flip();
            int count = outBuffer.limit();
            String continuation = " {" + count + "+}";
            write(continuation);
            lineEnd();
            out.client();
            while (outBuffer.hasRemaining()) {
                final byte next = outBuffer.get();
                print (next);
                if (next == '\n') {
                    out.client();
                }
            }
            outBuffer.rewind();
            while (outBuffer.hasRemaining()) {
                sump.write(outBuffer);
            }
        }

        public void readResponse() throws Exception {
            isLineTagged = false;
            while (!isLineTagged) {
                readLine();
            }
        }
        
        private byte next() throws Exception {
            byte result;
            if (inBuffer.hasRemaining()) {
                result = inBuffer.get();
                print(result);
            } else {
                inBuffer.compact();
                while (source.read(inBuffer) == 0);
                inBuffer.flip();
                result = next();
            }
            return result;
        }
        
        private void print(char next) {
            out.print(next);
        }
            
        private void print(byte next) {
            print((char) next);
        }
        
        public void lineStart() throws Exception {
            out.client();
        }
        
        public void write(String phrase) throws Exception {
            out.print(phrase);
            final ByteBuffer buffer = ASCII.encode(phrase);
            writeRemaining(buffer);
        }
        
        public void writeLine(String line) throws Exception {
            lineStart();
            write(line);
            lineEnd();
        }

        private void writeRemaining(final ByteBuffer buffer) throws IOException {
            while (buffer.hasRemaining()) {
                sump.write(buffer);
            }
        }
        
        public void lineEnd() throws Exception {
            out.lineEnd();
            crlf.rewind();
            writeRemaining(crlf);
        }
        
        private void readLine() throws Exception {
            out.server();
            
            final byte next = next();
            isLineTagged = next != '*';
            readRestOfLine(next);
        }

        private void readRestOfLine(byte next) throws Exception {
            while (next != '\r') {
                if (next == '{') {
                    startContinuation();
                }
                next = next();
            }
            next();
        }

        private void startContinuation() throws Exception {
            continuationBytes = 0;
            continuation();
        }
        
        private void continuation() throws Exception {
            byte next = next();
            switch(next) {
                case '0':
                    continuationDigit(0);
                    break;
                case '1':
                    continuationDigit(1);
                    break;
                case '2':
                    continuationDigit(2);
                    break;
                case '3':
                    continuationDigit(3);
                    break;
                case '4':
                    continuationDigit(4);
                    break;
                case '5':
                    continuationDigit(5);
                    break;
                case '6':
                    continuationDigit(6);
                    break;
                case '7':
                    continuationDigit(7);
                    break;
                case '8':
                    continuationDigit(8);
                    break;
                case '9':
                    continuationDigit(9);
                    break;
                case '+':
                    next();
                    next();                   
                    readContinuation();
                    break;
                default:
                    next();
                    next();
                    readContinuation();
                    break;
            }
        }
        
        private void readContinuation() throws Exception {
           out.server();
            while (continuationBytes-- > 0) {
                int next = next();
                if (next == '\n') {
                   out.server();
                }
            }
        }
        
        private void continuationDigit(int digit) throws Exception {
            continuationBytes = 10*continuationBytes + digit;
            continuation();
        }
        
        public void close() throws Exception {
            source.close();
            sump.close();
        }
    }
    
    private static final class Out {
        private boolean isClient = false;
        public void client() {
            System.out.print("C: ");
            isClient = true;
        }
        
        public void print(char next) {
            if (!isClient) {
                escape(next);
            }
            System.out.print(next);
        }

        private void escape(char next) {
            if (next == '\\' || next == '*' || next=='.' || next == '[' || next == ']' || next == '+'
                || next == '(' || next == ')') {
                System.out.print('\\');
            }
        }

        public void server() {
            System.out.print("S: ");
            isClient = false;
        }
        
        public void print(String phrase) {
            if (!isClient) {
                phrase = StringUtils.replace(phrase, "\\", "\\\\");
                phrase = StringUtils.replace(phrase, "*", "\\*");
                phrase = StringUtils.replace(phrase, ".", "\\.");
                phrase = StringUtils.replace(phrase, "[", "\\[");
                phrase = StringUtils.replace(phrase, "]", "\\]");
                phrase = StringUtils.replace(phrase, "+", "\\+");
                phrase = StringUtils.replace(phrase, "(", "\\(");
                phrase = StringUtils.replace(phrase, ")", "\\)");
            }
            System.out.print(phrase);
        }
        
        public void lineEnd() {
            System.out.println();
        }
    }
    
    private static final class IgnoreHeaderInputStream extends InputStream {

        private boolean isFinishedHeaders = false;
        private final InputStream delegate;
        
        public IgnoreHeaderInputStream(final InputStream delegate) {
            super();
            this.delegate = delegate;
        }

        public int read() throws IOException {
            final int result;
            final int next = delegate.read();
            if (isFinishedHeaders) {
                result = next;
            } else {
                switch (next) {
                    case -1:
                        isFinishedHeaders = true;
                        result = next;
                        break;
                    case '#':
                        readLine();
                        result = read();
                        break;
                        
                    case '\r':
                    case '\n':
                        result = read();
                        break;
                        
                    default:
                        isFinishedHeaders = true;
                        result = next;
                        break;
                }
            }
            return result;
        }
        private void readLine() throws IOException {
            int next = delegate.read();
            while (next != -1 && next !='\r' && next !='\n') {
                next = delegate.read();
            }
        }
    }
}
