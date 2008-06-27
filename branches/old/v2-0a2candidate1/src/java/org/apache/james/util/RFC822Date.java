/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.ParseException;

/**
 * A utility class to allow creation of RFC822 date strings from Dates 
 * and dates from RFC822 strings<br>
 * It provides for conversion between timezones, 
 * And easy manipulation of RFC822 dates<br>
 * example - current timestamp: String nowdate = new RFC822Date().toString()<br>
 * example - convert into java.util.Date: Date usedate = new RFC822Date("3 Oct 2001 08:32:44 -0000").getDate()<br>
 * example - convert to timezone: String yourdate = new RFC822Date("3 Oct 2001 08:32:44 -0000", "GMT+02:00").toString()<br>
 * example - convert to local timezone: String mydate = new RFC822Date("3 Oct 2001 08:32:44 -0000").toString()<br>
 * @author Danny Angus (danny) <Danny@thought.co.uk><br>
 */
public class RFC822Date {
    private static DateFormat df;
    private static SimpleDateFormat dx;
    private static SimpleDateFormat dy;
    private static SimpleDateFormat dz;
    private static DecimalFormat tz;
    private TimeZone defaultTZ;
    private Date d;
   
    static {
        df = new SimpleDateFormat("EE, d MMM yyyy HH:mm:ss", Locale.US);
        dx = new SimpleDateFormat("EE, d MMM yyyy HH:mm:ss zzzzz", Locale.US);
        dy = new SimpleDateFormat("EE d MMM yyyy HH:mm:ss zzzzz", Locale.US);
	  	dz = new SimpleDateFormat("d MMM yyyy HH:mm:ss zzzzz", Locale.US);
	  	tz = new DecimalFormat("00"); 
	  }   
   
   /**
	* creates a current timestamp 
	* using this machines system timezone<br>
	* 
	*/
public RFC822Date(){
    	d = new Date();
    	defaultTZ = TimeZone.getDefault();
    }
    
   /**
	* creates object using date supplied 
	* and this machines system timezone<br>
	* @param da java.util.Date, A date object
	*/
    public RFC822Date(Date da){
    	d = da;
    	defaultTZ = TimeZone.getDefault();
    }
    
   /**
	* creates object using date supplied 
	* and the timezone string supplied<br>
	* useTZ can be either an abbreviation such as "PST",
	* a full name such as "America/Los_Angeles",<br> 
	* or a custom ID such as "GMT-8:00".<br>
	* Note that this is dependant on java.util.TimeZone<br>
	* Note that the support of abbreviations is for 
	* JDK 1.1.x compatibility only and full names should be used.<br>
	* @param da java.util.Date, a date object
	* @param useTZ java.lang.Sting, a timezone string such as "America/Los_Angeles" or "GMT+02:00"
	*/
    public RFC822Date(Date da, String useTZ){
    	d = da;
    	defaultTZ = TimeZone.getTimeZone(useTZ);
    }

	/**
	* creates object from 
	* RFC822 date string supplied 
	* and the system default time zone <br>
	* In practice it converts RFC822 date string to the local timezone<br>
	* @param rfcdate java.lang.String - date in RFC822 format "3 Oct 2001 08:32:44 -0000"
	*/
	public RFC822Date(String rfcdate) {
		setDate(rfcdate);
		defaultTZ = TimeZone.getDefault();
	}
	/**
	* creates object from 
	* RFC822 date string supplied 
	* using the supplied time zone string<br>
	* @param rfcdate java.lang.String - date in RFC822 format
	* @param useTZ java.lang.String - timezone string *doesn't support Z style or UT*
	*/	
	public RFC822Date(String rfcdate, String useTZ)  {
		setDate(rfcdate);
		defaultTZ = TimeZone.getTimeZone(useTZ);
	}	

    public void setDate(Date da){
    	d = da;
    }
    
 /**
 * The following styles of rfc date strings can be parsed<br>
 *	Wed, 3 Oct 2001 06:42:27 GMT+02:10<br>
 *	Wed 3 Oct 2001 06:42:27 PST <br>
 *	3 October 2001 06:42:27 +0100  <br>  
 * the military style timezones, ZM, ZA, etc cannot (yet) <br>
 * @param rfcdate java.lang.String - date in RFC822 format
 */
    public void setDate(String rfcdate)  {
    	String exceptions;
    	int exceptionoff;
		try{
			d= dx.parse(rfcdate);
		}catch(ParseException  e){
			try{
				d= dz.parse(rfcdate);
			}catch(ParseException  f){
				try{
					d= dy.parse(rfcdate);
				}catch(ParseException  g){
					d = new Date();
				}
			}
			
		}
    	
    }
 
    public void setTimeZone(TimeZone useTZ){
    	defaultTZ = useTZ;
    }
    
    public void setTimeZone(String useTZ){
    	defaultTZ = TimeZone.getTimeZone(useTZ);
    }
    

	/**
     * returns the java.util.Date object this RFC822Date represents.
     * @return java.util.Date - the java.util.Date object this RFC822Date represents.
     */
    public Date getDate(){
    	return d;
    }

    /**
     * returns the date as a string formated for RFC822 compliance
     * ,accounting for timezone and daylight saving.
     * @return java.lang.String - date as a string formated for RFC822 compliance
     * 
     */
    public  String toString() {
        StringBuffer sb = new StringBuffer(df.format(d));
        sb.append(' ');
        int min = defaultTZ.getRawOffset() / 1000 / 60;
        if (defaultTZ.useDaylightTime() && defaultTZ.inDaylightTime(d)) {
            min += 60;
        }
        if (min >= 0) {
            sb.append('+');
        } else {
            sb.append('-');
        }
        min = Math.abs(min);
        sb.append(tz.format(min / 60));
        sb.append(tz.format(min % 60));
        return sb.toString();
    }
}
