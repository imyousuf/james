public class SMTPHandler implements ProtocolHandler, Configurable, Runnable {

    public void init(Configuration conf) {

            // get private configurations
        this.conf = conf;
        logger = conf.getChild("Logger").getValue();
        spool = conf.getChil("Spool").getValue();
        ....
    }
        
    parseRequest(Socket socket) {
    
        this.socket = socket;
    }
    
    public void run() {
    
        // do the actual handling work.

    }
    
    .....
}
