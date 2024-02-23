/**
 * Written by Fedor Burdun of Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 * @author Fedor Burdun
 */
package org.sjbcp.code;

import org.sjbcp.impl.SJBCP;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;

import javax.tools.ToolProvider;

public class Transformer implements ClassFileTransformer {
    private final SJBCP sjbcp;
    
    private final CodeWriter codeWriter;


    public Transformer(SJBCP sjbcp, CodeWriter codeWriter) {
        this.sjbcp = sjbcp;
        this.codeWriter = codeWriter;
        
        codeWriter.init(sjbcp);
    }
    
    @Override
    public byte[] transform(ClassLoader loader, String className,
            Class clazz, java.security.ProtectionDomain domain,
            byte[] bytes) {

        if (SJBCP.traceClasses) {
            SJBCP.println(">> " + className);
        }
        
        if (!codeWriter.needInstrument(className)) {
            return bytes;
        }
        
        if (SJBCP.traceClasses) {
            SJBCP.println(">> " + className + " will be instrumented! source=" + domain.getCodeSource());
        }

        bytes = SJBCP.beforeTransformation(loader, className, clazz, domain, bytes);
        bytes = doClass(className, clazz, bytes);
        bytes = SJBCP.afterTransformation(loader, className, clazz, domain, bytes);
        return bytes;
    }

    public byte[] doClass(String className, Class clazz, byte[] b) {
        try {
            ClassPool pool = ClassPool.getDefault();

            CtClass cl = null;

            String code = null;
            String methodDescription = null;

            try {

                for (String cp : SJBCP.classPathPrepend.keySet()) {
                    pool.appendClassPath(cp);
                    pool.importPackage(SJBCP.classPathPrepend.get(cp));
                }

                cl = pool.makeClass(new java.io.ByteArrayInputStream(b));

                if (cl.isInterface() == false) {

                    for (String varDeclaration : codeWriter.classNewFields(className)) { 
                        if (SJBCP.traceClasses) {
                            SJBCP.println(">> " + className + " will have new field " + varDeclaration);
                        }
                        
                        String[] var = varDeclaration.split(" ");
                        if (var.length != 2) {
                            code = null;
                            throw new Exception("Check your codeWriter implementation: var declaration array size != 2, ==" + var.length);
                        }
                        CtClass typeClass = pool.get(var[0]);

                        code = "adding field " + Arrays.deepToString(var);

                        CtField field = new CtField(typeClass, var[1], cl);
                        cl.addField(field);
                    }

                    CtBehavior[] methods = cl.getDeclaredBehaviors();

                    for (CtBehavior method : methods) {
                        if (SJBCP.traceMethods) {
                            SJBCP.println(">>> " + className + " go over method " + method.getLongName());
                        }
                        
                        if ( method.isEmpty() == false && !Modifier.isNative(method.getModifiers()) ) {
                            String pre = codeWriter.preCode(method.getLongName());
                            String post = codeWriter.postCode(method.getLongName());

                            if (pre != null && pre.length() > 0) {
                                code = pre;
                                methodDescription = "insert before method " + method.getLongName();
                                method.insertBefore(pre);
                            }
                            if (post != null && post.length() > 0) {
                                code = post;
                                methodDescription = "insert before method " + method.getLongName();
                                method.insertAfter(post);
                            }

                            if (SJBCP.traceMethods && (pre != null && pre.length() > 0 || post != null && post.length() > 0)) {
                                SJBCP.println(">>> " + className + " method " + method.getLongName() + " will be instrumented.");
                            }
                        }
                    }

                    code = null;
                    methodDescription = null;
                    b = cl.toBytecode();
                }
            } catch (Exception e) {
                System.err.println("Could not instrument class=" + className
                        + " (" + methodDescription + ")"
                        + "\n, code = " + code + "\n, exception : " + 
                        e.getMessage() + "\n:" + e);
                System.err.flush();

                e.printStackTrace();
            } finally {
                if (cl != null) {
                    cl.detach();
                }
            }
            return b;
        } catch (RuntimeException e) {
            System.err.println("Transformation failed with : " + e);
            throw e;
        } catch (Throwable t) {
            System.err.println("Transformation failed with : " + t);
            System.exit(1);
            return null;
        }
    }

}
