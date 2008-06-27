package org.apache.james.conf.stub;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.james.conf.ConfigurationUtil;
import org.osgi.service.cm.Configuration;



public class StubConfiguration implements Configuration {

	String factoryPid;
	String pid;
	Dictionary properties;

	public void delete() throws IOException {
		throw new RuntimeException("not implemented");		
	}

	public String getBundleLocation() {
		throw new RuntimeException("not implemented");	}

	public String getFactoryPid() {
		return factoryPid;
	}

	public String getPid() {
		return pid;
	}

	public Dictionary getProperties() {
		return properties;
	}

	public void setBundleLocation(String bundleLocation) {
		throw new RuntimeException("not implemented");
	}

	public void update() throws IOException {
		throw new RuntimeException("not implemented");
	}

	public void update(Dictionary properties) throws IOException {
		this.properties=properties;
	}

	public void dump() {
		ConfigurationUtil.dump(this);
	}

}
