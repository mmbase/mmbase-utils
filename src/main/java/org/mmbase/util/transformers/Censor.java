/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.transformers;

import java.util.regex.*;
import java.util.*;
import org.mmbase.util.Entry;

/**
 * Replaces certain 'forbidden' words by something more decent. Of course, censoring is evil, but
 * sometimes it can be amusing too. This is only an example implementation.
 *
 * @author Michiel Meeuwissen 
 * @since MMBase-1.7
 * @version $Id$
 */

public class Censor extends RegexpReplacer {
    private static final long serialVersionUID = 0L;

    protected static final Collection<Entry<Pattern,String>> forbidden = new ArrayList<Entry<Pattern,String>>();
    
    static {        
        new Censor().readPatterns(forbidden);   
    }

    @Override
    protected Collection<Entry<Pattern,String>> getPatterns() {        
        return forbidden;
    }

    @Override
    protected String getConfigFile() {
        return "censor.xml";
    }



    @Override
    protected void readDefaultPatterns(Collection<Entry<Pattern,String>> patterns) {
        patterns.add(new Entry<Pattern,String>(Pattern.compile("(?i)mmbase"),      "MMBase"));
        patterns.add(new Entry<Pattern,String>(Pattern.compile("(?i)microsoft"),   "Micro$soft"));
        patterns.add(new Entry<Pattern,String>(Pattern.compile("(?i)fuck"),        "****"));
    }
    

    @Override
    public String toString() {
        return "CENSOR";
    }
}
