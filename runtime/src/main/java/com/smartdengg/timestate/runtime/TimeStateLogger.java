package com.smartdengg.timestate.runtime;

import android.util.Log;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 创建时间: 2020/03/06 22:54 <br>
 * 作者: dengwei <br>
 * 描述: 记录函数的进入和退出，并打印函数的耗时
 */
public final class TimeStateLogger {

  private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");
  private static final ThreadLocal<LinkedList<Method>> threadLocal = new ThreadLocal<>();

  //set by compile
  private static String TAG;
  private static boolean SUPPORT_EMOJI;

  /**
   * 函数的进入
   *
   * @param isEnclosing 是否为顶层函数调用
   * @param descriptor 函数描述符，eg: onCreate/android.os.Bundle/void
   */
  public static void entry(boolean isEnclosing, String descriptor) {

    final long time = System.nanoTime();

    final Deque<Method> stackTrace = getOrCreateThreadStackTrace();

    final String[] result = descriptor.split("/");
    final String owner = result[0];
    final String name = result[1];
    final String arguments = result[2];
    final String returnType = result[3];

    final Method method = Method.create(descriptor, owner, name, arguments, returnType);
    method.entryTimestamp = time;

    if (isEnclosing) {// enclosing method entry
      stackTrace.offerFirst(method);
    } else {// find enclosing method
      stackTrace.peek().batch(descriptor, method);
    }
  }

  /**
   * 函数的退出
   *
   * @param isEnclosing 是否为顶层函数调用
   * @param descriptor 函数描述符
   * @param lineNumber 函数调用所在行号，只有顶层函数才会拥有这个值
   */
  public static void exit(boolean isEnclosing, String descriptor, String lineNumber) {

    final long timestamp = System.nanoTime();

    final Deque<Method> stackTrace = getOrCreateThreadStackTrace();
    final Method topMethod = stackTrace.peek();
    if (isEnclosing) {// enclosing method exit
      topMethod.exitTimestamp = timestamp;
      topMethod.lineNumber = lineNumber;
    } else {
      topMethod.find(descriptor).exitTimestamp = timestamp;
    }
  }

  public static void log(String owner) {

    final StringBuilder log = new StringBuilder();
    append(log, "");
    append(log, DrawToolbox.TOP_BORDER);
    append(log, DrawToolbox.HORIZONTAL_LINE + " " + currentThread());

    final Method enclosingMethod = getOrCreateThreadStackTrace().poll();
    String simpleClassName = owner.substring(owner.lastIndexOf(".") + 1);
    final Matcher matcher = ANONYMOUS_CLASS.matcher(simpleClassName);
    if (matcher.find()) simpleClassName = matcher.replaceAll("");

    final String enclosingInfo = owner
            + "#"
            + enclosingMethod.getName()
            + "("
            + enclosingMethod.getArguments()
            + "):"
            + enclosingMethod.getReturnType()
            + "("
            + simpleClassName
            + ".java:"
            + enclosingMethod.lineNumber
            + ")"
            + " ===> COST:"
            + calculateTime(enclosingMethod.exitTimestamp - enclosingMethod.entryTimestamp);

    append(log, DrawToolbox.HORIZONTAL_LINE + " " + enclosingInfo);

    if (enclosingMethod.hasMethods()) {

      append(log, DrawToolbox.MIDDLE_BORDER);

      for (Map.Entry<String, Method> entry : enclosingMethod.getOutingCalls().entrySet()) {

        final Method method = entry.getValue();

        String info = DrawToolbox.HORIZONTAL_LINE
                + "  ____/ "
                + method.getOwner()
                + "#"
                + method.getName()
                + "("
                + method.getArguments()
                + "):"
                + method.getReturnType();

        if (method.count > 1) {
          info += " * " + method.count;
        }

        append(log, info + " ===> COST:" + calculateTime(method.exitTimestamp - method.entryTimestamp));
      }
    }
    append(log, DrawToolbox.BOTTOM_BORDER);
    Log.d(TAG, log.toString());
  }

  private static Thread currentThread() {
    return Thread.currentThread();
  }

  private static String calculateTime(long duration) {
    if (duration < 0) {
      return SUPPORT_EMOJI ? "-1ms \u2620" : "-1ms";
    } else if (duration >= 1_000_000) {
      long t = TimeUnit.NANOSECONDS.toMillis(duration);
      return t >= 10 ? t + (SUPPORT_EMOJI ? "ms \u26C4" : "ms")
              : t + (SUPPORT_EMOJI ? "ms \u2618" : "ms");
    } else {
      return TimeUnit.MICROSECONDS.toMillis(duration) + (SUPPORT_EMOJI ? "μs \u26A1" : "μs");
    }
  }

  private static Deque<Method> getOrCreateThreadStackTrace() {
    LinkedList<Method> currentThreadMethodStack = threadLocal.get();
    if (currentThreadMethodStack == null) {
      currentThreadMethodStack = new LinkedList<>();
      threadLocal.set(currentThreadMethodStack);
    }
    return currentThreadMethodStack;
  }

  private static StringBuilder append(StringBuilder stringBuilder, String message) {
    return stringBuilder.append("\t\t").append(message).append("\n");
  }
}
