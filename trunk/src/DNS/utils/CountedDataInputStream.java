package DNS.utils;

// Copyright (c) 1999 Brian Wellington (bwelling@xbill.org)
// Portions Copyright (c) 1999 Network Associates, Inc.

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class CountedDataInputStream {

int counter;
DataInputStream in;

public
CountedDataInputStream(InputStream i) {
	in = new DataInputStream(i);
	counter = 0;
}
public int
getPos() {
	return counter;
}
public int
read(byte b[]) throws IOException {
	in.readFully(b);
	int out = b.length;
	counter += out;
	return out;
}
public byte
readByte() throws IOException {
	counter += 1;
	return in.readByte();
}
public int
readInt() throws IOException {
	counter += 4;
	return in.readInt();
}
public long
readLong() throws IOException {
	counter += 8;
	return in.readLong();
}
public short
readShort() throws IOException {
	counter += 2;
	return in.readShort();
}
public String
readString() throws IOException {
	int len = in.readByte();
	counter++;
	byte [] b = new byte[len];
	in.readFully(b);
	counter+=len;
	return new String(b);
}
public int
readUnsignedByte() throws IOException {
	counter += 1;
	return in.readUnsignedByte();
}
public int
readUnsignedShort() throws IOException {
	counter += 2;
	return in.readUnsignedShort();
}
public int
skipBytes(int n) throws IOException {
	counter += n;
	return in.skipBytes(n);
}
}