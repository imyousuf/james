/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/


package org.apache.james.util;

import org.apache.oro.text.perl.MalformedPerl5PatternException;
import org.apache.oro.text.perl.Perl5Util;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
 * This is derived from the SQLResources.java class that we have been
 * using to provide SQL strings based upon the particular drive being
 * used.
 *
 * The format is indentical to the format used by SQLResources.  The
 * only difference are the names of the elements and attributes.  A
 * mapping is as follows:
 *
 *      sqlResources            xmlResources
 *      ------------            ------------
 *      sqlResources            resources
 *      dbMatchers              matchers
 *      dbMatcher               matcher
 *      db                      for
 *      databaseProductName     match
 *      sqlDefs                 group
 *      sql                     resource
 *
 * This class provides String resources defined in XML.  Resources are
 * organized into groups, and identified by name.  For each resource
 * there can be a standard value, and custom values matched by regular
 * expression.
 *
 * The structure of the XML file is:
 *
 *  &lt;resources&gt;
 *    &lt;matchers&gt;
 *      &lt;matcher for="<i>label</i>" match="<i>regular expression</i>"/&gt;
 *      ...
 *    &lt;/matchers&gt;
 *    &lt;group name="<i>group name</i>"&gt;
 *      &lt;resource name="<i>resouce name</i>" [for="<i>match label</i>"]&gt;<i>text, including ${placeholders}, which will be replaced at runtime.</i>&lt;/resource&gt;
 *      ...
 *    &lt;/group&gt;
 *    <i>... more &lt;group&gt; elements ...</i>
 *  &lt;/resources&gt;
 * 
 */
public class XMLResources
{
    /**
     * A map of statement types to resource strings
     */
    private Map m_resource = new HashMap();

    /**
     * A set of all used String values
     */
    static private Map stringTable = java.util.Collections.synchronizedMap(new HashMap());

    /**
     * A Perl5 regexp matching helper class
     */
    private Perl5Util m_perl5Util = new Perl5Util();

    /**
     * Configures an XMLResources object to provide string statements from a file.
     * 
     * Parameters encoded as $(parameter} in the input file are
     * replace by values from the parameters Map, if the named parameter exists.
     * Parameter values may also be specified in the resourceSection element.
     * 
     * @param xmlFile    the input file containing the string definitions
     * @param group      xml element containing the strings to be used
     * @param select     if customized elements exist for this value, use them instead of the default
     * @param configParameters a map of parameters (name-value string pairs) which are
     *                   replaced where found in the input strings
     */
    public void init(File xmlFile, String group,
                     String select, Map configParameters)
        throws Exception
    {
        // Parse the xmlFile as an XML document.
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        // First process the matchers, to select the statements to use.
        Element matcherElement = 
            (Element)(doc.getElementsByTagName("matchers").item(0));
        String selectTag = null;
        if ( matcherElement != null ) {
            selectTag = match(select, matcherElement);
            m_perl5Util = null;     // release the PERL matcher!
        }

        // Now get the section defining strings for the group required.
        NodeList sections = doc.getElementsByTagName("group");
        int sectionsCount = sections.getLength();
        Element sectionElement = null;
        for (int i = 0; i < sectionsCount; i++ ) {
            sectionElement = (Element)(sections.item(i));
            String sectionName = sectionElement.getAttribute("name");
            if ( sectionName != null && sectionName.equals(group) ) {
                break;
            }

        }
        if ( sectionElement == null ) {
            StringBuffer exceptionBuffer =
                new StringBuffer(64)
                        .append("Error loading string definition file. ")
                        .append("The element named \'")
                        .append(group)
                        .append("\' does not exist.");
            throw new RuntimeException(exceptionBuffer.toString());
        }

        // Get parameters defined within the file as defaults,
        // and use supplied parameters as overrides.
        Map parameters = new HashMap();
        // First read from the <params> element, if it exists.
        Element parametersElement = 
            (Element)(sectionElement.getElementsByTagName("parameters").item(0));
        if ( parametersElement != null ) {
            NamedNodeMap params = parametersElement.getAttributes();
            int paramCount = params.getLength();
            for (int i = 0; i < paramCount; i++ ) {
                Attr param = (Attr)params.item(i);
                String paramName = param.getName();
                String paramValue = param.getValue();
                parameters.put(paramName, paramValue);
            }
        }
        // Then copy in the parameters supplied with the call.
        parameters.putAll(configParameters);

        // 2 maps - one for storing default statements,
        // the other for statements with a "for" attribute matching this 
        // connection.
        Map defaultStrings = new HashMap();
        Map selectTagStrings = new HashMap();

        // Process each string resource, replacing string parameters,
        // and adding to the appropriate map..
        NodeList resDefs = sectionElement.getElementsByTagName("resource");
        int resCount = resDefs.getLength();
        for ( int i = 0; i < resCount; i++ ) {
            // See if this needs to be processed (is default or product specific)
            Element resElement = (Element)(resDefs.item(i));
            String resSelect = resElement.getAttribute("for");
            Map resMap;
            if ( resSelect.equals("")) {
                // default
                resMap = defaultStrings;
            }
            else if (resSelect.equals(selectTag) ) {
                // Specific to this product
                resMap = selectTagStrings;
            }
            else {
                // for a different product
                continue;
            }

            // Get the key and value for this string resource.
            String resKey = resElement.getAttribute("name");
            if ( resKey == null ) {
                // ignore elements without a "name" attribute.
                continue;
            }
            String resString = resElement.getFirstChild().getNodeValue();

            // Do parameter replacements for this string resource.
            Iterator paramNames = parameters.keySet().iterator();
            while ( paramNames.hasNext() ) {
                String paramName = (String)paramNames.next();
                String paramValue = (String)parameters.get(paramName);

                StringBuffer replaceBuffer =
                    new StringBuffer(64)
                            .append("${")
                            .append(paramName)
                            .append("}");
                resString = substituteSubString(resString, replaceBuffer.toString(), paramValue);
            }

            // See if we already have registered a string of this value
            String shared = (String) stringTable.get(resString);
            // If not, register it -- we will use it next time
            if (shared == null) {
                stringTable.put(resString, resString);
            } else {
                resString = shared;
            }

            // Add to the resMap - either the "default" or the "product" map
            resMap.put(resKey, resString);
        }

        // Copy in default strings, then overwrite product-specific ones.
        m_resource.putAll(defaultStrings);
        m_resource.putAll(selectTagStrings);
    }

