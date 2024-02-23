/**
 * Written by Fedor Burdun of Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 * @author Fedor Burdun
 */
package org.sjbcp.impl;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.ClassPool;
import org.sjbcp.code.CodeWriter;
import org.sjbcp.code.TransformationVisitor;
import org.sjbcp.code.Transformer;

public class SJBCP {

    public static boolean traceClasses = false;
    public static boolean traceMethods = false;

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
        SJBCP.println("sjbcp.jar doesn't have now functional main method. Please rerun your application as:\n\t"
                + "java -javaagent:sjbcp.jar -jar yourapp.jar\n\n"
                + "For full documentation please read https://github.com/fijiol/Simple-Java-bytecode-patcher/blob/master/README.md");
        System.exit(1);
    }

    private static List<TransformationVisitor> visitors = new ArrayList<>();
    public static byte[] beforeTransformation(ClassLoader loader, String className, Class clazz, ProtectionDomain domain, byte[] bytes) {
        for (TransformationVisitor v : visitors) {
            bytes = v.beforeTransformation(loader, className, clazz, domain, bytes);
        }
        return bytes;
    }

    public static byte[] afterTransformation(ClassLoader loader, String className, Class clazz, ProtectionDomain domain, byte[] bytes) {
        for (TransformationVisitor v : visitors) {
            bytes = v.afterTransformation(loader, className, clazz, domain, bytes);
        }
        return bytes;
    }

    public void premain(String agentArgument, Instrumentation instrumentation) {
        parseArguments(agentArgument, codeWrapper, instrumentation);
        instrument(agentArgument, instrumentation);
    }

    private static String readFile(String fileName) {
        try {
            Path filePath = Paths.get(fileName);
            byte[] fileBytes = Files.readAllBytes(filePath);
            return new String(fileBytes);
        } catch (IOException e) {
            throw new RuntimeException("failed to get content of file '" + fileName + "'.", e);
        }
    }
    public void parseArguments(String agentArgument, CodeWriter writer, Instrumentation instrumentation) throws NumberFormatException {
        if (null != agentArgument) {
            /*
            Example
            -javaagent:./jfPath.jar='className::=java.io.IOException,,methodName::=<init>,,pre::= System.out.println("Hi, there!"); ;;className=java.lang.Exception,,'
            */
            Pattern includePattern = Pattern.compile("@@([^\\[\\]]+)");
            Matcher matcher = includePattern.matcher(agentArgument);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(sb, readFile(matcher.group(1).trim()));
            }
            matcher.appendTail(sb);

            agentArgument = sb.toString();
            
            for (String code : agentArgument.split(";;")) {
                if (!code.contains(",,") && !code.contains("::=")) {
                    // this is parameters section
                    for (String pair : code.split(",")) {
                        String[] kv = pair.split("=");
                        String k = kv[0].trim();
                        if (kv.length < 2) {
                            throw new RuntimeException("Expecting string '" + pair + "' to be a key=value pair. (k='" + k + "'");
                        }
                        String v = kv[1].trim();
                        if ("traceClasses".equals(k)) {
                            traceClasses = Boolean.parseBoolean(v);
                            continue;
                        }
                        if ("traceMethods".equals(k)) {
                            traceMethods = Boolean.parseBoolean(v);
                            continue;
                        }
                        throw new RuntimeException("Bad args: unknown property " + k);
                    }
                    continue;
                }
                boolean add = false;
                String className = null;
                String methodName = null;
                String pre = null;
                String post = null;

                String[] codeSplit = code.split(",,");
                if (codeSplit.length == 1) {
                    String kv = codeSplit[0];
                    String[] vArr = kv.split("::=");
                    if (vArr.length == 2) {
                        String k = vArr[0].trim();
                        String v = vArr[1];
                        if (k.equals("source")) {
                            defineClass(instrumentation, v);
                            continue;
                        }
                        if (k.equals("visitor")) {
                            Class<TransformationVisitor> c = defineClass(instrumentation, v);
                            try {
                                visitors.add(c.newInstance());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            continue;
                        }
                        if (k.equals("appendBootClassPath")) {
                            try {
                                instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(v.trim(), false));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            continue;
                        }
                    }
                    throw new RuntimeException("bad arg: " + kv);
                } else
                for (String kv : codeSplit) {
                    String[] vArr = kv.split("::=");

                    if (vArr.length == 2) {
                        String k = vArr[0].trim();
                        String v = vArr[1];
                        if (k.equals("className")) {
                            className = v.trim();
                            add = true;
                        }
                        if (k.equals("methodName")) {
                            methodName = v.trim();
                            add = true;
                        }
                        if (k.equals("pre")) {
                            pre = v;
                            add = true;
                        }
                        if (k.equals("post")) {
                            post = v;
                            add = true;
                        }
                    } else {
                        throw new RuntimeException("bad args: " + agentArgument); 
                    }
                }
                
                if (add) {
                    codeWrapper.addEntry(className, methodName, pre, post);
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
        ArrayList<Class> ac = new ArrayList();
        
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

    private static Class systemDefineClass(Instrumentation instrumentation, String classPath, String className, byte[] bytecode) {
        try {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

            // Find the defineClass method in the ClassLoader class
            Method defineClassMethod = null;
                defineClassMethod = ClassLoader.class.getDeclaredMethod(
                        "defineClass", String.class, byte[].class, int.class, int.class);

            // Make the defineClass method accessible
            defineClassMethod.setAccessible(true);

            // Call defineClass method using reflection
            return (Class<?>) defineClassMethod.invoke(
                    systemClassLoader, null /* className.replaceAll('/', '.') */, bytecode, 0, bytecode.length);

//            // append to class path approach: (does not work)
//            //TODO: tested only for JDK11 at the moment.
//            Field mNativeAgent = instrumentation.getClass().getDeclaredField("mNativeAgent");
//            mNativeAgent.setAccessible(true);
//            long mna = (Long)mNativeAgent.get(instrumentation);
//            Method appendToClassLoaderSearch0 = instrumentation.getClass().getDeclaredMethod("appendToClassLoaderSearch0", long.class, String.class, boolean.class);
//            appendToClassLoaderSearch0.setAccessible(true);
//            //instrumentation.appendToBootstrapClassLoaderSearch(classPath); // <!-- need to create a jar file!
//            System.out.println(">>> mna=" + mna + ", instrumentation=" + instrumentation + ", classPath=" + classPath);
//            for (Class c : appendToClassLoaderSearch0.getParameterTypes()) {
//                System.out.println(">>> c=" + c);
//            }
//            appendToClassLoaderSearch0.invoke(instrumentation, mna, classPath, true); // <!-- IllegalArgumentException?
//            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void compile(String destination, String source) {
        try {
            if (System.getProperty("java.version").startsWith("1.")) {
                Class c = Class.forName("javax.tools.ToolProvider");
                Method m = c.getDeclaredMethod("getSystemJavaCompiler");
                m.setAccessible(true);
                Object javaCompiler = m.invoke(null);
                Method m2 = javaCompiler.getClass().getDeclaredMethod("run", InputStream.class, OutputStream.class, OutputStream.class, String[].class);
                m2.invoke(javaCompiler, null, System.out, System.err, new String[] {"-d", destination, source});
            } else {
                Class c = Class.forName("java.util.spi.ToolProvider");
                Method m = c.getDeclaredMethod("findFirst", String.class);
                m.setAccessible(true);
                Optional o = (Optional)m.invoke(null, "javac");
                Object javaCompiler = o.get();
                Method m2 = javaCompiler.getClass().getDeclaredMethod("run", PrintWriter.class, PrintWriter.class, String[].class);
                m2.invoke(javaCompiler, new PrintWriter(System.out), new PrintWriter(System.err), new String[] {"-d", destination, source});
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String findRegex(String regex, String content) {
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            throw new RuntimeException("Failed to find '" + regex + "' pattern in '" + content + "'");
        }
        return matcher.group(1);
    }

    public static Map<String, String> classPathPrepend = Collections.synchronizedMap(new HashMap<>());
    public static Class defineClass(Instrumentation instrumentation, String content) {
        try {
            String pkg = findRegex("\\bpackage\\s+([\\w.]+)\\s*;", content);
            String name = findRegex("\\bclass\\s+(\\w+)", content);
            Path tempDirectory = Files.createTempDirectory("sjbcp-defineClass-" + name);

            String source = tempDirectory + File.separator + name + ".java";
            Files.write(Paths.get(source), content.getBytes());
            compile(tempDirectory.toString(), source);
            classPathPrepend.put(tempDirectory.toString(), pkg);
            String classfile = tempDirectory + File.separator + pkg.replace('.', File.separatorChar) + File.separator + name + ".class";
            byte[] bytecode = Files.readAllBytes(Paths.get(classfile));
            return systemDefineClass(instrumentation, tempDirectory.toString(), name, bytecode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
