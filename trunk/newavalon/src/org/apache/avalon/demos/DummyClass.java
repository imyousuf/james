/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.avalon.demos;

import java.io.Serializable;

/**
 * This is a ddummy class to test Store
 *
 * @author Charles Benett <charles@benett1.demon.co.uk>
 */
public class DummyClass implements Serializable {

    private String name;

    public DummyClass() {
    }

    public void setName(String txt) {
	name = txt;
    }

    public String getName() {
	return name;
    }
 
}



