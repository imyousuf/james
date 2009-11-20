package org.apache.james.transport;

import javax.mail.MessagingException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.util.ConfigurationAdapter;
import org.apache.mailet.Mailet;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Names;

public class AvalonJamesMailetLoader extends AbstractAvalonJamesLoader implements MailetLoader{

    

    private MailetLoader loader;
    private ConfigurationAdapter config;


    public void initialize() throws Exception {
        loader = Guice.createInjector(new Jsr250Module(), new Module()).getInstance(JamesMailetLoader.class);
    }
 
    public Mailet getMailet(String mailetName, Configuration configuration) throws MessagingException {
        return loader.getMailet(mailetName, configuration);
    }
}
