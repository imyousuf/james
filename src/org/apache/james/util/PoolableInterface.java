package org.apache.james.util;

/**
 * Interface that must be followed for any objects that are used in a
 *  object pool
 * @author Matt Petteys <matt@arcticmail.com>
 */

public interface PoolableInterface
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

