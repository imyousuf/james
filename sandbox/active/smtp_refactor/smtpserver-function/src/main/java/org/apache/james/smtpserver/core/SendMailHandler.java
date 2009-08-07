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



package org.apache.james.smtpserver.core;

import org.apache.activemq.transport.stomp.Stomp.Responses;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.services.MailServer;
import org.apache.james.smtpserver.MessageHandler;
import org.apache.james.smtpserver.MessageSizeException;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;

import java.util.Collection;


/**
  * Adds the header to the message
  */
public class SendMailHandler
    extends AbstractCmdHandler<PostDataListener>
    implements MessageHandler, Serviceable,PostDataListener {

    private MailServer mailServer;

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager serviceManager) throws ServiceException {
        mailServer = (MailServer) serviceManager.lookup(MailServer.ROLE);
    }

    /**
     * Adds header to the message
     * @see org.apache.james.smtpserver#onMessage(SMTPSession)
     */
    public void onMessage(SMTPSession session) {
        String responseString = null;
        
        Mail mail = session.getMail();
        
        for (int i = 0; i < listeners.size(); i++) {
        	responseString = listeners.get(i).onData(session, mail);
        	if (responseString != null) {
        		session.abortMessage();
                session.writeResponse(responseString);
                session.setStopHandlerProcessing(true);
                break;
        	}
        }

    
    }

	@Override
	protected PostDataListener getLastListener() {
		// TODO Auto-generated method stub
		return null;
	}

	public String onData(SMTPSession session, Mail mail) {
    	getLogger().debug("sending mail");

		 String responseString = null;
	        try {
	            mailServer.sendMail(mail);
	            Collection theRecipients = mail.getRecipients();
	            String recipientString = "";
	            if (theRecipients != null) {
	                recipientString = theRecipients.toString();
	            }
	            if (getLogger().isInfoEnabled()) {
	                StringBuffer infoBuffer =
	                     new StringBuffer(256)
	                         .append("Successfully spooled mail ")
	                         .append(mail.getName())
	                         .append(" from ")
	                         .append(mail.getSender())
	                         .append(" on ")
	                         .append(session.getRemoteIPAddress())
	                         .append(" for ")
	                         .append(recipientString);
	                getLogger().info(infoBuffer.toString());
	            }
	         } catch (MessagingException me) {
	              // Grab any exception attached to this one.
	              Exception e = me.getNextException();
	              // If there was an attached exception, and it's a
	              // MessageSizeException
	              if (e != null && e instanceof MessageSizeException) {
	                   // Add an item to the state to suppress
	                   // logging of extra lines of data
	                   // that are sent after the size limit has
	                   // been hit.
	                   session.getState().put(SMTPSession.MESG_FAILED, Boolean.TRUE);
	                   // then let the client know that the size
	                   // limit has been hit.
	                   responseString = "552 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.SYSTEM_MSG_TOO_BIG)+" Error processing message.";
	                   StringBuffer errorBuffer =
	                     new StringBuffer(256)
	                         .append("Rejected message from ")
	                         .append(session.getState().get(SMTPSession.SENDER).toString())
	                         .append(" from host ")
	                         .append(session.getRemoteHost())
	                         .append(" (")
	                         .append(session.getRemoteIPAddress())
	                         .append(") exceeding system maximum message size of ")
	                         .append(session.getConfigurationData().getMaxMessageSize());
	                   getLogger().error(errorBuffer.toString());
	              } else {
	                   responseString = "451 "+DSNStatus.getStatus(DSNStatus.TRANSIENT,DSNStatus.UNDEFINED_STATUS)+" Error processing message.";
	                   getLogger().error("Unknown error occurred while processing DATA.", me);
	              }
	              return responseString;
	         }
	         responseString = "250 "+DSNStatus.getStatus(DSNStatus.SUCCESS,DSNStatus.CONTENT_OTHER)+" Message received";
	         return responseString;
	}

}
