package org.apache.james.mailboxmanager.torque;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TorqueTestSuite {

    public static Test suite() {
        TestSuite suite = new TestSuite(
                "Test for org.apache.james.mailboxmanager.torque");
        //$JUnit-BEGIN$
        suite.addTestSuite(TorqueMailboxManagerSelfTestCase.class);
        suite.addTestSuite(TorqueMailboxManagerTest.class);
        suite.addTestSuite(TorqueImapMailboxSelfTestCase.class);
        suite.addTestSuite(TorqueMailboxTestCase.class);
        //$JUnit-END$
        return suite;
    }

}
