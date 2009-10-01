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



package org.apache.james.pop3server.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.pop3server.CommandHandler;
import org.apache.james.pop3server.POP3Handler;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.socket.BytesWrittenResetOutputStream;
import org.apache.james.util.stream.ExtraDotOutputStream;
import org.apache.mailet.Mail;

/**
  * Handles TOP command
  */
public class TopCmdHandler implements CommandHandler, CapaCapability {
	private final static String COMMAND_NAME = "TOP";


	/**
     * Handler method called upon receipt of a TOP command.
     * This command retrieves the top N lines of a specified
     * message in the mailbox.
     *
     * The expected command format is
     *  TOP [mail message number] [number of lines to return]
     *
	 * @see org.apache.james.pop3server.CommandHandler#onCommand(org.apache.james.pop3server.POP3Session, java.lang.String, java.lang.String)
	 */
    public POP3Response onCommand(POP3Session session, String command, String parameters) {
        POP3Response response = null;
        
        if (parameters == null) {
            response = new POP3Response(POP3Response.ERR_RESPONSE, "Usage: TOP [mail number] [Line number]");
            return response;
        }
        
        String argument = "";
        String argument1 = "";
        int pos = parameters.indexOf(" ");
        if (pos > 0) {
            argument = parameters.substring(0,pos);
            argument1 = parameters.substring(pos+1);
        }

        if (session.getHandlerState() == POP3Handler.TRANSACTION) {
            int num = 0;
            int lines = 0;
            try {
                num = Integer.parseInt(argument);
                lines = Integer.parseInt(argument1);
            } catch (NumberFormatException nfe) {
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Usage: TOP [mail number] [Line number]");
                return response;
            }
            try {
                Mail mc = session.getUserMailbox().get(num);
                if (mc != POP3Handler.DELETED) {
                    response = new POP3Response(POP3Response.OK_RESPONSE, "Message follows");
                    session.writePOP3Response(response);
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
                    StringBuilder responseBuffer =
                        new StringBuilder(64)
                                .append("Message (")
                                .append(num)
                                .append(") already deleted.");
                    response = new POP3Response(POP3Response.ERR_RESPONSE, responseBuffer.toString());
                }
            } catch (IOException ioe) {
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Error while retrieving message.");
            } catch (MessagingException me) {
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Error while retrieving message.");
            } catch (IndexOutOfBoundsException iob) {
                StringBuilder exceptionBuffer =
                    new StringBuilder(64)
                            .append("Message (")
                            .append(num)
                            .append(") does not exist.");
                response = new POP3Response(POP3Response.ERR_RESPONSE, exceptionBuffer.toString());
            }
        } else {
            response = new POP3Response(POP3Response.ERR_RESPONSE);
        }
        return response;    }


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
    

   /**
     * @see org.apache.james.pop3server.core.CapaCapability#getImplementedCapabilities(org.apache.james.pop3server.POP3Session)
     */
	public List<String> getImplementedCapabilities(POP3Session session) {
		List<String> caps = new ArrayList<String>();
		if (session.getHandlerState() == POP3Handler.TRANSACTION) {
			caps.add(COMMAND_NAME);
			return caps;
		}
		return caps;
	}

	/**
	 * @see org.apache.james.socket.CommonCommandHandler#getImplCommands()
	 */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

}
