
public class MessageSpoolManager implements Configurable, Runnable{

    public void init(Configuration conf) {

            // save Configuration instance
        this.conf = conf;
        logger = conf.getChild("Logger").getValue();
        store = conf.getChil("Store").getValue();
        .....
    }
    
    public void run() {

        MessageContainer message = mailBuffer.accept();
        
        while (message need processing) {
            message = process(message)
        }

        if (message is to remote delivery)
            dispacher.send(message);
        else if (message is to local delivery)
            store.add(message)
        }
    }

    .....
}
