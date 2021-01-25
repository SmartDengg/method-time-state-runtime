package com.smartdengg.timestate.runtime;

import java.util.HashMap;
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
  private final Map<String, Method> internalCalls = new LinkedHashMap<>();
  String lineNumber;// only the enclose method has
  long entry;
  long exit;
  int count = 1;

  Method(String descriptor, String owner, String name, String arguments, String returnType) {
    this.descriptor = descriptor;
    this.owner = owner;
    this.name = name;
    this.arguments = arguments;
    this.returnType = returnType;
  }

  void batchIfNeeded(String descriptor, Method method) {
    final Method bathedMethod = internalCalls.get(descriptor);
    if (bathedMethod == null) {
      internalCalls.put(descriptor, method);
    } else {
      bathedMethod.count++;
    }
  }

  Method find(String descriptor) {
    return internalCalls.get(descriptor);
  }

  Map<String, Method> getInternalCalls() {
    return internalCalls;
  }

  String getDescriptor() {
    return descriptor;
  }

  String getOwner() {
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
    return internalCalls.size() != 0;
  }
}
