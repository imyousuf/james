package org.apache.james.util.mailet;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public final class StringUtils {

    private StringUtils() {
        // make this class non instantiable
    }
    
	/**
	 * Split a string given a pattern (regex), considering escapes
	 * <p> E.g: considering a pattern "," we have:
	 * one,two,three => {one},{two},{three}
	 * one\,two\\,three => {one,two\\},{three}
	 *
	 * NOTE: Untested with pattern regex as pattern and untested for escape chars in text or pattern.
	 */
	public static String[] split(String text, String pattern) {
		String[] array = text.split(pattern, -1);
		ArrayList list = new ArrayList();
		for (int i = 0; i < array.length; i++) {
			boolean escaped = false;
			if (i > 0 && array[i - 1].endsWith("\\")) {
				// When the number of trailing "\" is odd then there was no separator and this pattern is part of
				// the previous match.
				int depth = 1;
				while (depth < array[i-1].length() && array[i-1].charAt(array[i-1].length() - 1 - depth) == '\\') depth ++;
				escaped = depth % 2 == 1;
			}
			if (!escaped) list.add(array[i]);
			else {
				String prev = (String) list.remove(list.size() - 1); 
				list.add(prev.substring(0, prev.length() - 1) + pattern + array[i]);
			}
		}
		return (String[]) list.toArray(new String[0]);
	}

	public static String md5(java.lang.String message) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			StringBuffer sb = new StringBuffer();
			byte buf[] = message.getBytes();
			byte[] md5 = md.digest(buf);
			//System.out.println(message);
			for (int i = 0; i < md5.length; i++) {
				String tmpStr = "0" + Integer.toHexString((0xff & md5[i]));
				sb.append(tmpStr.substring(tmpStr.length() - 2));
			}
			return sb.toString();
			
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	public static String capitalizeWords(String data) {
		if (data==null) return null;
		StringBuffer res = new StringBuffer();
		char ch;
		char prevCh = '.';
		for ( int i = 0;  i < data.length();  i++ ) {
			ch = data.charAt(i);
			if ( Character.isLetter(ch)) {
				if (!Character.isLetter(prevCh) ) res.append( Character.toUpperCase(ch) );
				else res.append( Character.toLowerCase(ch) );
			} else res.append( ch );
			prevCh = ch;
		}
		return res.toString();
	}
}
