package org.apache.james.imapserver.processor.imap4rev1;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import org.apache.james.imap.message.response.imap4rev1.FetchResponse.BodyElement;

/**
 * Wraps full content to implement a partial fetch.
 */
final class PartialFetchBodyElement implements BodyElement {

    private final BodyElement delegate;
    private final long firstOctet;
    private final long numberOfOctets;
    
    public PartialFetchBodyElement(final BodyElement delegate, final long firstOctet, 
            final long numberOfOctets) {
        super();
        this.delegate = delegate;
        this.firstOctet = firstOctet;
        this.numberOfOctets = numberOfOctets;
    }

    public String getName() {
        return delegate.getName();
    }

    public long size() {
        final long size = delegate.size();
        final long numberOfOctets;
        if (size > this.numberOfOctets) {
            numberOfOctets = this.numberOfOctets;
        } else {
            numberOfOctets = size;
        }
        final long result = numberOfOctets - firstOctet;
        return result;
    }

    public void writeTo(WritableByteChannel channel) throws IOException {
        PartialWritableByteChannel partialChannel = 
            new PartialWritableByteChannel(channel, firstOctet, size());
        delegate.writeTo(partialChannel);
    }
    
}