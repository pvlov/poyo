package org.pvlov;

import java.util.Optional;

import org.javacord.api.DiscordApi;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class Utils {

    public static AudioQueue buildQueue(AudioPlayerManager playerManager, DiscordApi api) {
        AudioSourceManagers.registerRemoteSources(playerManager);
        return new AudioQueue(new LavaAudioPlayer(api, playerManager.createPlayer()));
    }

    public static Optional<AudioTrack> decodeTrack(AudioPlayerManager playerManager, String link) {
        FixWeirdResultHandler result = new FixWeirdResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                super.loadedTrack = Optional.of(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
            }

            @Override
            public void noMatches() {
            }

            @Override
            public void loadFailed(FriendlyException exception) {
            }
        };

        playerManager.loadItemSync(link, result);
        return result.getTrack();
    }
}
