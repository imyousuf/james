package org.apache.james.imapserver.util;

import java.util.HashSet;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

public class UnsolicitedResponseGenerator
{
	Set responseSet = new HashSet();
	int recent=0;
	
	public void addByMessages(MimeMessage[] msgs) throws MessagingException {
		addExists(msgs.length);
		int recent=0;
		int firstUnseen=0;
		for (int i=0; i<msgs.length; i++) {
			if (msgs[i].isSet(Flag.RECENT)) {
				recent++;
			}
			if (firstUnseen==0 && !msgs[i].isSet(Flag.SEEN)) {
				firstUnseen=i+1;
			}
			
		}
		addRecent(recent);
		addFlags();
		addFirstUnseen(firstUnseen);			
		addPermanentFlags();
	}

	public void addExists(int i)
	{
		responseSet.add("* " + i + " EXISTS");
	}

	public void addRecent(int i)
	{
		recent=i;
		responseSet.add("* " + i + " RECENT");
	}

	public void addFlags()
	{
		responseSet
				.add("* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Recent \\Seen)");
	}

	public void addUidValidity(long uidv)
	{
		responseSet.add("* OK [UIDVALIDITY " + uidv + "]");
	}

	public void addFirstUnseen(int firstUnseen)
	{
		if (firstUnseen > 0) {
			responseSet.add("* OK [UNSEEN "+firstUnseen+"] Message "+firstUnseen+" is the first unseen");
		} else {
			responseSet.add("* OK No messages unseen");
		}
	}

	public void addPermanentFlags()
	{
		responseSet
				.add("* OK [PERMANENTFLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Recent \\Seen)]");
	}

	public Set getResponseSet()
	{

		return responseSet;
	}

	public int getRecent() {

		return recent;
	}

}
