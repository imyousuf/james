package org.apache.james.jms.consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.api.jms.MailConsumer;
import org.apache.mailet.Mail;

/**
 * Sample consumer just logs.
 * Useful for debugging.
 */
public final class LogConsumer implements MailConsumer {

	private Log log = LogFactory.getLog(LogConsumer.class);
	
	public void consume(Mail mail) {
		log.info(mail);
	}

	public void reportIssue(Exception e, Object message) {
		log.info(message, e);
	}

	public Log getLog() {
		return log;
	}

	public void setLog(Log log) {
		this.log = log;
	}

	/**
	 * Renders into a string suitable for logging.
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    return "LogConsumer";
	}	
}
