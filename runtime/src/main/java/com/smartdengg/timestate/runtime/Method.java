package com.smartdengg.timestate.runtime;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * 创建时间: 2020/03/06 22:58 <br>
 * 作者: dengwei <br>
 * 描述: 记录调用函数的信息
 */
class Method {

  String lineNumber;// only the enclose method has
  long entry;
  long exit;
  private String descriptor;
  private String owner;
  private String name;
  private String arguments;
  private String returnType;
  private Map<String, Queue<Method>> methods = new HashMap<>();

  Method(String descriptor, String owner, String name, String arguments, String returnType) {
    this.descriptor = descriptor;
    this.owner = owner;
    this.name = name;
    this.arguments = arguments;
    this.returnType = returnType;
  }

  void batch(String descriptor, Method method) {
    Queue<Method> methodQueue = methods.get(descriptor);
    if (methodQueue == null) {
      methodQueue = new LinkedList<>();
      methods.put(descriptor, methodQueue);
    }
    methodQueue.offer(method);
  }

  Method find(String descriptor) {
    //noinspection ConstantConditions
    return ((LinkedList<Method>) methods.get(descriptor)).peekLast();
  }

  Map<String, Queue<Method>> getMethods() {
    return methods;
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
    return methods.size() != 0;
  }
}
