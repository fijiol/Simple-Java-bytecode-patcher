# Simple-Java-bytecode-patcher
Simple Java bytecode patcher

You can run your application in next manner:
java -javaagent:./sjbcp.jar='className::=com/your/Class,,methdoName::=com.your.Class.getNumber(int),,pre::= System.out.println("Code to be called before method); ,,post::= System.out.println("Code to be called after method");'
