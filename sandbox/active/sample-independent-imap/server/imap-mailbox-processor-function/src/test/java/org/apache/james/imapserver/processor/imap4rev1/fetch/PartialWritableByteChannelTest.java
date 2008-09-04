/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.imapserver.processor.imap4rev1.fetch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import junit.framework.TestCase;

public class PartialWritableByteChannelTest extends TestCase implements WritableByteChannel  {

    private static final int CAPACITY = 2048;
    
    ByteBuffer sink;
    
    protected void setUp() throws Exception {
        super.setUp();
        sink = ByteBuffer.allocate(CAPACITY);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testShouldPassFullBufferWhenStartZeroSizeLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 0, 10);
        assertEquals(10, channel.write(src));
        assertEquals(10, sink.position());
        sink.flip();
        for (int i=0;i<10;i++) {
            assertEquals(i, sink.get());
        }
    }
    
    public void testShouldIgnoreBytesAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 0, 4);
        assertEquals(10, channel.write(src));
        assertEquals(4, sink.position());
        sink.flip();
        for (int i=0;i<4;i++) {
            assertEquals(i, sink.get());
        }
    }
    
    public void testShouldIgnoreBytesBeforeStart() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 4, 6);
        assertEquals(10, channel.write(src));
        assertEquals(6, sink.position());
        sink.flip();
        for (int i=4;i<10;i++) {
            assertEquals(i, sink.get());
        }
    }
    
    public void testShouldIgnoreBytesBeforeStartAndAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 4, 2);
        assertEquals(10, channel.write(src));
        assertEquals(2, sink.position());
        sink.flip();
        for (int i=4;i<6;i++) {
            assertEquals(i, sink.get());
        }
    }

    public void testMultiBufferShouldPassFullBufferWhenStartZeroSizeLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 0, 50);
        for (int i=0;i<5;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(50, sink.position());
        sink.flip();
        for (int l=0;l<5;l++) {
            for (int i=0;i<10;i++) {
                assertEquals(i, sink.get());
            }
        }
    }
    
    public void testMultiBufferOnBoundaryShouldIgnoreBytesAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 0, 30);
        for (int i=0;i<5;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(30, sink.position());
        sink.flip();
        for (int l=0;l<3;l++) {
            for (int i=0;i<10;i++) {
                assertEquals(i, sink.get());
            }
        }
    }
    
    public void testMultiBufferBeforeBoundaryShouldIgnoreBytesAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 0, 39);
        for (int i=0;i<5;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(39, sink.position());
        sink.flip();
        for (int l=0;l<3;l++) {
            for (int i=0;i<10;i++) {
                assertEquals(i, sink.get());
            }
        }
        for (int i=0;i<9;i++) {
            assertEquals(i, sink.get());
        }
    }
    
    public void testMultiBufferAfterBoundaryShouldIgnoreBytesAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 0, 31);
        for (int i=0;i<5;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(31, sink.position());
        sink.flip();
        for (int l=0;l<3;l++) {
            for (int i=0;i<10;i++) {
                assertEquals(i, sink.get());
            }
        }
        assertEquals(0, sink.get());
    }
    
    public void testMultiBufferOnBoundaryOctetsOverBufferShouldIgnoreBytesBeforeStart() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 20, 21);
        for (int i=0;i<5;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(21, sink.position());
        sink.flip();
        for (int l=0;l<2;l++) {
            for (int i=0;i<10;i++) {
                assertEquals(i, sink.get());
            }
        }
        assertEquals(0, sink.get());
    }
    
    public void testMultiBufferAfterBoundaryOctetsOverBufferShouldIgnoreBytesBeforeStart() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 21, 21);
        for (int i=0;i<5;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(21, sink.position());
        sink.flip();
        for (int l=0;l<2;l++) {
            for (int i=1;i<11;i++) {
                assertEquals(i % 10, sink.get());
            }
        }
        assertEquals(1, sink.get());
    }
    
    public void testMultiBufferBeforeBoundaryOctetsOverBufferShouldIgnoreBytesBeforeStart() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 19, 21);
        for (int i=0;i<5;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(21, sink.position());
        sink.flip();
        for (int l=0;l<2;l++) {
            for (int i=9;i<19;i++) {
                assertEquals(i % 10, sink.get());
            }
        }
        assertEquals(9, sink.get());
    }
    
    public void testMultiBufferOnBoundaryOctetsOnBufferShouldIgnoreBytesBeforeStart() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 20, 20);
        for (int i=0;i<5;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(20, sink.position());
        sink.flip();
        for (int l=0;l<2;l++) {
            for (int i=0;i<10;i++) {
                assertEquals(i, sink.get());
            }
        }
    }
    
    public void testMultiBufferAfterBoundaryOctetsObBufferShouldIgnoreBytesBeforeStart() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 21, 20);
        for (int i=0;i<5;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(20, sink.position());
        sink.flip();
        for (int l=0;l<2;l++) {
            for (int i=1;i<11;i++) {
                assertEquals(i % 10, sink.get());
            }
        }
    }
    
    public void testMultiBufferBeforeBoundaryOctetsOnBufferShouldIgnoreBytesBeforeStart() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 19, 20);
        for (int i=0;i<5;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(20, sink.position());
        sink.flip();
        for (int l=0;l<2;l++) {
            for (int i=9;i<19;i++) {
                assertEquals(i % 10, sink.get());
            }
        }
    }
    
    public void testMultiBufferOnBoundaryOctetsUnderBufferShouldIgnoreBytesBeforeStart() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 20, 19);
        for (int i=0;i<5;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(19, sink.position());
        sink.flip();
        for (int l=0;l<2;l++) {
            for (int i=0;i<10;i++) {
                if (sink.hasRemaining()) {
                    assertEquals(i, sink.get());
                }
            }
        }
    }
    
    public void testMultiBufferAfterBoundaryOctetsUnderBufferShouldIgnoreBytesBeforeStart() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 21, 19);
        for (int i=0;i<5;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(19, sink.position());
        sink.flip();
        for (int l=0;l<2;l++) {
            for (int i=1;i<11;i++) {
                if (sink.hasRemaining()) {
                    assertEquals(i % 10, sink.get());
                }
            }
        }
    }
    
    public void testMultiBufferBeforeBoundaryOctetsUnderBufferShouldIgnoreBytesBeforeStart() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 19, 19);
        for (int i=0;i<5;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(19, sink.position());
        sink.flip();
        for (int l=0;l<2;l++) {
            for (int i=9;i<19;i++) {
                if (sink.hasRemaining()) {
                    assertEquals(i % 10, sink.get());
                }
            }
        }
    }
    
    public void testMultiBufferShouldIgnoreBytesBeforeStartAndAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 4, 2);
        assertEquals(10, channel.write(src));
        assertEquals(2, sink.position());
        sink.flip();
        for (int i=4;i<6;i++) {
            assertEquals(i, sink.get());
        }
    }
    
    public void testMultiBufferOnBoundaryOnBufferShouldIgnoreBytesBeforeStartAndAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 30, 30);
        for (int i=0;i<8;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(30, sink.position());
        sink.flip();
        for (int i=0;i<30;i++) {
            assertEquals(i % 10, sink.get());
        }
    }
    
    public void testMultiBufferBeforeBoundaryOnBufferShouldIgnoreBytesBeforeStartAndAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 29, 30);
        for (int i=0;i<8;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(30, sink.position());
        sink.flip();
        for (int i=9;i<39;i++) {
            assertEquals(i % 10, sink.get());
        }
    }
    
    public void testMultiBufferAfterBoundaryOnBufferShouldIgnoreBytesBeforeStartAndAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 31, 30);
        for (int i=0;i<8;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(30, sink.position());
        sink.flip();
        for (int i=1;i<31;i++) {
            assertEquals(i % 10, sink.get());
        }
    }
    
    public void testMultiBufferOnBoundaryAfterBufferShouldIgnoreBytesBeforeStartAndAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 30, 31);
        for (int i=0;i<8;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(31, sink.position());
        sink.flip();
        for (int i=0;i<31;i++) {
            assertEquals(i % 10, sink.get());
        }
    }
    
    public void testMultiBufferBeforeBoundaryAfterBufferShouldIgnoreBytesBeforeStartAndAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 29, 31);
        for (int i=0;i<8;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(31, sink.position());
        sink.flip();
        for (int i=9;i<40;i++) {
            assertEquals(i % 10, sink.get());
        }
    }
    
    public void testMultiBufferAfterBoundaryAfterBufferShouldIgnoreBytesBeforeStartAndAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 31, 31);
        for (int i=0;i<8;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(31, sink.position());
        sink.flip();
        for (int i=1;i<32;i++) {
            assertEquals(i % 10, sink.get());
        }
    }
    
    public void testMultiBufferOnBoundaryBeforeBufferShouldIgnoreBytesBeforeStartAndAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 30, 29);
        for (int i=0;i<8;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(29, sink.position());
        sink.flip();
        for (int i=0;i<29;i++) {
            assertEquals(i % 10, sink.get());
        }
    }
    
    public void testMultiBufferBeforeBoundaryBeforeBufferShouldIgnoreBytesBeforeStartAndAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 29, 29);
        for (int i=0;i<8;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(29, sink.position());
        sink.flip();
        for (int i=9;i<38;i++) {
            assertEquals(i % 10, sink.get());
        }
    }
    
    public void testMultiBufferAfterBoundaryBeforeBufferShouldIgnoreBytesBeforeStartAndAfterLimit() throws Exception {
        byte [] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer src = ByteBuffer.wrap(bytes);
        PartialWritableByteChannel channel = new PartialWritableByteChannel(this, 31, 29);
        for (int i=0;i<8;i++) {
            assertEquals(10, channel.write(src));
            src.rewind();
        }
        assertEquals(29, sink.position());
        sink.flip();
        for (int i=1;i<30;i++) {
            assertEquals(i % 10, sink.get());
        }
    }
    
    public int write(ByteBuffer src) throws IOException {
        int result = src.remaining();
        sink.put(src);
        return result;
    }

    public void close() throws IOException {
    }

    public boolean isOpen() {
        return true;
    }
}
