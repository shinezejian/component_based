package com.netease.arouter.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import com.netease.arouter.annotation.model.RouterBean;
import com.netease.arouter.api.core.ARouterLoadGroup;
import com.netease.arouter.api.core.ARouterLoadPath;

import java.util.Map;

/**
 * 路由加载管理器
 */
public class RouterManager {

    //模块组名
    private String mGroup;

    //具体activity的路径
    private String mPath;

    // Lru缓存，key:类名, value:路由组Group加载接口
    private LruCache<String, ARouterLoadGroup> mGroupCache;

    // Lru缓存，key：类名，value:路由组Group对应的详细Path加载接口
    private LruCache<String, ARouterLoadPath> mPathCache;

    // APT生成的路由组Group源文件前缀名
    private static final String GROUP_FILE_PREFIX_NAME = ".ARouter$$Group$$";

    private static RouterManager mInstance;

    public static RouterManager getInstance() {
        if (mInstance == null) {
            synchronized (RouterManager.class) {
                if (mInstance == null) {
                    mInstance = new RouterManager();
                }
            }
        }
        return mInstance;
    }

    private RouterManager() {
        mGroupCache = new LruCache<>(163);
        mPathCache = new LruCache<>(163);
    }

    public BundleManager build(String path) {
        // @ARouter注解中的path值，必须要以 / 开头（模仿阿里Arouter规范）
        if (TextUtils.isEmpty(path) || !path.startsWith("/")) {
            throw new IllegalArgumentException("未按规范配置，如：/app/MainActivity");
        }

        this.mGroup = subFromPathToGroup(path);
        this.mPath = path;//检查后再赋值
        return new BundleManager();
    }


    private String subFromPathToGroup(String path) {
        // 比如开发者代码为：path = "/MainActivity"，最后一个 / 符号必然在字符串第1位
        if (path.lastIndexOf("/") == 0) {
            // 架构师定义规范，让开发者遵循
            throw new IllegalArgumentException("@ARouter注解未按规范配置，如：/app/MainActivity");
        }

        // 从第一个 / 到第二个 / 中间截取，如：/app/MainActivity 截取出 app 作为group
        String finalGroup = path.substring(1, path.indexOf("/", 1));

        if (TextUtils.isEmpty(finalGroup)) {
            // 架构师定义规范，让开发者遵循
            throw new IllegalArgumentException("@ARouter注解未按规范配置，如：/app/MainActivity");
        }

        // 最终组名：app
        return finalGroup;
    }

    /**
     * 开始跳转
     *
     * @param context       上下文
     * @param bundleManager Bundle拼接参数管理类
     * @param code          这里的code，可能是requestCode，也可能是resultCode。取决于isResult
     * @return 普通跳转可以忽略，用于跨模块CALL接口
     */
    public Object navigation(Context context, BundleManager bundleManager, int code) {
        // 精华：阿里的路由path随意写，导致无法找到随意拼接APT生成的源文件，如：ARouter$$Group$$abc
        // 找不到，就加载私有目录下apk中的所有dex并遍历，获得所有包名为xxx的类。并开启了线程池工作
        // 这里的优化是：代码规范写法，准确定位ARouter$$Group$$app

        //获取实现了ARouterLoadGroup接口的分组类
        String groupClassName = context.getPackageName() + ".apt" + GROUP_FILE_PREFIX_NAME + mGroup;
        Log.e("netease >>> ", "groupClassName -> " + groupClassName);

        ARouterLoadGroup loadGroup = mGroupCache.get(groupClassName);
        try {
            if (loadGroup == null) {
                Class<?> groupClass = Class.forName(groupClassName);
                if (groupClass != null) loadGroup = (ARouterLoadGroup) groupClass.newInstance();
                if (loadGroup != null) mGroupCache.put(groupClassName, loadGroup);
            }

            // 获取路由路径类ARouter$$Path$$app
            if (loadGroup.loadGroup().isEmpty()) {
                throw new RuntimeException("路由加载失败");
            }

            //先判断缓存是否存在
            ARouterLoadPath loadPath = mPathCache.get(mPath);
            if (loadPath == null) {
                Map<String, Class<? extends ARouterLoadPath>> pathClassMap = loadGroup.loadGroup();
                Class<?> pathClass = pathClassMap.get(mGroup);
                if (pathClass != null) loadPath = (ARouterLoadPath) pathClass.newInstance();
                if (loadPath != null) mPathCache.put(mPath, loadPath);
            }

            //获取ARounterBean
            if (loadPath != null) {
                Map<String, RouterBean> routerBeanMap = loadPath.loadPath();

                if (routerBeanMap.isEmpty()) {
                    throw new RuntimeException("路由路径加载失败");
                }

                RouterBean routerBean = routerBeanMap.get(mPath);

                if (routerBean != null) {

                    //开始跳转activity 或 其他业务处理
                    switch (routerBean.getType()) {
                        case ACTIVITY:
                            Intent intent = new Intent(context, routerBean.getClazz());
                            //携带额外参数
                            intent.putExtras(bundleManager.getBundle());
                            //判断是否要有返回值得调转 startActivityForResult -> setResult
                            if (bundleManager.isResult()) {
                                //Activity结束跳转返回
                                ((Activity) context).setResult(code, intent);
                                ((Activity) context).finish();
                            } else {
                                if (code > 0) {
                                    ((Activity) context).startActivityForResult(intent, code, bundleManager.getBundle());
                                } else {
                                    ((Activity) context).startActivity(intent);
                                }
                            }

                            break;
                        case CALL:
                            return routerBean.getClazz().newInstance();

                    }
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
