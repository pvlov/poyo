package org.poyo;

import java.util.*;

import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.poyo.audio.AudioLoadError;
import org.poyo.audio.CustomAudioPlayerManager;

public class Cache {

    private final HashMap<String, List<AudioTrack>> cache;
    private final CustomAudioPlayerManager playerManager;

    public Cache(CustomAudioPlayerManager playerManager) {
        this.cache = new HashMap<>();
        this.playerManager = playerManager;
    }

    public Optional<List<AudioTrack>> retrieve(String link) {
        if (cache.containsKey(link)) {
            return Optional.of(cache.get(link));
        }
        return Optional.empty();
    }

    public Iterable<List<AudioTrack>> iter() {
        return cache.values();
    }

    public void store(String link) {
        if (cache.containsKey(link)) {
            return;
        }
        var result = playerManager.loadItemSync(new AudioReference(link, null));
        if (result.isOk()) {
            cache.put(link, result.orElseThrow());
        } else {
            Bot.LOG.warn("Could not load VIP-Track with the identifier: " + link);
        }
    }

    public void store(Collection<String> links) {
       for(var link : links) {
           store(link);
       }
    }

    public boolean empty() {
        return this.cache.isEmpty();
    }

}
