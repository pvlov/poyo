package org.poyo.audio;

import org.javacord.api.DiscordApi;
import org.poyo.Cache;
import org.poyo.util.result.Err;
import org.poyo.util.result.Result;

public class AudioManager {

    private final CustomAudioPlayerManager playerManager;
    private final AudioQueue queue;
    private final Cache audioCache;

    public AudioManager(DiscordApi api) {
        this.playerManager = new CustomAudioPlayerManager();
        this.queue = AudioQueue.buildQueue(this.playerManager, api);
        this.audioCache = new Cache(playerManager);
    }

    public Result<Void, RuntimeException> playAudioTrack(String identifier) {
        return new Err<>(new AudioLoadError(""));
    }
}
