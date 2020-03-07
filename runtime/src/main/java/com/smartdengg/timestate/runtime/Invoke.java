package com.smartdengg.timestate.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 创建时间: 2020/03/06 22:58 <br>
 * 作者: dengwei <br>
 * 描述: 记录调用函数的信息
 */
public class Invoke {

  private String descriptor;
  private String className;
  private String methodName;
  private String arguments;
  private String returnType;
  String lineNumber;// only the enclose method has
  long entry;
  long exit;

  private Map<String, Invoke> subInvokes = new LinkedHashMap<>();

  Invoke(String descriptor, String className, String methodName, String arguments,
      String returnType) {
    this.descriptor = descriptor;
    this.className = className;
    this.methodName = methodName;
    this.arguments = arguments;
    this.returnType = returnType;
  }

  @SuppressWarnings("SameParameterValue") void add(String descriptor, Invoke invoke) {
    subInvokes.put(descriptor, invoke);
  }

  public Map<String, Invoke> getSubInvokes() {
    return subInvokes;
  }

  public String getDescriptor() {
    return descriptor;
  }

  public String getClassName() {
    return className;
  }

  public String getMethodName() {
    return methodName;
  }

  public String getArguments() {
    return arguments;
  }

  public String getReturnType() {
    return returnType;
  }
}
