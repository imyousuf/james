/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import org.apache.james.nntpserver.NNTPException;

import javax.mail.internet.InternetHeaders;
import java.io.*;

/** 
 * Please see NNTPArticle for comments
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
class NNTPArticleImpl implements NNTPArticle {
    private final File f;
    NNTPArticleImpl(File f) {
        this.f = f;
    }
    public NNTPGroup getGroup() {
        return new NNTPGroupImpl(f.getParentFile());
    }
    public int getArticleNumber() {
        return Integer.parseInt(f.getName());
    }
    public String getUniqueID() {
        try {
            FileInputStream fin = new FileInputStream(f);
            InternetHeaders headers = new InternetHeaders(fin);
            String[] idheader = headers.getHeader("Message-Id");
            fin.close();
            return ( idheader.length > 0 ) ? idheader[0] : null;
        } catch(Exception ex) { throw new NNTPException(ex); }
    }
    public void writeArticle(PrintWriter prt) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(f));
            String line = null;
            while ( ( line = reader.readLine() ) != null ) {
                prt.println(line);
            }
        } catch(IOException ex) {
            throw new NNTPException(ex);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ioe) {
                throw new NNTPException(ioe);
            }
        }
    }
    public void writeHead(PrintWriter prt) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = null;
            while ( ( line = reader.readLine() ) != null ) {
                if ( line.trim().length() == 0 )
                    break;
                prt.println(line);
            }
            reader.close();
        } catch(IOException ex) { throw new NNTPException(ex); }
    }
    public void writeBody(PrintWriter prt) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = null;
            boolean startWriting = false;
            while ( ( line = reader.readLine() ) != null ) {
                if ( startWriting )
                    prt.println(line);
                else
                    startWriting = ( line.trim().length() == 0 );
            }
            reader.close();
        } catch(IOException ex) { throw new NNTPException(ex); }
    }

    // rfc2980: 2.8 XOVER
    // requires newline and tab to be converted to space
    private String cleanHeader(String field) {
        if ( field == null )
            field = "";
        StringBuffer sb = new StringBuffer(field);
        for( int i=0 ; i<sb.length() ; i++ ) {
            char c = sb.charAt(i);
            if( (c=='\n') || (c=='\t') ) 
                sb.setCharAt(i, ' ');
        }
        return sb.toString();
    }
    
    public void writeOverview(PrintWriter prt) {
        try {
            FileInputStream fin = new FileInputStream(f);
            InternetHeaders hdr = new InternetHeaders(fin);
            fin.close();
            int articleNumber = getArticleNumber();
            String subject = hdr.getHeader("Subject",null);
            String author = hdr.getHeader("From",null);
            String date = hdr.getHeader("Date",null);
            String msgId = hdr.getHeader("Message-Id",null);
            String references = hdr.getHeader("References",null);
            long byteCount = f.length();
            long lineCount = -1;
            StringBuffer line=new StringBuffer(128)
                .append(articleNumber + "\t")
                .append(cleanHeader(subject))    .append("\t")
                .append(cleanHeader(author))     .append("\t")
                .append(cleanHeader(date))       .append("\t")
                .append(cleanHeader(msgId))      .append("\t")
                .append(cleanHeader(references)) .append("\t")         
                .append(byteCount + "\t")
                .append(lineCount + "");
            prt.println(line.toString());
        } catch(Exception ex) { throw new NNTPException(ex); }
    }
    public String getHeader(String header) {
        try {
            FileInputStream fin = new FileInputStream(f);
            InternetHeaders hdr = new InternetHeaders(fin);
            fin.close();
            return hdr.getHeader(header,null);
        } catch(Exception ex) { throw new NNTPException(ex); }
    }
}
