
public class POP3Server implements Block, ProtocolHandler {

    public void initBlock (BlockManager manager) {

            // save BlockManager instance
        this.manager = manager;

            // get private configurations
        this.conf = manager.getConfiguration();

            // get from manager a Logger implementation
        logger = manager.getBlock("org.apache.avalon.blocks.Logger");

            // get from manager a Store implementation. Here is where POP3 retrive 
            // mails.
        store = manager.getBlock("org.apache.avanon.blocks.Store");
        
            // add the instance the logger in the POP3Handler configuration node.
        conf.getChild("POP3Handler").addNode("Logger", logger);

            // add the instance the store in the POP3Handler configuration node.
        conf.getChild("POP3Handler").addNode("Store", store);

            // open a new RunnablePool wich is a generic pool or Runnable class. 
            // this is where I think Matthew class for reclycling should go
        pop3handlerpool = manager.getRunnablePool("org.apache.james.server.POP3Handler", conf.getChild("POP3Handler"));
    }
    
        // this is called by the SocketListener whenever a connection is open.
    parseRequest(Socket socket) {

            // get an POP3Handler from the pool
        pop3handler =  pop3handlerpool.get();
            // fill it with socket opened.
        pop3handler.parseRequest(socket);
            // start the handler.
        manager.execute(pop3handler);
    }
    
    .....
}
