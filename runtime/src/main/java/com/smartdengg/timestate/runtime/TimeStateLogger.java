package com.smartdengg.timestate.runtime;

import android.util.Log;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 创建时间: 2020/03/06 22:54 <br>
 * 作者: dengwei <br>
 * 描述: 记录函数的进入和退出，并打印函数的耗时
 */
@SuppressWarnings("ConstantConditions") public final class TimeStateLogger {

  private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");
  private static final ThreadLocal<LinkedList<Method>> threadLocal = new ThreadLocal<>();

  //set by compile
  private static String TAG;

  public static void entry(boolean isEnclosing, String descriptor) {

    final long time = System.nanoTime();

    final LinkedList<Method> stackTrace = getThreadStackTraceOrCreate();

    final String[] res = descriptor.split("/");
    final String owner = res[0];
    final String name = res[1];
    final String arguments = res[2];
    final String returnType = res[3];

    if (isEnclosing) {// enclosing method entry
      final Method enclosingMethod = new Method(descriptor, owner, name, arguments, returnType);
      enclosingMethod.entry = time;
      stackTrace.addFirst(enclosingMethod);
    } else {
      final Method method = new Method(descriptor, owner, name, arguments, returnType);
      method.entry = System.nanoTime();
      // find enclosing method
      stackTrace.peekFirst().batch(method);
    }
  }

  public static void exit(boolean isEnclosing, String descriptor, String lineNumber) {

    final long time = System.nanoTime();

    final LinkedList<Method> stackTrace = getThreadStackTraceOrCreate();
    final Method topMethod = stackTrace.peekFirst();
    if (isEnclosing) {// enclosing method exit
      topMethod.exit = time;
      topMethod.lineNumber = lineNumber;
    } else {
      topMethod.find(descriptor).exit = time;
    }
  }

  public static void log() {

    Log.d(TAG, DrawToolbox.TOP_BORDER);
    Log.d(TAG, DrawToolbox.HORIZONTAL_LINE + " " + currentThread());

    final Method enclosingMethod = getThreadStackTraceOrCreate().pollFirst();
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
        + calculateTime(enclosingMethod.exit - enclosingMethod.entry);

    Log.d(TAG, DrawToolbox.HORIZONTAL_LINE + " " + enclosingInfo);

    if (enclosingMethod.hasMethods()) {

      Log.d(TAG, DrawToolbox.MIDDLE_BORDER);

      for (Map.Entry<String, Queue<Method>> entry : enclosingMethod.getCalls().entrySet()) {

        final Queue<Method> methods = entry.getValue();
        final Method method = methods.poll();

        long cost = method.exit - method.entry;
        int count = 1;
        while (!methods.isEmpty()) {
          final Method m = methods.poll();
          cost += m.exit - m.entry;
          count++;
        }

        String info = DrawToolbox.HORIZONTAL_LINE
            + "  ____/ "
            + method.getOwner()
            + "#"
            + method.getName()
            + "("
            + method.getArguments()
            + "):"
            + method.getReturnType();

        if (count != 1) {
          info += " * " + count;
        }

        info += " ===> COST:" + calculateTime(cost);

        Log.d(TAG, info);
      }
    }

    Log.d(TAG, DrawToolbox.BOTTOM_BORDER);
  }

  private static Thread currentThread() {
    return Thread.currentThread();
  }

  private static String calculateTime(long duration) {
    if (duration < 0) {
      return "-1ms \u2620";
    } else if (duration >= 1_000_000) {
      long t = TimeUnit.NANOSECONDS.toMillis(duration);
      return t >= 10 ? t + "ms \u26C4" : t + "ms \u2618";
    } else {
      return TimeUnit.MICROSECONDS.toMillis(duration) + "μs \u26A1";
    }
  }

  private static LinkedList<Method> getThreadStackTraceOrCreate() {
    LinkedList<Method> currentThreadMethodStack = threadLocal.get();
    if (currentThreadMethodStack == null) {
      currentThreadMethodStack = new LinkedList<>();
      threadLocal.set(currentThreadMethodStack);
    }
    return currentThreadMethodStack;
  }
}
