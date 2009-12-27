package org.apache.james.container.spring.lifecycle;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

public abstract class AbstractLifeCycleBeanPostProcessor<T> implements BeanPostProcessor, Ordered{

	private Map<String, String> mappings;

	public void setMappings(Map<String,String> mappings) {
		this.mappings = mappings;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object, java.lang.String)
	 */
	public Object postProcessAfterInitialization(Object bean, String name)
			throws BeansException {
		return bean;
	}
	
	protected abstract Class<T> getLifeCycleInterface();
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public Object postProcessBeforeInitialization(Object bean, String name)
			throws BeansException {
		try {
			Class<T> lClass = getLifeCycleInterface();
			if (lClass.isInstance(bean)) executeLifecycleMethod((T)bean, name, getMapping(name));
		} catch (Exception e) {
			throw new FatalBeanException("Unable to execute lifecycle method on bean" + name,e);
		}
		return bean;
	}

	protected abstract void executeLifecycleMethod(T bean, String beanname, String lifecyclename) throws Exception;
	
	private String getMapping(String name) {
		String newname = mappings.get(name);
		if (newname == null) newname = name;
		return newname;
	}
	
}
