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

    /**
     * The file that stores the article data
     */
    private final File articleFile;

    /**
     * The newsgroup containing this article.
     */
    private final NNTPGroup group;

    /**
     * The sole constructor for this class.
     *
     * @param group the news group containing this article
     * @param f the file that stores the article data
     */
    NNTPArticleImpl(NNTPGroup group, File f) {
        articleFile = f;
        this.group = group;
    }

    /**
     * @see org.apache.james.nntpsever.repository.NNTPArticle#getGroup()
     */
    public NNTPGroup getGroup() {
        return group;
    }

    /**
     * @see org.apache.james.nntpsever.repository.NNTPArticle#getArticleNumber()
     */
    public int getArticleNumber() {
        return Integer.parseInt(articleFile.getName());
    }

    /**
     * @see org.apache.james.nntpsever.repository.NNTPArticle#getUniqueID()
     */
    public String getUniqueID() {
        try {
            FileInputStream fin = new FileInputStream(articleFile);
            InternetHeaders headers = new InternetHeaders(fin);
            String[] idheader = headers.getHeader("Message-Id");
            fin.close();
            return ( idheader.length > 0 ) ? idheader[0] : null;
        } catch(Exception ex) { throw new NNTPException(ex); }
    }

    /**
     * @see org.apache.james.nntpsever.repository.NNTPArticle#writeArticle(PrintWriter)
     */
    public void writeArticle(PrintWriter prt) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(articleFile));
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

    /**
     * @see org.apache.james.nntpsever.repository.NNTPArticle#writeHead(PrintWriter)
     */
    public void writeHead(PrintWriter prt) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(articleFile));
            String line = null;
            while ( ( line = reader.readLine() ) != null ) {
                if ( line.trim().length() == 0 )
                    break;
                prt.println(line);
            }
            reader.close();
        } catch(IOException ex) { throw new NNTPException(ex); }
    }

    /**
     * @see org.apache.james.nntpsever.repository.NNTPArticle#writeBody(PrintWriter)
     */
    public void writeBody(PrintWriter prt) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(articleFile));
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

    /**
     * @see org.apache.james.nntpsever.repository.NNTPArticle#writeOverview(PrintWriter)
     */
    public void writeOverview(PrintWriter prt) {
        try {
            FileInputStream fin = new FileInputStream(articleFile);
            InternetHeaders hdr = new InternetHeaders(fin);
            fin.close();
            String subject = hdr.getHeader("Subject",null);
            String author = hdr.getHeader("From",null);
            String date = hdr.getHeader("Date",null);
            String msgId = hdr.getHeader("Message-Id",null);
            String references = hdr.getHeader("References",null);
            long byteCount = articleFile.length();
            long lineCount = -1;
            StringBuffer line=new StringBuffer(128)
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

    /**
     * @see org.apache.james.nntpsever.repository.NNTPArticle#getHeader(String)
     */
    public String getHeader(String header) {
        try {
            FileInputStream fin = new FileInputStream(articleFile);
            InternetHeaders hdr = new InternetHeaders(fin);
            fin.close();
            return hdr.getHeader(header,null);
        } catch(Exception ex) {
            throw new NNTPException(ex);
        }
    }

    /**
     * Strips out newlines and tabs, converting them to spaces.
     * rfc2980: 2.8 XOVER requires newline and tab to be converted to spaces
     *
     * @param the input String
     *
     * @return the cleaned string
     */
    private String cleanHeader(String field) {
        if ( field == null )
            field = "";
        StringBuffer sb = new StringBuffer(field);
        for( int i=0 ; i<sb.length() ; i++ ) {
            char c = sb.charAt(i);
            if( (c=='\n') || (c=='\t') ) {
                sb.setCharAt(i, ' ');
            }
        }
        return sb.toString();
    }
}
