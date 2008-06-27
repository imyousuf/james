/*
 * Created on Jan 5, 2007
 *
 * PVCS Workfile Details:
 * $Workfile$
 * $Revision$
 * $Author$
 * $Date$
 * $Modtime$
 */

package org.apache.mailet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author angusd 
 * @author $Author$ 
 * @version $Revision$
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

public @interface ServiceInject{
    /**
     * @return
     */
    String serviceKey();
    
}


/* 
 *
 * PVCS Log History:
 * $Log$
 *
 */
