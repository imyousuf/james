public class POP3Handler implements ProtocolHandler, Configurable, Runnable {

    public void init(Configuration conf) {

            // get private configurations
        this.conf = conf;
        logger = conf.getChild("Logger").getValue();
        store = conf.getChil("Store").getValue();
        ....
    }
        
    parseRequest(Socket socket) {
    
        this.socket = socket;
    }
    
    public void run() {
    
        do the actual handling work.

    }
    
    .....
}
