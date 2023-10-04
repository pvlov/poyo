package org.pvlov;

import java.util.Optional;

import org.javacord.api.DiscordApi;

import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;

public class Utils {

    public enum SlashCommand {
        PING,
        PLAY,
        PLAYLIST,
        SKIP,
        STOP,
        VOLUME,

        JUMP,

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
    public static void simulateJoinEvent(Bot bot, ServerVoiceChannel channel, long userID) {
        bot.onServerVoiceChannelMemberJoin(new ServerVoiceChannelMemberJoinEvent() {

            @Override
            public ServerVoiceChannel getChannel() {
                return channel;
            }

            @Override
            public DiscordApi getApi() {
                return bot.api;
            }

            @Override
            public User getUser() {
                return bot.api.getUserById(userID).join();
            }

            @Override
            public Optional<ServerVoiceChannel> getOldChannel() {
                return Optional.empty();
            }

            @Override
            public boolean isMove() {
                return false;
            }

        });
    }
}
