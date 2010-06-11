package org.apache.james.api.protocol;

import java.util.LinkedList;

public interface ProtocolHandlerChain {

    /**
     * Returns a list of handler of the requested type.
     * @param <T>
     * 
     * @param type the type of handler we're interested in
     * @return a List of handlers
     */
    public abstract <T> LinkedList<T> getHandlers(Class<T> type);

}