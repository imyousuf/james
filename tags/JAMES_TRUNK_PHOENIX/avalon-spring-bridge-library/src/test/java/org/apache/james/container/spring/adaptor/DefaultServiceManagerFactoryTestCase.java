/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.container.spring.adaptor;

import junit.framework.TestCase;
import org.apache.james.container.spring.beanfactory.AvalonBeanDefinition;
import org.apache.james.container.spring.beanfactory.AvalonServiceReference;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.springframework.util.ClassUtils;
import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.BeansException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 */
public class DefaultServiceManagerFactoryTestCase extends TestCase {

    private class TestApplicationContext extends AbstractRefreshableApplicationContext {
        
        private HashMap<String,BeanDefinition> beannameToDefinitionMap = new HashMap<String,BeanDefinition>();

        public void addBean(String beanName, BeanDefinition beanDefinition) {
            beannameToDefinitionMap.put(beanName, beanDefinition);
        }

        protected void loadBeanDefinitions(DefaultListableBeanFactory defaultListableBeanFactory) throws IOException, BeansException {
            Iterator<String> iterator = beannameToDefinitionMap.keySet().iterator();
            while (iterator.hasNext()) {
                String beanname = iterator.next();
                BeanDefinition beanDefinition = (BeanDefinition) beannameToDefinitionMap.get(beanname);
                defaultListableBeanFactory.registerBeanDefinition(beanname, beanDefinition);
            }
        }
    }

    public void testTrivialCase() throws ClassNotFoundException {
        AvalonBeanDefinition definition = new AvalonBeanDefinition();

        DefaultServiceManagerFactory managerFactory = new DefaultServiceManagerFactory();
        managerFactory.setApplicationContext(new TestApplicationContext());
        ServiceManager manager = managerFactory.getInstanceFor("referencesNothingBean", definition);
        assertNotNull(manager);
    }

    public void testRejectNonAvalonBeanDefs() throws ClassNotFoundException {
        BeanDefinition definition = new RootBeanDefinition();

        DefaultServiceManagerFactory managerFactory = new DefaultServiceManagerFactory();
        managerFactory.setApplicationContext(new TestApplicationContext());
        ServiceManager manager = managerFactory.getInstanceFor("referencesNothingBean", definition);
        assertNull(manager);
    }

    public void testReferenceResolving() throws ClassNotFoundException, ServiceException {
        
        // bean "referencingBean" of type Date references bean "referenced" of type StringBuffer
        
        AvalonBeanDefinition referencingBeanDefinition = new AvalonBeanDefinition();
        referencingBeanDefinition.setBeanClass(ClassUtils.forName("java.util.Date", getClass().getClassLoader()));
        referencingBeanDefinition.addServiceReference(new AvalonServiceReference("referenced", "java.lang.StringBuffer"));

        RootBeanDefinition referencedBeanDefinition = new RootBeanDefinition();
        referencedBeanDefinition.setBeanClass(ClassUtils.forName("java.lang.StringBuffer", getClass().getClassLoader()));

        TestApplicationContext testApplicationContext = new TestApplicationContext();
        testApplicationContext.addBean("referenced", referencedBeanDefinition);
        testApplicationContext.refresh();

        DefaultServiceManagerFactory managerFactory = new DefaultServiceManagerFactory();
        managerFactory.setApplicationContext(testApplicationContext);
        ServiceManager manager = managerFactory.getInstanceFor("referencingBean", referencingBeanDefinition);
        assertNotNull(manager);
        Object referencedInstance = manager.lookup("java.lang.StringBuffer");
        assertNotNull(referencedInstance);
        assertTrue(referencedInstance instanceof StringBuffer);
    }
    
    public void testRolenameCheck() throws ClassNotFoundException, ServiceException {
        
        AvalonBeanDefinition referencingBeanDefinition = new AvalonBeanDefinition();
        referencingBeanDefinition.setBeanClass(ClassUtils.forName("java.util.Date", getClass().getClassLoader()));
        referencingBeanDefinition.addServiceReference(new AvalonServiceReference("referenced1", "java.lang.StringBuffer"));
        referencingBeanDefinition.addServiceReference(new AvalonServiceReference("referenced2", "java.lang.StringBuffer"));

        RootBeanDefinition referencedBeanDefinition1 = new RootBeanDefinition();
        referencedBeanDefinition1.setBeanClass(ClassUtils.forName("java.lang.StringBuffer", getClass().getClassLoader()));

        RootBeanDefinition referencedBeanDefinition2 = new RootBeanDefinition();
        referencedBeanDefinition2.setBeanClass(ClassUtils.forName("java.lang.StringBuffer", getClass().getClassLoader()));

        TestApplicationContext testApplicationContext = new TestApplicationContext();
        testApplicationContext.addBean("referenced1", referencedBeanDefinition1);
        testApplicationContext.addBean("referenced2", referencedBeanDefinition2);
        testApplicationContext.refresh();

        
        DefaultServiceManagerFactory managerFactory = new DefaultServiceManagerFactory();
        managerFactory.setApplicationContext(testApplicationContext);
        try {
            managerFactory.getInstanceFor("referencingBean", referencingBeanDefinition);
            fail("must throw exception");
        } catch (Exception e) {
            assertTrue("cannot have 2 references of same type!", true);
        }
    }
    
    public void testTwoRoles() throws ClassNotFoundException, ServiceException {
        
        AvalonBeanDefinition referencingBeanDefinition = new AvalonBeanDefinition();
        referencingBeanDefinition.setBeanClass(ClassUtils.forName("java.util.Date", getClass().getClassLoader()));
        referencingBeanDefinition.addServiceReference(new AvalonServiceReference("referenced1", "java.lang.StringBuffer"));
        referencingBeanDefinition.addServiceReference(new AvalonServiceReference("referenced2", "java.util.Random"));

        RootBeanDefinition referencedBeanDefinition1 = new RootBeanDefinition();
        referencedBeanDefinition1.setBeanClass(ClassUtils.forName("java.lang.StringBuffer", getClass().getClassLoader()));

        RootBeanDefinition referencedBeanDefinition2 = new RootBeanDefinition();
        referencedBeanDefinition2.setBeanClass(ClassUtils.forName("java.util.Random", getClass().getClassLoader()));

        TestApplicationContext testApplicationContext = new TestApplicationContext();
        testApplicationContext.addBean("referenced1", referencedBeanDefinition1);
        testApplicationContext.addBean("referenced2", referencedBeanDefinition2);
        testApplicationContext.refresh();

        
        DefaultServiceManagerFactory managerFactory = new DefaultServiceManagerFactory();
        managerFactory.setApplicationContext(testApplicationContext);
        ServiceManager manager = managerFactory.getInstanceFor("referencingBean", referencingBeanDefinition);
        assertNotNull(manager);
        Object referencedInstance1 = manager.lookup("java.lang.StringBuffer");
        assertNotNull(referencedInstance1);
        assertTrue(referencedInstance1 instanceof StringBuffer);

        Object referencedInstance2 = manager.lookup("java.util.Random");
        assertNotNull(referencedInstance2);
        assertTrue(referencedInstance2 instanceof java.util.Random);
    }
}
