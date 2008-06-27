/*
 * Created on Sat Oct 14 08:50:40 GMT+01:00 2006
 */
package org.apache.james.conf;

import java.io.File;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

public class Activator implements BundleActivator {
  
  /* (non-Javadoc)
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
	  ServiceReference configururationAdminReference=context.getServiceReference(ConfigurationAdmin.class.getName());
	  ConfigurationAdmin configururationAdmin=(ConfigurationAdmin) context.getService(configururationAdminReference);
	  ConfigReader configReader=new ConfigReader();
	  configReader.setConfigurationAdmin(configururationAdmin);
	  configReader.delete();
	  configReader.read();
	  configReader.dump();
  }

  /* (non-Javadoc)
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
  }
}