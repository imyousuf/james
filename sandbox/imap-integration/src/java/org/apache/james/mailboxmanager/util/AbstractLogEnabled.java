package org.apache.james.mailboxmanager.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SimpleLog;

public abstract class AbstractLogEnabled {
	
	private Log log;

	protected Log getLog() {
		if (log==null) {
			log=new SimpleLog(this.getClass().getName());
		}
		return log;
	}
	
	public void setLog(Log log) {
		this.log=log;
	}

}
