/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.avalon.excalibur.io.AndFileFilter;
import org.apache.avalon.excalibur.io.ExtensionFileFilter;
import org.apache.avalon.excalibur.io.InvertedFileFilter;
import org.apache.james.nntpserver.DateSinceFileFilter;

/**
 * Group is represented by a directory.
 * Articles are stored in files with the name of file == article number
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
class NNTPGroupImpl implements NNTPGroup {
    private final File root;
    private int currentArticle = -1;
    private int lastArticle;
    private int firstArticle;
    private int numOfArticles;
    // an instance may collect range info once. This involves disk I/O
    private boolean articleRangeInfoCollected = false;
    NNTPGroupImpl(File root) {
        this.root = root;
    }
    public String getName() {
        return root.getName();
    }
    public String getDescription() {
        return getName();
    }
    public boolean isPostAllowed() {
        return true;
    }
    private void collectArticleRangeInfo() {
        if ( articleRangeInfoCollected )
            return;
        String[] list = root.list();
        //new InvertedFileFilter(new ExtensionFileFilter(".id")));
        int first = -1;
        int last = -1;
        for ( int i = 0 ; i < list.length ; i++ ) {
            int num = Integer.parseInt(list[i]);
            if ( first == -1 || num < first )
                first = num;
            if ( num > last )
                last = num;
        }
        numOfArticles = list.length;
        firstArticle = Math.max(first,0);
        lastArticle = Math.max(last,0);
        articleRangeInfoCollected = true;
    }
    public int getNumberOfArticles() {
        collectArticleRangeInfo();
        return numOfArticles;
    }
    public int getFirstArticleNumber() {
        collectArticleRangeInfo();
        return firstArticle;
    }
    public int getLastArticleNumber() {
        collectArticleRangeInfo();
        return lastArticle;
    }
    public int getCurrentArticleNumber() {
        collectArticleRangeInfo();
        // this is not as per RFC, but this is not significant.
        if ( currentArticle == -1 && firstArticle > 0 )
            currentArticle = firstArticle;
        return currentArticle;
    }
    public void setCurrentArticleNumber(int articleNumber) {
        this.currentArticle = articleNumber;
    }

    public NNTPArticle getCurrentArticle() {
        return getArticle(getCurrentArticleNumber());
    }
    public NNTPArticle getArticle(int number) {
        File f = new File(root,number+"");
        return f.exists() ? new NNTPArticleImpl(f) : null;
    }
//     public NNTPArticle getArticleFromID(String id) {
//         if ( id == null )
//             return null;
//         int idx = id.indexOf('@');
//         if ( idx != -1 )
//             id = id.substring(0,idx);
//         File f = new File(root,id+".id");
//         if ( f.exists() == false )
//             return null;
//         try {
//             FileInputStream fin = new FileInputStream(f);
//             int count = fin.available();
//             byte[] ba = new byte[count];
//             fin.read(ba);
//             fin.close();
//             String str = new String(ba);
//             int num = Integer.parseInt(str);
//             return getArticle(num);
//         } catch(IOException ioe) {
//             throw new NNTPException("could not fectch article: "+id,ioe);
//         }
//     }
    public Iterator getArticlesSince(Date dt) {
        File[] f = root.listFiles(new AndFileFilter
            (new DateSinceFileFilter(dt.getTime()),
             new InvertedFileFilter(new ExtensionFileFilter(".id"))));
        List list = new ArrayList();
        for ( int i = 0 ; i < f.length ; i++ )
            list.add(new NNTPArticleImpl(f[i]));
        return list.iterator();
    }

    public Iterator getArticles() {
        File[] f = root.listFiles();
        //(new InvertedFileFilter(new ExtensionFileFilter(".id")));
        List list = new ArrayList();
        for ( int i = 0 ; i < f.length ; i++ )
            list.add(new NNTPArticleImpl(f[i]));
        return list.iterator();
    }
    public Object getPath() {
        return root;
    }
}
