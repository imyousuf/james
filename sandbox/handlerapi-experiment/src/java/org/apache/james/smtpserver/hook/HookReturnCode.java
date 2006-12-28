package org.apache.james.smtpserver.hook;

public class HookReturnCode {
    public final static int OK = 0;
    public final static int DENY = 1;
    public final static int DENYSOFT = 2;
    public final static int DECLINED = 3;
}
