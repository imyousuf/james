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
package org.apache.james.core;

import org.apache.james.util.InternetPrintWriter;
import org.apache.james.util.io.IOUtil;

import javax.activation.UnsupportedDataTypeException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Enumeration;

/**
 * Utility class to provide optimized write methods for the various MimeMessage
 * implementations.
 */
public class MimeMessageUtil {

    /**
     * Convenience method to take any MimeMessage and write the headers and body to two
     * different output streams
     */
    public static void writeTo(MimeMessage message, OutputStream headerOs, OutputStream bodyOs) throws IOException, MessagingException {
        writeTo(message, headerOs, bodyOs, null);
    }

    /**
     * Convenience method to take any MimeMessage and write the headers and body to two
     * different output streams, with an ignore list
     */
    public static void writeTo(MimeMessage message, OutputStream headerOs, OutputStream bodyOs, String[] ignoreList) throws IOException, MessagingException {
        MimeMessage testMessage = message;
        if (message instanceof MimeMessageCopyOnWriteProxy) {
            MimeMessageCopyOnWriteProxy wr = (MimeMessageCopyOnWriteProxy) message;
            testMessage = wr.getWrappedMessage();
        }
        if (testMessage instanceof MimeMessageWrapper) {
            MimeMessageWrapper wrapper = (MimeMessageWrapper)testMessage;
            if (!wrapper.isModified()) {
                wrapper.writeTo(headerOs, bodyOs, ignoreList);
                return;
            }
        }
        writeToInternal(message, headerOs, bodyOs, ignoreList);
    }

    /**
     * @param message
     * @param headerOs
     * @param bodyOs
     * @param ignoreList
     * @throws MessagingException
     * @throws IOException
     * @throws UnsupportedDataTypeException
     */
    public static void writeToInternal(MimeMessage message, OutputStream headerOs, OutputStream bodyOs, String[] ignoreList) throws MessagingException, IOException, UnsupportedDataTypeException {
        if(message.getMessageID() == null) {
            message.saveChanges();
        }

        writeHeadersTo(message, headerOs, ignoreList);

        // Write the body to the output stream
        writeMessageBodyTo(message, bodyOs);
    }

    public static void writeMessageBodyTo(MimeMessage message, OutputStream bodyOs) throws IOException, UnsupportedDataTypeException, MessagingException {
        OutputStream bos;
        InputStream bis;

        try {
            // Get the message as a stream.  This will encode
            // objects as necessary, and we have some input from
            // decoding an re-encoding the stream.  I'd prefer the
            // raw stream, but see
            bos = MimeUtility.encode(bodyOs, message.getEncoding());
            bis = message.getInputStream();
        } catch(UnsupportedDataTypeException udte) {
            /* If we get an UnsupportedDataTypeException try using
             * the raw input stream as a "best attempt" at rendering
             * a message.
             *
             * WARNING: JavaMail v1.3 getRawInputStream() returns
             * INVALID (unchanged) content for a changed message.
             * getInputStream() works properly, but in this case
             * has failed due to a missing DataHandler.
             *
             * MimeMessage.getRawInputStream() may throw a "no
             * content" MessagingException.  In JavaMail v1.3, when
             * you initially create a message using MimeMessage
             * APIs, there is no raw content available.
             * getInputStream() works, but getRawInputStream()
             * throws an exception.  If we catch that exception,
             * throw the UDTE.  It should mean that someone has
             * locally constructed a message part for which JavaMail
             * doesn't have a DataHandler.
            */

            try {
                bis = message.getRawInputStream();
                bos = bodyOs;
            } catch(javax.mail.MessagingException _) {
                throw udte;
            }
        }
        catch(javax.mail.MessagingException me) {
            /* This could be another kind of MessagingException
             * thrown by MimeMessage.getInputStream(), such as a
             * javax.mail.internet.ParseException.
             *
             * The ParseException is precisely one of the reasons
             * why the getRawInputStream() method exists, so that we
             * can continue to stream the content, even if we cannot
             * handle it.  Again, if we get an exception, we throw
             * the one that caused us to call getRawInputStream().
             */
            try {
                bis = message.getRawInputStream();
                bos = bodyOs;
            } catch(javax.mail.MessagingException _) {
                throw me;
            }
        }

        try {
            copyStream(bis, bos);
        }
        finally {
            IOUtil.shutdownStream(bis);
        }
    }

