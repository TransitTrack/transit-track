package org.transitclock.config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.transitclock.core.avl.AvlReportProcessor.AvlReportProcessingTask;
import org.transitclock.core.avl.AvlReportProcessorQueue;
import org.transitclock.properties.AvlProperties;
import org.transitclock.utils.ExceptionHandlingAsyncTaskExecutor;
import org.transitclock.utils.threading.NamedThreadFactory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
@Profile("!testdev & !testprod")
public class AsyncConfiguration implements AsyncConfigurer {

    private final TaskExecutionProperties taskExecutionProperties;

    public AsyncConfiguration(TaskExecutionProperties taskExecutionProperties) {
        this.taskExecutionProperties = taskExecutionProperties;
    }

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        logger.debug("Creating Async Task Executor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(taskExecutionProperties.getPool().getCoreSize());
        executor.setMaxPoolSize(taskExecutionProperties.getPool().getMaxSize());
        executor.setQueueCapacity(taskExecutionProperties.getPool().getQueueCapacity());
        executor.setThreadNamePrefix(taskExecutionProperties.getThreadNamePrefix());
        return new ExceptionHandlingAsyncTaskExecutor(executor);
    }

    @Bean(name = "avlExecutingThreadPool")
    public Executor avlExecutingThreadPool(AvlProperties avlProperties) {
        final int numberThreads = avlProperties.getNumThreads();
        final int maxAVLQueueSize = avlProperties.getQueueSize();

        RejectedExecutionHandler rejectedHandler = (arg0, arg1) -> {
            logger.error("Rejected AVL report {}. The work queue with capacity {} must be full.",
                ((AvlReportProcessingTask) arg0).getAvlReport(),
                maxAVLQueueSize);
        };

        logger.info("Creating Avl Task Executor for handling AVL reports [queue={} and threads={}].", maxAVLQueueSize, numberThreads);

        return new ThreadPoolExecutor(0,
            numberThreads,
            1,
            TimeUnit.HOURS,
            (BlockingQueue) new AvlReportProcessorQueue(maxAVLQueueSize),
            new NamedThreadFactory("avl-executor"),
            rejectedHandler);

    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
