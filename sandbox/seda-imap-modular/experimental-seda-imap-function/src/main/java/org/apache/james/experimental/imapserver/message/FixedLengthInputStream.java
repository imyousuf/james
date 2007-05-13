package org.apache.james.experimental.imapserver.message;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
     * This class is not yet used in the AppendCommand.
     *
     * An input stream which reads a fixed number of bytes from the underlying
     * input stream. Once the number of bytes has been read, the FixedLengthInputStream
     * will act as thought the end of stream has been reached, even if more bytes are
     * present in the underlying input stream.
     */
    class FixedLengthInputStream extends FilterInputStream
    {
        private long pos = 0;
        private long length;

        public FixedLengthInputStream( InputStream in, long length )
        {
            super( in );
            this.length = length;
        }

        public int read() throws IOException
        {
            if ( pos >= length ) {
                return -1;
            }
            pos++;
            return super.read();
         }

        public int read( byte b[] ) throws IOException
        {
            if ( pos >= length ) {
                return -1;
            }

            if ( pos + b.length >= length ) {
                pos = length;
                return super.read( b, 0, (int) (length - pos) );
            }

            pos += b.length;
            return super.read( b );
        }

        public int read( byte b[], int off, int len ) throws IOException
        {
            throw new IOException("Not implemented");
//            return super.read( b, off, len );
        }

        public long skip( long n ) throws IOException
        {
            throw new IOException( "Not implemented" );
//            return super.skip( n );
        }

        public int available() throws IOException
        {
            return super.available();
        }

        public void close() throws IOException
        {
            // Don't do anything to the underlying stream.
        }

        public synchronized void mark( int readlimit )
        {
            // Don't do anything.
        }

        public synchronized void reset() throws IOException
        {
            throw new IOException( "mark not supported" );
        }

        public boolean markSupported()
        {
            return false;
        }
    }
