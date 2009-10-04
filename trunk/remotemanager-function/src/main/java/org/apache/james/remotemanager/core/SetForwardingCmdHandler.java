package org.apache.james.remotemanager.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import javax.mail.internet.ParseException;

import org.apache.james.api.user.JamesUser;
import org.apache.james.api.user.User;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.user.UsersStore;
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;
import org.apache.mailet.MailAddress;

/**
 * Handler called upon receipt of an SETFORWARDING command
 *
 */
public class SetForwardingCmdHandler implements CommandHandler{

    private final static String COMMAND_NAME = "SETFORWARDING";
    private CommandHelp help = new CommandHelp("setforwarding [username] [emailaddress]","forwards a user's email to another email address");

    private UsersStore uStore;

    /**
     * Sets the users store.
     * 
     * @param users
     *            the users to set
     */
    @Resource(name = "users-store")
    public final void setUsers(UsersStore uStore) {
        this.uStore = uStore;
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
    public RemoteManagerResponse onCommand(RemoteManagerSession session, String command, String parameters) {
        RemoteManagerResponse response;
        int breakIndex = -1;
        if ((parameters == null) ||
            (parameters.equals("")) ||
            ((breakIndex = parameters.indexOf(" ")) < 0)) {
            response = new RemoteManagerResponse("Usage: " + help.getSyntax());
            return response;
        }
        String username = parameters.substring(0,breakIndex);
        String forward = parameters.substring(breakIndex + 1);
        if (username.equals("") || forward.equals("")) {
            response = new RemoteManagerResponse("Usage: " + help.getSyntax());
            return response;
        }
        UsersRepository users = uStore.getRepository((String) session.getState().get(RemoteManagerSession.CURRENT_USERREPOSITORY));

        // Verify user exists
        User baseuser = users.getUserByName(username);
        if (baseuser == null) {
            response = new RemoteManagerResponse("No such user " + username);
            return response;
        } else if (! (baseuser instanceof JamesUser ) ) {
            response = new RemoteManagerResponse("Can't set forwarding for this user type.");
            return response;
        }
        JamesUser user = (JamesUser)baseuser;
        // Verify acceptable email address
        MailAddress forwardAddr;
        try {
             forwardAddr = new MailAddress(forward);
        } catch(ParseException pe) {
            session.getLogger().error("Parse exception with that email address: ", pe);
            response = new RemoteManagerResponse("Forwarding address not added for " + username);
            return response;
        }

        boolean success = user.setForwardingDestination(forwardAddr);
        if (success) {
            user.setForwarding(true);
            users.updateUser(user);
            StringBuilder responseBuffer =
                new StringBuilder(64)
                        .append("Forwarding destination for ")
                        .append(username)
                        .append(" set to:")
                        .append(forwardAddr.toString());
            String responseString = responseBuffer.toString();
            response = new RemoteManagerResponse(responseString);
            session.getLogger().info(responseString);
        } else {
            response = new RemoteManagerResponse("Error setting forwarding");
            session.getLogger().error("Error setting forwarding");
        }
        return response;
    }


    /**
     * @see org.apache.james.socket.CommonCommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

}
