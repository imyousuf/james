package org.apache.james.mailboxmanager.torque.om;


import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.torque.TorqueException;
import org.apache.torque.om.BaseObject;
import org.apache.torque.om.ComboKey;
import org.apache.torque.om.DateKey;
import org.apache.torque.om.NumberKey;
import org.apache.torque.om.ObjectKey;
import org.apache.torque.om.SimpleKey;
import org.apache.torque.om.StringKey;
import org.apache.torque.om.Persistent;
import org.apache.torque.util.Criteria;
import org.apache.torque.util.Transaction;



            

/**
 * This class was autogenerated by Torque on:
 *
 * [Tue Sep 19 10:06:28 CEST 2006]
 *
 * You should not use this class directly.  It should not even be
 * extended all references should be to MailboxRow
 */
public abstract class BaseMailboxRow extends BaseObject
{
    /** The Peer class */
    private static final MailboxRowPeer peer =
        new MailboxRowPeer();

        
    /** The value for the mailboxId field */
    private long mailboxId;
      
    /** The value for the name field */
    private String name;
      
    /** The value for the uidValidity field */
    private long uidValidity;
      
    /** The value for the lastUid field */
    private long lastUid;
                                          
    /** The value for the messageCount field */
    private int messageCount = 0;
                                          
    /** The value for the size field */
    private long size = 0;
  
            
    /**
     * Get the MailboxId
     *
     * @return long
     */
    public long getMailboxId()
    {
        return mailboxId;
    }

                                              
    /**
     * Set the value of MailboxId
     *
     * @param v new value
     */
    public void setMailboxId(long v) throws TorqueException
    {
    
                  if (this.mailboxId != v)
              {
            this.mailboxId = v;
            setModified(true);
        }
    
          
                                  
                  // update associated MessageRow
        if (collMessageRows != null)
        {
            for (int i = 0; i < collMessageRows.size(); i++)
            {
                ((MessageRow) collMessageRows.get(i))
                    .setMailboxId(v);
            }
        }
                                }
          
    /**
     * Get the Name
     *
     * @return String
     */
    public String getName()
    {
        return name;
    }

                        
    /**
     * Set the value of Name
     *
     * @param v new value
     */
    public void setName(String v) 
    {
    
                  if (!ObjectUtils.equals(this.name, v))
              {
            this.name = v;
            setModified(true);
        }
    
          
              }
          
    /**
     * Get the UidValidity
     *
     * @return long
     */
    public long getUidValidity()
    {
        return uidValidity;
    }

                        
    /**
     * Set the value of UidValidity
     *
     * @param v new value
     */
    public void setUidValidity(long v) 
    {
    
                  if (this.uidValidity != v)
              {
            this.uidValidity = v;
            setModified(true);
        }
    
          
              }
          
    /**
     * Get the LastUid
     *
     * @return long
     */
    public long getLastUid()
    {
        return lastUid;
    }

                        
    /**
     * Set the value of LastUid
     *
     * @param v new value
     */
    public void setLastUid(long v) 
    {
    
                  if (this.lastUid != v)
              {
            this.lastUid = v;
            setModified(true);
        }
    
          
              }
          
    /**
     * Get the MessageCount
     *
     * @return int
     */
    public int getMessageCount()
    {
        return messageCount;
    }

                        
    /**
     * Set the value of MessageCount
     *
     * @param v new value
     */
    public void setMessageCount(int v) 
    {
    
                  if (this.messageCount != v)
              {
            this.messageCount = v;
            setModified(true);
        }
    
          
              }
          
    /**
     * Get the Size
     *
     * @return long
     */
    public long getSize()
    {
        return size;
    }

                        
    /**
     * Set the value of Size
     *
     * @param v new value
     */
    public void setSize(long v) 
    {
    
                  if (this.size != v)
              {
            this.size = v;
            setModified(true);
        }
    
          
              }
  
         
                                
            
          /**
     * Collection to store aggregation of collMessageRows
     */
    protected List collMessageRows;

