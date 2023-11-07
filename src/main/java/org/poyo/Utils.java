package org.poyo;

import java.util.Optional;
import java.util.stream.Stream;

import org.javacord.api.DiscordApi;

import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;

public class Utils {
	public enum SlashCommand {
		PING("ping"),
		PERMISSION_SET("permission_set"),
		PLAY("play"),
		PLAYLIST("playlist"),
		SKIP("skip"),
		STOP("stop"),
		VOLUME("volume"),
		JUMP("jump"),
		UNEXPECTED(null);

		public final String commandName;

		SlashCommand(String commandName) {
			this.commandName = commandName;
		}

		public static SlashCommand fromCommandName(String commandName) {
			return Stream.of(values()).filter(v -> commandName.equals(v.commandName)).findAny().orElse(UNEXPECTED);
		}
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
