package com.smartdengg.timestate.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 创建时间: 2020/03/06 22:58 <br>
 * 作者: dengwei <br>
 * 描述: 记录调用函数的信息
 */
class Method {

  private final String descriptor;
  private final String owner;
  private final String name;
  private final String arguments;
  private final String returnType;
  private final Map<String, Method> outgoingCalls = new LinkedHashMap<>();
  String lineNumber;// only the enclose method has
  long entryTimestamp;
  long exitTimestamp;
  int count = 1;

  public static Method create(String descriptor, String owner, String name, String arguments, String returnType) {
    return new Method(descriptor, owner, name, arguments, returnType);
  }

  private Method(String descriptor, String owner, String name, String arguments, String returnType) {
    this.descriptor = descriptor;
    this.owner = owner;
    this.name = name;
    this.arguments = arguments;
    this.returnType = returnType;
  }

  void batch(String descriptor, Method method) {
    final Method bathedMethod = outgoingCalls.get(descriptor);
    if (bathedMethod == null) {
      outgoingCalls.put(descriptor, method);
    } else {
      bathedMethod.count++;
    }
  }

  Method find(String descriptor) {
    return outgoingCalls.get(descriptor);
  }

  Map<String, Method> getOutingCalls() {
    return outgoingCalls;
  }

  String getDescriptor() {
    return descriptor;
  }

  public String getOwner() {
    return owner;
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

  boolean hasMethods() {
    return outgoingCalls.size() != 0;
  }
}
