package com.netease.modular;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.netease.arouter.annotation.ARouter;
import com.netease.arouter.annotation.Parameter;
import com.netease.arouter.annotation.model.RouterBean;
import com.netease.arouter.api.ParameterManager;
import com.netease.arouter.api.RouterManager;
import com.netease.arouter.api.core.ARouterLoadGroup;
import com.netease.arouter.api.core.ARouterLoadPath;
import com.netease.common.base.BaseActivity;
import com.netease.common.order.OrderAddress;
import com.netease.common.order.drawable.OrderDrawable;
import com.netease.common.utils.Cons;
import com.netease.modular.apt.ARouter$$Group$$order;
import com.netease.modular.apt.ARouter$$Path$$order;
import com.netease.modular.order.Order_MainActivity;
import com.netease.modular.personal.Personal_MainActivity;

import java.io.IOException;
import java.util.Map;

@ARouter(path = "/app/MainActivity")
public class MainActivity extends BaseActivity {

    @Parameter
    String userName;

    @Parameter
    int age;

    /**
     * 实际上各个模块的业务数据都可以通过这种方式或者，业务类要实现call接口，并是@ARouter进行注解，说明路径
     */
    //获取订单模块的orderAddress信息，通过parameter注解处理器实现
    @Parameter(name = "/order/getOrderBean")
    OrderAddress orderAddress;

    @Parameter(name = "/order/getDrawable")
    OrderDrawable orderDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (BuildConfig.isRelease) {
            Log.e(Cons.TAG, "当前为：集成化模式，除app可运行，其他子模块都是Android Library");
        } else {
            Log.e(Cons.TAG, "当前为：组件化模式，app/order/personal子模块都可独立运行");
        }
        //加载传递过来的参数
        ParameterManager.getInstance().loadParameter(this);
        Log.d("ARouter", "userName=" + userName + ",age=" + age);
        Log.d("ARouter", "orderDrawable=" + orderDrawable.getDrawable() );
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d("ARouter", "orderAddress=" + orderAddress.getOrderBean("aa205eeb45aa76c6afe3c52151b52160", "144.34.161.97").toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void jumpOrder(View view) {

//        // 最终集成化模式，所有子模块app/order/personal通过APT生成的类文件都会打包到apk里面，不用担心找不到
//        ARouterLoadGroup group = new ARouter$$Group$$order();
//        Map<String, Class<? extends ARouterLoadPath>> map = group.loadGroup();
//        // 通过order组名获取对应路由路径对象
//        Class<? extends ARouterLoadPath> clazz = map.get("order");
//
//        try {
//            // 类加载动态加载路由路径对象
//            ARouter$$Path$$order path = (ARouter$$Path$$order) clazz.newInstance();
//            Map<String, RouterBean> pathMap = path.loadPath();
//            // 获取目标对象封装
//            RouterBean bean = pathMap.get("/order/Order_MainActivity");
//
//            if (bean != null) {
//                Intent intent = new Intent(this, bean.getClazz());
//                intent.putExtra("name", "simon");
//                startActivity(intent);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        RouterManager.getInstance()
                .build("/order/Order_MainActivity")
                .withString("root", "myself!!!!")
                .withString("userName","wangdazhao")
                .withInt("age",25)
                .navigation(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);//更新intent
        ParameterManager.getInstance().loadParameter(this);
//        userName = intent.getStringExtra("userName");
        Log.d("ARouter", "onNewIntent??>>>>userName=" + userName + ",age=" + age);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("ARouter", "requestCode=" + requestCode + ",resultCode=" + resultCode + ",data =" + data);
        if (requestCode == 100) {
            if (resultCode == 99 && data != null) {
                String root = data.getStringExtra("root2");
                Log.d("ARouter", "root2=" + root);
            }
        }
    }

    private void startSendEmail() {
        String address = "test@sina.com";
        String subject = "邮件的标题wuzejian....";
        String body = "邮件的内容wuzejian......";
        String content = "mailto:" + address + "?subject=" + subject + "&body=" + body;
        Intent returnIt = new Intent(Intent.ACTION_SENDTO);
        returnIt.setData(Uri.parse(content));
        startActivity(Intent.createChooser(returnIt, "Send email tips"));
    }

    private void send1() {
        //系统邮件系统的动作为android.content.Intent.ACTION_SEND
        Intent email = new Intent(Intent.ACTION_SENDTO);
        email.setType("text/plain");
        String[] emailReciver = new String[]{"pop1030123@sina.com", "fulon@sina.com", "yuyulon@sina.com"};
        String emailSubject = "你有一条短信主题来了";
        String emailBody = "wuzejian....发送邮件";
//        email.setData(Uri.parse("mailto:"));
//        email.setData(Uri.parse("mailto:pop1030123@sina.com,fulon@sina.com,fulonggg@sina.com,yuyulon@sina.com"));
        //设置邮件默认地址
        email.putExtra(android.content.Intent.EXTRA_EMAIL, emailReciver);
        //设置邮件默认标题
        email.putExtra(android.content.Intent.EXTRA_SUBJECT, emailSubject);
        //设置要默认发送的内容
        email.putExtra(android.content.Intent.EXTRA_TEXT, emailBody);
        //调用系统的邮件系统
        startActivity(Intent.createChooser(email, "请选择邮件发送软件"));
    }

    public void jumpPersonal(View view) {
        Intent intent = new Intent(this, Personal_MainActivity.class);
        intent.putExtra("name", "simon");
        startActivity(intent);
    }
}
