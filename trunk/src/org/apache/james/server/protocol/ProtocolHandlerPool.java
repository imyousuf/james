package org.apache.james.server.protocol;

import java.util.*;
import org.apache.james.server.JamesServ;
import org.apache.james.util.PoolInterface;
import org.apache.james.util.PoolableInterface;
import org.apache.james.util.Logger;


/**
 * Pool of Protocol Handlers that handles sockets from a Socket Listener
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class ProtocolHandlerPool 
	implements PoolInterface
{
	private Stack threads;
	private int NumThreads;
	private int MaxThreads;
	private int HardMaxThreads;
	private Class WorkerClass;
	private JamesServ james;
	private Properties props;
	private long maxLastUsed;
	private Logger l;
	
	private int threadCount;

	/**
	 * Creates the Pool
	 */
	public ProtocolHandlerPool() {}

	/**
	 * Initializes the Pool
	 * @param org.apache.james.Jameserv js
	 * @param java.util.Properties ps
	 * @throws Exception
	 */
	public void init(JamesServ js, Properties ps)
 		throws Exception
	{
		l = (Logger) js.getLogger();
		l.log("Initializing the ObjectPool..");
		
		threadCount = 0;
		threads = new Stack();
		james = js;
		props = ps;
		
		Properties poolProp = james.getBranchProperties( props, "pool." );

		String clsName = poolProp.getProperty("class");
		
		if (clsName == null) {
		 throw new Exception("No class name specified for pool");
		}

		WorkerClass = Class.forName( clsName );
		maxLastUsed = (Long.parseLong(poolProp.getProperty("timeout", "300"))*100);
		NumThreads = Integer.parseInt(poolProp.getProperty("initsize", "3"));
		MaxThreads = Integer.parseInt(poolProp.getProperty("topsize", "10"));
		HardMaxThreads = Integer.parseInt(poolProp.getProperty("maxthread", "2"));

		l.log("Initial Pool Thread Count set to " +  new Integer(NumThreads));
		l.log("Max Pool Thread Count set to " +  new Integer(MaxThreads));
		l.log("Hard Max Thread Count set to " +  new Integer(HardMaxThreads));
		l.log("Timeout Interval set to " + new Long(maxLastUsed/100) + " seconds ");

		PoolableInterface poolClass;
		WorkerThread t;
		for ( int i = 0; i < NumThreads; i++ )
		{
			l.log("Init PoolableInterface " + i);

			poolClass = (PoolableInterface) WorkerClass.newInstance();
			poolClass.init(this.james, this.props);
			t = new WorkerThread("JamesWorkerThread#"+i, poolClass);
			t.start();
			threads.push (t);
			
			threadCount++;
		}
	}

	/**
	 * Clean up for the protocol pool
	 * @return void
	 */
	public void destroy()
	{}

	/**
	 * Request the Pool to perform some work. 
	 * @param data Data to give to the WorkerThread
	 * @return void
	 * @throws InstantiationException Thrown if additional WorkerThread can't be created
	 */
	public void service(Object workObject)
		throws Exception
	{

		l.log("Pool servicing connection.");

		boolean maxThreads = false;
		WorkerThread t = null;
		
		// Will block until a WorkerThread opens up..
		while (t == null) {
		
			if (maxThreads) {
				l.log("Max thread limit reached:" + String.valueOf(HardMaxThreads) );
				
				// Hopefully this should put this thread to sleep for a bit
				try {
					Thread th = Thread.currentThread();
					th.sleep(300);
				} catch (InterruptedException ie) {}
			}
		
			synchronized (threads) {
			
				if ( threads.empty() ) {
				
					// See if this thread (current num + 1) would violate max thread rule
					// 	continuing to prevent dead lock
					if ((threadCount + 1) > HardMaxThreads) {
						maxThreads = true;
						continue;
					}
				
					try {
					  int newCount = threadCount + 1;
						PoolableInterface poolClass = (PoolableInterface) WorkerClass.newInstance();
						poolClass.init(this.james, this.props);
						t = new WorkerThread("JamesWorkerThread#" + newCount, poolClass);
						t.start();
						threadCount = newCount;
					} catch (Exception e) {
						throw new InstantiationException ("Problem creating instance of PoolableInterface.class: " + e.getMessage());
					}
					
				} else {
					t = (WorkerThread) threads.pop();
				}
			}			
		}
	
		l.log("Found worker thread and waking.");
		t.wake(workObject);		
	}

	/**
	 * Method used by WorkerThread to put Thread back on the stack
	 * @param w WorkerThread to push
	 * @return boolean True if pushed, false otherwise
	 */
	private boolean returnThread (WorkerThread t)
	{		
		synchronized (threads) {
		
			// System time this worker last ran plus the timeout period
			//  if system time is greater than this worker is underworked so kill
			long timeout = t.getLastRun() + maxLastUsed;
			
			// the maxLastUsed is not zero
			//  and currenttime greater than the timeout period
			// or thread count is greater than the maximum number of threads allowd in the pool
			// and thread count is more than the min base in the pool
			//  then we can throw this out
			if (( ((maxLastUsed != 0) && (System.currentTimeMillis() > timeout))
				|| (threadCount > MaxThreads)) && (threadCount > NumThreads) )
			{		
				l.log("Removing Workerthread from pool. ");
				threadCount--;
				return false;
			}	else {
				threads.push(t);
				return true;
			}
		}
	}
	

	/**
	 * Actual thread that processes the request
	 */
	class WorkerThread extends Thread
	{
		private PoolableInterface workClass;
		private Object workObject;
		private long lastrun;
		private boolean stop;

		/**
		 * Creates a new WorkerThread
		 * @param id Thread ID
		 * @param worker WorkerThread instance associated with the WorkerThread
		 */
		WorkerThread(String id, PoolableInterface ph)
		{
			super(id);
			workClass = ph;
			workObject = null;
			updateLastTime();
		}

		void updateLastTime() {
			lastrun = System.currentTimeMillis();
		}

		/**
		 * Wakes the thread and does some work
		 * @param Object o - Data to send to the Worker
		 * @return void
		 */
		synchronized void wake(Object o)
		{
			//System.out.println("Assigning work object to worker class");
			workObject = o;
			notify();
		}

		/**
		 * Returns the last time (in seconds from epoche) this worker ran
		 * @return long
		 */
		public long getLastRun() {
			return lastrun;
		}
		
		/**
		 * WorkerThread's thread routine
		 * return void
		 */
		synchronized public void run()
		{
			while (!stop) {
				// Wait until there is a work object assigned to this thread
				if ( workObject == null ) {
					try {
						wait();
					} catch (InterruptedException e) {
						continue;
					}
				}
				// Double check to make sure workObject isn't null
				if ( workObject != null ) {
						try {
							//System.out.println("Calling service on worker class");
							workClass.service(workObject);
						} catch (Exception e) {
							System.out.println("Caught error in WorkerThread of Pool..");
							e.printStackTrace(System.out);
						} //TODO maybe log an exception
				}
				// Set the work object back to null
				workObject = null;
				// Check to see if we should stop this thread
				stop = !(returnThread(this));
				// Update the last time this thread ran
				updateLastTime();
			}
			// Clean up workClass
			workClass.destroy();	
		}
	}
}
	

