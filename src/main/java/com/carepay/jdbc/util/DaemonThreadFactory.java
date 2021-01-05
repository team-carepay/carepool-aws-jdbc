package com.carepay.jdbc.util;

import java.util.concurrent.ThreadFactory;

public class DaemonThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        final Thread thread = new Thread();
        thread.setDaemon(true);
        return thread;
    }
}
