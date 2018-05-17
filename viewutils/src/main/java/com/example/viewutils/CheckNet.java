package com.example.viewutils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类描述：CheckNet检测网络注解
 * 创建人：吴冬冬
 * 创建时间：2018/4/7 16:49
 */
@Target(ElementType.METHOD)//属性的位置，FIELD代表属性；METHOD代表方法；TYPE代表类上
@Retention(RetentionPolicy.RUNTIME)//什么时候生效，CLASS编译时 RUNTIME运行时 SOURCE源码时
public @interface CheckNet {
}