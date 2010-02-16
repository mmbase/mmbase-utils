/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.transformers;

import java.util.regex.*;
import org.mmbase.util.logging.*;

/**
 * This straight-forward transformer wraps {@link Long#toString(long, int)} and {@link
 * Long#parseLong(String, int)}. This means that it only works on simple strings that actually
 * represent long values. Other strings are left untransformed.
 *
 * @author Michiel Meeuwissen
 * @since MMBase-1.9.2
 * @version $Id$
 */

public class RadixTransformer extends StringTransformer {

    private static final Logger log = Logging.getLoggerInstance(RadixTransformer.class);

    protected int radix = 36;

    public void setRadix(int r) {
        radix = r;
    }

    public int getRadix() {
        return radix;
    }

    public  String transform(String r) {
        try {
            long l = Long.parseLong(r);
            String result = Long.toString(l, radix);
            return result;
        } catch (Exception e) {
            return r;
        }
    }

    public String transformBack(String r) {
        return "" + Long.parseLong(r, radix);
    }

    public String toString() {
        return "RADIX" + radix;
    }



}
