package org.apache.james.mailrepository;

import java.io.*;
import com.workingdogs.town.*;
import org.apache.james.core.*;
import org.apache.avalon.blocks.*;
import org.apache.avalon.*;
import org.apache.avalon.utils.*;

public class AvalonMimeMessageInputStream extends JamesMimeMessageInputStream {
    //Define how to get to the data
    Store.StreamRepository sr = null;
    String key = null;

    public AvalonMimeMessageInputStream(Store.StreamRepository sr, String key) throws IOException {
        this.sr = sr;
        this.key = key;
    }

    public Store.StreamRepository getStreamStore() {
        return sr;
    }

    public String getKey() {
        return key;
    }

    protected synchronized InputStream openStream() throws IOException {
        return sr.retrieve(key);
    }

    public boolean equals(Object obj) {
        if (obj instanceof AvalonMimeMessageInputStream) {
            AvalonMimeMessageInputStream in = (AvalonMimeMessageInputStream)obj;
            return in.getStreamStore().equals(sr) && in.getKey().equals(key);
        }
        return false;
    }
}