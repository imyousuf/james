package org.apache.james.experimental.imapserver.mock;

import org.apache.james.experimental.imapserver.TestConstants;
import org.apache.james.services.User;

public class MockUser implements User,TestConstants
{

    public String getUserName()
    {
        return USER_NAME;
    }

    public boolean verifyPassword(String pass)
    {
    
        return USER_PASSWORD.equals(pass);
    }

    public boolean setPassword(String newPass)
    {
        throw new RuntimeException("not implemented");
    }

}
