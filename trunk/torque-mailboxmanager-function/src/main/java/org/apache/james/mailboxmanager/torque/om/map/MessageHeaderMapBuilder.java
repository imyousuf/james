package org.apache.james.mailboxmanager.torque.om.map;

import java.util.Date;
import java.math.BigDecimal;

import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.map.MapBuilder;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.map.TableMap;
import org.apache.torque.map.ColumnMap;
import org.apache.torque.map.InheritanceMap;

/**
  *  This class was autogenerated by Torque on:
  *
  * [Sun Dec 09 17:45:09 GMT 2007]
  *
  */
public class MessageHeaderMapBuilder implements MapBuilder
{
    /**
     * The name of this class
     */
    public static final String CLASS_NAME =
        "org.apache.james.mailboxmanager.torque.om.map.MessageHeaderMapBuilder";

    /**
     * The database map.
     */
    private DatabaseMap dbMap = null;

    /**
     * Tells us if this DatabaseMapBuilder is built so that we
     * don't have to re-build it every time.
     *
     * @return true if this DatabaseMapBuilder is built
     */
    public boolean isBuilt()
    {
        return (dbMap != null);
    }

    /**
     * Gets the databasemap this map builder built.
     *
     * @return the databasemap
     */
    public DatabaseMap getDatabaseMap()
    {
        return this.dbMap;
    }

    /**
     * The doBuild() method builds the DatabaseMap
     *
     * @throws TorqueException
     */
    public synchronized void doBuild() throws TorqueException
    {
        if ( isBuilt() ) {
            return;
        }
        dbMap = Torque.getDatabaseMap("mailboxmanager");

        dbMap.addTable("message_header");
        TableMap tMap = dbMap.getTable("message_header");
        tMap.setJavaName("MessageHeader");
        tMap.setOMClass( org.apache.james.mailboxmanager.torque.om.MessageHeader.class );
        tMap.setPeerClass( org.apache.james.mailboxmanager.torque.om.MessageHeaderPeer.class );
        tMap.setPrimaryKeyMethod("none");

        ColumnMap cMap = null;


    // ------------- Column: mailbox_id --------------------
        cMap = new ColumnMap( "mailbox_id", tMap);
        cMap.setType( new Long(0) );
        cMap.setTorqueType( "BIGINT" );
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(true);
        cMap.setNotNull(true);
        cMap.setJavaName( "MailboxId" );
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
            cMap.setDescription("Mailbox Id");
              cMap.setInheritance("false");
                  cMap.setForeignKey("message", "mailbox_id");
            cMap.setPosition(1);
          tMap.addColumn(cMap);
    // ------------- Column: uid --------------------
        cMap = new ColumnMap( "uid", tMap);
        cMap.setType( new Long(0) );
        cMap.setTorqueType( "BIGINT" );
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(true);
        cMap.setNotNull(true);
        cMap.setJavaName( "Uid" );
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
            cMap.setDescription("");
              cMap.setInheritance("false");
                  cMap.setForeignKey("message", "uid");
            cMap.setPosition(2);
          tMap.addColumn(cMap);
    // ------------- Column: line_number --------------------
        cMap = new ColumnMap( "line_number", tMap);
        cMap.setType( new Integer(0) );
        cMap.setTorqueType( "INTEGER" );
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(true);
        cMap.setNotNull(true);
        cMap.setJavaName( "LineNumber" );
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
            cMap.setDescription("");
              cMap.setInheritance("false");
                    cMap.setPosition(3);
          tMap.addColumn(cMap);
    // ------------- Column: field --------------------
        cMap = new ColumnMap( "field", tMap);
        cMap.setType( "" );
        cMap.setTorqueType( "VARCHAR" );
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(false);
        cMap.setNotNull(true);
        cMap.setJavaName( "Field" );
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
            cMap.setDescription("field");
              cMap.setInheritance("false");
                cMap.setSize( 256 );
                  cMap.setPosition(4);
          tMap.addColumn(cMap);
    // ------------- Column: value --------------------
        cMap = new ColumnMap( "value", tMap);
        cMap.setType( "" );
        cMap.setTorqueType( "VARCHAR" );
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(false);
        cMap.setNotNull(true);
        cMap.setJavaName( "Value" );
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
            cMap.setDescription("value");
              cMap.setInheritance("false");
                cMap.setSize( 1024 );
                  cMap.setPosition(5);
          tMap.addColumn(cMap);
        tMap.setUseInheritance(false);
    }
}
