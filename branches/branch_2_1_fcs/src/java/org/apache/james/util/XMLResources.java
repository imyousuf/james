/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.util;

import org.apache.oro.text.perl.MalformedPerl5PatternException;
import org.apache.oro.text.perl.Perl5Util;
import org.w3c.dom.*;

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
            String resDb = resElement.getAttribute("for");
            Map resMap;
            if ( resDb.equals("")) {
                // default
                resMap = defaultStrings;
            }
            else if (resDb.equals(selectTag) ) {
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
     * @param dbMatchersElement the XML element containing the database type information
     *
     * @return the type of database to which James is connected
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
    private String substituteSubString( String input, 
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
     * Returns a named string for the specified connection, replacing
     * parameters with the values set.
     * 
     * @param name   the name of the String resource required.
     * @return the requested resource
     */
    public String getString(String name)
    {
        return (String)m_resource.get(name);
    }

    /**
     * Returns a named string for the specified connection, replacing
     * parameters with the values set.
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

        if ( str == null ) {
            StringBuffer exceptionBuffer =
                new StringBuffer(64)
                        .append("Required String resource: '")
                        .append(name)
                        .append("' was not found.");
            throw new RuntimeException(exceptionBuffer.toString());
        }
        return str;
    }

    /**
     * Returns a named string for the specified connection, replacing
     * parameters with the values set.
     * 
     * @param name          the name of the String resource required.
     * @param parameters    a map of parameters (name-value string pairs) which are
     *                      replaced where found in the input strings
     * @return the requested resource
     */
    public String getString(String name, Map parameters)
    {
        String str = getString(name);

        // Do parameter replacements for this string resource.
        Iterator paramNames = parameters.keySet().iterator();
        while ( paramNames.hasNext() ) {
            String paramName = (String)paramNames.next();
            String paramValue = (String)parameters.get(paramName);

            StringBuffer replaceBuffer = new StringBuffer(64).append("${").append(paramName).append("}");
            str = substituteSubString(str, replaceBuffer.toString(), paramValue);
        }

        return str;
    }
}
