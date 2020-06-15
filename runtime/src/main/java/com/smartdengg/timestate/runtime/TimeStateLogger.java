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

  private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");
  private static final Map<String, Map<String, Stack<Invoke>>> MAP = new HashMap<>();

  private static String TAG;

  public static void entry(String encloseDescriptor, String descriptor, String className,
      String methodName, String arguments, String returnType) {

    final String currentThread = currentThread().toString();

    Map<String, Stack<Invoke>> invokeMap = MAP.get(currentThread);
    if (invokeMap == null) {
      invokeMap = new LinkedHashMap<>();
      MAP.put(currentThread, invokeMap);
    }

    Stack<Invoke> invokeStack = invokeMap.get(encloseDescriptor);
    if (invokeStack == null) {
      invokeStack = new Stack<>();
      invokeMap.put(encloseDescriptor, invokeStack);
    }

    if (encloseDescriptor.equals(descriptor)) { // enclosing method start
      final Invoke encloseInvoke =
          new Invoke(descriptor, className, methodName, arguments, returnType);
      encloseInvoke.entry = System.nanoTime();
      invokeStack.push(encloseInvoke);
    } else {
      final Invoke encloseInvoke = invokeStack.peek();
      final Invoke subInvoke = new Invoke(descriptor, className, methodName, arguments, returnType);
      subInvoke.entry = System.nanoTime();
      encloseInvoke.add(subInvoke.getDescriptor(), subInvoke);
    }
  }

  public static void exit(String encloseDescriptor, String descriptor, String lineNumber) {

    final Invoke encloseInvoke = getInvokes(encloseDescriptor).peek();

    if (encloseDescriptor.equals(descriptor)) {// enclosing method stop
      encloseInvoke.exit = System.nanoTime();
      encloseInvoke.lineNumber = lineNumber;
    } else {
      final Invoke subInvoke = encloseInvoke.getSubInvokes().get(descriptor);
      //noinspection ConstantConditions
      subInvoke.exit = System.nanoTime();
    }
  }

  public static void log(String encloseDescriptor) {

    final Invoke encloseInvoke = getInvokes(encloseDescriptor).pop();

    Log.d(TAG, DrawToolbox.TOP_BORDER);

    Log.d(TAG, DrawToolbox.HORIZONTAL_LINE + " " + currentThread());

    final String className = encloseInvoke.getClassName();
    String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
    final Matcher matcher = ANONYMOUS_CLASS.matcher(simpleClassName);
    if (matcher.find()) simpleClassName = matcher.replaceAll("");

    final String basic = className
        + "#"
        + encloseInvoke.getMethodName()
        + "("
        + encloseInvoke.getArguments()
        + ")"
        + encloseInvoke.getReturnType()
        + " ("
        + simpleClassName
        + ".java:"
        + encloseInvoke.lineNumber
        + ")"
        + " ===> COST: "
        + calculateTime(encloseInvoke.entry, encloseInvoke.exit);

    Log.d(TAG, DrawToolbox.HORIZONTAL_LINE + " " + basic);

    if (encloseInvoke.getSubInvokes().size() != 0) {
      Log.d(TAG, DrawToolbox.MIDDLE_BORDER);

      final Collection<Invoke> values = encloseInvoke.getSubInvokes().values();
      for (Invoke invoke : values) {

        final String log = DrawToolbox.HORIZONTAL_LINE
            + "  ____/ "
            + invoke.getClassName()
            + "#"
            + invoke.getMethodName()
            + "("
            + invoke.getArguments()
            + ")"
            + invoke.getReturnType()
            + " ===> COST: "
            + calculateTime(invoke.entry, invoke.exit);
        Log.d(TAG, log);
      }
    }

    Log.d(TAG, DrawToolbox.BOTTOM_BORDER);
  }

  @SuppressWarnings("ConstantConditions")
  private static Stack<Invoke> getInvokes(String encloseDescriptor) {
    final String currentThread = currentThread().toString();
    final Map<String, Stack<Invoke>> invokeMap = MAP.get(currentThread);
    return invokeMap.get(encloseDescriptor);
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
