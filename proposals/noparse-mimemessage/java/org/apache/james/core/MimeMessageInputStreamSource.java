package org.apache.james.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MimeMessageInputStreamSource extends MimeMessageSource {

    String key = null;
	InputStream in = null;
    File file = null;

    //If you try to access this size first, it will load it into a temp file
    //  and work from there.

	public MimeMessageInputStreamSource(String key, InputStream in) {
        this.key = key;
		this.in = in;
	}

	/**
	 * Return an input stream to the data
	 */
    public synchronized InputStream getInputStream() throws IOException {
        if (file == null) {
            return in;
        } else {
            return new FileInputStream(file);
        }
	}

    /**
     * If not already, read the stream into a temp file
     */
    public synchronized long getSize() throws IOException {
        if (file == null) {
            //Create a temp file and channel the input stream into it
            file = File.createTempFile(key, ".m64");
            FileOutputStream fout = new FileOutputStream(file);
            int b = -1;
            while ((b = in.read()) != -1) {
                fout.write(b);
            }
            fout.close();
            in.close();
            file.deleteOnExit();
        }
        return file.length();
    }
}
