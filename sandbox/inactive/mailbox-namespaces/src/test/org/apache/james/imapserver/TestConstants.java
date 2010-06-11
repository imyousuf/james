package org.apache.james.imapserver;

public interface TestConstants
{
    final static String USER_NAME="tuser";
    final static String USER_MAILBOX_ROOT=ImapConstants.USER_NAMESPACE+"."+USER_NAME;
    final static String USER_INBOX=USER_MAILBOX_ROOT+".INBOX";
    
    final static String USER_PASSWORD="abc";
    final static String USER_REALNAME="Test User";
    
    final static String HOST_NAME="localhost";
    final static String HOST_ADDRESS="127.0.0.1";
    final static int HOST_PORT=10143;
    

    
}
