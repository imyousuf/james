package org.apache.james.cheapospool;

import java.io.*;
/**
 * This filters files based on the extension (what the filename ends with).  This is used in retrieving
 * all the files of a particular type (such as .state, .message, or .checked).
 * @author Serge Knystautas <sergek@lokitech.com>
 * @version 0.9
 */
public class CheapoExtensionFilter implements FilenameFilter 
{
	String extension;
/**
 * CheapoFilenameFilter constructor comment.
 */
public CheapoExtensionFilter(String extension)
{
	this.extension = extension;
}
/**
 * accept method comment.
 */
public boolean accept(File dir, String name)
{
	return name.endsWith (extension);
}
}