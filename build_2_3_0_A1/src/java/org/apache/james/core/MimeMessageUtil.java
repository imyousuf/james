/***********************************************************************
 * Copyright (c) 2000-2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/
package org.apache.james.core;

import org.apache.james.util.InternetPrintWriter;
import org.apache.james.util.io.IOUtil;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import java.io.BufferedWriter;
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
        if (message instanceof MimeMessageCopyOnWriteProxy) {
            MimeMessageCopyOnWriteProxy wr = (MimeMessageCopyOnWriteProxy) message;
            MimeMessage m = wr.getWrappedMessage();
            if (m instanceof MimeMessageWrapper) {
                MimeMessageWrapper wrapper = (MimeMessageWrapper)m;
                wrapper.writeTo(headerOs, bodyOs, ignoreList);
                return;
            }
        } else if (message instanceof MimeMessageWrapper) {
            MimeMessageWrapper wrapper = (MimeMessageWrapper)message;
            wrapper.writeTo(headerOs, bodyOs, ignoreList);
            return;
        }
        if(message.getMessageID() == null) {
            message.saveChanges();
        }

        //Write the headers (minus ignored ones)
        Enumeration headers = message.getNonMatchingHeaderLines(ignoreList);
        PrintWriter hos = new InternetPrintWriter(new BufferedWriter(new OutputStreamWriter(headerOs), 512), true);
        while (headers.hasMoreElements()) {
            hos.println((String)headers.nextElement());
        }
        // Print header/data separator
        hos.println();
        hos.flush();

        InputStream bis;
        OutputStream bos;
        // Write the body to the output stream

        /*
        try {
            bis = message.getRawInputStream();
            bos = bodyOs;
        } catch(javax.mail.MessagingException me) {
            // we may get a "No content" exception
            // if that happens, try it the hard way

            // Why, you ask?  In JavaMail v1.3, when you initially
            // create a message using MimeMessage APIs, there is no
            // raw content available.  getInputStream() works, but
            // getRawInputStream() throws an exception.

            bos = MimeUtility.encode(bodyOs, message.getEncoding());
            bis = message.getInputStream();
        }
        */

        try {
            // Get the message as a stream.  This will encode
            // objects as necessary, and we have some input from
            // decoding an re-encoding the stream.  I'd prefer the
            // raw stream, but see
            bos = MimeUtility.encode(bodyOs, message.getEncoding());
            bis = message.getInputStream();
        } catch(javax.activation.UnsupportedDataTypeException udte) {
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


}
