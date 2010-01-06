package org.apache.james.remotemanager.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;

/**
 * Handler called upon receipt of an MEMSTAT command.
 * 
 */
public class MemStatCmdHandler implements CommandHandler {
    private CommandHelp help = new CommandHelp("memstat ([-gc])","shows memory usage. When called with -gc the garbage collector get called");

    public final static String COMMAND_NAME = "MEMSTAT";

    /**
     * @see org.apache.james.remotemanager.CommandHandler#onCommand(org.apache.james.remotemanager.RemoteManagerSession,
     *      java.lang.String, java.lang.String)
     */
    public RemoteManagerResponse onCommand(RemoteManagerSession session, String command, String parameters) {
        RemoteManagerResponse response = new RemoteManagerResponse("Current memory statistics:");
        response.appendLine("\tFree Memory: " + Runtime.getRuntime().freeMemory());
        response.appendLine("\tTotal Memory: " + Runtime.getRuntime().totalMemory());
        response.appendLine("\tMax Memory: " + Runtime.getRuntime().maxMemory());

        if ("-gc".equalsIgnoreCase(parameters)) {
            System.gc();
            response.appendLine("And after System.gc():");
            response.appendLine("\tFree Memory: " + Runtime.getRuntime().freeMemory());
            response.appendLine("\tTotal Memory: " + Runtime.getRuntime().totalMemory());
            response.appendLine("\tMax Memory: " + Runtime.getRuntime().maxMemory());
        }

        return response;
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.protocol.CommonCommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

    /**
     * @see org.apache.james.remotemanager.CommandHandler#getHelp()
     */
    public CommandHelp getHelp() {
        return help;
    }

}
