public class SMTPServer implements Block, ProtocolHandler {

    public void initBlock (BlockManager manager) {

            // save BlockManager instance
        this.manager = manager;
        
            // get private configurations
        this.conf = manager.getConfiguration();
        
            // get from manager a Logger implementation
        logger = manager.getBlock("org.apache.avalon.blocks.Logger");

            // get from manager a Store implementation. Here is where mail ready
            // to delivery goes (after been processed).
        store = manager.getBlock("org.apache.avanon.blocks.Store");

            // create a new buffer for arrived mail. Here is where protocol 
            // handlers store arriving mail before processing.
        spool = new Spool()
        spool.init(conf.getChild("spool"));

            // add the instance the logger in the SMTPHandler configuration node.
        conf.getChild("SMTPHandler").addNode("Logger", logger);

            // add the instance the store in the SMTPHandler configuration node.
        conf.getChild("SMTPHandler").addNode("Store", store);

            // add the instance the mailBuffer in the SMTPHandler configuration node.
        conf.getChild("SMTPHandler").addNode("Spool", mailBuffer);

            // open a new RunnablePool wich is a generic pool or Runnable class. 
            // this is where I think Matthew class for reclycling should go
        smtphandlerpool = manager.getRunnablePool("org.apache.james.server.SMTPHandler", conf.getChild("SMTPHandler"));

            // add the instance the logger in the messagespoolmanager configuration node.
        conf.getChild("messagespoolmanager").addNode("Logger", logger);

            // add the instance the store in the messagespoolmanager configuration node.
        conf.getChild("messagespoolmanager").addNode("Store", store);

            // add the instance the mailBuffer in the messagespoolmanager configuration node.
        conf.getChild("messagespoolmanager").addNode("Spool", mailBuffer);

            // create as many MessageSpoolManager as specified in conf.
            // These will grab mail from the buffer, process them and drop them in the store
        messageSpoolManager = new MessageSpoolManager();
        messageSpoolManager.init(conf.getChild("messagespoolmanager"));
        for (int i = conf.getChild("messagespoolmanagernumber").getValueAsInt(); i > 0; i--) {
            manager.execute(messageSpoolManager)
        }
    }
    
        // this is called by the SocketListener whenever a connection is open.
    parseRequest(Socket socket) {

            // get an SMTPHandler from the pool
        smtphandler =  smtphandlerpool.get();
            // fill it with socket opened.
        smtphandler.parseRequest(socket);
            // start the handler.
        manager.execute(smtphandler);   // or maybe 
                                        //smtphandler.start();
    }
    
    .....
}
