
public class Spool implements Configurable {

    public void init(Configuration conf) {

            // save Configuration instance
        this.conf = conf;
        
        .....
    }
    
    public OutputStream store(Object key, InternetAddress sender, Vector recipients)
    
    public MessageContainer retrive(Object key)
    
    public MessageContainer accept()
    
    public Enumeration list()
    
    .....
}
