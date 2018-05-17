package com.example.viewutils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 类描述：findViewById 和 setOnClick 方法
 * 创建人：吴冬冬
 * 创建时间：2018/4/7 17:00
 */
public class ViewUtils {

    /**
     * 注入方法
     */
    public static void inject(View view) {
        inject(new ViewFinder(view), view);
    }

    /**
     * 注入方法
     */
    public static void inject(Activity activity) {
        inject(new ViewFinder(activity), activity);
    }

    /**
     * 注入方法
     */
    public static void inject(View view, Object object) {
        inject(new ViewFinder(view), object);
    }

    /**
     * @param viewFinder
     * @param object     反射需要执行的类
     */
    private static void inject(ViewFinder viewFinder, Object object) {
        //注入属性
        injectFiled(viewFinder, object);
        //注入事件
        injectEvent(viewFinder, object);
    }

    /**
     * 注入事件
     */
    private static void injectEvent(ViewFinder viewFinder, Object object) {
        //1.获取类中所有方法
        Class<?> aClass = object.getClass();
        Method[] methods = aClass.getDeclaredMethods();

        //2.获取OnClick中的Value值
        for (Method method : methods) {
            OnClick onClick = method.getAnnotation(OnClick.class);
            if (onClick == null) {
                continue;
            }

            //可能多个也可能一个
            int[] values = onClick.value();
            if (values == null) {
                continue;
            }
            for (int value : values) {
                //3.findViewById找到View
                //通过辅助类ViewFinder中的findViewById方法找到View
                View view = viewFinder.findViewById(value);
                if (view == null) {
                    continue;
                }

                boolean isCheckNet = method.getAnnotation(CheckNet.class) != null;

                //4.View.setOnClickListener
                view.setOnClickListener(new DeclaredOnClickListener(method, object, isCheckNet));
            }
        }
    }


    private static class DeclaredOnClickListener implements View.OnClickListener {

        private boolean mIsCheckNet;
        private Method mMethod;
        private Object mObject;

        /**
         * @param method     方法
         * @param object     谁要去执行
         * @param isCheckNet 是否要检测网络
         */
        public DeclaredOnClickListener(Method method, Object object, boolean isCheckNet) {
            mMethod = method;
            mObject = object;
            mIsCheckNet = isCheckNet;
        }

        @Override
        public void onClick(View v) {
            //判断点击事件是否需要检测网络
            if (mIsCheckNet) {
                //如果需要网络，则需要判断当前设备是否有网络
                if (!networkAvailable(v.getContext())) {
                    //当前无网络打印toast
                    Toast.makeText(v.getContext(), "当前无网络服务！", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            //5.反射注入方法
            mMethod.setAccessible(true);
            try {
                //先调用有参数的方法，如果没有再调用无参数方法
                mMethod.invoke(mObject, v);
            } catch (Exception e) {
//                e.printStackTrace();
                try {
                    mMethod.invoke(mObject, null);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    }


    /**
     * 注入属性
     */
    private static void injectFiled(ViewFinder viewFinder, Object object) {
        //1.获取类中所有属性
        Class<?> aClass = object.getClass();
        //获取所有属性
        Field[] fields = aClass.getDeclaredFields();

        //2.获取ViewById中的value中的值
        for (Field field : fields) {
            ViewById viewById = field.getAnnotation(ViewById.class);
            if (viewById == null) {
                continue;
            }
            //获取注解中的id值
            int value = viewById.value();

            //3.通过findViewById找到View
            View view = viewFinder.findViewById(value);
            if (view == null) {
                continue;
            }

            //4.动态的注入找到的View
            field.setAccessible(true);
            try {
                field.set(object, view);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 判断当前是否有网络
     */
    private static boolean networkAvailable(Context context) {
        //得到链接管理器对象
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            @SuppressLint("MissingPermission")
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
