package org.apache.james.mailboxmanager;

import javax.mail.Message;

/**
 * Used to define a range of messages by uid or msn, or a individual message by key or message object.<br />
 * The type of the set should be defined by using an appropriate constructor. 
 * 
 */

public interface GeneralMessageSet {
	
    public static int TYPE_NONE=0;
	public static int TYPE_MSN=1;
	public static int TYPE_UID=2;
	public static int TYPE_KEY=4;
	public static int TYPE_MESSAGE=8;
    public static int TYPE_ALL=16;
	
	int getType();
	
	long getUidFrom();
	
	long getUidTo();
	
	int getMsnFrom();
	
	int getMsnTo();
	
	String getKey();
	
	Message getMessage();
    
    boolean isValid();

}
