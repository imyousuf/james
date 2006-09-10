package org.apache.james.transport.mailets;

import org.apache.james.util.mailet.StringUtils;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;

import javax.mail.MessagingException;
import javax.mail.internet.ContentType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Replace text contents
 * <p>This mailet allow to specific regular expression to replace text in subject and content.
 * 
 * Each expression is defined as:
 * /REGEX_PATTERN/SUBSTITUTION_PATTERN/FLAGS/
 * 
 * - REGEX_PATTERN is a regex used for the match
 * - SUBSTITUTION_PATTERN is a substitution pattern
 * - FLAGS flags supported for the pattern:
 *   i: case insensitive
 *   m: multi line
 *   x: extended (N/A)
 *   r: repeat - keep matching until a substitution is possible
 * 
 * To identify subject and body pattern we use the tags &lt;subjectPattern&gt; and &lt;bodyPattern&gt;
 * 
 * Rules can be specified in external files.
 * Lines must be CRLF terminated and lines starting with # are considered commments.
 * Tags used to include external files are &lt;subjectPatternFile&gt; and 
 * &lt;bodyPatternFile&gt;
 * If file path starts with # then the file is loaded as a reasource.
 * 
 * Use of both files and direct patterns at the same time is allowed.
 * 
 * This mailet allow also to enforce the resulting charset for messages processed.
 * To do that the tag &lt;charset&gt; must be specified.
 * 
 * NOTE:
 * Regexp rules must be escaped by regexp excaping rules and applying this 2 additional rules:
 * - "/" char inside an expression must be prefixed with "\":
 *   e.g: "/\//-//" replaces "/" with "-"
 * - when the rules are specified using &lt;subjectPattern&gt; or &lt;bodyPattern&gt; and
 *   "/,/" has to be used in a pattern string it must be prefixed with a "\".
 *   E.g: "/\/\/,//" replaces "/" with "," (the rule would be "/\//,//" but the "/,/" must
 *   be escaped.
 */
public class ReplaceContent extends GenericMailet {
	private static final String PARAMETER_NAME_SUBJECT_PATTERN = "subjectPattern";
	private static final String PARAMETER_NAME_BODY_PATTERN = "bodyPattern";
	private static final String PARAMETER_NAME_SUBJECT_PATTERNFILE = "subjectPatternFile";
	private static final String PARAMETER_NAME_BODY_PATTERNFILE = "bodyPatternFile";
	private static final String PARAMETER_NAME_CHARSET = "charset";
	
	public static final int FLAG_REPEAT = 1;
	
	private Pattern[] subjectPatterns;
	private String[] subjectSubstitutions;
	private Integer[] subjectFlags;
	private Pattern[] bodyPatterns;
	private String[] bodySubstitutions;
	private Integer[] bodyFlags;
	private String charset;
	private int debug = 0;
	
	/**
	 * returns a String describing this mailet.
	 * 
	 * @return A desciption of this mailet
	 */
	public String getMailetInfo() {
		return "ReplaceContent";
	}

	/**
	 * @return an array containing Pattern and Substitution of the input stream
	 * @throws MailetException 
	 */
	protected static Object[] getPattern(String line) throws MailetException {
		String[] pieces = StringUtils.split(line, "/");
		if (pieces.length < 3) throw new MailetException("Invalid expression: " + line);
		int options = 0;
		//if (pieces[2].indexOf('x') >= 0) options += Pattern.EXTENDED;
		if (pieces[2].indexOf('i') >= 0) options += Pattern.CASE_INSENSITIVE;
		if (pieces[2].indexOf('m') >= 0) options += Pattern.MULTILINE;
		if (pieces[2].indexOf('s') >= 0) options += Pattern.DOTALL;
		
		int flags = 0;
		if (pieces[2].indexOf('r') >= 0) flags += FLAG_REPEAT;
		
		if (pieces[1].indexOf("\\r") >= 0) pieces[1] = pieces[1].replaceAll("\\\\r", "\r");
		if (pieces[1].indexOf("\\n") >= 0) pieces[1] = pieces[1].replaceAll("\\\\n", "\n");
		if (pieces[1].indexOf("\\t") >= 0) pieces[1] = pieces[1].replaceAll("\\\\t", "\t");
		
		return new Object[] {Pattern.compile(pieces[0], options), pieces[1] , new Integer(flags)};
	}
	
	protected static List[] getPatternsFromString(String pattern) throws MailetException {
		pattern = pattern.trim();
		if (pattern.length() < 2 && !pattern.startsWith("/") && !pattern.endsWith("/")) throw new MailetException("Invalid parameter value: " + PARAMETER_NAME_SUBJECT_PATTERN);
		pattern = pattern.substring(1, pattern.length() - 1);
		String[] patternArray = StringUtils.split(pattern, "/,/");
		
		List patterns = new ArrayList();
		List substitutions = new ArrayList();
		List flags = new ArrayList();
		for (int i = 0; i < patternArray.length; i++) {
			Object[] o = getPattern(patternArray[i]);
			patterns.add(o[0]);
			substitutions.add(o[1]);
			flags.add(o[2]);
		}
		
		return new List[] {patterns, substitutions, flags};
	}

	protected static List[] getPatternsFromStream(InputStream stream, String charset) throws MailetException, IOException {
		List patterns = new ArrayList();
		List substitutions = new ArrayList();
		List flags = new ArrayList();
		BufferedReader reader = new BufferedReader(charset != null ? new InputStreamReader(stream, charset) : new InputStreamReader(stream));
		//BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("q:\\correzioniout"), "utf-8"));
		
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.length() > 0 && !line.startsWith("#")) {
				if (line.length() < 2 && !line.startsWith("/") && !line.endsWith("/")) throw new MailetException("Invalid expression: " + line);
				Object[] o = getPattern(line.substring(1, line.length() - 1));
				patterns.add(o[0]);
				substitutions.add(o[1]);
				flags.add(o[2]);
			}
		}
		reader.close();
		return new List[] {patterns, substitutions, flags};
	}
	
	/**
	 * @param filepar File path list (or resources if the path starts with #) comma separated
	 */
	private List[] getPatternsFromFileList(String filepar) throws MailetException, IOException {
		List patterns = new ArrayList();
		List substitutions = new ArrayList();
		List flags = new ArrayList();
		String[] files = filepar.split(",");
		for (int i = 0; i < files.length; i++) {
			files[i] = files[i].trim();
			if (debug > 0) log("Loading patterns from: " + files[i]);
			String charset = null;
			int pc = files[i].lastIndexOf('?');
			if (pc >= 0) {
				charset = files[i].substring(pc + 1);
				files[i] = files[i].substring(0, pc);
			}
			InputStream is = null;
			if (files[i].startsWith("#")) is = getClass().getResourceAsStream(files[i].substring(1));
			else {
				File f = new File(files[i]);
				if (f.isFile()) is = new FileInputStream(f);
			}
			if (is != null) {
				List[] o = getPatternsFromStream(is, charset);
				patterns.addAll(o[0]);
				substitutions.addAll(o[1]);
				flags.addAll(o[2]);
				is.close();
			}
		}
		return new List[] {patterns, substitutions, flags};
	}
	
	protected static String applyPatterns(Pattern[] patterns, String[] substitutions, Integer[] pflags, String text, int debug, GenericMailet logOwner) {
		for (int i = 0; i < patterns.length; i ++) {
			int flags = pflags[i].intValue();
			boolean changed = false;
			do {
				changed = false;
				String replaced = patterns[i].matcher(text).replaceAll(substitutions[i]);
				if (!replaced.equals(text)) {
					if (debug > 0) logOwner.log("Subject rule match: " + patterns[i].pattern());
					text = replaced;
					changed = true;
				}
			} while ((flags & FLAG_REPEAT) > 0 && changed);
		}
		
		return text;
	}
	

	public void init() throws MailetException {
		charset = getInitParameter(PARAMETER_NAME_CHARSET);
		debug = Integer.parseInt(getInitParameter("debug", "0"));
	}
	
	private void initPatterns() throws MailetException {
		try {
			List bodyPatternsList = new ArrayList();
			List bodySubstitutionsList = new ArrayList();
			List bodyFlagsList = new ArrayList();
			List subjectPatternsList = new ArrayList();
			List subjectSubstitutionsList = new ArrayList();
			List subjectFlagsList = new ArrayList();

			String pattern = getInitParameter(PARAMETER_NAME_SUBJECT_PATTERN);
			if (pattern != null) {
				List[] o = getPatternsFromString(pattern);
				subjectPatternsList.addAll(o[0]);
				subjectSubstitutionsList.addAll(o[1]);
				subjectFlagsList.addAll(o[2]);
			}
			
			pattern = getInitParameter(PARAMETER_NAME_BODY_PATTERN);
			if (pattern != null) {
				List[] o = getPatternsFromString(pattern);
				bodyPatternsList.addAll(o[0]);
				bodySubstitutionsList.addAll(o[1]);
				bodyFlagsList.addAll(o[2]);
			}
			
			String filepar = getInitParameter(PARAMETER_NAME_SUBJECT_PATTERNFILE);
			if (filepar != null) {
				List[] o = getPatternsFromFileList(filepar);
				subjectPatternsList.addAll(o[0]);
				subjectSubstitutionsList.addAll(o[1]);
				subjectFlagsList.addAll(o[2]);
			}
		
			filepar = getInitParameter(PARAMETER_NAME_BODY_PATTERNFILE);
			if (filepar != null) {
				List[] o = getPatternsFromFileList(filepar);
				bodyPatternsList.addAll(o[0]);
				bodySubstitutionsList.addAll(o[1]);
				bodyFlagsList.addAll(o[2]);
			}
			
			subjectPatterns = (Pattern[]) subjectPatternsList.toArray(new Pattern[0]);
			subjectSubstitutions = (String[]) subjectSubstitutionsList.toArray(new String[0]);
			subjectFlags = (Integer[]) subjectFlagsList.toArray(new Integer[0]);
			bodyPatterns = (Pattern[]) bodyPatternsList.toArray(new Pattern[0]);
			bodySubstitutions = (String[]) bodySubstitutionsList.toArray(new String[0]);
			bodyFlags = (Integer[]) bodyFlagsList.toArray(new Integer[0]);
			
		} catch (FileNotFoundException e) {
			throw new MailetException("Failed initialization", e);
			
		} catch (MailetException e) {
			throw new MailetException("Failed initialization", e);
			
		} catch (IOException e) {
			throw new MailetException("Failed initialization", e);
			
		}
	}

	public void service(Mail mail) throws MailetException {
		initPatterns();
		
		try {
			boolean mod = false;
			boolean contentChanged = false;
			
			if (subjectPatterns != null && subjectPatterns.length > 0) {
				String subject = mail.getMessage().getSubject();
				if (subject == null) subject = "";
				subject = applyPatterns(subjectPatterns, subjectSubstitutions, subjectFlags, subject, debug, this);
				if (charset != null) mail.getMessage().setSubject(subject, charset);
				else mail.getMessage().setSubject(subject);
				mod = true;
			}
			
			if (bodyPatterns != null && bodyPatterns.length > 0) {
				Object bodyObj = mail.getMessage().getContent();
				if (bodyObj == null) bodyObj = "";
				if (bodyObj instanceof String) {
					String body = (String) bodyObj;
					body = applyPatterns(bodyPatterns, bodySubstitutions, bodyFlags, body, debug, this);
					String contentType = mail.getMessage().getContentType();
					if (charset != null) {
						ContentType ct = new ContentType(contentType);
						ct.setParameter("charset", charset);
						contentType = ct.toString();
					}
					mail.getMessage().setContent(body, contentType);
					mod = true;
					contentChanged = true;
				}
			}
			
			if (charset != null && !contentChanged) {
				ContentType ct = new ContentType(mail.getMessage().getContentType());
				ct.setParameter("charset", charset);
				String contentType = mail.getMessage().getContentType();
				mail.getMessage().setContent(mail.getMessage().getContent(), contentType);
			}
			
			if (mod) mail.getMessage().saveChanges();
			
		} catch (MessagingException e) {
			throw new MailetException("Error in replace", e);
			
		} catch (IOException e) {
			throw new MailetException("Error in replace", e);
		}
	}

}
