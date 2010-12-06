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
package org.apache.james.domainlist.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnit;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.domainlist.jpa.model.JPADomain;
import org.apache.james.domainlist.lib.AbstractDomainList;
import org.apache.james.lifecycle.Configurable;

/**
 * JPA implementation of the DomainList.
 * This implementation is compatible with the JDBCDomainList, meaning same database schema can be reused.
 *
 */
public class JPADomainList extends AbstractDomainList implements Configurable {
    
    /**
     * The entity manager to access the database.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * Set the entity manager to use.
     * 
     * @param entityManagerFactory
     */
    @PersistenceUnit
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }
    
    @PostConstruct
    public void init() {
        createEntityManager().close();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    @SuppressWarnings("unchecked")
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        // TODO The common configuration could be migrated to AbstractDomainList (should it implement Configurable?)
        setAutoDetect(config.getBoolean("autodetect", true));    
        setAutoDetectIP(config.getBoolean("autodetectIP", true));    
    }
   
    /**
     * @see org.apache.james.domainlist.lib.AbstractDomainList#getDomainListInternal()
     */
    protected List<String> getDomainListInternal() {
        List<String> domains = new ArrayList<String>();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            domains = entityManager.createNamedQuery("listDomainNames").getResultList();
            transaction.commit();
        } catch (PersistenceException e) {
            getLogger().debug("Failed to list domains", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
        } finally {
            entityManager.close();
        }
        if (domains.size() == 0) {
            return null;
        } else {
            return new ArrayList<String>(domains);
        }
    }

    /**
     * @see org.apache.james.domainlist.api.DomainList#containsDomain(java.lang.String)
     */
    public boolean containsDomain(String domain) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            JPADomain jpaDomain = (JPADomain) entityManager.createNamedQuery("findDomainByName").setParameter("name", domain).getSingleResult();
            transaction.commit();
            return (jpaDomain != null) ? true : false;
        } catch (PersistenceException e) {
            getLogger().debug("Failed to find domain", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
        } finally {
            entityManager.close();
        }    
        return false;
    }

    /**
     * @see org.apache.james.domainlist.lib.AbstractDomainList#addDomainInternal(java.lang.String)
     */
    protected boolean addDomainInternal(String domain) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            JPADomain jpaDomain = new JPADomain(domain);
            entityManager.persist(jpaDomain);
            transaction.commit();
            return true;
        } catch (PersistenceException e) {
            getLogger().debug("Failed to save domain", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
        } finally {
            entityManager.close();
        }
        return false;
    }

    /**
     * @see org.apache.james.domainlist.lib.AbstractDomainList#removeDomainInternal(java.lang.String)
     */
    protected boolean removeDomainInternal(String domain) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.createNamedQuery("deleteDomainByName").setParameter("name", domain).executeUpdate();
            transaction.commit();
            return true;
        } catch (PersistenceException e) {
            getLogger().debug("Failed to remove domain", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
        } finally {
            entityManager.close();
        }
        return false;
    }

    /**
     * Return a new {@link EntityManager} instance
     * 
     * @return manager
     */
    private EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

}
