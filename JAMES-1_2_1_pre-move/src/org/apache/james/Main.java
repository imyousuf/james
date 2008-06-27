/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james;

/**
 * This is a fake class to display an error message if you try to execute James
 * without having Avalon intalled.
 *
 * @version 1.0.0 (CVS $Revision: 1.3 $ $Date: 2000/12/11 05:41:45 $)
 * @author <a href="mailto:scoobie@pop.systemy.it">Federico Barbieri</a>
 */

public class Main {

    public static void main(String[] args) {

        System.out.println("ERROR!");
        System.out.println("Cannot execute James as a stand alone application.");
        System.out.println("To run James, you need to have the Avalon framework installed.");
        System.out.println("Please refer to the Readme file to know how to run James.");
    }
}

