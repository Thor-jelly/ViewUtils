# IOC注解框架

[TOC]

> 模仿xutils3和butterKnife两个注解框架，主要是模仿xutils3这个注解框架。
> 下面是两篇关于注解的知识点(引用)：
> [秒懂，Java 注解 （Annotation）你可以这样学](https://blog.csdn.net/briblue/article/details/73824058)
> [轻松学，Java 中的代理模式及动态代理](https://blog.csdn.net/briblue/article/details/73928350) 
> 我的模仿框架在最后有写怎么实现的

# 我的IOC使用说明
- ViewById：是findViewById注解
- OnClick：是设置点击事件注解
- CheckNet：是在点击事件的时候添加是否检测网络的注解

# 了解XUtils3使用和源码一点解析

## 新建项目在依赖中添加compile 'org.xutils:xutils:3.5.0'  

1. 初始化  

	```
		官方方法   
		在onCreat方法中使用  
		x.Ext.init(this);  
		x.Ext.setDebug(BuildConfig.DEBUG);//是否输出日志  
	```

	其实我们通过源码可以看到直接调用view()方法是一样的效果  

	```
	public static ViewInjector view() {
        if (Ext.viewInjector == null) {
            ViewInjectorImpl.registerInstance();
        }
        return Ext.viewInjector;
    }
	```
	调用其view方法就可以实现findViewById注解方法初始化

2. 在跳转到ViewInjectorImpl类中我们可以了解到，基本方法为inject就可以实现注解导入

	```
		也就是说我们可以在需要注解的地方直接调用
		x.view().inject(参数);这个方式实现注入。
	```

3. 怎么实现id和控件绑定

	```
		@ViewInject(R.id.tv)
		private TextView mTv;
	```
	通过上面就可以实现控件绑定

4. 怎么实现控件点击事件

	```
		@Event(value=R.id.tv)
		private void onMyTvOnClick(View view){
		//点击事件处理
	}
	```
	
	**官网给的使用方法是：**

	```
	 1.方法必须私有限定,
	 2.方法参数形式必须和type对应的Listener接口一致.
	 3.注解参数value支持数组: value={id1, id2, id3}
	 4.其它参数说明见{@link org.xutils.event.annotation.Event}类的说明.  
		@Event(value = R.id.btn_test_baidu1,
		        type = View.OnClickListener.class/*可选参数, 默认是View.OnClickListener.class*/)
		private void onTestBaidu1Click(View view) {
		...
		}
	```

## XUtils3源码简单了解

**主要了解inject的方法**
官方给了4中注入的方式

### view注入

```
	@Override
    public void inject(View view) {
		//通过injectObject(内部通过反射的方法获取控件，并设置点击事件)分别加入
		//1.当前view
		//2.当前view对应的class
		//3.保存view对象，通过内部方法发现其view
        injectObject(view, view.getClass(), new ViewFinder(view));
    }
```

### 在activity中注入

```注入activity
    @Override
    public void inject(Activity activity) {
        //获取Activity的ContentView的注解
        Class<?> handlerType = activity.getClass();
        try {
			/*
				在activity中第一个注解,ContentView(添加布局文件，可以取消setContentView()方法，通过注解方式摄入)
			*/
            ContentView contentView = findContentView(handlerType);
            if (contentView != null) {
                int viewId = contentView.value();
                if (viewId > 0) {
                    Method setContentViewMethod = handlerType.getMethod("setContentView", int.class);
                    setContentViewMethod.invoke(activity, viewId);
                }
            }
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }

        injectObject(activity, handlerType, new ViewFinder(activity));
    }

```

### handler中注入和fragment中注入方式

```
    @Override
    public void inject(Object handler, View view) {
        injectObject(handler, handler.getClass(), new ViewFinder(view));
    }

    @Override
    public View inject(Object fragment, LayoutInflater inflater, ViewGroup container) {
        // inject ContentView
        View view = null;
        Class<?> handlerType = fragment.getClass();
        try {
            ContentView contentView = findContentView(handlerType);
            if (contentView != null) {
                int viewId = contentView.value();
                if (viewId > 0) {
                    view = inflater.inflate(viewId, container, false);
                }
            }
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }

        // inject res & event
        injectObject(fragment, handlerType, new ViewFinder(view));

        return view;
    }
```

### 反射获取view和点击事件

```
private static void injectObject(Object handler, Class<?> handlerType, ViewFinder finder) {

        if (handlerType == null || IGNORED.contains(handlerType)) {
            return;
        }

        // 从父类到子类递归
        injectObject(handler, handlerType.getSuperclass(), finder);

        // inject view 获取自己声明的字段如Lpublic,private,protected,默认。
        Field[] fields = handlerType.getDeclaredFields();
        if (fields != null && fields.length > 0) {
            for (Field field : fields) {

				//获取当前变量类型
                Class<?> fieldType = field.getType();
                if (
                /* 不注入静态字段 */     Modifier.isStatic(field.getModifiers()) ||
                /* 不注入final字段 */    Modifier.isFinal(field.getModifiers()) ||
                /* 不注入基本类型字段 */  fieldType.isPrimitive() ||
                /* 不注入数组类型字段 */  fieldType.isArray()) {
                    continue;
                }

				//获取成员变量上的注解，要是ViewInject的才需要处理
                ViewInject viewInject = field.getAnnotation(ViewInject.class);
                if (viewInject != null) {
                    try {
						//通过findViewById获取view
                        View view = finder.findViewById(viewInject.value(), viewInject.parentId());
                        if (view != null) {
                            field.setAccessible(true);
							//反射注入属性
                            field.set(handler, view);
                        } else {
                            throw new RuntimeException("Invalid @ViewInject for "
                                    + handlerType.getSimpleName() + "." + field.getName());
                        }
                    } catch (Throwable ex) {
                        LogUtil.e(ex.getMessage(), ex);
                    }
                }
            }
        } // end inject view

        // inject event 点击事件反射
        Method[] methods = handlerType.getDeclaredMethods();
        if (methods != null && methods.length > 0) {
            for (Method method : methods) {

                if (Modifier.isStatic(method.getModifiers())
                        || !Modifier.isPrivate(method.getModifiers())) {
                    continue;
                }

                //检查当前方法是否是event注解的方法
                Event event = method.getAnnotation(Event.class);
                if (event != null) {
                    try {
                        // id参数
                        int[] values = event.value();
                        int[] parentIds = event.parentId();
                        int parentIdsLen = parentIds == null ? 0 : parentIds.length;
                        //循环所有id，生成ViewInfo并添加代理反射
                        for (int i = 0; i < values.length; i++) {
                            int value = values[i];
                            if (value > 0) {
                                ViewInfo info = new ViewInfo();
                                info.value = value;
                                info.parentId = parentIdsLen > i ? parentIds[i] : 0;
                                method.setAccessible(true);
								//内部使用了动态代理的方式实现点击事件
                                EventListenerManager.addEventMethod(finder, info, event, handler, method);
                            }
                        }
                    } catch (Throwable ex) {
                        LogUtil.e(ex.getMessage(), ex);
                    }
                }
            }
        } // end inject event

    }
```

**对于动态代理还不熟就不详细深入进去了，有兴趣的可以点击进去看看源码**

# 了解ButterKnife使用和源码一点解析

## 添加依赖

```
dependencies {
  implementation 'com.jakewharton:butterknife:8.8.1'
  annotationProcessor 'com.jakewharton:butterknife-compiler:8.8.1'
}
```

## 初始化

```
ButterKnife.bind(this);

@BindView(R.id.one_tv)
TextView mOneTv;


@OnClick(R.id.one_tv)
public void onViewClicked() {
}
```

## 简单源码解析

> 源码我理解的不太透彻，就先引用两篇文章，等了解透彻了之后有时间再补。

- [ButterKnife源码分析(一)](https://blog.csdn.net/zmtnt0709/article/details/61616525)
- [ButterKnife源码分析(二)](https://blog.csdn.net/zmtnt0709/article/details/64680448)

# 自己模仿一个(XUtils3),butterKnife中自己生成源码过程还不了解

## 1.生成一个ViewById注解类

```
/**
 * 类描述：findViewById注解
 * 创建人：吴冬冬
 * 创建时间：2018/4/7 16:49
 */
@Target(ElementType.FIELD)//属性的位置，FIELD代表属性；METHOD代表方法；TYPE代表类上
@Retention(RetentionPolicy.RUNTIME)//什么时候生效，CLASS编译时 RUNTIME运行时 SOURCE源码时
public @interface ViewById {
    int value();
}
```

## 2.先定义出findViewById 反射注解方法

**ViewFinder暂时没有用，为了后面兼容而添加的**

```
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
                return;
            }
            //获取注解中的id值
            int value = viewById.value();

            //3.通过findViewById找到View
            View view = viewFinder.findViewById(value);
            if (view == null) {
                return;
            }

            //4.动态的注入找到的View
            field.setAccessible(true);
            try {
                field.set(object, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }
    }
}
```

## 3.设置OnClick事件注入方法

### 先创建一个view点击事件注解

```
/**
 * 类描述：OnClickd注解
 * 创建人：吴冬冬
 * 创建时间：2018/4/7 16:49
 */
@Target(ElementType.METHOD)//属性的位置，FIELD代表属性；METHOD代表方法；TYPE代表类上
@Retention(RetentionPolicy.RUNTIME)//什么时候生效，CLASS编译时 RUNTIME运行时 SOURCE源码时
public @interface OnClick {
    int[] value();//这里数组型表示可以配置多个
}
```

### 通过反射注解方式设置点击事件

```
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
            return;
        }

        //可能多个也可能一个
        int[] values = onClick.value();
        if (values == null) {
            return;
        }
        for (int value : values) {
            //3.findViewById找到View
            //通过辅助类ViewFinder中的findViewById方法找到View
            View view = viewFinder.findViewById(value);
            if (view == null) {
                return;
            }

            //4.View.setOnClickListener
            view.setOnClickListener(new DeclaredOnClickListener(method, object));
        }
    }
}


private static class DeclaredOnClickListener implements View.OnClickListener {

    private Method mMethod;
    private Object mObject;

    /**
     * @param method 方法
     * @param object 谁要去执行
     */
    public DeclaredOnClickListener(Method method, Object object) {
        mMethod = method;
        mObject = object;
    }

    @Override
    public void onClick(View v) {
        //5.反射注入方法
        mMethod.setAccessible(true);
        try {
            //先调用有参数的方法，如果没有再调用无参数方法
            mMethod.invoke(mObject, v);
        } catch (Exception e) {
            //e.printStackTrace();
            try {
                mMethod.invoke(mObject, null);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}
```

## 4.为点击事件添加检测网络方法

### 创建注解类

```
/**
 * 类描述：CheckNet检测网络注解
 * 创建人：吴冬冬
 * 创建时间：2018/4/7 16:49
 */
@Target(ElementType.METHOD)//属性的位置，FIELD代表属性；METHOD代表方法；TYPE代表类上
@Retention(RetentionPolicy.RUNTIME)//什么时候生效，CLASS编译时 RUNTIME运行时 SOURCE源码时
public @interface CheckNet {
}
```

### 在执行点击事件反射方法前，先判断是否有网络

```
//首先获得注解，判断点击事件方法中是否添加了改注解
 boolean isCheckNet = method.getAnnotation(CheckNet.class) != null;

//然后再点击事件中添加判断是否有网络的方法
//判断点击事件是否需要检测网络
if (mIsCheckNet) {
    //如果需要网络，则需要判断当前设备是否有网络
    if (!networkAvailable(v.getContext())) {
        //当前无网络打印toast
        Toast.makeText(v.getContext(), "当前无网络服务！", Toast.LENGTH_SHORT).show();
        return;
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
```


