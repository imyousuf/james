package org.apache.james.server.command.smtp;

import javax.mail.internet.*;
import javax.activation.*; // Required for javax.mail
import org.apache.james.server.protocol.ProtocolHandler;
import org.apache.james.server.command.*;

/**
 * Misc functions for SMTP command handlers
 * @author Matthew Pettys <matt@arcticmail.com>
 */
	
public class utils
{

	/* RFC 821
	
		VALID FORMAT OF EMAIL ADDRESSES
	
		<reverse-path> ::= <path>
    <forward-path> ::= <path>
    <path> ::= "<" [ <a-d-l> ":" ] <mailbox> ">"
		<a-d-l> ::= <at-domain> | <at-domain> "," <a-d-l>
		<at-domain> ::= "@" <domain>
		<domain> ::=  <element> | <element> "." <domain>
		<element> ::= <name> | "#" <number> | "[" <dotnum> "]"
		<mailbox> ::= <local-part> "@" <domain>
		<local-part> ::= <dot-string> | <quoted-string>
		<name> ::= <a> <ldh-str> <let-dig>
		<ldh-str> ::= <let-dig-hyp> | <let-dig-hyp> <ldh-str>
		<let-dig> ::= <a> | <d>
		<let-dig-hyp> ::= <a> | <d> | "-"
		<dot-string> ::= <string> | <string> "." <dot-string>
		<string> ::= <char> | <char> <string>
    <quoted-string> ::=  """ <qtext> """
		<qtext> ::=  "\" <x> | "\" <x> <qtext> | <q> | <q> <qtext>
		<char> ::= <c> | "\" <x>
		<dotnum> ::= <snum> "." <snum> "." <snum> "." <snum>
		<number> ::= <d> | <d> <number>
		<CRLF> ::= <CR> <LF>
		<CR> ::= the carriage return character (ASCII code 13)
		<LF> ::= the line feed character (ASCII code 10)
		<SP> ::= the space character (ASCII code 32)
		<snum> ::= one, two, or three digits representing a decimal
                      integer value in the range 0 through 255
		<a> ::= any one of the 52 alphabetic characters A through Z
                      in upper case and a through z in lower case
		<c> ::= any one of the 128 ASCII characters, but not any
                      <special> or <SP>
		<d> ::= any one of the ten digits 0 through 9
		<q> ::= any one of the 128 ASCII characters except <CR>,
                      <LF>, quote ("), or backslash (\)
		<x> ::= any one of the 128 ASCII characters (no exceptions)
		<special> ::= "<" | ">" | "(" | ")" | "[" | "]" | "\" | "."
                      | "," | ";" | ":" | "@"  """ | the control
                      characters (ASCII codes 0 through 31 inclusive and 127)
	*/

	/**
	 * Converts a address from the rcpt or mail from line to a usable address
	 * @param s java.lang.String
	 * @return java.lang.String
	 */
	public static String convertAddress(String s)
		throws AddressException
	{
		StringBuffer address = new StringBuffer(s);
		int found = findLastChar(address, ':');
		if ( found != -1 ) {
			// Looks like it is a message with the <@relayhost:matt@host.com> format
			throw new AddressException("Relaying not supported");
			//address = substring(found+1);
		}
		
		// Hack to remove any enclosing <>s
		// Now just replaces with spaces and trims
		//  Should delete but can't use delCharAt with jdk1.1x
		address = replaceChar(address, '<', ' ');
		address = replaceChar(address, '>', ' ');
		return address.toString().trim();
	}

	/**
	 * Replaces all characters in a StringBuffer to another character
	 * @param s java.lang.StringBuffer
	 * @param c char - character to replace
	 * @param r char - character to replace with
	 * @return java.lang.StringBuffer
	 */
	public static StringBuffer replaceChar(StringBuffer s, char c, char r)
	{
		int found = findLastChar(s, c);
		while ( found != -1 ) {
			s.setCharAt(found, r);
			found = findLastChar(s, c);
		}
		return s;
	}

	/**
	 * Finds the last instance of a character in a StringBuffer
	 * @param s java.lang.StringBuffer
	 * @param c char - character to find
	 * @return int
	 */
	public static int findLastChar(StringBuffer s, char c)
	{
		int found = s.toString().lastIndexOf(c);
		while ( found != -1 ) {
			if ( found == 0 || s.charAt(found-1) != '\\') {
				return found;
			} else {
				found = s.toString().lastIndexOf(c, found);
			}
		}
		return found;
	}
}

