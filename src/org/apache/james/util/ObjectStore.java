/**
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */

package org.apache.james.util;

import java.util.*;
import org.apache.james.*;
import org.apache.james.server.*;

public interface ObjectStore extends Configurable {

    public Object get(Object key);

    public void store(Object key, Object value);

    public void remove(Object key);

    public boolean containsKey(Object key);

    public Enumeration list();
}
