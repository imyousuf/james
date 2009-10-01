package org.apache.james.socket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;

public abstract class AbstractCommandDispatcher<CommandHandler extends CommonCommandHandler> implements ExtensibleHandler {
    /**
     * The list of available command handlers
     */
    private HashMap<String, List<CommandHandler>> commandHandlerMap = new HashMap<String, List<CommandHandler>>();

    /**
     * Add it to map (key as command name, value is an array list of CommandHandlers)
     *
     * @param commandName the command name which will be key
     * @param cmdHandler The CommandHandler object
     */
    protected void addToMap(String commandName, CommandHandler cmdHandler) {
        List<CommandHandler> handlers = commandHandlerMap.get(commandName);
        if(handlers == null) {
            handlers = new ArrayList<CommandHandler>();
            commandHandlerMap.put(commandName, handlers);
        }
        handlers.add(cmdHandler);
    }


    /**
     * Returns all the configured CommandHandlers for the specified command
     *
     * @param command the command name which will be key
     * @param session not null
     * @return List of CommandHandlers
     */
    protected List<CommandHandler> getCommandHandlers(String command, TLSSupportedSession session) {
        if (command == null) {
            return null;
        }
        if (session.getLogger().isDebugEnabled()) {
            session.getLogger().debug("Lookup command handler for command: " + command);
        }
        List<CommandHandler> handlers =  commandHandlerMap.get(command);
        if(handlers == null) {
            handlers = commandHandlerMap.get(getUnknownCommandHandlerIdentifier());
        }

        return handlers;
    }

    /**
     * @throws WiringException 
     * @see org.apache.james.socket.ExtensibleHandler#wireExtensions(java.lang.Class, java.util.List)
     */
    @SuppressWarnings("unchecked")
    public void wireExtensions(Class interfaceName, List extension) throws WiringException {
        this.commandHandlerMap = new HashMap<String, List<CommandHandler>>();

        for (Iterator it = extension.iterator(); it.hasNext(); ) {
            CommandHandler handler = (CommandHandler) it.next();
            Collection implCmds = handler.getImplCommands();
    
            for (Iterator i = implCmds.iterator(); i.hasNext(); ) {
                String commandName = ((String) i.next()).trim().toUpperCase(Locale.US);
                /*
                if (getLog().isInfoEnabled()) {
                    getLog().info(
                            "Added Commandhandler: " + handler.getClass() + " for command "+commandName);
                }
                */
                addToMap(commandName, (CommandHandler) handler);
            }
        }

        addToMap(getUnknownCommandHandlerIdentifier(), getUnknownCommandHandler());

        if (commandHandlerMap.size() < 2) {
            if (getLog().isErrorEnabled()) {
                getLog().error("No commandhandlers configured");
            }
            throw new WiringException("No commandhandlers configured");
        } else {
            boolean found = true;
            List<String> mandatoryCommands = getMandatoryCommands();
            for (int i = 0; i < mandatoryCommands.size(); i++) {
                if (!commandHandlerMap.containsKey(mandatoryCommands.get(i))) {
                    if (getLog().isErrorEnabled()) {
                        getLog().error(
                                "No commandhandlers configured for the command:"
                                        + mandatoryCommands.get(i));
                    }
                    found = false;
                    break;
                }
            }

            if (!found) {
                throw new WiringException(
                        "No commandhandlers configured for mandatory commands");
            }


        }

    }
    protected abstract Log getLog();
    protected abstract List<String> getMandatoryCommands();
    protected abstract String getUnknownCommandHandlerIdentifier();
    protected abstract CommandHandler getUnknownCommandHandler();
}
