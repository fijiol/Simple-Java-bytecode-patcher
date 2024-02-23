package org.sjbcp.code;

import java.security.ProtectionDomain;

public interface TransformationVisitor {
    byte[] beforeTransformation(ClassLoader loader, String className, Class clazz, ProtectionDomain domain, byte[] bytes);
    byte[] afterTransformation(ClassLoader loader, String className, Class clazz, ProtectionDomain domain, byte[] bytes);

}
