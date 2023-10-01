package org.pvlov;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;
import java.util.function.Consumer;

public class AudioTrackLoadResult {

    private final LoadResultType type;
    private final List<AudioTrack> audioTracks;

    private final FriendlyException exception;

    public enum LoadResultType {
        OK,
        NOMATCH,
        ERROR
    }

    private AudioTrackLoadResult(List<AudioTrack> audioTracks, LoadResultType type, FriendlyException exception){
        this.audioTracks = audioTracks;
        this.type = type;
        this.exception = exception;
    }

    public static AudioTrackLoadResult Ok(List<AudioTrack> audioTracks, LoadResultType type) {
        return new AudioTrackLoadResult(
            audioTracks,
            type,
            null
        );
    }

    public static AudioTrackLoadResult Err(FriendlyException exception, LoadResultType type) {
        return new AudioTrackLoadResult(
                null,
                type,
                exception
        );
    }

    public boolean isErr() {
        return exception != null;
    }

    public boolean isOk() {
        return exception == null;
    }

    public List<AudioTrack> unwrap() {
        if (type == LoadResultType.ERROR || type == LoadResultType.NOMATCH) {
            throw new RuntimeException("Calling unwrap() on erroneous AudioTrackLoadResult!");
        }
        return audioTracks;
    }

    public List<AudioTrack> unwrapOr(List<AudioTrack> tracks) {
        if (type == LoadResultType.ERROR || type == LoadResultType.NOMATCH) {
          return tracks;
        }
        return audioTracks;
    }

    public List<AudioTrack> unwrapOrElse(Consumer<FriendlyException> errorHandler) {
        if (type == LoadResultType.ERROR || type == LoadResultType.NOMATCH) {
            errorHandler.accept(exception);
            return null;
        }
        return audioTracks;
    }

    public void addToAudioTracks(AudioTrack track) {
        audioTracks.add(track);
    }

    public void addToAudioTracks(List<AudioTrack> track) {
        audioTracks.addAll(track);
    }
}
