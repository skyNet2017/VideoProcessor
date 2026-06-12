package com.hss01248.videocompress;

import androidx.annotation.Nullable;

import com.blankj.utilcode.util.LogUtils;

/**
 * @Despciption todo
 * @Author hss
 * @Date 9/27/24 2:28 PM
 * @Version 1.0
 */
public interface MyCommonCallback5<T> {
    void onSuccess(T t);


    default void onError( String msg){
        onError("-1",msg,null);
    }

    default void onError(String code, String msg,@Nullable Throwable throwable){
        LogUtils.w(code,msg,throwable);
    }
}