    /**
     * Temporary storage of collMessageRows to save a possible db hit in
     * the event objects are add to the collection, but the
     * complete collection is never requested.
     */
    protected void initMessageRows()
    {
        if (collMessageRows == null)
        {
            collMessageRows = new ArrayList();
        }
    }

        
    /**
     * Method called to associate a MessageRow object to this object
     * through the MessageRow foreign key attribute
     *
     * @param l MessageRow
     * @throws TorqueException
     */
    public void addMessageRow(MessageRow l) throws TorqueException
    {
        getMessageRows().add(l);
        l.setMailboxRow((MailboxRow) this);
    }

    /**
     * The criteria used to select the current contents of collMessageRows
     */
    private Criteria lastMessageRowsCriteria = null;
      
    /**
                   * If this collection has already been initialized, returns
     * the collection. Otherwise returns the results of
     * getMessageRows(new Criteria())
                   *
     * @return the collection of associated objects
           * @throws TorqueException
           */
    public List getMessageRows()
              throws TorqueException
          {
                      if (collMessageRows == null)
        {
            collMessageRows = getMessageRows(new Criteria(10));
        }
                return collMessageRows;
          }

    /**
           * If this collection has already been initialized with
     * an identical criteria, it returns the collection.
     * Otherwise if this MailboxRow has previously
           * been saved, it will retrieve related MessageRows from storage.
     * If this MailboxRow is new, it will return
     * an empty collection or the current collection, the criteria
     * is ignored on a new object.
     *
     * @throws TorqueException
     */
    public List getMessageRows(Criteria criteria) throws TorqueException
    {
              if (collMessageRows == null)
        {
            if (isNew())
            {
               collMessageRows = new ArrayList();
            }
            else
            {
                        criteria.add(MessageRowPeer.MAILBOX_ID, getMailboxId() );
                        collMessageRows = MessageRowPeer.doSelect(criteria);
            }
        }
        else
        {
            // criteria has no effect for a new object
            if (!isNew())
            {
                // the following code is to determine if a new query is
                // called for.  If the criteria is the same as the last
                // one, just return the collection.
                            criteria.add(MessageRowPeer.MAILBOX_ID, getMailboxId());
                            if (!lastMessageRowsCriteria.equals(criteria))
                {
                    collMessageRows = MessageRowPeer.doSelect(criteria);
                }
            }
        }
        lastMessageRowsCriteria = criteria;

        return collMessageRows;
          }

    /**
           * If this collection has already been initialized, returns
     * the collection. Otherwise returns the results of
     * getMessageRows(new Criteria(),Connection)
           * This method takes in the Connection also as input so that
     * referenced objects can also be obtained using a Connection
     * that is taken as input
     */
    public List getMessageRows(Connection con) throws TorqueException
    {
              if (collMessageRows == null)
        {
            collMessageRows = getMessageRows(new Criteria(10), con);
        }
        return collMessageRows;
          }

    /**
           * If this collection has already been initialized with
     * an identical criteria, it returns the collection.
     * Otherwise if this MailboxRow has previously
           * been saved, it will retrieve related MessageRows from storage.
     * If this MailboxRow is new, it will return
     * an empty collection or the current collection, the criteria
     * is ignored on a new object.
     * This method takes in the Connection also as input so that
     * referenced objects can also be obtained using a Connection
     * that is taken as input
     */
    public List getMessageRows(Criteria criteria, Connection con)
            throws TorqueException
    {
              if (collMessageRows == null)
        {
            if (isNew())
            {
               collMessageRows = new ArrayList();
            }
            else
            {
                         criteria.add(MessageRowPeer.MAILBOX_ID, getMailboxId());
                         collMessageRows = MessageRowPeer.doSelect(criteria, con);
             }
         }
         else
         {
             // criteria has no effect for a new object
             if (!isNew())
             {
                 // the following code is to determine if a new query is
                 // called for.  If the criteria is the same as the last
                 // one, just return the collection.
                             criteria.add(MessageRowPeer.MAILBOX_ID, getMailboxId());
                             if (!lastMessageRowsCriteria.equals(criteria))
                 {
                     collMessageRows = MessageRowPeer.doSelect(criteria, con);
                 }
             }
         }
         lastMessageRowsCriteria = criteria;

         return collMessageRows;
           }

                  
              
                    
                              
                                
                                                              
                                        
                    
                    
          
