package org.apache.james.imapserver.mock;

import org.apache.james.imapserver.ImapHandlerConfigurationData;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.mailboxmanager.torque.TorqueMailboxManagerProvider;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;

public class MockImapHandlerConfigurationData implements
		ImapHandlerConfigurationData
{

	public MailServer mailServer;
	public UsersRepository usersRepository = new MockUsersRepository();
	public MailboxManagerProvider mailboxManagerProvider;

	public String getHelloName()
	{
		return "thats.my.host.org";
	}

	public int getResetLength()
	{
		return 24*1024;
	}

	public MailServer getMailServer()
	{
		if (mailServer==null) {
			mailServer=new MockMailServer();
		}
		return mailServer;
	}

	public UsersRepository getUsersRepository()
	{
		
		return usersRepository;
	}


    public MailboxManagerProvider getMailboxManagerProvider() {
        if (mailboxManagerProvider==null) {
            try {
				mailboxManagerProvider=MailboxManagerProviderSingleton.getMailboxManagerProviderInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
        }
        return mailboxManagerProvider;
    }

	public boolean doStreamdump() {
		return true;
	}

	public String getStreamdumpDir() {
		return "streamdump";
	}

}
