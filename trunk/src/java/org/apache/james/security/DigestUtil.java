/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

package org.apache.james.security;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;


/**
 * Computes and verifies digests of files and strings
 *
 *
 * @version $Revision: 1.10 $
 */
public class DigestUtil {

    /**
     * Command line interface. Use -help for arguments.
     *
     * @param args the arguments passed in on the command line
     */
    public static void main(String[] args) {

        String alg = "SHA";
        boolean file = false;

        if (args.length == 0 || args.length > 4) {
            printUsage();
            return;
        }

        for (int i = 0; i < args.length; i++) {
            String currArg = args[i].toLowerCase(Locale.US);
            if (currArg.equals("-help")
                || currArg.equals("-usage")) {
                printUsage();
                return;
            }
            if (currArg.equals("-alg")) {
                alg = args[i+1];
            }
            if (currArg.equals("-file")) {
                file = true;
            }
        }

        if (file) {
            digestFile(args[args.length - 1], alg);
            return ;
        } else {
            try {
                String hash = digestString(args[args.length - 1], alg);
                System.out.println("Hash is: " + hash);
                return;
            } catch (NoSuchAlgorithmException nsae) {
                System.out.println("No such algorithm available");
            }
        }
    }

    /**
     * Print the command line usage string.
     */
    public static void printUsage() {
        System.out.println("Usage: "
                           + "java org.apache.james.security.DigestUtil"
                           + " [-alg algorithm]"
                           + " [-file] filename|string");
    }

    /**
     * Calculate digest of given file with given algorithm.
     * Writes digest to file named filename.algorithm .
     *
     * @param filename the String name of the file to be hashed
     * @param algorithm the algorithm to be used to compute the digest
     */
    public static void digestFile(String filename, String algorithm) {
        byte[] b = new byte[65536];
        int count = 0;
        int read = 0;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            fis = new FileInputStream(filename);
            while (fis.available() > 0) {
                read =  fis.read(b);
                md.update(b, 0, read);
                count += read;
            }
            byte[] digest = md.digest();
            StringBuffer fileNameBuffer =
                new StringBuffer(128)
                        .append(filename)
                        .append(".")
                        .append(algorithm);
            fos = new FileOutputStream(fileNameBuffer.toString());
            OutputStream encodedStream = MimeUtility.encode(fos, "base64");
            encodedStream.write(digest);
            fos.flush();
        } catch (Exception e) {
            System.out.println("Error computing Digest: " + e);
        } finally {
            try {
                fis.close();
                fos.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Calculate digest of given String using given algorithm.
     * Encode digest in MIME-like base64.
     *
     * @param pass the String to be hashed
     * @param algorithm the algorithm to be used
     * @return String Base-64 encoding of digest
     *
     * @throws NoSuchAlgorithmException if the algorithm passed in cannot be found
     */
    public static String digestString(String pass, String algorithm )
            throws NoSuchAlgorithmException  {

        MessageDigest md;
        ByteArrayOutputStream bos;

        try {
            md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(pass.getBytes("iso-8859-1"));
            bos = new ByteArrayOutputStream();
            OutputStream encodedStream = MimeUtility.encode(bos, "base64");
            encodedStream.write(digest);
            return bos.toString("iso-8859-1");
        } catch (IOException ioe) {
            throw new RuntimeException("Fatal error: " + ioe);
        } catch (MessagingException me) {
            throw new RuntimeException("Fatal error: " + me);
        }
    }

    /**
     * Private constructor to prevent instantiation of the class
     */
    private DigestUtil() {}
}
