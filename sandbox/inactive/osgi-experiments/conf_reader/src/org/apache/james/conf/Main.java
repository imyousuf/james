package org.apache.james.conf;

import java.io.IOException;

import org.apache.james.conf.stub.StubConfigurationAdmin;
import org.jdom.JDOMException;
import org.osgi.framework.InvalidSyntaxException;

public class Main {

	public static void main(String[] args) throws JDOMException, IOException, InvalidSyntaxException {
		StubConfigurationAdmin configurationAdmin=new StubConfigurationAdmin();
		ConfigReader configReader=new ConfigReader();
		configReader.setConfigurationAdmin(configurationAdmin);
		configReader.read();
		configurationAdmin.dump();
	}

}
