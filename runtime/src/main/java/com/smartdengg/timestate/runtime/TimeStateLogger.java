package com.smartdengg.timestate.runtime;

import android.util.Log;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 创建时间: 2020/03/06 22:54 <br>
 * 作者: dengwei <br>
 * 描述: 记录函数的进入和退出，并打印函数的耗时
 */
@SuppressWarnings("ConstantConditions")
public final class TimeStateLogger {

  private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");
  private static final ThreadLocal<LinkedList<Method>> threadLocal = new ThreadLocal<>();

  //set by compile
  private static String TAG;

  public static void entry(boolean isEnclosing, String descriptor) {

    final LinkedList<Method> threadMethodStack = getThreadMethodStackOrCreate();

    final String[] res = descriptor.split("/");
    final String owner = res[0];
    final String name = res[1];
    final String arguments = res[2];
    final String returnType = res[3];

    if (isEnclosing) {// enclosing method start
      final Method enclosingMethod = new Method(descriptor, owner, name, arguments, returnType);
      enclosingMethod.entry = System.nanoTime();
      threadMethodStack.addFirst(enclosingMethod);
    } else {
      final Method method = new Method(descriptor, owner, name, arguments, returnType);
      method.entry = System.nanoTime();
      threadMethodStack.peekFirst().add(method.getDescriptor(), method);
    }
  }

  public static void exit(boolean isEnclosing, String descriptor, String lineNumber) {

    final LinkedList<Method> threadMethodStack = getThreadMethodStackOrCreate();
    final Method enclosingMethod = threadMethodStack.peekFirst();
    if (isEnclosing) {// enclosing method stop
      enclosingMethod.exit = System.nanoTime();
      enclosingMethod.lineNumber = lineNumber;
    } else {
      final Method subMethod = enclosingMethod.getMethods().get(descriptor);
      subMethod.exit = System.nanoTime();
    }
  }

  public static void log() {

    final Method enclosingMethod = getThreadMethodStackOrCreate().pollFirst();

    Log.d(TAG, DrawToolbox.TOP_BORDER);

    Log.d(TAG, DrawToolbox.HORIZONTAL_LINE + " " + currentThread());

    final String className = enclosingMethod.getOwner();
    String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
    final Matcher matcher = ANONYMOUS_CLASS.matcher(simpleClassName);
    if (matcher.find()) simpleClassName = matcher.replaceAll("");

    final String enclosingInfo = className
        + "#"
        + enclosingMethod.getName()
        + "("
        + enclosingMethod.getArguments()
        + "):"
        + enclosingMethod.getReturnType()
        + " ("
        + simpleClassName
        + ".java:"
        + enclosingMethod.lineNumber
        + ")"
        + " ===> COST:"
        + calculateTime(enclosingMethod.entry, enclosingMethod.exit);

    Log.d(TAG, DrawToolbox.HORIZONTAL_LINE + " " + enclosingInfo);

    if (enclosingMethod.getMethods().size() != 0) {

      Log.d(TAG, DrawToolbox.MIDDLE_BORDER);

      final Collection<Method> methods = enclosingMethod.getMethods().values();
      for (Method method : methods) {

        final String subInfo =
            DrawToolbox.HORIZONTAL_LINE
                + "  ____/ "
                + method.getOwner()
                + "#"
                + method.getName()
                + "("
                + method.getArguments()
                + "):"
                + method.getReturnType()
                + " ===> COST:"
                + calculateTime(method.entry, method.exit);
        Log.d(TAG, subInfo);
      }
    }

    Log.d(TAG, DrawToolbox.BOTTOM_BORDER);
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

  private static LinkedList<Method> getThreadMethodStackOrCreate() {
    LinkedList<Method> currentThreadMethodStack = threadLocal.get();
    if (currentThreadMethodStack == null) {
      currentThreadMethodStack = new LinkedList<>();
      threadLocal.set(currentThreadMethodStack);
    }
    return currentThreadMethodStack;
  }
}
