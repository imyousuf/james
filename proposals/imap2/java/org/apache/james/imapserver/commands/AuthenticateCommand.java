/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ProtocolException;

/**
 * Handles processeing for the AUTHENTICATE imap command.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.3 $
 */
class AuthenticateCommand extends NonAuthenticatedStateCommand
{
    public static final String NAME = "AUTHENTICATE";
    public static final String ARGS = "<auth_type> *(CRLF base64)";

    /** @see CommandTemplate#doProcess */
    protected void doProcess( ImapRequestLineReader request,
                              ImapResponse response,
                              ImapSession session
                              ) throws ProtocolException
    {
        String authType = parser.astring( request );
        parser.endLine( request );

        response.commandFailed( this, "Unsupported authentication mechanism '" +
                                      authType + "'" );
    }

    /** @see ImapCommand#getName */
    public String getName()
    {
        return NAME;
    }

    /** @see CommandTemplate#getArgSyntax */
    public String getArgSyntax()
    {
        return ARGS;
    }
}

/*
6.2.1.  AUTHENTICATE Command

   Arguments:  authentication mechanism name

   Responses:  continuation data can be requested

   Result:     OK - authenticate completed, now in authenticated state
               NO - authenticate failure: unsupported authentication
                    mechanism, credentials rejected
              BAD - command unknown or arguments invalid,
                    authentication exchange cancelled

      The AUTHENTICATE command indicates an authentication mechanism,
      such as described in [IMAP-AUTH], to the server.  If the server
      supports the requested authentication mechanism, it performs an
      authentication protocol exchange to authenticate and identify the
      client.  It MAY also negotiate an OPTIONAL protection mechanism
      for subsequent protocol interactions.  If the requested
      authentication mechanism is not supported, the server SHOULD
      reject the AUTHENTICATE command by sending a tagged NO response.

      The authentication protocol exchange consists of a series of
      server challenges and client answers that are specific to the
      authentication mechanism.  A server challenge consists of a
      command continuation request response with the "+" token followed
      by a BASE64 encoded string.  The client answer consists of a line
      consisting of a BASE64 encoded string.  If the client wishes to
      cancel an authentication exchange, it issues a line with a single
      "*".  If the server receives such an answer, it MUST reject the
      AUTHENTICATE command by sending a tagged BAD response.

      A protection mechanism provides integrity and privacy protection
      to the connection.  If a protection mechanism is negotiated, it is
      applied to all subsequent data sent over the connection.  The
      protection mechanism takes effect immediately following the CRLF
      that concludes the authentication exchange for the client, and the
      CRLF of the tagged OK response for the server.  Once the
      protection mechanism is in effect, the stream of command and
      response octets is processed into buffers of ciphertext.  Each
      buffer is transferred over the connection as a stream of octets
      prepended with a four octet field in network byte order that
      represents the length of the following data.  The maximum
      ciphertext buffer length is defined by the protection mechanism.

      Authentication mechanisms are OPTIONAL.  Protection mechanisms are
      also OPTIONAL; an authentication mechanism MAY be implemented
      without any protection mechanism.  If an AUTHENTICATE command
      fails with a NO response, the client MAY try another
      authentication mechanism by issuing another AUTHENTICATE command,
      or MAY attempt to authenticate by using the LOGIN command.  In
      other words, the client MAY request authentication types in
      decreasing order of preference, with the LOGIN command as a last
      resort.

   Example:    S: * OK KerberosV4 IMAP4rev1 Server
               C: A001 AUTHENTICATE KERBEROS_V4
               S: + AmFYig==
               C: BAcAQU5EUkVXLkNNVS5FRFUAOCAsho84kLN3/IJmrMG+25a4DT
                  +nZImJjnTNHJUtxAA+o0KPKfHEcAFs9a3CL5Oebe/ydHJUwYFd
                  WwuQ1MWiy6IesKvjL5rL9WjXUb9MwT9bpObYLGOKi1Qh
               S: + or//EoAADZI=
               C: DiAF5A4gA+oOIALuBkAAmw==
               S: A001 OK Kerberos V4 authentication successful

      Note: the line breaks in the first client answer are for editorial
      clarity and are not in real authenticators.
*/
