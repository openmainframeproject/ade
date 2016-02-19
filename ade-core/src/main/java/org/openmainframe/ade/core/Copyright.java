/* (C) Copyright IBM Corp. 2011 */

package org.openmainframe.ade.core;

public final class Copyright {
    
    static final String IBM_COPYRIGHT = "(C) Copyright IBM Corp. 2011, 2016";
    
    private Copyright() {
        // Private constructor to hide the implicit public one.
    }
    
    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }
    
}
