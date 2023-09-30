package org.pvlov;

import java.util.Optional;

import org.javacord.api.DiscordApi;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

public class Utils {

    public enum SlashCommand {
        PING,
        PLAY,
        PLAYLIST,
        SKIP,
        STOP,
        VOLUME,

        UNEXPECTED,
    }

    public static SlashCommand parseCommandName(String commandName) {
        try {
            return SlashCommand.valueOf(commandName.toUpperCase());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return SlashCommand.UNEXPECTED;
    }

    public static void sendQuickEphemeralResponse(SlashCommandInteraction interaction, String response) {
        interaction.createImmediateResponder()
                .setContent(response)
                .setFlags(MessageFlag.EPHEMERAL)
                .respond();
    }

    public static void sendQuickEphemeralResponse(SlashCommandInteraction interaction, EmbedBuilder embed) {
        interaction.createImmediateResponder()
                .addEmbed(embed)
                .setFlags(MessageFlag.EPHEMERAL)
                .respond();
    }

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
