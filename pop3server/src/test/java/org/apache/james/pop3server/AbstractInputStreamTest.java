package org.apache.james.pop3server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import junit.framework.TestCase;


public abstract class AbstractInputStreamTest extends TestCase{

    protected void checkRead(InputStream in, String expected) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int i = -1;
        while ((i = in.read()) != -1) {
            out.write(i);
        }
        in.close();
        out.close();        
        assertEquals(expected, new String(out.toByteArray()));

        
        
    }
    
    protected void checkReadViaArray(InputStream in, String expected) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] buf = new byte[3];
        int i = 0;
        while ((i = in.read(buf)) != -1) {
            out.write(buf, 0, i);
        }
        
       
        in.close();
        out.close();
        assertEquals(expected, new String(out.toByteArray()));
        
        
    }
}
