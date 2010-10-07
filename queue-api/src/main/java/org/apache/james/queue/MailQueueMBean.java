package org.apache.james.queue;

public interface MailQueueMBean {

    /**
     * Return the name of the queue
     * 
     * @return queue
     */
    public String getName();
    
    /**
     * Return the count of the queued mails
     * 
     * @return size
     */
    public int getSize();
    
    
}
