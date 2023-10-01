package org.pvlov.audio;

import java.util.Optional;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public abstract class FixWeirdResultHandler implements AudioLoadResultHandler {
    protected Optional<AudioTrack> loadedTrack = Optional.empty();

    public Optional<AudioTrack> getTrack() {
        return loadedTrack;
    }
}
