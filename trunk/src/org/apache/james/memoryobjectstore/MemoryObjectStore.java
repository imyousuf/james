/**
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */

package org.apache.james.memoryobjectstore;

import java.util.*;
import org.apache.james.*;
import org.apache.james.util.*;
import org.apache.james.server.*;

public class MemoryObjectStore implements ObjectStore {

    private Hashtable storage = new Hashtable();

    public MemoryObjectStore() {
    }
    
    public void init(JamesServ serv, Properties props) {
    }

    public void store(Object key, Object data) {

        storage.put(key, data);
    }

    public Object get(Object key) {

        return storage.get(key);
    }

    public void remove(Object key) {

        storage.remove(key);
    }

    public boolean isEmpty() {

        return storage.isEmpty();
    }

    public boolean containsKey(Object key) {
        
        return storage.containsKey(key);
    }

    public Enumeration list() {
        
        return storage.keys();
    }
}