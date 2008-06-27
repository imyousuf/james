package org.apache.james;

import java.net.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.xbill.DNS.*;
import org.apache.arch.*;
import java.util.*;

/**
 * This sends a MimeMessage to specified recipients, using MX record lookups
 * Creation date: (3/4/00 12:11:14 PM)
 * @author: Serge Knystautas <sergek@lokitech.com>
 */
public class SmartTransport implements Component {
	private Resolver resolver;
	private Cache cache;
	private byte dnsCredibility;

/**
 * Simplest constructor.  Assumes localhost is a DNS server
 * Creation date: (3/16/00 6:13:04 PM)
 */
public SmartTransport() throws UnknownHostException {
	this ("127.0.0.1");
}
/**
 * Assumes NONAUTH_ANSWER is ok
 * Creation date: (3/16/00 6:14:32 PM)
 * @param dnsServers java.lang.String
 */
public SmartTransport(String dnsServers) throws UnknownHostException {
	this (dnsServers, false);
}
/**
 * Creates SmartTransport based on servers and whether requires authoritative answers
 * Creation date: (3/16/00 6:15:07 PM)
 * @param dnsServers java.lang.String
 * @param authority boolean
 */
public SmartTransport(String dnsServers, boolean authoritative) throws UnknownHostException {
	StringTokenizer serverTokenizer = new StringTokenizer (dnsServers, ", ", false);
	String servers[] = new String [serverTokenizer.countTokens ()];
	for (int i = 0; i < servers.length; i++) {
		servers[i] = serverTokenizer.nextToken ();
	}
	resolver = new ExtendedResolver (servers);

	dnsCredibility = authoritative ? Credibility.AUTH_ANSWER : Credibility.NONAUTH_ANSWER;

	cache = new Cache ();
}
/**
 * Internal dns lookup on host name (using MX records), returned as Vector, sorted by priority
 * Creation date: (2/25/00 12:52:33 AM)
 * @return java.lang.String[]
 * @param name java.lang.String
 */
private Vector dnsLookup(String name) {
	Record answers[] = rawDNSLookup(name, false);
	
	Vector servers = new Vector ();
	if (answers == null) {
		servers.addElement (name);
    	return servers;
	}

	MXRecord mxAnswers[] = new MXRecord[answers.length];
	for (int i = 0; i < answers.length; i++) {
		mxAnswers[i] = (MXRecord)answers[i];
	}

	Comparator prioritySort = new Comparator () {
		public int compare (Object a, Object b) {
			MXRecord ma = (MXRecord)a;
			MXRecord mb = (MXRecord)b;
			return mb.getPriority () - ma.getPriority ();
		}
	};
	Arrays.sort (mxAnswers, prioritySort);

	for (int i = 0; i < mxAnswers.length; i++) {
		servers.addElement (mxAnswers[i].getTarget ().toString ());
	}
	//If we found no results, we'll add the original domain name
	if (servers.size () == 0)
		servers.addElement (name);
	return servers;
}
/**
 * Returns MX records for a namestring
 * Creation date: (2/24/00 11:45:56 PM)
 * @return org.xbill.DNS.Record[]
 * @param name java.lang.String
 */
private Record[] rawDNSLookup(String namestr, boolean querysent) {
	Name name = new Name(namestr);
	short type = Type.MX;
	short dclass = DClass.IN;

	Record [] answers;
	int answerCount = 0, n = 0;
	Enumeration e;

	SetResponse cached = cache.lookupRecords(name, type, dclass, dnsCredibility);
	if (cached.isSuccessful()) {
		RRset [] rrsets = cached.answers();
		answerCount = 0;
		for (int i = 0; i < rrsets.length; i++)
			answerCount += rrsets[i].size();

		answers = new Record[answerCount];

		for (int i = 0; i < rrsets.length; i++) {
			e = rrsets[i].rrs();
			while (e.hasMoreElements()) {
				Record r = (Record)e.nextElement();
				answers[n++] = r;
			}
		}
	}
	else if (cached.isNegative()) {
		return null;
	}
	else if (querysent) {
		return null;
	}
	else {
		Record question = Record.newRecord(name, type, dclass);
		org.xbill.DNS.Message query = org.xbill.DNS.Message.newQuery(question);
		org.xbill.DNS.Message response;

		try {
			response = resolver.send(query);
		}
		catch (Exception ex) {
			return null;
		}

		short rcode = response.getHeader().getRcode();
		if (rcode == Rcode.NOERROR || rcode == Rcode.NXDOMAIN)
			cache.addMessage(response);

		if (rcode != Rcode.NOERROR)
			return null;

		return rawDNSLookup(namestr, true);
	}

	return answers;
}
/**
 * Sends the MimeMessage to the array of recipients.
 * Creation date: (3/4/00 12:15:26 PM)
 * @param message javax.mail.internet.MimeMessage
 * @param recipients javax.mail.internet.InternetAddress[]
 */
public void sendMessage(MimeMessage message, InternetAddress[] recipients) throws MessagingException {
	if (recipients.length == 0)
		return;
	String host = recipients[0].toString ();
	host = host.substring (host.indexOf ("@") + 1);

	//Lookup the possible targets
	for (Enumeration e = dnsLookup(host).elements (); e.hasMoreElements ();) {
		try {
			String outgoingmailserver = e.nextElement ().toString ();
			URLName urlname = new URLName("smtp://" + outgoingmailserver);

			Transport transport = Session.getDefaultInstance(System.getProperties(), null).getTransport(urlname);

			transport.connect ();
			transport.sendMessage(message, recipients);
			transport.close ();
			return;
		} catch (MessagingException me) {
			if (!e.hasMoreElements ())
				throw me;
		}
	}
	throw new MessagingException("No route found to " + host);
}
}
