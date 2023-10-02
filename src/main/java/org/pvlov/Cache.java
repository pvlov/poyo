package org.pvlov;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class Cache {

    private HashMap<String, AudioTrack> cache;
    private AudioPlayerManager playerManager;

    /**
     * Create an audio cache for a specific player manager.
     * 
     * @param playerManager
     */
    public Cache(AudioPlayerManager playerManager) {
        this.cache = new HashMap<>();
        this.playerManager = playerManager;
    }

    /**
     * Returns the cached track of a link.
     * 
     * @param link The audio track or empty if it did not exist.
     * @return
     */
    public Optional<AudioTrack> retrieve(String link) {
        if (cache.containsKey(link)) {
            return Optional.of(cache.get(link));
        }
        return Optional.empty();
    }

    /**
     * Iterate over all cached objects.
     * 
     * @return
     */
    public Iterable<AudioTrack> iter() {
        return cache.values();
    }

    /**
     * Download and cache an audio track for a given link.
     * 
     * @param link The link to the audio track.
     * @return True if the track was not already cached, false otherwise.
     */
    public boolean store(String link) {
        if (!cache.containsKey(link)) {
            Utils.decodeTrack(playerManager, link).ifPresent(audioTrack -> cache.put(link, audioTrack));
            return true;
        }
        return false;
    }

    /**
     * Cache all audio files of the given links.
     * 
     * @param links The links to the audio files.
     * @return True if all audio tracks were successfully cached, false otherwise.
     *         (also false if a track was already cached previously)
     */
    public boolean store(List<String> links) {
        if (links == null) {
            return true;
        }

        boolean success = true;
        for (String link : links) {
            if (!store(link)) {
                success = false;
            }
        }
        return success;
    }

    /**
     * 
     * @return True if the cache is empty, false otherwise.
     */
    public boolean empty() {
        return this.cache.isEmpty();
    }

    /**
     * Removes a audio track from the cache.
     * 
     * @param link The link to the track.
     * @return true if the track was cached, false otherwise.
     */
    public boolean free(String link) {
        if (cache.containsKey(link)) {
            cache.remove(link);
            return true;
        }
        return false;
    }

}
