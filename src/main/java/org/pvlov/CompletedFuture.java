package org.pvlov;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompletedFuture implements Future<AudioTrackLoadResult> {

    AudioTrackLoadResult result;

    public CompletedFuture(AudioTrackLoadResult result) {
        this.result = result;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public AudioTrackLoadResult get() throws InterruptedException, ExecutionException {
        return result;
    }

    @Override
    public AudioTrackLoadResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return result;
    }
}
