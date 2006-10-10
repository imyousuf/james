package org.apache.james.imapserver.handler.commands;

import org.apache.james.imapserver.ImapSessionState;
import org.apache.james.imapserver.ProtocolException;



public class LoginTest extends AbstractCommandTest
{

    public void testValidUserStateNonAuth() throws ProtocolException {
        mockSession.expects(atLeastOnce()).method("getState").will(returnValue(ImapSessionState.NON_AUTHENTICATED));
        mockSession.expects(atLeastOnce()).method("getUsers").will(returnValue(mockUsersRepository.proxy()));
        
        mockUsersRepository.expects(once()).method("test").with( eq("joachim2"),eq("abc")).will(returnValue(true));
        mockUsersRepository.expects(once()).method("getUserByName").with( eq("joachim2")).will(returnValue(mockUser.proxy()));
        
        mockSession.expects(once()).method("setAuthenticated").with( same(mockUser.proxy()));

        String response = handleRequest("1 LOGIN joachim2 abc\n");

        assertEquals("1 OK LOGIN completed.\r\n",response);
    }
    public void testInvalidUserStateNonAuth() throws ProtocolException {
        mockSession.expects(atLeastOnce()).method("getState").will(returnValue(ImapSessionState.NON_AUTHENTICATED));
        mockSession.expects(atLeastOnce()).method("getUsers").will(returnValue(mockUsersRepository.proxy()));
        
        mockUsersRepository.expects(once()).method("test").with( eq("joachim2"),eq("abc")).will(returnValue(false));

        String response = handleRequest("1 LOGIN joachim2 abc\n");

        assertEquals("1 NO LOGIN failed. Invalid login/password\r\n",response);
    }
    public void testValidUserStateAuth() throws ProtocolException {
        mockSession.expects(atLeastOnce()).method("getState").will(returnValue(ImapSessionState.AUTHENTICATED));

        String response = handleRequest("1 LOGIN joachim2 abc\n");
        assertEquals("1 NO LOGIN failed. Command not valid in this state\r\n",response);
    }

    public void testValidUserStateLogout() throws ProtocolException {
        mockSession.expects(atLeastOnce()).method("getState").will(returnValue(ImapSessionState.LOGOUT));

        String response = handleRequest("1 LOGIN joachim2 abc\n");
        assertEquals("1 NO LOGIN failed. Command not valid in this state\r\n",response);
    }
    public void testValidUserStateSelected() throws ProtocolException {
        mockSession.expects(atLeastOnce()).method("getState").will(returnValue(ImapSessionState.SELECTED));

        String response = handleRequest("1 LOGIN joachim2 abc\n");
        assertEquals("1 NO LOGIN failed. Command not valid in this state\r\n",response);
    }
}
