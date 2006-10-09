package org.apache.james.imapserver.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.mail.MessagingException;

import org.apache.james.imapserver.ImapHandler;
import org.apache.james.imapserver.TestConstants;
import org.apache.james.imapserver.mock.MockImapHandlerConfigurationData;
import org.apache.james.imapserver.mock.MockWatchdog;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.manager.GeneralManager;
import org.apache.james.test.mock.avalon.MockLogger;

public class ImapServerLauncher  implements TestConstants 
{


	public void go() throws IOException, MessagingException, MailboxManagerException
	{
		ServerSocket ss = new ServerSocket(HOST_PORT);
		final MockImapHandlerConfigurationData theConfigData=new MockImapHandlerConfigurationData();
		while (true) {
            
			final Socket s=ss.accept();
			new Thread() {
				public void run() {
					try {
						ImapHandler imapHandler=new ImapHandler();
						imapHandler.enableLogging(new MockLogger());
						imapHandler.setConfigurationData(theConfigData);
						imapHandler.setWatchdog(new MockWatchdog());
						System.out.println("Handle connection "+s);
						imapHandler.handleConnection(s);
						System.out.println("Handle connection finished."+s);
	
					} catch (IOException e) {
						throw new RuntimeException(e);
					}		
				}
			}.start();
			
			
		}

	}

	public static void main(String[] args)
	{
		try {
			new ImapServerLauncher().go();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
