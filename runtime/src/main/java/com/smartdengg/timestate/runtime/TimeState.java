package com.smartdengg.timestate.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 创建时间: 2020/03/06 22:58 <br>
 * 作者: dengwei <br>
 * 描述: 计算单个函数耗时
 */
@Retention(RetentionPolicy.CLASS) @Target({ ElementType.CONSTRUCTOR, ElementType.METHOD })
public @interface TimeState {
}
