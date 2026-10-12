package org.poc.orderplacement.events;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Explicitly selects Boot's auto-configured {@code applicationTaskExecutor} (sized via
 * {@code spring.task.execution.*}) for {@code @Async}, rather than relying on "it's the only
 * TaskExecutor bean in the context": once Flyway/Testcontainers/docker-compose support is on the
 * classpath, that's no longer true, and {@code @Async} silently falls back to a
 * {@code SimpleAsyncTaskExecutor} (unbounded, unnamed threads) instead.
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

    /**
     * The order is already committed by the time an AFTER_COMMIT listener runs, so a saturated
     * pool must never reject the submission back to the (unrelated) caller as a 500 — that would
     * read as "placing the order failed" when it already succeeded, and risks the client retrying
     * into a duplicate order. CallerRunsPolicy degrades to synchronous execution on the submitting
     * thread instead of rejecting, trading request latency for correctness under load.
     * <p>
     * Static: a non-static {@code @Bean} method here would require this configuration's own
     * instance (and therefore {@code applicationTaskExecutor} above) before Boot's executor
     * builder has finished applying customizers to that very executor — a bean-creation cycle.
     */
    @Bean
    public static ThreadPoolTaskExecutorCustomizer callerRunsOnRejectionCustomizer() {
        return executor -> executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private void logUncaughtException(final Throwable throwable, final Method method, final Object... params) {
        log.error("Uncaught exception in async method {}", method.getName(), throwable);
    }
}
