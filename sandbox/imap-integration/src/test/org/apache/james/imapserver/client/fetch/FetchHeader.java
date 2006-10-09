package org.apache.james.imapserver.client.fetch;

import java.util.Enumeration;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class FetchHeader {

	private String[] fields;

	public String getCommand() {
		if (fields == null) {
			return "HEADER";
		} else {
			return "HEADER.FIELDS ("+getFormattedFieldList()+")";
		}
	}

	private String getFormattedFieldList() {
		String result ="";
		for (int i = 0; i < fields.length; i++) {
			result +=" "+fields[i];
			
		}
		if (result.length()>0) {
			result=result.substring(1);
		}
		return result;
	}

	public String getData(MimeMessage m) throws MessagingException {
		String result = "";
		final Enumeration e;
		if (fields==null) {
		e= m.getAllHeaderLines();
		} else {
			e = m.getMatchingHeaderLines(fields);
		}
		while (e.hasMoreElements()) {
			String line = (String) e.nextElement();
			result += line + "\r\n";
		}
		result += "\r\n"; // TODO Should this be counted for size?
		return result;
	}

	public void setFields(String[] fields) {
		this.fields = fields;

	}

}
