/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;

import java.io.PrintWriter;
import org.apache.james.nntpserver.repository.NNTPArticle;


/**
 * used by ARTICLE, HEAD, BODY, STAT command.
 * these commands are identical except in the writing the Article header 
 * and body to the response stream
 * ARTICLE - writes header and body
 * HEAD - writes headers
 * BODY - writes body
 * STAT - does not write anything
 *
 * @author  Harmeet <harmeet@kodemuse.com>
 */
interface ArticleWriter {
    void write(NNTPArticle article);
    class Factory {
        static ArticleWriter ARTICLE(final PrintWriter prt) {
            return new ArticleWriter() {
                    public void write(NNTPArticle article) {
                        article.writeArticle(prt);
                        prt.println(".");
                    }
                };
        }
        static ArticleWriter HEAD(final PrintWriter prt) {
            return new ArticleWriter() {
                    public void write(NNTPArticle article) {
                        article.writeHead(prt);
                        prt.println(".");
                    }
                };
        }
        static ArticleWriter BODY(final PrintWriter prt) {
            return new ArticleWriter() {
                    public void write(NNTPArticle article) {
                        article.writeBody(prt);
                        prt.println(".");
                    }
                };
        }
        static ArticleWriter STAT(final PrintWriter prt) {
            return new ArticleWriter() {
                    public void write(NNTPArticle article) { }
                };
        }
        static ArticleWriter OVER(final PrintWriter prt) {
            return new ArticleWriter() {
                    public void write(NNTPArticle article) { 
                        article.writeOverview(prt);
                    }
                };
        }
    } // class Factory
}
