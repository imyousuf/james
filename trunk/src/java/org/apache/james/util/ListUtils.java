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
package org.apache.james.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Miscelaneous utilities to manipulate Lists.
 *
 * @deprecated use org.apache.commons.collections.ListUtils instead
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @version CVS $Revision: 1.1 $ $Date: 2004/07/09 17:43:07 $
 * @since 4.0
 */
public class ListUtils
{
    public static List intersection( final List list1, final List list2 )
    {
        final ArrayList result = new ArrayList();
        final Iterator iterator = list2.iterator();

        while( iterator.hasNext() )
        {
            final Object o = iterator.next();

            if( list1.contains( o ) )
            {
                result.add( o );
            }
        }

        return result;
    }

    public static List subtract( final List list1, final List list2 )
    {
        final ArrayList result = new ArrayList( list1 );
        final Iterator iterator = list2.iterator();

        while( iterator.hasNext() )
        {
            result.remove( iterator.next() );
        }

        return result;
    }

    public static List sum( final List list1, final List list2 )
    {
        return subtract( union( list1, list2 ),
                         intersection( list1, list2 ) );
    }

    public static List union( final List list1, final List list2 )
    {
        final ArrayList result = new ArrayList( list1 );

        final Iterator iterator = list2.iterator();
        while( iterator.hasNext() )
        {
            final Object o = iterator.next();
            if( !result.contains( o ) )
            {
                result.add( o );
            }
        }
        return result;
    }
}
