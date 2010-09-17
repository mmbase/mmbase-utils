/*

  This software is OSI Certified Open Source Software.
  OSI Certified is a certification mark of the Open Source Initiative.

  The license (Mozilla version 1.0) can be read at the MMBase site.
  See http://www.MMBase.org/license

*/

package org.mmbase.module.core;

import java.util.*;
import org.junit.*;
import org.springframework.mock.web.*;
import org.springframework.core.io.*;
import javax.servlet.ServletContext;
import static org.junit.Assert.*;
import static org.junit.Assume.*;


/**
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public class MMBaseContextTest  {


    @BeforeClass
    public static void setup() throws Exception {
        ServletContext sx = new MockServletContext("/src/test/files/",  new FileSystemResourceLoader());
        org.mmbase.module.core.MMBaseContext.init(sx);
    }



    @Test
    public void getHtmlRoot() throws Exception {
        System.out.println(org.mmbase.module.core.MMBaseContext.getHtmlRoot());
    }

}