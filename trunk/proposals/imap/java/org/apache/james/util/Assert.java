package org.apache.james.util;

import org.apache.avalon.framework.CascadingRuntimeException;

/**
 * A set of debugging utilities.
 */ 
public final class Assert
{
    // Can't instantiate.
    private Assert() {};
    
    /**
     * Checks the supplied boolean expression, throwing an AssertionException if false;
     */ 
    public static void isTrue( boolean expression )
    {
        if ( ! expression ) {
            throw new RuntimeException( "Assertion Failed." );
        }
    }

    /**
     * Fails with an assertion exception.
     */
    public static void fail()
    {
        throw new RuntimeException( "Assertion error - should not reach here." );
    }

    /**
     * Fails, indicating not-yet-implemented features.
     */
    public static void notImplemented()
    {
        throw new RuntimeException( "Not implemented" );
    }

}
