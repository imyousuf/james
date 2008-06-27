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
 * without having Avalon installed.
 *
 * @author <a href="mailto:scoobie@pop.systemy.it">Federico Barbieri</a>
 * @version 1.0.0 (CVS $Revision: 1.3 $ $Date: 2002/09/14 09:00:56 $)
 */

public class Main {

    /**
     * Displays an error message indicating that James requires an Avalon framework
     * compatible container.
     *
     * @param args the command line arguments, ignored
     */
    public static void main(String[] args) {

        System.out.println("ERROR!");
        System.out.println("Cannot execute James as a stand alone application.");
        System.out.println("To run James, you need to have the Avalon framework installed.");
        System.out.println("Please refer to the Readme file to know how to run James.");
    }
}

