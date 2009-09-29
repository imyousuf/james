package org.apache.james.pop3server;

import java.io.IOException;

public class StlsCmdHandler implements CommandHandler{

	/**
	 * @throws  
	 * 
	 */
	public void onCommand(POP3Session session)  {
		if (session.isStartTLSSupported() && session.getHandlerState() == POP3Handler.TRANSACTION && session.isTLSStarted() == false) {
			session.writeResponse(POP3Handler.OK_RESPONSE+ " Begin TLS negotiation");
			try {
				session.secure();
			} catch (IOException e) {
				session.getLogger().info("Error while trying to secure connection",e);
				session.endSession();
			}
		} else {
			session.writeResponse(POP3Handler.ERR_RESPONSE);
		}
	}

}
