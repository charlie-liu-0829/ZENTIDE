package com.zentide.entity.enums;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public enum ExecutorServiceSingletonEnum {
    INSTANCE;

    private final ExecutorService executorService;

    ExecutorServiceSingletonEnum() {
        executorService = Executors.newFixedThreadPool(50);
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

}