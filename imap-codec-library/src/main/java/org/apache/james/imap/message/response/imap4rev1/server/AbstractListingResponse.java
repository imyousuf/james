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

package org.apache.james.imap.message.response.imap4rev1.server;

/**
 * <code>LIST</code> and <code>LSUB</code> return identical data.
 */
public abstract class AbstractListingResponse {

    private final boolean noInferiors;
    private final boolean noSelect;
    private final boolean marked;
    private final boolean unmarked;
    private final String hierarchyDelimiter;
    private final String name;

    public AbstractListingResponse(final boolean noInferiors, final boolean noSelect, 
            final boolean marked, final boolean unmarked, 
            final String hierarchyDelimiter, final String name) {
        super();
        this.noInferiors = noInferiors;
        this.noSelect = noSelect;
        this.marked = marked;
        this.unmarked = unmarked;
        this.hierarchyDelimiter = hierarchyDelimiter;
        this.name = name;
    }

    /**
     * Gets hierarchy delimiter.
     * @return hierarchy delimiter, 
     * or null if no hierarchy exists
     */
    public final String getHierarchyDelimiter() {
        return hierarchyDelimiter;
    }

    /**
     * Is <code>Marked</code> name attribute set?
     * @return true if <code>Marked</code>, false otherwise
     */
    public final boolean isMarked() {
        return marked;
    }

    /**
     * Gets the listed name.
     * @return name of the listed mailbox, not null
     */
    public final String getName() {
        return name;
    }

    /**
     * Is <code>Noinferiors</code> name attribute set?
     * @return true if <code>Noinferiors</code>,
     * false otherwise
     */
    public final boolean isNoInferiors() {
        return noInferiors;
    }

    /**
     * Is <code>Noselect</code> name attribute set?
     * @return true if <code>Noselect</code>,
     * false otherwise
     */
    public final boolean isNoSelect() {
        return noSelect;
    }

    /**
     * Is <code>Unmarked</code> name attribute set?
     * @return true if <code>Unmarked</code>,
     * false otherwise
     */
    public final boolean isUnmarked() {
        return unmarked;
    }

    /**
     * Are any name attributes set?
     * @return true if {@link #isNoInferiors()}, 
     * {@link #isNoSelect()}, {@link #isMarked()} or 
     * {@link #isUnmarked(){
     */
    public final boolean isNameAttributed() {
        return noInferiors || noSelect || marked || unmarked;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((hierarchyDelimiter == null) ? 0 : hierarchyDelimiter.hashCode());
        result = PRIME * result + (marked ? 1231 : 1237);
        result = PRIME * result + ((name == null) ? 0 : name.hashCode());
        result = PRIME * result + (noInferiors ? 1231 : 1237);
        result = PRIME * result + (noSelect ? 1231 : 1237);
        result = PRIME * result + (unmarked ? 1231 : 1237);
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final AbstractListingResponse other = (AbstractListingResponse) obj;
        if (hierarchyDelimiter == null) {
            if (other.hierarchyDelimiter != null)
                return false;
        } else if (!hierarchyDelimiter.equals(other.hierarchyDelimiter))
            return false;
        if (marked != other.marked)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (noInferiors != other.noInferiors)
            return false;
        if (noSelect != other.noSelect)
            return false;
        if (unmarked != other.unmarked)
            return false;
        return true;
    }

    /**
     * Renders object as a string suitable for logging.
     * @return a <code>String</code> representation 
     * of this object.
     */
    public String toString()
    {
        final String TAB = " ";
        
        String retValue = getClass() + " ( "
            + super.toString() + TAB
            + "noInferiors = " + this.noInferiors + TAB
            + "noSelect = " + this.noSelect + TAB
            + "marked = " + this.marked + TAB
            + "unmarked = " + this.unmarked + TAB
            + "hierarchyDelimiter = " + this.hierarchyDelimiter + TAB
            + "name = " + this.name + TAB
            + " )";
    
        return retValue;
    }
    
    
}