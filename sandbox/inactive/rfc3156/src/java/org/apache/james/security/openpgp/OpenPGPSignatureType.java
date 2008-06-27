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

package org.apache.james.security.openpgp;

/**
 * Enumerates the types of signature enumerated in 
 * <code>RFC-3156 OpenPGG/MIME</code>.
 *
 */
public final class OpenPGPSignatureType {
    
    /** OpenPGP using MD5 as the hash algorithm. */
    public static final OpenPGPSignatureType PGP_MD5 = new OpenPGPSignatureType("pgp-md5");
    /** OpenPGP using MD2 as the hash algorithm. */
    public static final OpenPGPSignatureType PGP_MD2 = new OpenPGPSignatureType("pgp-md2");
    /** OpenPGP using SHA1 as the hash algorithm. */
    public static final OpenPGPSignatureType PGP_SHA1 = new OpenPGPSignatureType("pgp-sha1");
    /** OpenPGP using RIPE 160 as the hash algorithm. */
    public static final OpenPGPSignatureType PGP_RIPE_MD_160 = new OpenPGPSignatureType("pgp-ripemd160");
    /** OpenPGP using TIGER 192 as the hash algorithm. */
    public static final OpenPGPSignatureType PGP_TIGER_192 = new OpenPGPSignatureType("pgp-tiger192");
    /** OpenPGP using HAVEL 5 160 as the hash algorithm. */
    public static final OpenPGPSignatureType PGP_HAVEL_5_160 = new OpenPGPSignatureType("pgp-haval-5-160");
    
    private final String code;
    
    private OpenPGPSignatureType(final String code) {
        this.code = code;
    }
    
    /**
     * Gets the <code>micalg</code> value for this algorithm 
     * as defined in <code>RFC-3156 OpenPGG/MIME</code>
     * @return the algorithm name, not null
     */
    public String getMessageIntegrityCheckAlgorithmCode() {
        return code;
    }

    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + code.hashCode();
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final OpenPGPSignatureType other = (OpenPGPSignatureType) obj;
        if (code == null) {
            if (other.code != null)
                return false;
        } else if (!code.equals(other.code))
            return false;
        return true;
    }
    
    public String toString()
    {
        return code;
    }
}
