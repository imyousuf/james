package org.apache.james.server.command;

import java.lang.Integer;
import java.io.PrintWriter;
import java.util.Vector;
import java.util.Enumeration;
import org.apache.james.server.protocol.ProtocolHandler;

/**
 * Response object for the command handler classes
 *
 * These store the state of a response for a SMTP command.  Supports three return states and multiline responses.
 *
 * @author matt@arcticmail.com
*/

public class CommandHandlerResponse
{
	private int ResponseCode;
	private String ResponseString;
	private int ExitStatus;
	private Vector ResponseLines;

/**
 * Command Handler Response Return Code that should allow for the continued processing
 *  of Command Handlers.
 */
	public static int OK = 1;
	
/**
 * Command Handler Response Return Code that should terminate the connection after sending
 *  the response.
 */
	public static int EXIT = 2;
	
/**
 * Command Handler Response Return Code that should stop the processing of Command Handlers and send
 *  the response.
 */
	public static int DONE = 3;

/**
 * Empty constructor for the Command Handler Response.
 *
 * SMTP status code set to 500, Response Description is set to "COMMAND HANDLER ERROR", and
 * the command handler response status is set to EXIT; 
 */
	public CommandHandlerResponse() {
		this(500, "COMMAND HANDLER ERROR", EXIT);
	}
	
/**
 * Constructor for the Command Handler Response.
 *
 * Default command handler response status is set to DONE; 
 *	
 * @param RespCode int
 * @param RespDesc java.lang.String
 */
	public CommandHandlerResponse(int RespCode, String RespDesc) {
		this(RespCode, RespDesc, OK);
	}
	
/**
 * Constructor for the Command Handler Response.
 *
 * @param RespCode int
 * @param RespDesc java.lang.String
 * @param Status int
 */
	public CommandHandlerResponse(int RespCode, String RespDesc, int Status) {
		ResponseLines = null;
		this.setResponseCode(RespCode);
		this.setResponseString(RespDesc);
		this.setExitStatus(Status);
	}
	
/**
 * Adds a line to a multiple line Command Handler Response.
 *
 * @param RespLine java.lang.String
 * @return void
 */
	public void addResponseLine(String RespLine) {
		if (ResponseLines == null) {
			ResponseLines = new Vector(2);
			ResponseLines.addElement(ResponseString);
			ResponseString = null;
		}
		ResponseLines.addElement(RespLine);
	}
/**
 * Sets the Command Handler Response Description.
 *
 *  If the addResponseLine method has been called.  This will do nothing to the response.
 *
 * @param RespDesc java.lang.String
 * @return void
 */
	public void setResponseString(String RespDesc) {
			ResponseString = RespDesc;
	}
/**
 * Sets the Command Handler Response SMTP Response Code.
 *
 * @param RespCode int
 * @return void
 */
	public void setResponseCode(int RespCode) {
		ResponseCode = RespCode;
	}
	
/**
 * Sets the Command Handler Response Return Code.
 * 
 *  Enforces valid return codes being passed into it.
 *
 * @param ReturnCode int
 * @return void
 */
	public void setExitStatus(int ReturnCode) {
		if (ReturnCode == OK) {
			ExitStatus = OK;
		} else
		if (ReturnCode == EXIT) {
			ExitStatus = EXIT;
		} else
		if (ReturnCode == DONE) {
			ExitStatus = DONE;
		}
	}
	
/**
 * Returns the Command Handler Response Description.
 *
 *  Returns null if this is a multi line response.
 *
 * @return java.lang.String
 */
	public String getResponseString() {
		return ResponseString;
	}

/**
 * Returns the Command Handler Response SMTP Response Code.
 *
 * @return int
 */
	public int getResponseCode() {
		return ResponseCode;
	}
	
/**
 * Returns the Command Handler Response Return Code.
 *
 * @return int
 */
	public int getExitStatus() {
		return ExitStatus;
	}

	/**
	 * Prints this Command Handler Response to the PrintWriter in the appropriate format
	 *
	 *  From RFC 821
	 *
	 *	An SMTP reply consists of a three digit number (transmitted as
   *  three alphanumeric characters) followed by some text.  The number
   *  is intended for use by automata to determine what state to enter
   *  next; the text is meant for the human user.  It is intended that
   *  the three digits contain enough encoded information that the
   *  sender-SMTP need not examine the text and may either discard it or
   *  pass it on to the user, as appropriate.  In particular, the text
   *  may be receiver-dependent and context dependent, so there are
   *  likely to be varying texts for each reply code.  A discussion of
   *  the theory of reply codes is given in Appendix E.  Formally, a
   *  reply is defined to be the sequence:  a three-digit code, <SP>,
   *  one line of text, and <CRLF>, or a multiline reply (as defined in
   *  Appendix E).  Only the EXPN and HELP commands are expected to
   *  result in multiline replies in normal circumstances, however
   *  multiline replies are allowed for any command.
	 *
	 *  The format for multiline replies requires that every line,
   *  except the last, begin with the reply code, followed
   *  immediately by a hyphen, "-" (also known as minus), followed by
   *  text.  The last line will begin with the reply code, followed
   *  immediately by <SP>, optionally some text, and <CRLF>.
	 *
	 * @param p java.io.PrintWriter
	 */
	public void printResponse( PrintWriter p ) {

		String rcode = (new Integer(this.getResponseCode())).toString();
		
		if (ResponseLines == null) {	
			// Single line response
			p.println(rcode + " " + this.getResponseString());
		} else {
			// Multiple line response
			for (Enumeration e = ResponseLines.elements() ; e.hasMoreElements() ;) {
         	String rs = (String) e.nextElement();
					if (e.hasMoreElements()) {
						p.println( rcode + "-" + rs);
					} else {
						p.println( rcode + " " + rs);
					}
			}
		}
		p.flush();
	}
}

