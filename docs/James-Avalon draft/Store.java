

public class Store implements Block, Store {

    public void initBlock (BlockManager manager) {

            // save BlockManager instance
        this.manager = manager;

            // get private configurations
        this.conf = manager.getConfiguration();
        //....
    }
        
    public void store(Object key, Object data)
    
    public void retrive(Object key)
    
    //.....
}

<