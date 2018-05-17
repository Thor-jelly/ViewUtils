package com.example.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.viewutils.CheckNet;
import com.example.viewutils.OnClick;
import com.example.viewutils.ViewById;
import com.example.viewutils.ViewUtils;

public class MainActivity extends AppCompatActivity {

    @ViewById(R.id.one_tv)
    private TextView mOneTv;

    @ViewById(R.id.two_tv)
    private TextView mTwoTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewUtils.inject(this);
    }

    @OnClick(R.id.one_tv)
    private void oneTvOnClick() {
        //点击事件
        Integer a = 0;
        Log.d("123===", "oneTvOnClick: "+a);
    }

    //如果添加checkNet记得添加网络权限<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    @CheckNet
    @OnClick(R.id.two_tv)
    private void twoOnClick(TextView twoTv) {
        //点击事件
    }
}