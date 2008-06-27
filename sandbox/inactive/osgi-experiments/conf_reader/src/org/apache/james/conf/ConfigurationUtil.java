package org.apache.james.conf;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.TreeSet;

import org.osgi.service.cm.Configuration;

public class ConfigurationUtil {
	
	
	
	public static void sort(Configuration[] conf) {
		Arrays.sort(conf, new ConfigurationComparator());
	}

	public static void dump(Configuration conf) {
		if (conf.getFactoryPid() != null) {
			System.out.println("PID(FACTORY) " + conf.getPid());
		} else {
			System.out.println("PID          " + conf.getPid());
		}
		Dictionary dic = conf.getProperties();
		if (dic != null) {
			TreeSet keys = new TreeSet(Collections.list(dic.keys()));
			for (Iterator iter = keys.iterator(); iter.hasNext();) {
				String key = (String) iter.next();
				String value = "" + conf.getProperties().get(key);
				System.out.println("                " + key + "=" + value);
			}
		} else {
			System.out.println("                null");
		}
	}
	
	public static class ConfigurationComparator implements Comparator {

		public int compare(Object o1, Object o2) {
			Configuration conf1=(Configuration)o1;
			Configuration conf2=(Configuration)o2;
			return conf1.getPid().compareTo(conf2.getPid());
		}
		
	}

}
