package org.apache.james.mailboxmanager.impl;

import org.apache.james.mailboxmanager.Namespace;

public class NamespaceImpl implements Namespace {
    
    private String delimiter;
    private String name;
    
    public NamespaceImpl(String delimiter,String name) {
        this.delimiter=delimiter;
        this.name=name;
    }

    public String getHierarchyDelimter() {
        return delimiter;
    }

    public String getName() {
        return name;
    }

}
