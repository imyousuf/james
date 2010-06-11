package org.apache.james.conf.stub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class StubConfigurationAdmin implements ConfigurationAdmin {

	Map confs=new LinkedHashMap();
	
	Map factoryLastNumber=new HashMap();


	public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
		StubConfiguration configuration=new StubConfiguration();
		configuration.factoryPid=factoryPid;
		configuration.pid=factoryPid+"."+getNextFactoryNumber(factoryPid);
		confs.put(configuration.pid,configuration);
		return configuration;
	}
	
	private int  getNextFactoryNumber(String factoryPid) {
		Integer lastNumberObject=(Integer) factoryLastNumber.get(factoryPid);
		if (lastNumberObject==null) {
			lastNumberObject=new Integer(0);
		}
		int lastNumber=lastNumberObject.intValue();
		factoryLastNumber.put(factoryPid,new Integer(lastNumber+1));
		return lastNumber;
	}

	public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
		throw new RuntimeException("not implemented");
	}

	public Configuration getConfiguration(String pid) throws IOException {
		StubConfiguration configuration=(StubConfiguration) confs.get(pid);
		if (configuration==null) {
			configuration=new StubConfiguration();
			configuration.pid=pid;
			confs.put(configuration.pid, configuration);
		}
		return configuration;
	}

	public Configuration getConfiguration(String pid, String location) throws IOException {
		throw new RuntimeException("not implemented");
	}

	public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
		throw new RuntimeException("not implemented");
	}
	
	public void dump() {
		for (Iterator iter = confs.values().iterator(); iter.hasNext();) {
			StubConfiguration configuration = (StubConfiguration) iter.next();
			configuration.dump();
		}
	}

}
