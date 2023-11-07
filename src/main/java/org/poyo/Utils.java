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
		PERMISSION_SET("permission", "set"),
		PLAY("play"),
		PLAYLIST("playlist"),
		SKIP("skip"),
		STOP("stop"),
		VOLUME("volume"),
		JUMP("jump"),
		UNEXPECTED(null);

		public final String commandName;
		public final String[] subCommands;
		public final String fullCommandName;

		SlashCommand(String commandName, String... subCommands) {
			this.commandName = commandName;
			this.subCommands = subCommands;

			if (commandName == null) {
				this.fullCommandName = null;
				return;
			}
			StringBuilder sb = new StringBuilder(commandName);
			for (String sub : subCommands) {
				sb.append(" ").append(sub);
			}
			fullCommandName = sb.toString();
		}

		public static SlashCommand fromFullCommandName(String fullCommandName) {
			return Stream.of(values()).filter(v -> fullCommandName.equals(v.fullCommandName)).findAny().orElse(UNEXPECTED);
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
