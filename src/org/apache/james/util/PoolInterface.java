package org.apache.james.util;

/**
 * Interface for any pool object stores
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public interface PoolInterface
	extends Configurable
{

	/**
	 * Method that is called to add a object that need to be processed to the pool
	 * @return void
	 */
	public void service(Object workUnit)
		throws Exception;

	/**
	 * Method that should be called to clean up the pool
	 * @return void
	 */
	public void destroy();

}