    /**
     * Compares the "select" value against a set of regular expressions
     * defined in XML.  The first successful match defines the name of a
     * selector tag. This value is then used to choose the specific
     * expressions to use.
     *
     * @param select the String to be checked
     * @param matchersElement the XML element containing selector patterns
     *
     * @return the selector tag that will be used to select custom resources
     *
     */
    private String match(String select, Element matchersElement)
        throws MalformedPerl5PatternException
    {
        String selectTagName = select;
    
        NodeList matchers = matchersElement.getElementsByTagName("matcher");
        for ( int i = 0; i < matchers.getLength(); i++ ) {
            // Get the values for this matcher element.
            Element matcher = (Element)matchers.item(i);
            String matchName = matcher.getAttribute("for");
            StringBuffer selectTagPatternBuffer =
                new StringBuffer(64)
                        .append("/")
                        .append(matcher.getAttribute("match"))
                        .append("/i");

            // If the select string matches the pattern, use the match
            // name from this matcher.
            if ( m_perl5Util.match(selectTagPatternBuffer.toString(), selectTagName) ) {
                return matchName;
            }
        }
        return null;
    }

    /**
     * Replace substrings of one string with another string and return altered string.
     * @param input input string
     * @param find the string to replace
     * @param replace the string to replace with
     * @return the substituted string
     */
    static private String substituteSubString( String input, 
                                               String find,
                                               String replace )
    {
        int find_length = find.length();
        int replace_length = replace.length();

        StringBuffer output = new StringBuffer(input);
        int index = input.indexOf(find);
        int outputOffset = 0;

        while ( index > -1 ) {
            output.replace(index + outputOffset, index + outputOffset + find_length, replace);
            outputOffset = outputOffset + (replace_length - find_length);

            index = input.indexOf(find, index + find_length);
        }

        String result = output.toString();
        return result;
    }

    /**
     * Returns a named string for the specified key.
     * 
     * @param name   the name of the String resource required.
     * @return the requested resource
     */
    public String getString(String name)
    {
        return (String)m_resource.get(name);
    }

    /**
     * Returns a named string for the specified key.
     * 
     * @param name     the name of the String resource required.
     * @param required true if the resource is required
     * @return the requested resource
     * @throws ConfigurationException
     *         if a required resource cannot be found.
     */
    public String getString(String name, boolean required)
    {
        String str = getString(name);

        if (str == null && required) {
            StringBuffer exceptionBuffer =
                new StringBuffer(64)
                        .append("Required String resource: '")
                        .append(name)
                        .append("' was not found.");
            throw new IllegalArgumentException(exceptionBuffer.toString());
        }
        return str;
    }

    /**
     * Returns a named string, replacing parameters with the values set in a Map.
     * 
     * @param name          the name of the String resource required.
     * @param parameters    a map of parameters (name-value string pairs) which are
     *                      replaced where found in the input strings
     * @return the requested resource
     */
    public String getString(String name, Map parameters)
    {
        return replaceParameters(getString(name), parameters);
    }

    /**
     * Returns a named string, replacing parameters with the values set in a Map.
     * 
     * @param name          the name of the String resource required.
     * @param parameters    a map of parameters (name-value string pairs) which are
     *                      replaced where found in the input strings
     * @return the requested resource
     */
    public String getString(String name, Map parameters, boolean required)
    {
        return replaceParameters(getString(name, required), parameters);
    }

    /**
     * Returns a named string, replacing parameters with the values set.
     * 
     * @param name          the name of the String resource required.
     * @param parameters    a map of parameters (name-value string pairs) which are
     *                      replaced where found in the input strings
     * @return the requested resource
     */
    static public String replaceParameters(String str, Map parameters)
    {
        if (str != null && parameters != null) {
            // Do parameter replacements for this string resource.
            Iterator paramNames = parameters.keySet().iterator();
            StringBuffer replaceBuffer = new StringBuffer(64);
            while ( paramNames.hasNext() ) {
                String paramName = (String)paramNames.next();
                String paramValue = (String)parameters.get(paramName);
                replaceBuffer.append("${").append(paramName).append("}");
                str = substituteSubString(str, replaceBuffer.toString(), paramValue);
                if (paramNames.hasNext()) replaceBuffer.setLength(0);
            }
        }

        return str;
    }
}
