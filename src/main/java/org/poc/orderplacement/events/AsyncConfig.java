package org.poc.orderplacement.events;

import java.lang.reflect.Method;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * {@code @Async} resolves Boot's auto-configured {@code applicationTaskExecutor} (sized via
 * {@code spring.task.execution.*}) by default, since it's the only {@code TaskExecutor} bean in
 * the context — no explicit executor wiring needed here, just the exception handler and the
 * rejection policy below.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

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
     */
    @Bean
    public static ThreadPoolTaskExecutorCustomizer callerRunsOnRejectionCustomizer() {
        return executor -> executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private void logUncaughtException(final Throwable throwable, final Method method, final Object... params) {
        log.error("Uncaught exception in async method {}", method.getName(), throwable);
    }
}
