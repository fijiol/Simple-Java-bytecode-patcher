/*
 * Written by Fedor Burdun of Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 * 

 */
package org.sjbcp.mockup;

import org.sjbcp.code.CodeWriter;
import java.util.Collections;
import java.util.List;
import org.sjbcp.impl.SJBCP;

/**
 *
 * @author fijiol
 */
public class CodeWriterMockup implements CodeWriter {
    
    @Override
    public void init(SJBCP sjbcp) {
    }
    
    @Override
    public boolean needInstrument(String className) {
        return true;
    }

    @Override
    public Iterable<String> classNewFields(String className) {
        return Collections.emptyList();
    }

    @Override
    public String preCode(String methodName) {
        return null;
    }

    @Override
    public String postCode(String methodName) {
        return null;
    }

    @Override
    public Iterable<String> newMethods(String className) {
        return Collections.emptyList();
    }

    
}
