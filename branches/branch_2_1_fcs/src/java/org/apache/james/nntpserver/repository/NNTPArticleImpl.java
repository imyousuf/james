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

package org.apache.james.nntpserver.repository;

import org.apache.james.nntpserver.NNTPException;
import org.apache.avalon.excalibur.io.IOUtil;

import javax.mail.internet.InternetHeaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * Please see NNTPArticle for comments
 *
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
     * @see org.apache.james.nntpserver.repository.NNTPArticle#getGroup()
     */
    public NNTPGroup getGroup() {
        return group;
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPArticle#getArticleNumber()
     */
    public int getArticleNumber() {
        return Integer.parseInt(articleFile.getName());
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPArticle#getUniqueID()
     */
    public String getUniqueID() {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(articleFile);
            InternetHeaders headers = new InternetHeaders(fin);
            String[] idheader = headers.getHeader("Message-Id");
            return ( idheader.length > 0 ) ? idheader[0] : null;
        } catch(Exception ex) { 
            throw new NNTPException(ex); 
        } finally {
            IOUtil.shutdownStream(fin);
        }
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPArticle#writeArticle(PrintWriter)
     */
    public void writeArticle(PrintWriter prt) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(articleFile));
            String line = null;
            while ( ( line = reader.readLine() ) != null ) {
                // add extra dot if line starts with '.'
                // '.' indicates end of article.
                if ( line.startsWith(".") )
                    prt.print(".");
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
     * @see org.apache.james.nntpserver.repository.NNTPArticle#writeHead(PrintWriter)
     */
    public void writeHead(PrintWriter prt) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(articleFile));
            String line = null;
            while ( ( line = reader.readLine() ) != null ) {
                if ( line.trim().length() == 0 )
                    break;
                if ( line.startsWith(".") )
                    prt.print(".");
                prt.println(line);
            }
            reader.close();
        } catch(IOException ex) { throw new NNTPException(ex); }
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPArticle#writeBody(PrintWriter)
     */
    public void writeBody(PrintWriter prt) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(articleFile));
            String line = null;
            boolean startWriting = false;
            while ( ( line = reader.readLine() ) != null ) {
                if ( startWriting ) {
                    if ( line.startsWith(".") )
                        prt.print(".");
                    prt.println(line);
                } else
                    startWriting = ( line.trim().length() == 0 );
            }
            reader.close();
        } catch(IOException ex) { throw new NNTPException(ex); }
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPArticle#writeOverview(PrintWriter)
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
            // TODO: Address the line count issue.
            long lineCount = -1;
            StringBuffer line=new StringBuffer(256)
                .append(getArticleNumber())      .append("\t")
                .append(cleanHeader(subject))    .append("\t")
                .append(cleanHeader(author))     .append("\t")
                .append(cleanHeader(date))       .append("\t")
                .append(cleanHeader(msgId))      .append("\t")
                .append(cleanHeader(references)) .append("\t")
                .append(byteCount)               .append("\t")
                .append(lineCount);
            prt.println(line.toString());
        } catch(Exception ex) { throw new NNTPException(ex); }
    }

    /**
     * @see org.apache.james.nntpserver.repository.NNTPArticle#getHeader(String)
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
            if( (c=='\n') || (c=='\t') || (c=='\r') ) {
                sb.setCharAt(i, ' ');
            }
        }
        return sb.toString();
    }
}
