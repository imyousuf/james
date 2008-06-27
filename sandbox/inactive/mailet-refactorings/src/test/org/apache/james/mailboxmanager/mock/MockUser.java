package org.apache.james.mailboxmanager.mock;

import org.apache.mailet.User;

public class MockUser implements User {

    public String getUserName() {
        return "tuser";
    }

    public boolean setPassword(String newPass) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean verifyPassword(String pass) {
        // TODO Auto-generated method stub
        return false;
    }

}
