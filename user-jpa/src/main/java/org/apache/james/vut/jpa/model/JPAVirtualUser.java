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
package org.apache.james.vut.jpa.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * VirtualUserTable class for the James Virtual User Table to be used for JPA persistence.
 */
@Entity(name = "JamesVirtualUserTable")
@Table(name = "JAMES_VIRTUAL_USER_TABLE")
@NamedQueries( { 
    @NamedQuery(name = "selectMappings", query = "SELECT vut FROM JamesVirtualUserTable vut WHERE (vut.user LIKE :user OR vut.user='*') and (vut.domain like :domain or vut.domain='*') ORDER BY vut.domain DESC"),
    @NamedQuery(name = "selectUserDomainMapping", query = "SELECT vut FROM JamesVirtualUserTable vut WHERE vut.user=:user AND vut.domain=:domain"),
    @NamedQuery(name = "selectAllMappings", query = "SELECT vut FROM JamesVirtualUserTable vut"), 
    @NamedQuery(name = "deleteMapping", query = "DELETE FROM JamesVirtualUserTable vut WHERE vut.user=:user AND vut.domain=:domain AND vut.targetAddress=:targetAddress"),
    @NamedQuery(name = "updateMapping", query = "UPDATE JamesVirtualUserTable vut SET vut.targetAddress=:targetAddress WHERE vut.user=:user AND vut.domain=:domain")
})
@IdClass(JPAVirtualUser.VirtualUserTableId.class)
public class JPAVirtualUser {

    public static class VirtualUserTableId implements Serializable {

        private static final long serialVersionUID = 1L;

        private String user;
        
        private String domain;
        
        public VirtualUserTableId() {}

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + (int) (user.hashCode() ^ (user.hashCode() >>> 32));
            result = PRIME * result + (int) (domain.hashCode() ^ (domain.hashCode() >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final VirtualUserTableId other = (VirtualUserTableId) obj;
            if (! user.equals(other.user))
                return false;
            if (! domain.equals(other.domain))
                return false;
            return true;
        }
    }

    /**
     * The name of the user. 
     */
    @Id
    @Column(name = "USER_NAME", nullable = false, length = 100)
    private String user = "";
    
    /**
     * The name of the domain.
     * Column name is chosen to be compatible with the JDBCVirtualUserTableList.
     */
    @Id
    @Column(name = "DOMAIN_NAME", nullable = false, length = 100)
    private String domain = "";
    
    /**
     * The target address. column name is chosen to be compatible with the JDBCVirtualUserTableList.
     */
    @Column(name = "TARGET_ADDRESS", nullable = false, length = 100)
    private String targetAddress = "";
    
    /**
     * Use this simple constructor to create a new VirtualUserTable.
     * 
     * @param user, domain and their associated targetAddress
     */
    public JPAVirtualUser(String user, String domain, String targetAddress) {
        this.user = user;
        this.domain = domain;
        this.targetAddress = targetAddress;
    }
    
    public String getUser() {
        return user;
    }

    public String getDomain() {
        return domain;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

}
