package org.apache.james.conf;

import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Element;

public class ConfigNode {
	
	boolean required=true;
	private String qName;
	private String cardinality="1";
	private boolean bean=false;
	private boolean factory;
	private String keyPrefix="";
	
	public ConfigNode(Element element) throws DataConversionException {
		required=booleanAttribute(element.getAttribute("required"), required);
		cardinality=stringAttribute(element.getAttribute("cardinality"), cardinality);

		factory=cardinality.endsWith("..n");
		factory=booleanAttribute(element.getAttribute("factory"),factory);
		
		bean=booleanAttribute(element.getAttribute("bean"), bean);
		
		keyPrefix=stringAttribute(element.getAttribute("keyPrefix"), keyPrefix);
		qName=element.getQualifiedName();
	}



	public boolean isRequired() {
		final boolean isrequired;
		if (required) {
			isrequired=!cardinality.startsWith("0..");
		} else {
			isrequired=false;
		}
		return isrequired;
	}

	public String getQName() {
		return qName;		
	}

	public boolean isBean() {
		return bean || factory;
	}
	
	public boolean isFactory() {
		return factory;
	}
	
	private static boolean booleanAttribute(Attribute attr,boolean defaultValue) throws DataConversionException {
		if (attr!=null) {
			return attr.getBooleanValue();
		} else {
			return defaultValue;
		}
	}
	
	private static String stringAttribute(Attribute attribute, String defaultValue) {
		if (attribute!=null) {
			return attribute.getValue();
		} else {
			return defaultValue;
		}
	}



	public boolean isMoreThanOne() {
		return cardinality.endsWith("..n");
	}
	
	public String getKeyPrefix() {
		return keyPrefix;
	}

}
