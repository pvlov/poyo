package org.pvlov;

import java.util.Optional;
import java.util.stream.Stream;

import org.javacord.api.DiscordApi;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;
import org.javacord.api.interaction.SlashCommandInteraction;

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