    /**
     * Convenience method to copy streams
     */
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        // TODO: This is really a bad way to do this sort of thing.  A shared buffer to
        //       allow simultaneous read/writes would be a substantial improvement
        byte[] block = new byte[1024];
        int read = 0;
        while ((read = in.read(block)) > -1) {
            out.write(block, 0, read);
        }
        out.flush();
    }


    /**
     * Write the message headers to the given outputstream
     * 
     * @param message
     * @param headerOs
     * @param ignoreList
     * @throws MessagingException
     */
    private static void writeHeadersTo(MimeMessage message, OutputStream headerOs, String[] ignoreList) throws MessagingException {
        //Write the headers (minus ignored ones)
        Enumeration headers = message.getNonMatchingHeaderLines(ignoreList);
        writeHeadersTo(headers, headerOs);
    }

    /**
     * Write the message headers to the given outputstream
     * 
     * @param message
     * @param headerOs
     * @param ignoreList
     * @throws MessagingException
     */
    public static void writeHeadersTo(Enumeration headers, OutputStream headerOs) throws MessagingException {
        PrintWriter hos = new InternetPrintWriter(new BufferedWriter(new OutputStreamWriter(headerOs), 512), true);
        while (headers.hasMoreElements()) {
            hos.println((String)headers.nextElement());
        }
        // Print header/data separator
        hos.println();
        hos.flush();
    }
    
    /**
     * @param message
     * @param ignoreList
     * @return
     * @throws MessagingException
     */
    public static InputStream getHeadersInputStream(MimeMessage message, String[] ignoreList) throws MessagingException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        writeHeadersTo(message,bo,ignoreList);
        return new ByteArrayInputStream(bo.toByteArray());
    }


    /**
     * Slow method to calculate the exact size of a message!
     */
    private static final class SizeCalculatorOutputStream extends OutputStream {
        long size = 0;

        public void write(int arg0) throws IOException {
            size++;
        }

        public long getSize() {
            return size;
        }

        public void write(byte[] arg0, int arg1, int arg2) throws IOException {
            size += arg2;
        }

        public void write(byte[] arg0) throws IOException {
            size += arg0.length;
        }
    }

    /**
     * @return size of full message including headers
     * 
     * @throws MessagingException if a problem occours while computing the message size
     */
    public static long getMessageSize(MimeMessage message) throws MessagingException {
        //If we have a MimeMessageWrapper, then we can ask it for just the
        //  message size and skip calculating it
        long size = -1;
        
        if (message instanceof MimeMessageWrapper) {
            MimeMessageWrapper wrapper = (MimeMessageWrapper) message;
            size = wrapper.getMessageSize();
        } else if (message instanceof MimeMessageCopyOnWriteProxy) {
            MimeMessageCopyOnWriteProxy wrapper = (MimeMessageCopyOnWriteProxy) message;
            size = wrapper.getMessageSize();
        }
        
        if (size == -1) {
            size = calculateMessageSize(message);
        }
        
        return size;
    }

    /**
     * @param message
     * @return the calculated size
     * @throws MessagingException
     */
    public static long calculateMessageSize(MimeMessage message) throws MessagingException {
        long size;
        //SK: Should probably eventually store this as a locally
        //  maintained value (so we don't have to load and reparse
        //  messages each time).
        size = message.getSize();
        if (size != -1) {
            Enumeration e = message.getAllHeaderLines();
            if (e.hasMoreElements()) {
                size += 2;
            }
            while (e.hasMoreElements()) {
                // add 2 bytes for the CRLF
                size += ((String) e.nextElement()).length()+2;
            }
        }
        
        
        if (size == -1) {
            SizeCalculatorOutputStream out = new SizeCalculatorOutputStream();
            try {
                message.writeTo(out);
            } catch (IOException e) {
                // should never happen as SizeCalculator does not actually throw IOExceptions.
                throw new MessagingException("IOException wrapped by getMessageSize",e);
            }
            size = out.getSize();
        }
        return size;
    }
    

}
