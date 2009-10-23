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


package org.apache.james.smtpserver;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.internet.ParseException;

import junit.framework.TestCase;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.api.dnsservice.AbstractDNSServer;
import org.apache.james.smtpserver.integration.SMTPServerDNSServiceAdapter;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.core.fastfail.DNSRBLHandler;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.mailet.MailAddress;

public class DNSRBLHandlerTest extends TestCase {

    private SMTPServerDNSServiceAdapter mockedDnsServer;

    private SMTPSession mockedSMTPSession;

    private String remoteIp = "127.0.0.2";

    private boolean relaying = false;   
    
    public static final String RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME = "org.apache.james.smtpserver.rbl.blocklisted";
    
    public static final String RBL_DETAIL_MAIL_ATTRIBUTE_NAME = "org.apache.james.smtpserver.rbl.detail";

    protected void setUp() throws Exception {
        super.setUp();
        setupMockedDnsServer();
        setRelayingAllowed(false);
    }

    /**
     * Set the remoteIp
     * 
     * @param remoteIp The remoteIP to set
     */
    private void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    /**
     * Set relayingAllowed
     * 
     * @param relaying true or false
     */
    private void setRelayingAllowed(boolean relaying) {
        this.relaying = relaying;
    }

    /**
     * Setup the mocked dnsserver
     *
     */
    private void setupMockedDnsServer() {
        org.apache.james.api.dnsservice.DNSService dns  = new AbstractDNSServer() {

            public Collection findMXRecords(String hostname) {
                throw new UnsupportedOperationException("Unimplemented in mock");
            }

            public Collection findTXTRecords(String hostname) {
                List res = new ArrayList();
                if (hostname == null) {
                    return res;
                }
                ;
                if ("2.0.0.127.bl.spamcop.net.".equals(hostname)) {
                    res.add("Blocked - see http://www.spamcop.net/bl.shtml?127.0.0.2");
                }
                return res;
            }

            public InetAddress getByName(String host)
                    throws UnknownHostException {
                if ("2.0.0.127.bl.spamcop.net.".equals(host)) {
                    return InetAddress.getByName("127.0.0.1");
                } else if ("3.0.0.127.bl.spamcop.net.".equals(host)) {
                    return InetAddress.getByName("127.0.0.1");
                } else if ("1.0.168.192.bl.spamcop.net.".equals(host)) {
                    throw new UnknownHostException(host);
                }
                throw new UnsupportedOperationException("getByName("+host+") not implemented in DNSRBLHandlerTest mock");
            }
        };
        
        mockedDnsServer = new SMTPServerDNSServiceAdapter();
        mockedDnsServer.setDNSService(dns);
    }

    /**
     * Setup mocked smtpsession
     */
    private void setupMockedSMTPSession(final MailAddress rcpt) {
        mockedSMTPSession = new BaseFakeSMTPSession() {
            HashMap state = new HashMap();
            HashMap connectionState = new HashMap();
            
            public String getRemoteIPAddress() {
                return remoteIp;
            }

            public Map getState() {
                return state;
            }

            public boolean isRelayingAllowed() {
                return relaying;
            }

            public boolean isAuthSupported() {
                return false;
            }

            public int getRcptCount() {
                return 0;
            }

            public Map getConnectionState() {       
                return connectionState;
            }

        };
    }

    // ip is blacklisted and has txt details
    public void testBlackListedTextPresent() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        ContainerUtil.enableLogging(rbl, new MockLogger());

