package org.apache.james.core;

import java.io.IOException;
import java.io.InputStream;

public abstract class MimeMessageSource {
	/**
	 * Return an input stream to the data
	 */
    public abstract InputStream getInputStream() throws IOException;

    /**
     * Return the size of all the data.
     * Default implementation... others can override to do this much faster
     */
    public long getSize() throws IOException {
		int size = 0;
		InputStream in = getInputStream();
		int read = 0;
		byte[] data = new byte[1024];
		while ((read = in.read(data)) > 0) {
			size += read;
		}
        //System.err.println("size of " + this + " is " + size);
		return size;
	}
}
