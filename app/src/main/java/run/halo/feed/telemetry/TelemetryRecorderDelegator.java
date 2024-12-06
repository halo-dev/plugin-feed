package run.halo.feed.telemetry;

import java.time.Duration;
import java.time.Instant;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.DefaultController;
import run.halo.app.extension.controller.DefaultQueue;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.extension.controller.RequestQueue;
import run.halo.app.plugin.extensionpoint.ExtensionGetter;
import run.halo.feed.TelemetryEventInfo;
import run.halo.feed.TelemetryRecorder;

@Component
public class TelemetryRecorderDelegator implements Reconciler<TelemetryEventInfo>, SmartLifecycle {
    protected volatile boolean running = false;

    private final RequestQueue<TelemetryEventInfo> queue;

    protected final Controller controller;

    private final ExtensionGetter extensionGetter;

    public TelemetryRecorderDelegator(ExtensionGetter extensionGetter) {
        this.extensionGetter = extensionGetter;
        this.queue = new DefaultQueue<>(Instant::now);
        this.controller = this.setupWith(null);
    }

    /**
     * Add telemetry event to queue to process it in another thread.
     */
    public void record(TelemetryEventInfo telemetryEventInfo) {
        queue.addImmediately(telemetryEventInfo);
    }

    @Override
    public Result reconcile(TelemetryEventInfo eventInfo) {
        extensionGetter.getEnabledExtensions(TelemetryRecorder.class)
            .doOnNext(recorder -> recorder.record(eventInfo))
            .onErrorResume(Throwable.class, e -> Mono.empty())
            .then()
            .block();
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return new DefaultController<>(
            TelemetryRecorderDelegator.class.getName(),
            this,
            queue,
            null,
            Duration.ofMillis(100),
            Duration.ofMinutes(10)
        );
    }

    @Override
    public void start() {
        controller.start();
        running = true;
    }

    @Override
    public void stop() {
        running = false;
        controller.dispose();
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
