package org.apache.james.smtpserver.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.avalon.framework.logger.AbstractLogEnabled;

public abstract class AbstractCmdHandler<Listener> extends AbstractLogEnabled {
    protected List<Listener> listeners = new ArrayList<Listener>();
    
	public AbstractCmdHandler() {
		listeners.add(getLastListener());
    }
    
    public void setListener(List<Listener> listeners) {
    	this.listeners = listeners;
    	listeners.add(getLastListener());
    }
    
    public void addListener(Listener listener) {
    	listeners.add(listeners.size() -1,listener);
    }
    
    protected abstract Listener getLastListener();
}
