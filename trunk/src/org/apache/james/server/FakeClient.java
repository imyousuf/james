/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/


import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import org.apache.james.*;
import org.apache.james.util.*;

/**
 * <b>Run this class!!</b>
 * Pass it the name of a conf file.  This will be used to configure the server daemon, the spool, and
 * the mail servlets themselves.  Hope it works for you!
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class FakeClient {

    public static void main(String arg[]) {

        Object s = new FakeClient();
        System.out.println("original key: " + s);
        System.out.println("encoded: " + encode(s));
        System.out.println("decoded: " + decode(encode(s)));
    }

    private static  String encode(Object key) {
        byte[] b = key.toString().getBytes();
        char[] buf = new char[b.length * 2];

        for (int i = 0, j = 0, k; i < b.length; i++) {
            k = b[i];
            buf[j++] = hexDigits[(k >>> 4) & 0x0F];
            buf[j++] = hexDigits[k & 0x0F];
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append(buf);
        return buffer.toString();
    }

    private static String decode(String key) {
        
        byte[] b = new byte[key.length()];
        int t;
        for (int i = 0, j = 0; i < key.length(); j++) {
            b[j] = Byte.parseByte(key.substring(i, i + 2), 16);
            i +=2;
        }
        return new String(b);
    }
    
    private static final char[] hexDigits = {
        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };

    

/*        try {
            Socket s  = new Socket("localhost", 8181);
            PrintStream out = new PrintStream(s.getOutputStream());
            out.println("mail from:scoobie@maggie.galaxus");
            out.println("rcpt to:scoobie@maggie.galaxus");
            out.println("data");
            out.println("try");
            out.println();
            out.println(".");
            out.println();
    
            out.println("mail from:scoobie@maggie.galaxus");
            out.println("rcpt to:scoobie@maggie.galaxus");
            out.println("data");
            out.println("try");
            out.println();
            out.println(".");
            out.println();

            System.out.println("Sent");
        } catch (Exception e) {
            e.printStackTrace();
        }*/

}



/*--- formatting done in "Sun Java Convention" style on 07-11-1999 ---*/

