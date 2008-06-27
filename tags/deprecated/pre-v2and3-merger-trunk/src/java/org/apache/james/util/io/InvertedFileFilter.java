/* 
 * Copyright 2002-2004 The Apache Software Foundation
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.james.util.io;

import java.io.File;
import java.io.FilenameFilter;

/**
 * This takes a <code>FilenameFilter<code> as input and inverts the selection.
 * This is used in retrieving files that are not accepted by a filter.
 *
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @version CVS $Revision: 1.1 $ $Date: 2004/07/09 03:28:47 $
 * @since 4.0
 */
public class InvertedFileFilter
    implements FilenameFilter
{
    private final FilenameFilter m_originalFilter;

    public InvertedFileFilter( final FilenameFilter originalFilter )
    {
        m_originalFilter = originalFilter;
    }

    public boolean accept( final File file, final String name )
    {
        return !m_originalFilter.accept( file, name );
    }
}


