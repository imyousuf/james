package org.apache.james;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.activemq.broker.jmx.QueueViewMBean;
import org.apache.james.transport.camel.JamesCamelConstants;

public class MailQueue {
	private JMXServiceURL url;
	public void init() throws Exception{
		this.url = new JMXServiceURL(
		"service:jmx:rmi:///jndi/rmi://localhost:2011/jmxrmi");		
	}
	
	protected QueueViewMBean getQueue() throws IOException, MalformedObjectNameException, NullPointerException {
		JMXConnector connector = JMXConnectorFactory.connect(url, null);
		connector.connect();
MBeanServerConnection connection = connector.getMBeanServerConnection();
		
		QueueViewMBean queue = (QueueViewMBean) MBeanServerInvocationHandler.newProxyInstance(connection, new ObjectName("outgoing"),QueueViewMBean.class, true);
		return queue;
	}
	
	public void purge() throws MailQueueException{
		try {
			getQueue().purge();
		} catch (Exception e) {
			throw new MailQueueException("Unable to purge queue content",e);
		}
	}
	
	public long getSize() throws MailQueueException{
		try {
			return getQueue().getQueueSize();	
		} catch (Exception e) {
			throw new MailQueueException("Unable to get queue size",e);
		}
	}
	
	public void remove(String... names) throws MailQueueException {
		try {
		QueueViewMBean bean = getQueue();
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0 ; i < names.length; i++) {
			sb.append(JamesCamelConstants.JAMES_MAIL_NAME);
			sb.append("=");
			sb.append(names[i]);
			if (i +1 < names.length) {
				sb.append(" OR ");
			}
		}
		bean.removeMatchingMessages(sb.toString());
		} catch (Exception e) {
			throw new MailQueueException("Unable to remove messages with names " + names + " from queue", e);
		}
	}
	
	public class MailQueueException extends Exception {

		public MailQueueException(String string, Exception e) {
			super(string, e);
		}
		
	}
}
