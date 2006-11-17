package org.apache.james.imapserver.handler.commands;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.james.imapserver.ImapRequestHandler;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;
import org.apache.james.test.mock.avalon.MockLogger;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public abstract class AbstractCommandTest extends MockObjectTestCase
{

    ImapRequestHandler handler;
    Mock mockSession;
    Mock mockUsersRepository;
    Mock mockUser;

    public void setUp() {
        handler=new ImapRequestHandler();
        handler.enableLogging(new MockLogger());
        mockSession = mock ( ImapSession.class);
        mockUsersRepository = mock ( UsersRepository.class );
        mockUser = mock (User.class );
    }
    
    public String handleRequest(String s) throws ProtocolException {
        ByteArrayInputStream is=new ByteArrayInputStream(s.getBytes());
        ByteArrayOutputStream os=new ByteArrayOutputStream();
        System.out.println("IN :"+s);
        handler.handleRequest(is,os,(ImapSession) mockSession.proxy());
        String out=os.toString();
        System.out.println("OUT:"+out);
        return out;
    }
    

}
