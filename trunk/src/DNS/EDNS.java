package DNS;

// Copyright (c) 1999 Brian Wellington (bwelling@xbill.org)
// Portions Copyright (c) 1999 Network Associates, Inc.

public class EDNS {

public static OPTRecord
newOPT(int payloadSize) {
	return new OPTRecord(Name.root, (short)payloadSize, 0);
}
}