/**
 * Written by Fedor Burdun of Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 * @author Fedor Burdun
 */
package org.sjbcp.impl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.sjbcp.code.CodeWriter;
import org.sjbcp.code.Transformer;

public class SJBCP {

    public static void println(String str) {
        print(str + "\n");
    }
    
    public static void print(String str) {
        System.out.print(str);
        
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("log-" + ManagementFactory.getRuntimeMXBean().getName() + ".log", true)));
            pw.print(str);
            pw.flush();
            pw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void main(String[] args) {
        SJBCP.println("jfPath.jar doesn't have now functional main method. Please rerun your application as:\n\t"
                + "java -javaagent:jfPath.jar -jar yourapp.jar");
        System.exit(1);
    }

    private static String printKeys(String[] keys) {
        StringBuilder sb = new StringBuilder();
        for (String s : keys) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(s);
        }
        return sb.toString();
    }
    
    private static String printKeys(String[] keys, int align) {
        String st = printKeys(keys);
        return st + String.format("%0" + Math.max(1, align - st.length()) + "d", 0).replace('0', ' ');
    }
    
    public static void printHelpAndExit() {
        SJBCP.println("Usage:");
        printHelpParameters();
                
        SJBCP.println("\n");
        SJBCP.println("Please rerun application with proper CLI options.\n");
        
        System.exit(1);
    }

    public static void printHelpParameters() {
    }
    
    public static ConcurrentHashMap<String, SJBCP> jRTWorkers = new ConcurrentHashMap<String, SJBCP>();
    
    public void premain(String agentArgument, Instrumentation instrumentation) {
        parseArguments(agentArgument, codeWrapper);
        
        jRTWorkers.put("1", this);
        
        instrument(agentArgument, instrumentation);
        
        
        //Some temporary place to print collected statistic.
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                //TODO move/remove/improve
                SJBCP.println("***************************************************************");
                SJBCP.println(" AGENT FINISHED ");
                SJBCP.println("***************************************************************");
            }

        });

        if (true)
        (new Thread(
        new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        SJBCP.println("I'm still alive");
                        Thread.sleep(3999);
                    } catch (InterruptedException ex) {
                        SJBCP.println(ex.toString());
                    }
                }
            }
        })).start();
        
    }

    public void parseArguments(String agentArgument, CodeWriter writer) throws NumberFormatException {
        if (null != agentArgument) {
            /*
            Example
            -javaagent:./jfPath.jar='className::=java.io.IOException,,methodName::=<init>,,pre::= System.out.println("Hi, there!"); ;;className=java.lang.Exception,,'
            */
            
            for (String code : agentArgument.split(";;")) {
                boolean add = false;
                String className = null;
                String methodName = null;
                String pre = null;
                String post = null;
                boolean isNew = false;
                
                for (String v : code.split(",,")) {
                    String[] vArr = v.split("::=");
                    
                    if (vArr.length == 2) {
                        if (vArr[0].equals("className")) {
                            className = vArr[1];
                            add = true;
                        }
                        if (vArr[0].equals("methodName")) {
                            methodName = vArr[1];
                            add = true;
                        }
                        if (vArr[0].equals("pre")) {
                            pre = vArr[1];
                            add = true;
                        }
                        if (vArr[0].equals("post")) {
                            post = vArr[1];
                            add = true;
                        }
                        if (vArr[0].equals("isNew")) {
                            isNew = Boolean.valueOf(vArr[1]);
                            add = true;
                        }
                    } else { 
                        throw new RuntimeException("bad args: " + agentArgument); 
                    }
                }
                
                if (add) {
                    codeWrapper.addEntry(className, methodName, pre, post, isNew);
                }
            }
        }
    }

    public void instrument(String agentArgument, Instrumentation instrumentation) {
        instrumentation.addTransformer(new Transformer(this, codeWrapper), true);
        
    /*
    untested code to support attaching to existing java process
    */
        redeclare(instrumentation, codeWrapper);
    }
    private CodeWrapper codeWrapper = new CodeWrapper();
    
    /*
    untested code to support attaching to existing java process
    */
    private void redeclare(Instrumentation instrumentation, CodeWriter cw) {
        ArrayList<Class> ac = new ArrayList<Class>();
        
        for (Class c : instrumentation.getAllLoadedClasses()) {
            final String className = c.getName().replace(".", "/");
            
            if (cw.needInstrument(className)) {
                ac.add(c);
                try {
                    instrumentation.retransformClasses(c);
                } catch (UnmodifiableClassException ex) {
                    ex.printStackTrace();
                }
            }
        }
        
    }
    
    public static SJBCP premain0(String agentArgument, Instrumentation instrumentation) {
        
        SJBCP jfpatch = new SJBCP();
        jfpatch.premain(agentArgument, instrumentation);

        return jfpatch;
    }

    private static final String[] help = {"-h", "--help", "help", "h"};

    private static boolean hasKey(String[] list, String key) {
        for (String s : list) {
            if (s.equals(key)) {
                return true;
            }
        }
        return false;
    }
    
}
