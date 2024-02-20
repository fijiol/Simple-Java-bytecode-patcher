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

    public void addEntry(String className, String methodName, String pre, String post) {
        list.add(new CodeWrapperInstance(className, methodName, pre, post));
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
            if (ci.matchClassName(className)) {
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
            if (ci.matchMethodName(methodName)) {
                if (ci.pre == null) return null;
                return ci.pre.replaceAll("%METHOD%", methodName);
            }
        }
        return null;
    }

    @Override
    public String postCode(String methodName) {
        for (CodeWrapperInstance ci : list) {
            if (ci.matchMethodName(methodName)) {
                if (ci.post == null) return null;
                return ci.post.replaceAll("%METHOD%", methodName);
            }
        }
        return null;
    }

    public static final class CodeWrapperInstance {
        public final String className;
        public final int classNamePrefixLen;
        public final String methodName;
        public final int methodNamePrefixLen;
        public final String pre;
        public final String post;

        private CodeWrapperInstance(String className, String methodName, String pre, String post) {
            methodName = methodName.replace('.', '/');
            className = className.replace('.', '/');
            this.classNamePrefixLen = className.indexOf('*');
            this.className = classNamePrefixLen >= 0 ? className.substring(0, classNamePrefixLen) : className;
            this.methodNamePrefixLen = methodName.indexOf('*');
            this.methodName = methodNamePrefixLen >= 0 ? methodName.substring(0, methodNamePrefixLen) : methodName;
            this.pre = pre;
            this.post = post;
        }

        public boolean matchClassName(String className) {
            if (className == null) return false;
            if (this.className == null) return false;
            return classNamePrefixLen >= 0 ? this.className.equals(className.substring(0, Math.min(classNamePrefixLen, className.length()))) : this.className.equals(className);
        }

        public boolean matchMethodName(String methodName) {
            if (methodName == null) return false;
            if (this.methodName == null) return false;
            return methodNamePrefixLen >= 0 ? this.methodName.equals(methodName.substring(0, Math.min(methodNamePrefixLen, methodName.length()))) : this.methodName.equals(methodName);
        }
    }

}
