package org.apache.james.remotemanager.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersStore;

/**
 * Handler called upon receipt of a USER command
 */
public class UserCmdHandler implements CommandHandler{
    private final static String COMMAND_NAME = "USER";
    private CommandHelp help = new CommandHelp("user [repositoryname]", "change to another user repository");

    private UsersStore uStore;

    /**
     * Sets the users store.
     * @param users the users to set
     */
    @Resource(name="usersstore")
    public final void setUsers(UsersStore uStore) {
        this.uStore = uStore;
    }
    
    /**
     * @see org.apache.james.remotemanager.CommandHandler#getHelp()
     */
    public CommandHelp getHelp() {
        return help;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.CommandHandler#onCommand(org.apache.james.protocols.api.ProtocolSession, org.apache.james.protocols.api.Request)
     */
    public Response onCommand(RemoteManagerSession session, Request request) {
        RemoteManagerResponse response;
        String parameters = request.getArgument();
        
        if (parameters == null || parameters.equals("")) {
            response = new RemoteManagerResponse("Usage: " + help.getSyntax());
            return response;
        }
        String repositoryName = parameters.toLowerCase(Locale.US);
        UsersRepository repos = uStore.getRepository(repositoryName);
        if ( repos == null ) {
            response = new RemoteManagerResponse("No such repository: " + repositoryName);
        } else {
            session.getState().put(RemoteManagerSession.CURRENT_USERREPOSITORY,repos);
            StringBuilder responseBuffer =
                new StringBuilder(64)
                        .append("Changed to repository '")
                        .append(repositoryName)
                        .append("'.");
            response = new RemoteManagerResponse(responseBuffer.toString());
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
