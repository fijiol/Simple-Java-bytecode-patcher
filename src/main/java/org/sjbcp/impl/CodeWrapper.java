/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sjbcp.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sjbcp.code.CodeWriter;

/**
 *
 * @author fijiol
 */
class CodeWrapper implements CodeWriter {

    List<CodeWrapperInstance> list = new ArrayList();
    
    public CodeWrapper() {
    }
    
    public void addEntry(String className, String methodName, String pre, String post, boolean isNew) {
        list.add(new CodeWrapperInstance(className, methodName, pre, post, isNew));
    }

    public void addEntry(CodeWrapperInstance cwi) {
        list.add(cwi);
    }

    @Override
    public void init(SJBCP sjbcp) {
        
    }

    @Override
    public boolean needInstrument(String className) {
        for (CodeWrapperInstance ci : list) {
            if (ci.className != null && 
                    ci.className.replaceAll("/", ".")
                    .equals(className.replaceAll("/", ".")) && !ci.aNew) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterable<String> classNewFields(String className) {
        return Collections.emptyList();
    }

    @Override
    public String preCode(String methodName) {
        for (CodeWrapperInstance ci : list) {
            if (ci.methodName != null && 
                    ci.methodName.replaceAll("/", ".")
                    .equals(methodName.replaceAll("/", "."))) {
                return ci.pre;
            }
        }
        return null;
    }

    @Override
    public String postCode(String methodName) {
        for (CodeWrapperInstance ci : list) {
            if (ci.methodName != null && ci.methodName.equals(methodName)) {
                return ci.post;
            }
        }
        return null;
    }

    @Override
    public Iterable<String> newMethods(String className) {
        List<String> res = new ArrayList<String>();
        
        for (CodeWrapperInstance ci : list) {
            if (ci.className != null && ci.className.equals(className) && ci.aNew) {
                res.add(ci.className);
            }
        }
        return res;
    }

    public static class CodeWrapperInstance {
        public String className;
        public String methodName;
        public String pre;
        public String post;
        public boolean aNew;
        
        public CodeWrapperInstance() {
        }

        private CodeWrapperInstance(String className, String methodName, String pre, String post, boolean aNew) {
            this.className = className;
            this.methodName = methodName;
            this.pre = pre;
            this.post = post;
            this.aNew = aNew;
        }
    }
    
}
