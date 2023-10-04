package org.pvlov.audio;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class AudioTrackLoadResultHandler {

    /**
     * @param future    The task that should be waited on
     * @param onSuccess is called if the Result was Ok
     * @param onFail    is called if the Result was Err
     */
    public static void attachCallbacks(Future<AudioTrackLoadResult> future,
                                       Consumer<AudioTrackLoadResult> onSuccess,
                                       Consumer<AudioTrackLoadResult> onFail) {

        ExecutorService service = Executors.newSingleThreadExecutor();

        service.submit(() -> {
            try {
                var result = future.get();
                if (result.isOk()) {
                    onSuccess.accept(result);
                } else {
                    onFail.accept(result);
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                service.shutdown();
            }
        });
    }
}
