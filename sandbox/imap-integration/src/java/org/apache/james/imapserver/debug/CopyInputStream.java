package org.apache.james.imapserver.debug;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SimpleLog;
/**
 * 
 * @author Joachim Draeger <jd at joachim-draeger.de>
 *
 */
public class CopyInputStream extends InputStream
{

	private InputStream is;

	private OutputStream copy;

	private Log log;

	StringBuffer logString = new StringBuffer();
	
	private boolean DEEP_DEBUG = false;

	public CopyInputStream(InputStream is, OutputStream copy)
	{
		this.is = is;
		this.copy = copy;
	}

	public int read() throws IOException {
		int in = is.read();
		copy.write(in);
		if (DEEP_DEBUG) {
			if (in == 10) {
				getLog().debug(logString);
				logString = new StringBuffer();
			} else if (in != 13) {
				logString.append((char) in);
			}
		}
		return in;
	}
	
	protected Log getLog() {
		if (log==null) {
			log=new SimpleLog("CopyInputStream");
		}
		return log;
	}
	
	public void setLog(Log log) {
		this.log=log;
	}

}