        setupMockedSMTPSession(new MailAddress("any@domain"));
        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertEquals("Details","Blocked - see http://www.spamcop.net/bl.shtml?127.0.0.2",
               mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNotNull("Blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip is blacklisted and has txt details but we don'T want to retrieve the txt record
    public void testGetNoDetail() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        ContainerUtil.enableLogging(rbl, new MockLogger());

        setupMockedSMTPSession(new MailAddress("any@domain"));
        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(false);
        rbl.onConnect(mockedSMTPSession);
        assertNull("No details",mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNotNull("Blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip is allowed to relay
    public void testRelayAllowed() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        ContainerUtil.enableLogging(rbl, new MockLogger());

        setRelayingAllowed(true);
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull("No details",mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNull("Not blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip not on blacklist
    public void testNotBlackListed() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        ContainerUtil.enableLogging(rbl, new MockLogger());
        setRemoteIp("192.168.0.1");
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull("No details",mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNull("Not blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip on blacklist without txt details
    public void testBlackListedNoTxt() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        ContainerUtil.enableLogging(rbl, new MockLogger());
        setRemoteIp("127.0.0.3");
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull(mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNotNull("Blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip on whitelist
    public void testWhiteListed() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        ContainerUtil.enableLogging(rbl, new MockLogger());
        setRemoteIp("127.0.0.2");
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSService(mockedDnsServer);

        rbl.setWhitelist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull(mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNull("Not blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }
    
    public void testInvalidConfig() {
        boolean exception = false;
        DNSRBLHandler rbl = new DNSRBLHandler();
        try {
            rbl.configure(new Configuration() {
				
				public Configuration subset(String prefix) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public void setProperty(String key, Object value) {
					// TODO Auto-generated method stub
					
				}
				
				public boolean isEmpty() {
					// TODO Auto-generated method stub
					return true;
				}
				
				public String[] getStringArray(String key) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public String getString(String key, String defaultValue) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public String getString(String key) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public Short getShort(String key, Short defaultValue) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public short getShort(String key, short defaultValue) {
					// TODO Auto-generated method stub
					return 0;
				}
				
				public short getShort(String key) {
					// TODO Auto-generated method stub
					return 0;
				}
				
				public Object getProperty(String key) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public Properties getProperties(String key) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public Long getLong(String key, Long defaultValue) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public long getLong(String key, long defaultValue) {
					// TODO Auto-generated method stub
					return 0;
				}
				
				public long getLong(String key) {
					// TODO Auto-generated method stub
					return 0;
				}
				
				public List getList(String key, List defaultValue) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public List getList(String key) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public Iterator getKeys(String prefix) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public Iterator getKeys() {
					// TODO Auto-generated method stub
					return null;
				}
				
				public Integer getInteger(String key, Integer defaultValue) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public int getInt(String key, int defaultValue) {
					// TODO Auto-generated method stub
					return 0;
				}
				
				public int getInt(String key) {
					// TODO Auto-generated method stub
					return 0;
				}
				
				public Float getFloat(String key, Float defaultValue) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public float getFloat(String key, float defaultValue) {
					// TODO Auto-generated method stub
					return 0;
				}
				
				public float getFloat(String key) {
					// TODO Auto-generated method stub
					return 0;
				}
				
				public Double getDouble(String key, Double defaultValue) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public double getDouble(String key, double defaultValue) {
					// TODO Auto-generated method stub
					return 0;
				}
				
				public double getDouble(String key) {
					// TODO Auto-generated method stub
					return 0;
				}
				
				public Byte getByte(String key, Byte defaultValue) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public byte getByte(String key, byte defaultValue) {
					// TODO Auto-generated method stub
					return 0;
				}
				
				public byte getByte(String key) {
					// TODO Auto-generated method stub
					return 0;
				}
				
				public Boolean getBoolean(String key, Boolean defaultValue) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public boolean getBoolean(String key, boolean defaultValue) {
					// TODO Auto-generated method stub
					return false;
				}
				
				public boolean getBoolean(String key) {
					// TODO Auto-generated method stub
					return false;
				}
				
				public BigInteger getBigInteger(String key, BigInteger defaultValue) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public BigInteger getBigInteger(String key) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public BigDecimal getBigDecimal(String key) {
					// TODO Auto-generated method stub
					return null;
				}
				
				public boolean containsKey(String key) {
					// TODO Auto-generated method stub
					return false;
				}
				
				public void clearProperty(String key) {
					// TODO Auto-generated method stub
					
				}
				
				public void clear() {
					// TODO Auto-generated method stub
					
				}
				
				public void addProperty(String key, Object value) {
					// TODO Auto-generated method stub
					
				}
			});
        } catch (ConfigurationException e) {
            exception = true;
        }
        
        assertTrue("Invalid config",exception);
    }

}
