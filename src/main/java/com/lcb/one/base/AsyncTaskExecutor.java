package com.lcb.one.base;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 创建线程池
 */
public class AsyncTaskExecutor {
    private static AsyncTaskExecutor mInstance = null;
    private static final int maxExecutor = 4;
    private ExecutorService mExecutor = null;

    public static AsyncTaskExecutor getinstance() {
        if (mInstance == null) {
            mInstance = new AsyncTaskExecutor();
        }
        return mInstance;
    }

    private AsyncTaskExecutor() {
        mExecutor = Executors.newFixedThreadPool(maxExecutor);
    }

    public void submit(Runnable a) {
        mExecutor.execute(a);
    }

}