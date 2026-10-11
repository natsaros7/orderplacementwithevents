package org.poc.orderplacement.events;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Reuses Boot's auto-configured {@code applicationTaskExecutor} (sized via
 * {@code spring.task.execution.*}) for {@code @Async} listeners, and logs failures that
 * {@code @Async} would otherwise swallow.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    private final Executor applicationTaskExecutor;

    public AsyncConfig(
            @Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME) final Executor applicationTaskExecutor) {
        this.applicationTaskExecutor = applicationTaskExecutor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return applicationTaskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this::logUncaughtException;
    }

    private void logUncaughtException(final Throwable throwable, final Method method, final Object... params) {
        log.error("Uncaught exception in async method {}", method.getName(), throwable);
    }
}
