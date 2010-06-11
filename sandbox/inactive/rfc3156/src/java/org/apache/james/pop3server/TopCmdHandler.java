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



package org.apache.james.pop3server;

import org.apache.james.util.ExtraDotOutputStream;
import org.apache.james.util.watchdog.BytesWrittenResetOutputStream;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;

/**
  * Handles TOP command
  */
public class TopCmdHandler implements CommandHandler {

    /**
     * @see org.apache.james.pop3server.CommandHandler#onCommand(POP3Session)
     */
    public void onCommand(POP3Session session) {
        doTOP(session,session.getCommandArgument());
    }

    /**
     * Handler method called upon receipt of a TOP command.
     * This command retrieves the top N lines of a specified
     * message in the mailbox.
     *
     * The expected command format is
     *  TOP [mail message number] [number of lines to return]
     *
     * @param arguments the first argument parsed by the parseCommand method
     */
    private void doTOP(POP3Session session,String arguments) {
        String responseString = null;
        
        if (arguments == null) {
            responseString = POP3Handler.ERR_RESPONSE + " Usage: TOP [mail number] [Line number]";
            session.writeResponse(responseString);
            return;
        }
        
        String argument = "";
        String argument1 = "";
        int pos = arguments.indexOf(" ");
        if (pos > 0) {
            argument = arguments.substring(0,pos);
            argument1 = arguments.substring(pos+1);
        }

        if (session.getHandlerState() == POP3Handler.TRANSACTION) {
            int num = 0;
            int lines = 0;
            try {
                num = Integer.parseInt(argument);
                lines = Integer.parseInt(argument1);
            } catch (NumberFormatException nfe) {
                responseString = POP3Handler.ERR_RESPONSE + " Usage: TOP [mail number] [Line number]";
                session.writeResponse(responseString);
                return;
            }
            try {
                Mail mc = (Mail) session.getUserMailbox().get(num);
                if (mc != POP3Handler.DELETED) {
                    responseString = POP3Handler.OK_RESPONSE + " Message follows";
                    session.writeResponse(responseString);
                    try {
                        for (Enumeration e = mc.getMessage().getAllHeaderLines(); e.hasMoreElements(); ) {
                            session.writeResponse(e.nextElement().toString());
                        }
                        session.writeResponse("");
                        ExtraDotOutputStream edouts =
                                new ExtraDotOutputStream(session.getOutputStream());
                        OutputStream nouts = new BytesWrittenResetOutputStream(edouts,
                                                                  session.getWatchdog(),
                                                                  session.getConfigurationData().getResetLength());
                        writeMessageContentTo(mc.getMessage(),nouts,lines);
                        nouts.flush();
                        edouts.checkCRLFTerminator();
                        edouts.flush();
                    } finally {
                        session.writeResponse(".");
                    }
                } else {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(POP3Handler.ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") already deleted.");
                    responseString = responseBuffer.toString();
                    session.writeResponse(responseString);
                }
            } catch (IOException ioe) {
                responseString = POP3Handler.ERR_RESPONSE + " Error while retrieving message.";
                session.writeResponse(responseString);
            } catch (MessagingException me) {
                responseString = POP3Handler.ERR_RESPONSE + " Error while retrieving message.";
                session.writeResponse(responseString);
            } catch (IndexOutOfBoundsException iob) {
                StringBuffer exceptionBuffer =
                    new StringBuffer(64)
                            .append(POP3Handler.ERR_RESPONSE)
                            .append(" Message (")
                            .append(num)
                            .append(") does not exist.");
                responseString = exceptionBuffer.toString();
                session.writeResponse(responseString);
            }
        } else {
            responseString = POP3Handler.ERR_RESPONSE;
            session.writeResponse(responseString);
        }
    }


    /**
     * Writes the content of the message, up to a total number of lines, out to 
     * an OutputStream.
     *
     * @param out the OutputStream to which to write the content
     * @param lines the number of lines to write to the stream
     *
     * @throws MessagingException if the MimeMessage is not set for this MailImpl
     * @throws IOException if an error occurs while reading or writing from the stream
     */
    public void writeMessageContentTo(MimeMessage message, OutputStream out, int lines)
        throws IOException, MessagingException {
        String line;
        BufferedReader br;
        if (message != null) {
            br = new BufferedReader(new InputStreamReader(message.getRawInputStream()));
            try {
                while (lines-- > 0) {
                    if ((line = br.readLine()) == null) {
                        break;
                    }
                    line += "\r\n";
                    out.write(line.getBytes());
                }
            } finally {
                br.close();
            }
        } else {
            throw new MessagingException("No message set for this MailImpl.");
        }
    }

}
