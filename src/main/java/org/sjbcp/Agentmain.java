/**
 * Written by Fedor Burdun of Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 * @author Fedor Burdun
 */
package org.sjbcp;

import org.sjbcp.impl.SJBCP;
import java.lang.instrument.Instrumentation;

import java.util.jar.JarFile;

public class Agentmain {
    
    
    public static void premain(String agentArgument, Instrumentation instrumentation) {
        commonmain(agentArgument, instrumentation);

        try {
            SJBCP.premain0(agentArgument, instrumentation);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void commonmain(String arguments, Instrumentation instrumentation) {
        // Exclude CLI option Xbootclasspath
        try {
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(
                            Agentmain.class.getProtectionDomain().
                            getCodeSource().getLocation().getPath()));
        } catch (Exception e) {
            // can throw NPE when jar file is already in Xbootclasspath (f.e. by 
            // cli argument)
            e.printStackTrace();
        }
        
    }
    
    public static void agentmain(String agentArgument, Instrumentation instrumentation) {
        commonmain(agentArgument, instrumentation);
        
        try {
            SJBCP.premain0(agentArgument, instrumentation);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        System.err.println("There is no main() method.");
        System.exit(1);
    }
}
