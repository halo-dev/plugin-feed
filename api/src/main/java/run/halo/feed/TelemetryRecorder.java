package run.halo.feed;

import org.pf4j.ExtensionPoint;

public interface TelemetryRecorder extends ExtensionPoint {

    void record(TelemetryEventInfo eventInfo);
}
