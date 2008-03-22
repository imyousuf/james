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

package org.apache.james.imapserver.codec.encode.imap4rev1.legacy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.james.imap.message.response.imap4rev1.Literal;
import org.apache.james.imapserver.codec.encode.ImapResponseWriter;

public class MockImapResponseWriter implements ImapResponseWriter {

    public final List operations = new ArrayList();
    
    public void commandName(String commandName) {
        operations.add(new CommandNameOperation(commandName));

    }

    public void end() {
        operations.add(new EndOperation());
    }

    public void message(String message) {
        operations.add(new TextMessageOperation(message));
    }

    public void message(long number) {
        operations.add(new NumericMessageOperation(number));
    }

    public void responseCode(String responseCode) {
        operations.add(new ResponseCodeOperation(responseCode));
    }

    public void tag(String tag) {
        operations.add(new TagOperation(tag));
    }

    public void untagged() {
        operations.add(new UntaggedOperation());
    }

    public static class EndOperation {
        public boolean equals(Object obj) {
            return obj instanceof EndOperation;
        }

        public int hashCode() {
            return 3;
        }
        
    }
    
    public void quote(String message) {
        operations.add(new QuoteMessageOperation(message));
    }
    
    public static class QuoteMessageOperation {
        public final String message;
        public QuoteMessageOperation(final String message) {
            this.message = message;
        }

        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((message == null) ? 0 : message.hashCode());
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final QuoteMessageOperation other = (QuoteMessageOperation) obj;
            if (message == null) {
                if (other.message != null)
                    return false;
            } else if (!message.equals(other.message))
                return false;
            return true;
        }
    }
    
    public static class TextMessageOperation {
        public final String text;
        public TextMessageOperation(String text) {
            this.text = text;
        }
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((text == null) ? 0 : text.hashCode());
            return result;
        }
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final TextMessageOperation other = (TextMessageOperation) obj;
            if (text == null) {
                if (other.text != null)
                    return false;
            } else if (!text.equals(other.text))
                return false;
            return true;
        }
        
    }
    
    public static class NumericMessageOperation {
        public final long number;
        public NumericMessageOperation(long number) {
            this.number = number;
        }
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = (int) (PRIME * result + number);
            return result;
        }
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final NumericMessageOperation other = (NumericMessageOperation) obj;
            if (number != other.number)
                return false;
            return true;
        }
        
    }
    
    
    public static class ResponseCodeOperation {
        public final String responseCode;

        public ResponseCodeOperation(final String responseCode) {
            super();
            this.responseCode = responseCode;
        }

        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((responseCode == null) ? 0 : responseCode.hashCode());
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final ResponseCodeOperation other = (ResponseCodeOperation) obj;
            if (responseCode == null) {
                if (other.responseCode != null)
                    return false;
            } else if (!responseCode.equals(other.responseCode))
                return false;
            return true;
        }
        
    }
    
    public static class CommandNameOperation {
        public final String commandName;

        public CommandNameOperation(final String commandName) {
            super();
            this.commandName = commandName;
        }

        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((commandName == null) ? 0 : commandName.hashCode());
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final CommandNameOperation other = (CommandNameOperation) obj;
            if (commandName == null) {
                if (other.commandName != null)
                    return false;
            } else if (!commandName.equals(other.commandName))
                return false;
            return true;
        }
        
    }
    
    public static class UntaggedOperation {
        public boolean equals(Object obj) {
            return obj instanceof UntaggedOperation;
        }

        public int hashCode() {
            return 2;
        }
        
    }
    
    public static class TagOperation {

        private final String tag;
        public TagOperation(String tag) {
            this.tag = tag;
        }
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((tag == null) ? 0 : tag.hashCode());
            return result;
        }
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final TagOperation other = (TagOperation) obj;
            if (tag == null) {
                if (other.tag != null)
                    return false;
            } else if (!tag.equals(other.tag))
                return false;
            return true;
        }
        
    }

    public void closeParen() {
        operations.add(new BracketOperation(false, false));
    }

    public void openParen() {
        operations.add(new BracketOperation(true, false));
    }

    public static class BracketOperation {
        
        public static BracketOperation openSquare() {
            return new BracketOperation(true, true);
        }
        
        public static BracketOperation closeSquare() {
            return new BracketOperation(false, true);
        }
        
        private final boolean open;
        private final boolean square;
        public BracketOperation(final boolean open, boolean square) {
            this.open = open;
            this.square = square;
        }
        
        /**
         * Is this an open paren?
         * @return the open
         */
        public final boolean isOpen() {
            return open;
        }

        /**
         * Is this a square bracket?
         * @return true if this is a square bracket, 
         * false otherwise
         */
        public final boolean isSquare() {
            return square;
        }

        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + (open ? 1231 : 1237);
            result = PRIME * result + (square ? 1231 : 1237);
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final BracketOperation other = (BracketOperation) obj;
            if (open != other.open)
                return false;
            if (square != other.square)
                return false;
            return true;
        }

        
    }

    public void literal(Literal literal) throws IOException {
        operations.add(new LiteralOperation(literal));
    }
    
    public static final class LiteralOperation {
        public final Literal literal;
        public LiteralOperation(Literal literal) {
            this.literal = literal;
        }

        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((literal == null) ? 0 : literal.hashCode());
            return result;
        }
        
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final LiteralOperation other = (LiteralOperation) obj;
            if (literal == null) {
                if (other.literal != null)
                    return false;
            } else if (!literal.equals(other.literal))
                return false;
            return true;
        }
        
        
    }

    public void skipNextSpace() throws IOException {
    }

    public void closeSquareBracket() throws IOException {
        operations.add(new BracketOperation(false, true));
    }

    public void openSquareBracket() throws IOException {
        operations.add(new BracketOperation(true, true));
    }
    
}
