package org.apache.james.util;

import java.util.*;

/*
// header - edit "Data/yourJavaHeader" to customize
// contents - edit "EventHandlers/Java file/onCreate" to customize
//
*/
public class Utils
{

    /**
     * This converts a comma-delimited set for name=value pairs into a Properties object
     * @return java.util.Properties
     * @param config java.lang.String
     */
    public static Properties parseArgs(String config) {
        if (config == null) {
            return new Properties();
        } 

        StringTokenizer st = new StringTokenizer(config, ",", false);
        Properties props = new Properties();

        while (st.hasMoreTokens()) {
            String arg = st.nextToken().trim();
            int index = arg.indexOf('=');

            if (index == -1) {
                continue;
            } 

            String name = arg.substring(0, index).trim();
            String value = arg.substring(index + 1).trim();

            props.put(name, value);
        }

        return props;
    }

		public static Properties getBranchProperties(Properties props, String prefix) {
        Properties branch = new Properties();
        String rootName;
        int prefixLength = prefix.length();
        for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
            rootName = (String) e.nextElement();
            if (rootName.startsWith(prefix)) {
                branch.put(rootName.substring(prefixLength), props.getProperty(rootName));
            }
        }
        return branch;
    }

}

