/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.avalon.demos;


import org.apache.avalon.services.Service;
import org.apache.avalon.services.SocketServer.SocketHandler;

/**
 * This is an empty service interface for the SimpleServer demo block
 *
 * @author Charles Benett <charles@benett1.demon.co.uk>
 */
public interface SimpleService extends Service, SocketHandler {
 
}