    /**
                 * If this collection has already been initialized with
     * an identical criteria, it returns the collection.
     * Otherwise if this MailboxRow is new, it will return
                 * an empty collection; or if this MailboxRow has previously
     * been saved, it will retrieve related MessageRows from storage.
     *
     * This method is protected by default in order to keep the public
     * api reasonable.  You can provide public methods for those you
     * actually need in MailboxRow.
     */
    protected List getMessageRowsJoinMailboxRow(Criteria criteria)
        throws TorqueException
    {
                    if (collMessageRows == null)
        {
            if (isNew())
            {
               collMessageRows = new ArrayList();
            }
            else
            {
                              criteria.add(MessageRowPeer.MAILBOX_ID, getMailboxId());
                              collMessageRows = MessageRowPeer.doSelectJoinMailboxRow(criteria);
            }
        }
        else
        {
            // the following code is to determine if a new query is
            // called for.  If the criteria is the same as the last
            // one, just return the collection.
                                    criteria.add(MessageRowPeer.MAILBOX_ID, getMailboxId());
                                    if (!lastMessageRowsCriteria.equals(criteria))
            {
                collMessageRows = MessageRowPeer.doSelectJoinMailboxRow(criteria);
            }
        }
        lastMessageRowsCriteria = criteria;

        return collMessageRows;
                }
                            


          
    private static List fieldNames = null;

    /**
     * Generate a list of field names.
     *
     * @return a list of field names
     */
    public static synchronized List getFieldNames()
    {
        if (fieldNames == null)
        {
            fieldNames = new ArrayList();
              fieldNames.add("MailboxId");
              fieldNames.add("Name");
              fieldNames.add("UidValidity");
              fieldNames.add("LastUid");
              fieldNames.add("MessageCount");
              fieldNames.add("Size");
              fieldNames = Collections.unmodifiableList(fieldNames);
        }
        return fieldNames;
    }

    /**
     * Retrieves a field from the object by name passed in as a String.
     *
     * @param name field name
     * @return value
     */
    public Object getByName(String name)
    {
          if (name.equals("MailboxId"))
        {
                return new Long(getMailboxId());
            }
          if (name.equals("Name"))
        {
                return getName();
            }
          if (name.equals("UidValidity"))
        {
                return new Long(getUidValidity());
            }
          if (name.equals("LastUid"))
        {
                return new Long(getLastUid());
            }
          if (name.equals("MessageCount"))
        {
                return new Integer(getMessageCount());
            }
          if (name.equals("Size"))
        {
                return new Long(getSize());
            }
          return null;
    }

    /**
     * Retrieves a field from the object by name passed in
     * as a String.  The String must be one of the static
     * Strings defined in this Class' Peer.
     *
     * @param name peer name
     * @return value
     */
    public Object getByPeerName(String name)
    {
          if (name.equals(MailboxRowPeer.MAILBOX_ID))
        {
                return new Long(getMailboxId());
            }
          if (name.equals(MailboxRowPeer.NAME))
        {
                return getName();
            }
          if (name.equals(MailboxRowPeer.UID_VALIDITY))
        {
                return new Long(getUidValidity());
            }
          if (name.equals(MailboxRowPeer.LAST_UID))
        {
                return new Long(getLastUid());
            }
          if (name.equals(MailboxRowPeer.MESSAGE_COUNT))
        {
                return new Integer(getMessageCount());
            }
          if (name.equals(MailboxRowPeer.SIZE))
        {
                return new Long(getSize());
            }
          return null;
    }

    /**
     * Retrieves a field from the object by Position as specified
     * in the xml schema.  Zero-based.
     *
     * @param pos position in xml schema
     * @return value
     */
    public Object getByPosition(int pos)
    {
            if (pos == 0)
        {
                return new Long(getMailboxId());
            }
              if (pos == 1)
        {
                return getName();
            }
              if (pos == 2)
        {
                return new Long(getUidValidity());
            }
              if (pos == 3)
        {
                return new Long(getLastUid());
            }
              if (pos == 4)
        {
                return new Integer(getMessageCount());
            }
              if (pos == 5)
        {
                return new Long(getSize());
            }
              return null;
    }
     
    /**
     * Stores the object in the database.  If the object is new,
     * it inserts it; otherwise an update is performed.
     *
     * @throws Exception
     */
    public void save() throws Exception
    {
          save(MailboxRowPeer.getMapBuilder()
                .getDatabaseMap().getName());
      }

