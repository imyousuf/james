package org.apache.james.conf;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.james.conf.stub.StubConfigurationAdmin;
import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigReader {
	
	private static final String CONF_FILE="james/conf/config.mod.xml";
	
	private static final String DESC_FILE="james/conf/config.desc.xml";
	
	private static final String ROOT_PID="org.apache.james";
	
	
	private ConfigurationAdmin configurationAdmin;
	
	
	
	public ConfigReader() {
		
	}
	
	public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
		this.configurationAdmin=configurationAdmin;
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws JDOMException 
	 * @throws IOException
	 * @throws JDOMException
	 */
	public  void read() throws JDOMException, IOException {
		SAXBuilder builder = new SAXBuilder();
		builder.setValidation(false);
		Document desc_doc = builder.build(DESC_FILE);
		Document input_doc = builder.build(CONF_FILE);
		handle(ROOT_PID, "", null, null, desc_doc
				.getRootElement(), input_doc.getRootElement());
	}



	private void handle(String pid, String key, Dictionary properties,
			Map keyCounter, Element descElement, Element inputElement)
			throws DataConversionException, IOException {
		ConfigNode configNode = new ConfigNode(descElement);
		Configuration configuration = null;
		if (configNode.isBean()) {
			pid += "." + inputElement.getName();
			key = configNode.getKeyPrefix();
			if (configNode.isFactory()) {
				configuration = getConfigurationAdmin()
						.createFactoryConfiguration(pid);
				pid = configuration.getPid();
			} else {
				configuration = getConfigurationAdmin().getConfiguration(pid);
			}
			properties = new Hashtable();
			keyCounter = new HashMap();
		} else {
			if (key.length() > 0) {
				key += '.';
			}
			key += inputElement.getName();
			if (configNode.isMoreThanOne()) {
				key += "."+getNextNumber(keyCounter, key);
			}
		}

		handleAttributes(pid, key, properties, descElement, inputElement);
		handleContent(pid, key, properties, descElement, inputElement);

		Map expectedElementsMap = elementListToMap(descElement.getChildren(),
				"cnf");

		for (Iterator iter = inputElement.getChildren().iterator(); iter
				.hasNext();) {
			Element inputElementChild = (Element) iter.next();
			Element descChildElement = descElement.getChild(inputElementChild
					.getQualifiedName());
			String inputQName = inputElementChild.getQualifiedName();
			if (descChildElement == null) {
				throw new RuntimeException("No description found for: " + pid
						+ "/" + key + "." + inputQName);
			}
			expectedElementsMap.remove(inputQName);
			handle(pid, key, properties, keyCounter, descChildElement,
					inputElementChild);
		}
		for (Iterator iter = expectedElementsMap.values().iterator(); iter
				.hasNext();) {
			ConfigNode expectedConfigNode = new ConfigNode((Element) iter
					.next());
			if (expectedConfigNode.isRequired()) {
				throw new RuntimeException("Missing required node: " + pid
						+ "/" + key + "." + expectedConfigNode.getQName());
			}
		}

		if (configuration != null) {
			configuration.update(properties);
		}
	}

	private static int getNextNumber(Map map, String key) {
		Integer integer = (Integer) map.get(key);
		final int i;
		if (integer == null) {
			i = 0;
		} else {
			i = integer.intValue();
		}
		map.put(key, new Integer(i + 1));
		return i;
	}

	private void handleContent(String pid, String key, Dictionary properties,
			Element descElement, Element inputElement) {
		Element cnfContent = descElement.getChild("content", Namespace
				.getNamespace("http://config.loader/"));
		if (cnfContent != null) {
			properties.put(key, inputElement.getTextTrim());
		}

	}

	private void handleAttributes(String pid, String key,
			Dictionary properties, Element descElement, Element inputElement) {
		if (key.length() > 0) {
			key += '.';
		}
		List inputAttributes = inputElement.getAttributes();
		for (Iterator iter = inputAttributes.iterator(); iter.hasNext();) {
			Attribute attribute = (Attribute) iter.next();
			// TODO do the type casting
			properties.put(key + attribute.getQualifiedName(), attribute
					.getValue());
		}
	}

	private static Map elementListToMap(List elements, String ignorePrefix) {
		Map elementMap = new HashMap();
		for (Iterator iter = elements.iterator(); iter.hasNext();) {
			Element element = (Element) iter.next();
			if (!ignorePrefix.equals(element.getNamespacePrefix())) {
				elementMap.put(element.getQualifiedName(), element);
			}
		}
		return elementMap;
	}

	ConfigurationAdmin getConfigurationAdmin() {
		if (configurationAdmin == null) {
			configurationAdmin = new StubConfigurationAdmin();
		}
		return configurationAdmin;
	}
	
	private Configuration[] getConfigurations() throws IOException, InvalidSyntaxException {
		Configuration[] confs=getConfigurationAdmin().listConfigurations("(service.pid="+ROOT_PID+"*)");
		if (confs==null) {
			confs=new Configuration[0]; 
		}
		return confs;
	}
	
	public void delete() throws IOException, InvalidSyntaxException {
		Configuration[] confs=getConfigurations();
		for (int i = 0; i < confs.length; i++) {
			confs[i].delete();
		}
	}
	
	public void dump() throws IOException, InvalidSyntaxException {
		Configuration[] confs=getConfigurations();
		ConfigurationUtil.sort(confs);
		for (int i = 0; i < confs.length; i++) {
			ConfigurationUtil.dump(confs[i]);
		}
	}

}
