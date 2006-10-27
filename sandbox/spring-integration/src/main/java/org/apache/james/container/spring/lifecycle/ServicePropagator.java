package org.apache.james.container.spring.lifecycle;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.container.spring.adaptor.ServiceManagerBridge;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;

/**
 * calls service() for all avalon components
 */
public class ServicePropagator extends AbstractPropagator implements BeanFactoryPostProcessor, Ordered {

    private ServiceManagerBridge serviceManager;

    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

        serviceManager = (ServiceManagerBridge) configurableListableBeanFactory.getBean("serviceManager");

        super.postProcessBeanFactory(configurableListableBeanFactory);
    }

    protected Class getLifecycleInterface() {
        return Serviceable.class;
    }

    protected void invokeLifecycleWorker(String beanName, Object bean) {
        Serviceable serviceable = (Serviceable) bean;
        try {
            serviceable.service(serviceManager);
        } catch (ServiceException e) {
            throw new RuntimeException("could not successfully run service method on component of type " + serviceable.getClass(), e);
        }
    }

    public int getOrder() {
        return 2;
    }
}
