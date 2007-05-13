package org.apache.james.mailboxmanager.impl;

import org.apache.james.mailboxmanager.Namespace;
import org.apache.james.mailboxmanager.Namespaces;

public class NamespacesImpl implements Namespaces {
    
    private Namespace[] personal;
    
    private Namespace[] shared;
    
    private Namespace[] user;
    
    private Namespace personalDefault;
    
    public Namespace[] getPersonal() {
        return personal;
    }

    public Namespace getPersonalDefault() {
        return personalDefault;
    }

    public Namespace[] getShared() {
        return shared;
    }

    public Namespace[] getUser() {
        return user;
    }

    public void setPersonal(Namespace[] personal) {
        this.personal = personal;
    }

    public void setPersonalDefault(Namespace personalDefault) {
        this.personalDefault = personalDefault;
    }

    public void setShared(Namespace[] shared) {
        this.shared = shared;
    }

    public void setUser(Namespace[] user) {
        this.user = user;
    }
    
    

}
