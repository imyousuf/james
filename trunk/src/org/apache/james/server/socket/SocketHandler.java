package org.apache.james.server.socket;

import org.apache.james.util.Configurable;

/**
 * Interface for socket listeners
 * @author Federico Barbieri <scoobie@systemy.it>
 * @author Matthew Petteys <matt@arcticmail.com>
 * @version 0.9
 */

public interface SocketHandler
	extends Runnable, Configurable {}

