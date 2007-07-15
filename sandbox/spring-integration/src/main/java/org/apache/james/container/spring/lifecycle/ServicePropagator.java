package org.apache.james.container.spring.lifecycle;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.container.spring.adaptor.ServiceManagerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;

/**
 * calls service() for all avalon components
 */
public class ServicePropagator extends AbstractPropagator implements BeanFactoryPostProcessor, Ordered {

    private ServiceManagerFactory serviceManagerFactory;

    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

        serviceManagerFactory = (ServiceManagerFactory) configurableListableBeanFactory.getBean("serviceManager");

        super.postProcessBeanFactory(configurableListableBeanFactory);
    }

    protected Class getLifecycleInterface() {
        return Serviceable.class;
    }

    protected void invokeLifecycleWorker(String beanName, Object bean, BeanDefinition beanDefinition) {
        
        Serviceable serviceable = (Serviceable) bean;
        try {
            ServiceManager serviceManager = serviceManagerFactory.getInstanceFor(beanName, beanDefinition);
            if (serviceManager == null) {
                throw new RuntimeException("failed to create service manager for " + beanName);
            }
            serviceable.service(serviceManager);
        } catch (ServiceException e) {
            throw new RuntimeException("could not successfully run service method on component of type " + serviceable.getClass(), e);
        } catch (Exception e) {
            throw new RuntimeException("could not successfully run service method on component of type " + serviceable.getClass(), e);
        }
    }

    public int getOrder() {
        return 2;
    }
}
