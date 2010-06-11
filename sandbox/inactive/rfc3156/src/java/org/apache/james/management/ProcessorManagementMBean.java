package org.apache.james.management;

/**
 * Expose processor management functionality through JMX.
 *
 * @phoenix:mx-topic name="ProcessorAdministration"
 */
public interface ProcessorManagementMBean {

    /**
     * Retrieves all existing processors
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Retrieves all existing processors
     *
     * @return names of all configured processors
     */
    String[] getProcessorNames();

}
