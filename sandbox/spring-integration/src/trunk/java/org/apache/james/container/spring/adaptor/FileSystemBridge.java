package org.apache.james.container.spring.adaptor;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.james.services.FileSystem;

public class FileSystemBridge implements FileSystem {

	public File getBasedir() throws FileNotFoundException {
		return new File(".");
	}

    public File getFile(String fileURL) throws FileNotFoundException {
        if (fileURL.startsWith("file://")) {
            if (fileURL.startsWith("file://conf/")) {
                return new File("./src/trunk/config/"+fileURL.substring(12));
            } else {
            	return new File("./"+fileURL.substring(7));
            }
        } else {
            throw new UnsupportedOperationException("getFile: "+fileURL);
        }
    }

}
