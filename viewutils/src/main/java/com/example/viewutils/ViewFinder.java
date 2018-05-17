package com.example.viewutils;

import android.app.Activity;
import android.view.View;

import java.util.Queue;

/**
 * 类描述：viewFinder的辅助类
 * 创建人：吴冬冬
 * 创建时间：2018/4/7 17:06
 */
public class ViewFinder {
    private View mView;
    private Activity mActivity;

    public ViewFinder(Activity activity) {
        mActivity = activity;
    }

    public ViewFinder(View view) {
        mView = view;
    }

    /**
     * @param viewId
     */
    public View findViewById(int viewId) {
        return mActivity != null ?
                mActivity.findViewById(viewId) :
                mView.findViewById(viewId);
    }
}
