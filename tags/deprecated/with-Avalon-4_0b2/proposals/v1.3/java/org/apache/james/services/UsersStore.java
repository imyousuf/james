/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.services;

import org.apache.avalon.phoenix.Service;

/**
 * Interface for Phoenix blocks to access a store of Users. A UserStore
 * contains one or more UserRepositories. Multiple UserRepositories may or may
 * not have overlapping membership. 
 *
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 */
public interface UsersStore 
    extends Service {

    UsersRepository getRepository( String name );
}
