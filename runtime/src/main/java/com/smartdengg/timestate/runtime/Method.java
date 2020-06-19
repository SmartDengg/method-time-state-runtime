package com.smartdengg.timestate.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 创建时间: 2020/03/06 22:58 <br>
 * 作者: dengwei <br>
 * 描述: 记录调用函数的信息
 */
class Method {

  private String descriptor;
  private String declaringClassName;
  private String name;
  private String arguments;
  private String returnType;
  String lineNumber;// only the enclose method has
  long entry;
  long exit;

  private Map<String, Method> subMethods = new LinkedHashMap<>();

  Method(String descriptor, String declaringClassName, String name, String arguments,
      String returnType) {
    this.descriptor = descriptor;
    this.declaringClassName = declaringClassName;
    this.name = name;
    this.arguments = arguments;
    this.returnType = returnType;
  }

  @SuppressWarnings("SameParameterValue") void add(String descriptor, Method method) {
    subMethods.put(descriptor, method);
  }

  Map<String, Method> getSubMethods() {
    return subMethods;
  }

  String getDescriptor() {
    return descriptor;
  }

  String getDeclaringClassName() {
    return declaringClassName;
  }

  String getName() {
    return name;
  }

  String getArguments() {
    return arguments;
  }

  String getReturnType() {
    return returnType;
  }
}