    /**
     * Stores the object in the database.  If the object is new,
     * it inserts it; otherwise an update is performed.
       * Note: this code is here because the method body is
     * auto-generated conditionally and therefore needs to be
     * in this file instead of in the super class, BaseObject.
       *
     * @param dbName
     * @throws TorqueException
     */
    public void save(String dbName) throws TorqueException
    {
        Connection con = null;
          try
        {
            con = Transaction.begin(dbName);
            save(con);
            Transaction.commit(con);
        }
        catch(TorqueException e)
        {
            Transaction.safeRollback(con);
            throw e;
        }
      }

      /** flag to prevent endless save loop, if this object is referenced
        by another object which falls in this transaction. */
    private boolean alreadyInSave = false;
      /**
     * Stores the object in the database.  If the object is new,
     * it inserts it; otherwise an update is performed.  This method
     * is meant to be used as part of a transaction, otherwise use
     * the save() method and the connection details will be handled
     * internally
     *
     * @param con
     * @throws TorqueException
     */
    public void save(Connection con) throws TorqueException
    {
          if (!alreadyInSave)
        {
            alreadyInSave = true;


  
            // If this object has been modified, then save it to the database.
            if (isModified())
            {
                if (isNew())
                {
                    MailboxRowPeer.doInsert((MailboxRow) this, con);
                    setNew(false);
                }
                else
                {
                    MailboxRowPeer.doUpdate((MailboxRow) this, con);
                }
                }

                                      
                                    if (collMessageRows != null)
            {
                for (int i = 0; i < collMessageRows.size(); i++)
                {
                    ((MessageRow) collMessageRows.get(i)).save(con);
                }
            }
                                  alreadyInSave = false;
        }
      }

                        
      /**
     * Set the PrimaryKey using ObjectKey.
     *
     * @param key mailboxId ObjectKey
     */
    public void setPrimaryKey(ObjectKey key)
        throws TorqueException
    {
            setMailboxId(((NumberKey) key).longValue());
        }

    /**
     * Set the PrimaryKey using a String.
     *
     * @param key
     */
    public void setPrimaryKey(String key) throws TorqueException
    {
            setMailboxId(Long.parseLong(key));
        }

  
    /**
     * returns an id that differentiates this object from others
     * of its class.
     */
    public ObjectKey getPrimaryKey()
    {
          return SimpleKey.keyFor(getMailboxId());
      }
 

    /**
     * Makes a copy of this object.
     * It creates a new object filling in the simple attributes.
       * It then fills all the association collections and sets the
     * related objects to isNew=true.
       */
      public MailboxRow copy() throws TorqueException
    {
        return copyInto(new MailboxRow());
    }
  
    protected MailboxRow copyInto(MailboxRow copyObj) throws TorqueException
    {
          copyObj.setMailboxId(mailboxId);
          copyObj.setName(name);
          copyObj.setUidValidity(uidValidity);
          copyObj.setLastUid(lastUid);
          copyObj.setMessageCount(messageCount);
          copyObj.setSize(size);
  
                            copyObj.setMailboxId( 0);
                                          
                                      
                            
        List v = getMessageRows();
                            if (v != null)
        {
            for (int i = 0; i < v.size(); i++)
            {
                MessageRow obj = (MessageRow) v.get(i);
                copyObj.addMessageRow(obj.copy());
            }
        }
        else
        {
            copyObj.collMessageRows = null;
        }
                            return copyObj;
    }

    /**
     * returns a peer instance associated with this om.  Since Peer classes
     * are not to have any instance attributes, this method returns the
     * same instance for all member of this class. The method could therefore
     * be static, but this would prevent one from overriding the behavior.
     */
    public MailboxRowPeer getPeer()
    {
        return peer;
    }


    public String toString()
    {
        StringBuffer str = new StringBuffer();
        str.append("MailboxRow:\n");
        str.append("MailboxId = ")
               .append(getMailboxId())
             .append("\n");
        str.append("Name = ")
               .append(getName())
             .append("\n");
        str.append("UidValidity = ")
               .append(getUidValidity())
             .append("\n");
        str.append("LastUid = ")
               .append(getLastUid())
             .append("\n");
        str.append("MessageCount = ")
               .append(getMessageCount())
             .append("\n");
        str.append("Size = ")
               .append(getSize())
             .append("\n");
        return(str.toString());
    }
}
