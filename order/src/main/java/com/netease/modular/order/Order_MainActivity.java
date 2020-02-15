package com.netease.modular.order;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.netease.arouter.annotation.ARouter;
import com.netease.arouter.annotation.Parameter;
import com.netease.arouter.api.ParameterManager;
import com.netease.arouter.api.RouterManager;
import com.netease.common.base.BaseActivity;
import com.netease.common.utils.Cons;

@ARouter(path = "/order/Order_MainActivity")
public class Order_MainActivity extends BaseActivity {

    @Parameter
    String userName;

    @Parameter
    int age;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_activity_main);

        Log.e(Cons.TAG, "order/Order_MainActivity");
        ParameterManager.getInstance().loadParameter(this);
//        if (getIntent() != null) {
//            String content = getIntent().getStringExtra("userName");
            Log.e(Cons.TAG, "userName接收参数值：" + userName);
            Log.e(Cons.TAG, "age接收参数值：" + age);
//        }
    }



    public void jumpApp(View view) {
//        RouterManager.getInstance().build("/app/MainActivity")
//                .withResultString("root2", "root22332")
//                .withString("userName", "simon")
//                .withInt("age", 20)
//                .navigation(this, 99);

        RouterManager.getInstance().build("/app/MainActivity")
                .withString("userName", "simon")
                .withInt("age", 20)
                .navigation(this);
    }

    public void jumpPersonal(View view) {

    }
}
