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
package org.apache.james.vut;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnit;

import org.apache.james.api.vut.management.InvalidMappingException;
import org.apache.james.impl.vut.AbstractVirtualUserTable;
import org.apache.james.impl.vut.VirtualUserTableUtil;
import org.apache.james.vut.model.JPAVirtualUser;

/**
 * Class responsible to implement the Virtual User Table in database with JPA access.
 */
public class JPAVirtualUserTable extends AbstractVirtualUserTable {

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

    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#addMappingInternal(String, String, String)
     */
    protected boolean addMappingInternal(String user, String domain, String regex) throws InvalidMappingException {
        
        String newUser = getUserString(user);
        String newDomain = getDomainString(domain);
        Collection<String> map = getUserDomainMappings(newUser,newDomain);
    
        if (map != null && map.size() != 0) {
            map.add(regex);
            return updateMapping(newUser, newDomain, VirtualUserTableUtil.CollectionToMapping(map));
        }
    
        return addRawMapping(newUser,newDomain,regex);
    
    }

    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#mapAddressInternal(java.lang.String, java.lang.String)
     */
    protected String mapAddressInternal(String user, String domain) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            List<JPAVirtualUser> virtualUsers = entityManager.createNamedQuery("selectMappings")
                .setParameter("user", user)
                .setParameter("domain", domain).getResultList();
            transaction.commit();
            if(virtualUsers.size() > 0) {
                return virtualUsers.get(0).getTargetAddress();
            }
        } catch (PersistenceException e) {
            getLogger().debug("Failed to find mapping for  user=" + user + " and domain=" + domain, e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
        } finally {
            entityManager.close();
        }
        return null;
    }
    
    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#mapAddress(java.lang.String, java.lang.String)
     */
    protected Collection<String> getUserDomainMappingsInternal(String user, String domain) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            List<JPAVirtualUser> virtualUsers = entityManager.createNamedQuery("selectUserDomainMapping")
                .setParameter("user", user)
                .setParameter("domain", domain).getResultList();
            transaction.commit();
            if (virtualUsers.size() > 0) {
                return VirtualUserTableUtil.mappingToCollection(virtualUsers.get(0).getTargetAddress());
            }
        } catch (PersistenceException e) {
            getLogger().debug("Failed to get user domain mappings", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
        } finally {
            entityManager.close();
        }
        return null;
    }

    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#getAllMappingsInternal()
     */
    protected Map<String,Collection<String>> getAllMappingsInternal() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        final EntityTransaction transaction = entityManager.getTransaction();
        Map<String,Collection<String>> mapping = new HashMap<String,Collection<String>>();
        try {
            transaction.begin();
            List<JPAVirtualUser> virtualUsers = entityManager.createNamedQuery("selectAllMappings").getResultList();
            transaction.commit();
            for (JPAVirtualUser virtualUser: virtualUsers) {
                mapping.put(virtualUser.getUser()+ "@" + virtualUser.getDomain(), VirtualUserTableUtil.mappingToCollection(virtualUser.getTargetAddress()));
            }
            if (mapping.size() > 0) return mapping;
        } catch (PersistenceException e) {
            getLogger().debug("Failed to get all mappings", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
        } finally {
            entityManager.close();
        }
        return null;
    }

    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#removeMappingInternal(String, String, String)
     */
    protected boolean removeMappingInternal(String user, String domain, String mapping) throws InvalidMappingException {
        String newUser = getUserString(user);
        String newDomain = getDomainString(domain);
        Collection<String> map = getUserDomainMappings(newUser,newDomain);
        if (map != null && map.size() > 1) {
            map.remove(mapping);
            return updateMapping(newUser,newDomain,VirtualUserTableUtil.CollectionToMapping(map));
        } else {
            return removeRawMapping(newUser,newDomain,mapping);
        }
    }

    /**
     * Update the mapping for the given user and domain
     * 
     * @param user the user
     * @param domain the domain
     * @param mapping the mapping
     * @return true if update was successfully
     */
    private boolean updateMapping(String user, String domain, String mapping) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            int updated = entityManager.createNamedQuery("updateMapping")
                .setParameter("targetAddress", mapping)
                .setParameter("user", user)
                .setParameter("domain", domain).executeUpdate();
            transaction.commit();
            if (updated > 0) {
                return true;
            }
        } catch (PersistenceException e) {
            getLogger().debug("Failed to update mapping", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
        } finally {
            entityManager.close();
        }
        return false;
    }
    
    
    /**
     * Remove a mapping for the given user and domain
     * 
     * @param user the user
     * @param domain the domain
     * @param mapping the mapping
     * @return true if successful
     */
    private boolean removeRawMapping(String user, String domain, String mapping) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            int deleted = entityManager.createNamedQuery("deleteMapping")
                .setParameter("user", user)
                .setParameter("domain", domain)
                .setParameter("targetAddress", mapping).executeUpdate();
            transaction.commit();
            if (deleted > 0) {
                return true;
            }
        } catch (PersistenceException e) {
            getLogger().debug("Failed to remove mapping", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
        } finally {
            entityManager.close();
        }
        return false;
    }
    
    /**
     * Add mapping for given user and domain
     * 
     * @param user the user
     * @param domain the domain
     * @param mapping the mapping 
     * @return true if successfully
     */
    private boolean addRawMapping(String user, String domain, String mapping) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            JPAVirtualUser jpaVirtualUser = new JPAVirtualUser(user, domain, mapping);
            entityManager.persist(jpaVirtualUser);
            transaction.commit();
            return true;
        } catch (PersistenceException e) {
            getLogger().debug("Failed to save virtual user", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
        } finally {
            entityManager.close();
        }
        return false;
    }
    
    /**
     * Return user String for the given argument
     * 
     * @param user the given user String
     * @return user the user String
     * @throws InvalidMappingException get thrown on invalid argument
     */
    private String getUserString(String user) throws InvalidMappingException {
        if (user != null) {
            if(user.equals(WILDCARD) || user.indexOf("@") < 0) {
                return user;
            } else {
                throw new InvalidMappingException("Invalid user: " + user);
            }
        } else {
            return WILDCARD;
        }
    }
    
    /**
     * Return domain String for the given argument
     * 
     * @param domain the given domain String
     * @return domainString the domain String
     * @throws InvalidMappingException get thrown on invalid argument
     */
    private String getDomainString(String domain) throws InvalidMappingException {
        if(domain != null) {
            if (domain.equals(WILDCARD) || domain.indexOf("@") < 0) {
                return domain;  
            } else {
                throw new InvalidMappingException("Invalid domain: " + domain);
            }
        } else {
            return WILDCARD;
        }
    }
    
}
