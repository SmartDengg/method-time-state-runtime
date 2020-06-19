package com.smartdengg.timestate.runtime;

import android.util.Log;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 创建时间: 2020/03/06 22:54 <br>
 * 作者: dengwei <br>
 * 描述: 负责统计函数的进入和退出，以及函数耗时的打印等工作
 */
public final class TimeStateLogger {

  //set by compile
  private static String TAG;

  private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");
  private static final Map<String, Map<String, Stack<Method>>> MAP = new HashMap<>();

  public static void entry(String enclosingMethodDescriptor, String descriptor,
      String declaringClassName, String name, String arguments, String returnType) {

    final String currentThread = currentThread().toString();

    Map<String, Stack<Method>> threadMethodsMap = MAP.get(currentThread);
    if (threadMethodsMap == null) {
      threadMethodsMap = new LinkedHashMap<>();
      MAP.put(currentThread, threadMethodsMap);
    }

    Stack<Method> enclosingMethodsStack = threadMethodsMap.get(enclosingMethodDescriptor);
    if (enclosingMethodsStack == null) {
      enclosingMethodsStack = new Stack<>();
      threadMethodsMap.put(enclosingMethodDescriptor, enclosingMethodsStack);
    }

    if (enclosingMethodDescriptor.equals(descriptor)) { // enclosing method start
      final Method enclosingMethod =
          new Method(descriptor, declaringClassName, name, arguments, returnType);
      enclosingMethod.entry = System.nanoTime();
      enclosingMethodsStack.push(enclosingMethod);
    } else {
      final Method enclosingMethod = enclosingMethodsStack.peek();
      final Method subMethod =
          new Method(descriptor, declaringClassName, name, arguments, returnType);
      subMethod.entry = System.nanoTime();
      enclosingMethod.add(subMethod.getDescriptor(), subMethod);
    }
  }

  public static void exit(String enclosingMethodDescriptor, String descriptor, String lineNumber) {

    final Method enclosingMethod = getEnclosingMethod(enclosingMethodDescriptor, false);

    if (enclosingMethodDescriptor.equals(descriptor)) {// enclosing method stop
      enclosingMethod.exit = System.nanoTime();
      enclosingMethod.lineNumber = lineNumber;
    } else {
      final Method subMethod = enclosingMethod.getSubMethods().get(descriptor);
      //noinspection ConstantConditions
      subMethod.exit = System.nanoTime();
    }
  }

  public static void log(String enclosingDescriptor) {

    final Method enclosingMethod = getEnclosingMethod(enclosingDescriptor, true);

    Log.d(TAG, DrawToolbox.TOP_BORDER);

    Log.d(TAG, DrawToolbox.HORIZONTAL_LINE + " " + currentThread());

    final String className = enclosingMethod.getDeclaringClassName();
    String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
    final Matcher matcher = ANONYMOUS_CLASS.matcher(simpleClassName);
    if (matcher.find()) simpleClassName = matcher.replaceAll("");

    final String enclosingInfo = className
        + "#"
        + enclosingMethod.getName()
        + "("
        + enclosingMethod.getArguments()
        + ")"
        + enclosingMethod.getReturnType()
        + " ("
        + simpleClassName
        + ".java:"
        + enclosingMethod.lineNumber
        + ")"
        + " ===> COST: "
        + calculateTime(enclosingMethod.entry, enclosingMethod.exit);

    Log.d(TAG, DrawToolbox.HORIZONTAL_LINE + " " + enclosingInfo);

    if (enclosingMethod.getSubMethods().size() != 0) {
      Log.d(TAG, DrawToolbox.MIDDLE_BORDER);

      final Collection<Method> methods = enclosingMethod.getSubMethods().values();
      for (Method method : methods) {

        final String subInfo = DrawToolbox.HORIZONTAL_LINE
            + "  ____/ "
            + method.getDeclaringClassName()
            + "#"
            + method.getName()
            + "("
            + method.getArguments()
            + ")"
            + method.getReturnType()
            + " ===> COST: "
            + calculateTime(method.entry, method.exit);
        Log.d(TAG, subInfo);
      }
    }

    Log.d(TAG, DrawToolbox.BOTTOM_BORDER);
  }

  @SuppressWarnings("ConstantConditions")
  private static Method getEnclosingMethod(String enclosingDescriptor, boolean isPop) {
    final String currentThread = currentThread().toString();
    final Map<String, Stack<Method>> threadMethodsMap = MAP.get(currentThread);
    final Stack<Method> enclosingMethodStack = threadMethodsMap.get(enclosingDescriptor);
    if (isPop) {
      return enclosingMethodStack.pop();
    }
    return enclosingMethodStack.peek();
  }

  private static Thread currentThread() {
    return Thread.currentThread();
  }

  private static String calculateTime(long start, long stop) {
    final long duration = stop - start;
    String cost;

    if (duration < 0) return "-1";

    if (duration >= 1_000_000) {
      cost = TimeUnit.NANOSECONDS.toMillis(duration) + "ms";
    } else {
      cost = TimeUnit.MICROSECONDS.toMillis(duration) + "μs";
    }
    return cost;
  }
}
