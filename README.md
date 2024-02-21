
# Simple-Java-bytecode-patcher

This project offers a lightweight implementation of the simplest Java bytecode patcher based on the Javassist library. It provides the ability to dynamically modify the bytecode of compiled classes at runtime.

## Usage

```
java -javaagent:sjbcp.jar=<params> -jar YOUR_APPLICATION.jar
```

### Params

In the `<params>` string, occurrences of `@@<file-name>` will be replaced with the content of `<file-name>` during the first step. The remaining `<params>` will be processed according to the following BNF:


```
  <params> ::= <params> ";;" <params> | <properties> | <append-boot-classpath> | <patch-method> | <new-class>
  <properties> ::= <properties> "," <properties> | <allowed-property-key> "=" <boolean-property-value>
  <allowed-property-key> ::= "traceClasses" | "traceMethods"
  <append-boot-classpath> ::= "appendBootClassPath" "::=" <dir-or-jar>
  <patch-method> ::= <patch-method> ",," <patch-method> | <class-to-patch-definition> | <method-to-patch-definition> | <prepend-code> | <append-code>
  <class-to-patch-definition> ::= "className" "::=" <class-name>
  <method-to-patch-definition> ::= "methodName" "::=" <method-name>
  <prepend-code> ::= "pre" "::=" <java-code>
  <append-code> ::= "post" "::=" <java-code>
  <new-class> ::= "source" "::=" <class-source-to-be-compiled-and-loaded-at-runtime>
```

## Build

To build the project, execute the following command, which will generate sjbcp.jar in the root of the cloned repository.

```
  mvn install
```

## Examples

### One line example

```
java -javaagent:./sjbcp.jar='className::=com/your/Class,,methdoName::=com.your.Class.getNumber(int),,pre::= System.out.println("Code to be called before method); ,,post::= System.out.println("Code to be called after method");' -jar YOURAPP.jar
```

### With configuration in a text file

A simple example demonstrating logging all stack traces and calls to a specific method using a configuration file.

Configuration file (`config.in`):

```
traceClasses=true,
traceMethods=true
;;

source ::=
  public class A {
    private static Map<String, Throwable> stackTraces;
    public static void logMethod(String method) {
      stackTraces.put(method, new RuntimeException("dummy exception, for stack logging purpose");
    }
    static {
      Runtime.getRuntime().addShutdownHook(() -> {
        for (Throwable t : stackTraces.valueSet()) {
          t.printStackTrace();
        }
      });
    }
  }
```

Application command line:
```
java -javaagent:./sjbcp.jar='@@config.in ;; className::=com/your/Class,,methdoName::=com.your.Class.getNumber(int),,pre::= A.logMethod("%METHOD%");' -jar YOURAPP.jar
```

