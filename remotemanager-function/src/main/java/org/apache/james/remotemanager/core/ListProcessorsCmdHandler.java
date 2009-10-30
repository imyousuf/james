package org.apache.james.remotemanager.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import org.apache.james.management.ProcessorManagementService;
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;

public class ListProcessorsCmdHandler implements CommandHandler{
    
    private final static String COMMAND_NAME = "LISTPROCESSORS";
    private CommandHelp help = new CommandHelp("listprocessors [processorname]","list names of all processors");

    protected ProcessorManagementService processorManagementService;

       
    /**
     * Set the ProcessorManagementService
     * 
     * @param processorManagement the ProcessorManagementService
     */
    @Resource(name="org.apache.james.management.ProcessorManagementService")
    public final void setProcessorManagement(ProcessorManagementService processorManagement) {
        this.processorManagementService = processorManagement;
    }
    
    /**
     * @see org.apache.james.remotemanager.CommandHandler#getHelp()
     */
    public CommandHelp getHelp() {
        return help;
    }

    /**
     * @see org.apache.james.remotemanager.CommandHandler#onCommand(org.apache.james.remotemanager.RemoteManagerSession, java.lang.String, java.lang.String)
     */
    public RemoteManagerResponse onCommand(RemoteManagerSession session, String command, String params) {
        RemoteManagerResponse response = null;
        String[] processorNames = processorManagementService.getProcessorNames();
        response = new RemoteManagerResponse("Existing processors: " + processorNames.length);
        for (int i = 0; i < processorNames.length; i++) {
            response.appendLine("\t" + processorNames[i]);
         }
        return response;
    }
    
    /**
     * @see org.apache.james.api.protocol.CommonCommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

}
