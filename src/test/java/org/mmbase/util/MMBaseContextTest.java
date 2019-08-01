/*

  This software is OSI Certified Open Source Software.
  OSI Certified is a certification mark of the Open Source Initiative.

  The license (Mozilla version 1.0) can be read at the MMBase site.
  See http://www.MMBase.org/license

*/

package org.mmbase.util;

import javax.servlet.ServletContext;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.mock;


/**
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public class MMBaseContextTest  {


    @BeforeClass
    public static void setup() throws Exception {
        ServletContext sx = mock(ServletContext.class);
        MMBaseContext.init(sx);
    }



    @Test
    public void getHtmlRoot() throws Exception {
        System.out.println(MMBaseContext.getHtmlRoot());
    }

}
