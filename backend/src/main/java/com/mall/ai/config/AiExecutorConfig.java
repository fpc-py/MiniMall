package com.mall.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSE 后台执行线程池（JDK 原生，不引入额外中间件）。
 */
@Configuration
public class AiExecutorConfig {

    @Bean(name = "aiExecutor", destroyMethod = "shutdown")
    public ExecutorService aiExecutor() {
        return Executors.newFixedThreadPool(8, runnable -> {
            Thread thread = new Thread(runnable, "ai-sse-" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        });
    }
}
