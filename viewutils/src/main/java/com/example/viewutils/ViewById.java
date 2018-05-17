package com.example.viewutils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类描述：findViewById注解
 * 创建人：吴冬冬
 * 创建时间：2018/4/7 16:49
 */
//属性的位置，FIELD代表属性；METHOD代表方法；TYPE代表类上
@Target(ElementType.FIELD)
//什么时候生效，CLASS编译时 RUNTIME运行时 SOURCE源码时
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewById {
    int value();
}