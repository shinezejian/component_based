package com.netease.arouter.api;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.LruCache;

import com.netease.arouter.annotation.Parameter;
import com.netease.arouter.api.core.ParameterLoad;

public class ParameterManager {

    private static final String FILE_SUFFIX_NAME = "$$Parameter";

    //key:MainActivity类名 value：实现了ParameterLoad的参数获取类,如：MainActivity$$Parameter
    private LruCache<String, ParameterLoad> mParameterCache;

    private static volatile ParameterManager mInstance;

    private ParameterManager() {
        mParameterCache = new LruCache<>(163);
    }

    public static ParameterManager getInstance() {
        if (mInstance == null) {
            synchronized (ParameterManager.class) {
                if (mInstance == null) {
                    mInstance = new ParameterManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 传入的Activity中所有被@Parameter注解的属性。通过加载APT生成源文件，并给属性赋值
     *
     * @param activity 需要给属性赋值的类，如：MainActivity中所有被@Parameter注解的属性
     */
    public void loadParameter(@NonNull Activity activity) {
        //获取生成的参数类名
        String activityClass = activity.getClass().getName();

        ParameterLoad parameterLoad = mParameterCache.get(activityClass);
        try {
            if (parameterLoad == null) {
                //组成参数类名如MainActivity$$Parameter
                String clazzName = activityClass + FILE_SUFFIX_NAME;
                if (clazzName != null) {
                    Class<?> parameterClass = Class.forName(clazzName);
                    if (parameterClass != null)
                        parameterLoad = (ParameterLoad) parameterClass.newInstance();
                    if (parameterLoad != null)
                        mParameterCache.put(activityClass, parameterLoad);
                }
            }
            // 通过传入参数给生成的源文件中所有属性赋值
            parameterLoad.loadParameter(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
