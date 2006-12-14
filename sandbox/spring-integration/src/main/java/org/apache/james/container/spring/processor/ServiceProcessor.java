package org.apache.james.container.spring.processor;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.container.spring.adaptor.ServiceManagerBridge;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

/**
 * calls service() for all avalon components
 */
public class ServiceProcessor extends AbstractProcessor implements BeanPostProcessor, Ordered {

    private ServiceManagerBridge serviceManagerBridge;

    public int getOrder() {
        return 2;
    }

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Serviceable && isIncluded(beanName)) {
	        Serviceable serviceable = (Serviceable) bean;
	        try {
	            serviceable.service(serviceManagerBridge.getInstance(beanName));
	        } catch (ServiceException e) {
	            throw new RuntimeException("could not successfully run service method on component of type " + serviceable.getClass(), e);
	        }			
		}
		return bean;
	}
	
	public void setServiceManagerBridge(ServiceManagerBridge serviceManagerBridge) {
		this.serviceManagerBridge=serviceManagerBridge;
	}
}
