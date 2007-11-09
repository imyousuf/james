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

package org.apache.james.mailboxmanager.torque;

public class MailboxExpression {
    
    private final String base;
    private final String expression;
    private final char freeWildcard;
    private final char localWildcard;
    private final int expressionLength;
    
    /**
     * Constructs an expression determining a set of mailbox names.
     * @param base base reference name, not null
     * @param expression mailbox match expression, not null
     * @param freeWildcard matches any series of charaters
     * @param localWildcard matches any sequence of characters
     * up to the next hierarchy delimiter
     */
    public MailboxExpression(final String base, final String expression,
                            final char freeWildcard, final char localWildcard) {
        super();
        if (base == null) {
            this.base = "";
        } else {
            this.base = base;
        }
        if (expression == null) {
            this.expression = "";
        } else {
            this.expression = expression;
        }
        expressionLength = this.expression.length();
        this.freeWildcard = freeWildcard;
        this.localWildcard = localWildcard;
    }
    
    /**
     * Gets the base reference name for the search.
     * @return the base
     */
    public final String getBase() {
        return base;
    }

    /**
     * Gets the name search expression.
     * This may contain wildcards.
     * @return the expression
     */
    public final String getExpression() {
        return expression;
    }
    
    /**
     * Gets wildcard character that matches any series of characters.
     * @return the freeWildcard
     */
    public final char getFreeWildcard() {
        return freeWildcard;
    }

    /**
     * Gets wildacard character that matches any series of characters
     * excluding hierarchy delimiters. Effectively, this means that
     * it matches any sequence within a name part.
     * @return the localWildcard
     */
    public final char getLocalWildcard() {
        return localWildcard;
    }

    /**
     * Is the given name a match for {@link #getExpression()}?
     * @param name name to be matched
     * @param hierarchyDelimiter mailbox hierarchy delimiter
     * @return true if the given name matches this expression,
     * false otherwise
     */
    public final boolean isExpressionMatch(String name, char hierarchyDelimiter) {
        final boolean result;
        if (isWild()) {
            if (name == null)
            {
                result = false;                
            } else {
                result = isWildcardMatch(name, 0, 0, hierarchyDelimiter);
            }
        } else {
            result = expression.equals(name);
        }
        return result;
    }
    
    private final boolean isWildcardMatch(final String name, final int nameIndex, 
            final int expressionIndex, final char hierarchyDelimiter) {
        final boolean result;
        if (expressionIndex < expressionLength) {
            final char expressionNext = expression.charAt(expressionIndex);
            if (expressionNext == freeWildcard) {
                result = isFreeWildcardMatch(name, nameIndex, expressionIndex, hierarchyDelimiter);
            } else if (expressionNext == localWildcard) {
                result = isLocalWildcardMatch(name, nameIndex, expressionIndex, hierarchyDelimiter);
            } else {
                if (nameIndex < name.length()) {
                    final char nameNext = name.charAt(nameIndex);
                    if(nameNext == expressionNext) {
                        result = isWildcardMatch(name, nameIndex +1, expressionIndex +1, hierarchyDelimiter);
                    } else {
                        result = false;
                    }
                } else {
                    // more expression characters to match
                    // but no more name
                    result = false;
                }
            }
        } else {
            // no more expression characters to match
            result = true;
        }
        return result;
    }

    private boolean isLocalWildcardMatch(final String name, final int nameIndex, final int expressionIndex, final char hierarchyDelimiter) {
        final boolean result;
        if (expressionIndex < expressionLength) {
            final char expressionNext = expression.charAt(expressionIndex);
            if (expressionNext == localWildcard) {
                result = isLocalWildcardMatch(name, nameIndex, expressionIndex+1, hierarchyDelimiter);
            } else if (expressionNext == freeWildcard) {
                result = isFreeWildcardMatch(name, nameIndex, expressionIndex+1, hierarchyDelimiter);
            } else {
                boolean matchRest = false;
                for (int i=nameIndex; i< name.length() ; i++) {
                    final char tasteNextName = name.charAt(i);
                    if (expressionNext == tasteNextName) {
                        if (isLocalWildcardMatch(name, i, expressionIndex+1, hierarchyDelimiter)) {
                            matchRest = true;
                            break;
                        }
                    } else if (tasteNextName == hierarchyDelimiter) {
                        matchRest = false;
                        break;
                    }
                }
                result = matchRest;
            }
        } else {
            boolean containsDelimiter = false;
            for (int i=nameIndex; i< name.length() ; i++) {
                if (name.charAt(i) == hierarchyDelimiter) {
                    containsDelimiter = true;
                    break;
                }
            }
            result = !containsDelimiter;
        }
        return result;
    }

    private boolean isWildcard(char character) {
        return character == freeWildcard || character == localWildcard;
    }
    
    private boolean isFreeWildcardMatch(final String name, final int nameIndex, final int expressionIndex, final char hierarchyDelimiter) {
        final boolean result;
        int nextNormal = expressionIndex;
        while (nextNormal<expressionLength && isWildcard(expression.charAt(nextNormal))) {
            nextNormal++;
        }
        if (nextNormal < expressionLength) {
            final char expressionNextNormal = expression.charAt(nextNormal);
            boolean matchRest = false;
            for (int i=nameIndex; i< name.length() ; i++) {
                final char tasteNextName = name.charAt(i);
                if (expressionNextNormal == tasteNextName) {
                    if (isWildcardMatch(name, i, nextNormal, hierarchyDelimiter)) {
                        matchRest = true;
                        break;
                    }
                }
            }
            result = matchRest;
        } else {
            // no more expression characters to match
            result = true;
        }
        return result;
    }
    
    /**
     * Get combined name formed by adding expression to base using
     * the given hierarchy delimiter.
     * Note that the wildcards are retained in the combined name.
     * @param hierarchyDelimiter delimiter for mailbox hierarchy
     * @return {@link #getBase()} combined with {@link #getExpression()},
     * not null
     */
    public String getCombinedName(char hierarchyDelimiter) {
        final String result;
        final int baseLength = base.length();
        if (base != null && baseLength>0) {
            final int lastChar = baseLength-1;
            if(base.charAt(lastChar)==hierarchyDelimiter) {
                if (expression != null && expression.length() > 0) {
                    if (expression.charAt(0)==hierarchyDelimiter) {
                        result = base + expression.substring(1);
                    } else {
                        result = base + expression;
                    }
                } else {
                    result = base;
                }
            } else {
                if (expression != null && expression.length() > 0) {
                    if (expression.charAt(0)==hierarchyDelimiter) {
                        result = base + expression;
                    } else {
                        result = base + hierarchyDelimiter + expression;
                    }
                } else {
                    result = base;
                }
            }
       } else {
           result = expression;
       }
       return result; 
    }
    
    /**
     * Is this expression wild? 
     * @return true if wildcard contained, false otherwise
     */
    public boolean isWild() {
        return expression != null && (expression.indexOf(freeWildcard) >= 0 
                || expression.indexOf(localWildcard) >= 0);
    }

    /**
     * Renders a string sutable for logging.
     * @return a <code>String</code> representation 
     * of this object.
     */
    public String toString()
    {
        final String TAB = " ";
        
        String retValue = "";
        
        retValue = "MailboxExpression [ "
            + "base = " + this.base + TAB
            + "expression = " + this.expression + TAB
            + "freeWildcard = " + this.freeWildcard + TAB
            + "localWildcard = " + this.localWildcard + TAB
            + " ]";
    
        return retValue;
    }
    
    
}
