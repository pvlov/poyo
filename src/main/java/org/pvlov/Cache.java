package org.pvlov;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class Cache {

    private HashMap<String, AudioTrack> cache;
    private AudioPlayerManager playerManager;

    public Cache(AudioPlayerManager playerManager) {
        this.cache = new HashMap<>();
        this.playerManager = playerManager;
    }

    public Optional<AudioTrack> retrieve(String link) {
        if (cache.containsKey(link)) {
            return Optional.of(cache.get(link));
        }
        return Optional.empty();
    }

    public Iterable<AudioTrack> iter() {
        return cache.values();
    }

    public boolean store(String link) {
        if (!cache.containsKey(link)) {
            Utils.decodeTrack(playerManager, link).ifPresent(audioTrack -> cache.put(link, audioTrack));
            return true;
        }
        return false;
    }

    public boolean store(List<String> links) {
        if (links == null)
            return true;

        boolean success = true;
        for (String link : links) {
            if (!store(link)) {
                success = false;
            }
        }
        return success;
    }

    public boolean empty() {
        return this.cache.isEmpty();
    }

    public boolean free(String link) {
        if (cache.containsKey(link)) {
            cache.remove(link);
            return true;
        }
        return false;
    }

}
