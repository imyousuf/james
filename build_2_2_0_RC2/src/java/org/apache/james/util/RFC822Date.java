/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A utility class to allow creation of RFC822 date strings from Dates 
 * and dates from RFC822 strings<br>
 * It provides for conversion between timezones, 
 * And easy manipulation of RFC822 dates<br>
 * example - current timestamp: String nowdate = new RFC822Date().toString()<br>
 * example - convert into java.util.Date: Date usedate = new RFC822Date("3 Oct 2001 08:32:44 -0000").getDate()<br>
 * example - convert to timezone: String yourdate = new RFC822Date("3 Oct 2001 08:32:44 -0000", "GMT+02:00").toString()<br>
 * example - convert to local timezone: String mydate = new RFC822Date("3 Oct 2001 08:32:44 -0000").toString()<br>
 *
 * @deprecated Use java.util.Date in combination with org.apache.james.util.RFC822DateFormat.
 */
public class RFC822Date {
    private static SimpleDateFormat df;
    private static SimpleDateFormat dx;
    private static SimpleDateFormat dy;
    private static SimpleDateFormat dz;
    private Date d;
    private RFC822DateFormat rfc822Format = new RFC822DateFormat();
   
    static {
        df = new SimpleDateFormat("EE, d MMM yyyy HH:mm:ss", Locale.US);
        dx = new SimpleDateFormat("EE, d MMM yyyy HH:mm:ss zzzzz", Locale.US);
        dy = new SimpleDateFormat("EE d MMM yyyy HH:mm:ss zzzzz", Locale.US);
        dz = new SimpleDateFormat("d MMM yyyy HH:mm:ss zzzzz", Locale.US);
      }   
   
   /**
    * creates a current timestamp 
    * using this machines system timezone<br>
    * 
    */
    public RFC822Date(){
        d = new Date();
    }
    
   /**
    * creates object using date supplied 
    * and this machines system timezone<br>
    * @param da java.util.Date, A date object
    */
    public RFC822Date(Date da) {
        d = da;
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
        setTimeZone(useTZ);
    }   

    public void setDate(Date da){
        d = da;
    }
    
 /**
 * The following styles of rfc date strings can be parsed<br>
 *  Wed, 3 Oct 2001 06:42:27 GMT+02:10<br>
 *  Wed 3 Oct 2001 06:42:27 PST <br>
 *  3 October 2001 06:42:27 +0100  <br>  
 * the military style timezones, ZM, ZA, etc cannot (yet) <br>
 * @param rfcdate java.lang.String - date in RFC822 format
 */
    public void setDate(String rfcdate)  {
        try {
            synchronized (dx) {
                d= dx.parse(rfcdate);
            }
        } catch(ParseException e) {
            try {
                synchronized (dz) {
                    d= dz.parse(rfcdate);
                }
            } catch(ParseException f) {
                try {
                    synchronized (dy) {
                        d = dy.parse(rfcdate);
                    }
                } catch(ParseException g) {
                    d = new Date();
                }
            }
            
        }
        
    }
 
    public void setTimeZone(TimeZone useTZ) {
        rfc822Format.setTimeZone(useTZ);
    }
    
    public void setTimeZone(String useTZ) {
        setTimeZone(TimeZone.getTimeZone(useTZ));
    }
    

    /**
     * returns the java.util.Date object this RFC822Date represents.
     * @return java.util.Date - the java.util.Date object this RFC822Date represents.
     */
    public Date getDate() {
        return d;
    }

    /**
     * returns the date as a string formated for RFC822 compliance
     * ,accounting for timezone and daylight saving.
     * @return java.lang.String - date as a string formated for RFC822 compliance
     * 
     */
    public String toString() {
        return rfc822Format.format(d);
    }
}
