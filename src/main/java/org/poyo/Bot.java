package org.poyo;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.*;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberJoinListener;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberLeaveListener;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.javatuples.Pair;
import org.poyo.audio.AudioQueue;
import org.poyo.audio.CustomAudioPlayerManager;
import org.poyo.util.ResponseUtils;

import java.util.*;

import static org.poyo.PermissionSystem.PermissionLevel.*;
import static org.poyo.Utils.SlashCommand.*;

public class Bot implements ServerVoiceChannelMemberJoinListener, ServerVoiceChannelMemberLeaveListener,
		SlashCommandCreateListener {

	public static final Logger LOG = LogManager.getLogger();

	private static final String BOT_NAME = "Poyo";
	private static final String NEVER_GONNA_GIVE_YOU_UP = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
	private final PermissionSystem permissionSystem;
	private final CustomAudioPlayerManager playerManager;
	private final AudioQueue queue;
	private final Cache audioCache;
	private final List<Long> VIPs;
	public DiscordApi api;
	// TODO: Multiple connections at once
	private AudioConnection currConnection;

	public Bot() {
		this.api = new DiscordApiBuilder().setToken(Config.INSTANCE.getEntry("DISCORD_TOKEN", String.class).orElseGet(() -> {
			LOG.error("No DISCORD_TOKEN set. Abort.");
			return null;
		})).login().join();

		var permissionEntry = Config.INSTANCE.getNestedMapEntry("PERMISSIONS", Long.class, Long.class, String.class);
		permissionSystem = permissionEntry.map(PermissionSystem::new).orElseGet(PermissionSystem::new);

		this.playerManager = new CustomAudioPlayerManager();
		this.queue = AudioQueue.buildQueue(this.playerManager, api);
		this.audioCache = new Cache(playerManager);
		this.VIPs = new ArrayList<>();

		createSlashCommands();
		api.addServerVoiceChannelMemberJoinListener(this);
		api.addServerVoiceChannelMemberLeaveListener(this);
		api.addSlashCommandCreateListener(this);

		this.audioCache.store(Config.INSTANCE.getListEntry("VIP_TRACKS", String.class).orElseGet(() -> {
			LOG.warn("No VIP tracks set.");
			return new ArrayList<>();
		}));

		this.VIPs.addAll(Config.INSTANCE.getListEntry("VIPS", Long.class).orElseGet(() -> {
			LOG.warn("No VIP's set.");
			return new ArrayList<>();
		}));

		startupCheck();
	}

	private void startupCheck() {
		for (Server server : api.getServers()) {
			if (!server.getNickname(api.getYourself()).orElse("").equals(BOT_NAME)) {
				api.getYourself().updateNickname(server, BOT_NAME);
			}

			for (ServerVoiceChannel channel : server.getVoiceChannels()) {
				if (channel.getConnectedUsers().stream().anyMatch(user -> this.VIPs.contains(user.getId()))) {
					LOG.info("VIP found. Joing voice channel...");

					// Any VIP will do.
					Utils.simulateJoinEvent(this, channel, this.VIPs.get(0));
					// Just join the first matching channel.
					break;
				}
			}
		}

		api.updateActivity(ActivityType.WATCHING, "the one and only.");
	}

	private void createSlashCommands() {
		Set<SlashCommandBuilder> builders = new HashSet<>();
		builders.add(new SlashCommandBuilder()
				.setName(PING.commandName)
				.setDescription("For testing purposes."));
		permissionSystem.registerCommand(PING, IGNORED);

		builders.add(new SlashCommandBuilder()
				.setEnabledInDms(false)
				.setName(PERMISSION_SET.commandName)
				.addOption(new SlashCommandOptionBuilder()
						.setType(SlashCommandOptionType.USER)
						.setName("user")
						.setRequired(true)
						.setDescription("User whose permission level will be modified.")
						.build())
				.addOption(new SlashCommandOptionBuilder()
						.setType(SlashCommandOptionType.LONG)
						.setName("level")
						.setRequired(true)
						.setChoices(PermissionSystem.getSlashCommandOptionChoices())
						.setDescription("Permission level to give to the user.")
						.build())
				.setDescription("Sets the permission level of a user."));
		permissionSystem.registerCommand(PERMISSION_SET, ALL);

		builders.add(new SlashCommandBuilder()
				.setName(PLAY.commandName)
				.addOption(new SlashCommandOptionBuilder()
						.setRequired(false)
						.setType(SlashCommandOptionType.STRING)
						.setName("link")
						.setDescription("The link to the song.")
						.build())
				.setDescription("Play a song."));
		permissionSystem.registerCommand(PLAY, BASIC);

		builders.add(new SlashCommandBuilder()
				.setName(SKIP.commandName)
				.setDescription("Skip the current song."));
		permissionSystem.registerCommand(SKIP, BASIC);

		builders.add(new SlashCommandBuilder()
				.setName(PLAYLIST.commandName)
				.setDescription("Print the current playlist."));
		permissionSystem.registerCommand(PLAYLIST, IGNORED);

		builders.add(new SlashCommandBuilder()
				.setName(STOP.commandName)
				.setDescription("Stop playing."));
		permissionSystem.registerCommand(STOP, BASIC);

		builders.add(new SlashCommandBuilder()
				.setName(VOLUME.commandName)
				.addOption(new SlashCommandOptionBuilder()
						.setRequired(true)
						.setType(SlashCommandOptionType.LONG).setLongMinValue(0).setLongMaxValue(100).setName("value")
						.setDescription("The volume.")
						.build())
				.setDescription("Set the volume of the bot."));
		permissionSystem.registerCommand(VOLUME, ALL);
		builders.add(new SlashCommandBuilder()
				.setName(JUMP.commandName)
				.addOption(new SlashCommandOptionBuilder()
						.setRequired(true)
						.setType(SlashCommandOptionType.LONG)
						.setLongMinValue(1)
						.setName("index")
						.setDescription("the target index")
						.build())
				.setDescription("Jump to a specific Song in the Playlist"));
		permissionSystem.registerCommand(JUMP, ALL);

		api.bulkOverwriteGlobalApplicationCommands(builders).join();
	}

	@Override
	public void onServerVoiceChannelMemberJoin(ServerVoiceChannelMemberJoinEvent event) {
		if (!this.VIPs.contains(event.getUser().getId())) {
			return;
		}

		// If serving already another vip. Do not change.
		if (currConnection != null) {
			return;
		}

		LOG.info("VIP found. Joing voice channel...");

		event.getChannel().connect().thenAccept(audioConnection -> {
			// Only update connection if the Voice Channel has changed, unneeded
			// reconnection makes the bot leave and then rejoin the same channel
			if (currConnection == null || !currConnection.getChannel().equals(audioConnection.getChannel())) {
				currConnection = audioConnection;
				queue.registerAudioDestination(audioConnection);
			}

			if (this.audioCache.empty()) {
				LOG.warn("The VIP cache is empty. You can add tracks via the config file.");
				return;
			}
			// This shouldn't just play anything but the actual Playlist for the VIP
			//queue.playNowAll(this.audioCache.iter());
			api.getYourself().updateNickname(audioConnection.getServer(),
					Config.INSTANCE.getEntry("PLAY_NICKNAME", String.class).orElse(BOT_NAME));
		}).exceptionally(throwable -> {
			throwable.printStackTrace();
			return null;
		});
	}

	@Override
	public void onServerVoiceChannelMemberLeave(ServerVoiceChannelMemberLeaveEvent event) {
		if (currConnection != null) {
			if (currConnection.getChannel().equals(event.getChannel())) {
				if (event.getChannel().getConnectedUserIds().stream().noneMatch(this.VIPs::contains)) {
					LOG.info("All VIP's left. Leaving voice channel...");

					api.getYourself().updateNickname(currConnection.getServer(), BOT_NAME);

					currConnection.close().join();
					queue.clear();
					currConnection = null;
				}
			}
		}
	}


	@Override
	public void onSlashCommandCreate(SlashCommandCreateEvent event) {
		SlashCommandInteraction interaction = event.getSlashCommandInteraction();

		if (!permissionSystem.checkPermissions(interaction)) {
			ResponseUtils.respondInstantlyEphemeral(interaction, "Permission level not high enough for this command!");
			return;
		}

		var args = interaction.getArguments();

		switch (Utils.SlashCommand.fromCommandName(interaction.getCommandName())) {
			case PING -> {
				ResponseUtils.respondInstantlyEphemeral(interaction, "Pong!");
			}
			case PERMISSION_SET -> {
				permissionSystem.setOrUpdatePermission(
						interaction.getServer().orElseThrow(),
						args.get(0).getUserValue().orElseThrow(),
						PermissionSystem.PermissionLevel.values()[args.get(1).getLongValue().orElseThrow().intValue()]);
				ResponseUtils.respondInstantlyEphemeral(interaction, "Updated permissions!");
			}
			case PLAY -> {

				final String link;

				if (args.size() >= 1 && args.get(0).getStringValue().isPresent()) {
					link = args.get(0).getStringValue().get();
				} else {
					link = NEVER_GONNA_GIVE_YOU_UP;
				}

				if (queue.isRunning()) {
					var future = playerManager.loadItem(link);
					future.thenAccept(result -> {
						if (result.isOk()) {
							queue.enqeue(result.orElseThrow());
							ResponseUtils.respondLaterPublic(interaction, "Successfully added: " + result);
						} else {
							ResponseUtils.respondLaterEphemeral(interaction, "Something went wrong while loading tracks");
						}
					}).exceptionally(err -> {
						ResponseUtils.respondLaterEphemeral(interaction, "Something went wrong while loading tracks");
						return null;
					});
					return;
				}

				interaction.getUser().getConnectedVoiceChannel(interaction.getServer().orElseThrow())
						.ifPresentOrElse(
								targetVoiceChannel -> targetVoiceChannel.connect().thenAccept(audioConnection -> {
									queue.registerAudioDestination(audioConnection);
									var future = playerManager.loadItem(link);

									future.thenAccept(result -> {
										if (result.isOk()) {
											queue.enqeue(result.orElseThrow());
											queue.start();
											ResponseUtils.respondLaterPublic(interaction,
													new EmbedBuilder()
															.setAuthor(interaction.getUser())
															.addField("Playing: ", queue.getNowPlaying().getInfo().title));
										} else {
											ResponseUtils.respondLaterEphemeral(interaction, "Something went wrong while loading Tracks");
										}
									}).exceptionally(
											error -> {
												ResponseUtils.respondLaterEphemeral(interaction, "Something went wrong while loading Tracks");
												return null;
											});
								}),
								() -> ResponseUtils.respondInstantlyEphemeral(interaction,
										"You need to be in a Voice-Channel in order to use the /play command"));
			}

			case SKIP -> {
				if (!queue.isRunning()) {
					ResponseUtils.respondInstantlyEphemeral(interaction, "The Bot is not playing Music, skip ignored");
					return;
				}
				ResponseUtils.respondInstantlyEphemeral(interaction, "Skipped Track!");
				queue.skip();
			}

			case PLAYLIST -> {
				// TODO: Fails with empty queue, add if-check for empty queue
				var embedBuilder = new EmbedBuilder();

				for (Pair<Integer, AudioTrack> entry : queue) {
					embedBuilder.addField(String.valueOf(entry.getValue0() + 1), entry.getValue1().getInfo().title, true);
				}
				ResponseUtils.respondInstantlyEphemeral(interaction, embedBuilder);
			}

			case STOP -> {
				queue.clear();
				api.getYourself().getConnectedVoiceChannel(interaction.getServer().get())
						.ifPresent(ServerVoiceChannel::disconnect);
				ResponseUtils.respondInstantlyEphemeral(interaction, "Bot was stopped");
			}

			case VOLUME -> {
				if (!queue.isRunning()) {
					ResponseUtils.respondInstantlyEphemeral(interaction, "Bot is not currently playing!");
					return;
				}
				long arg = interaction.getArguments().get(0).getLongValue().get();

				ResponseUtils.respondInstantlyEphemeral(interaction, "Adjusted Volume!");
				queue.setVolume((int) arg);
			}

			case JUMP -> {

				final long jumpTarget;
				if (args.size() >= 1 && args.get(0).getLongValue().isPresent()) {
					jumpTarget = args.get(0).getLongValue().get();
				} else {
					ResponseUtils.respondInstantlyEphemeral(interaction, "Please only use a number as the target for the /jump command");
					return;
				}
				// Be aware that for easier use and compatibility with /playlist, jump will be 1-indexed
				if (jumpTarget > queue.getSize()) {
					ResponseUtils.respondInstantlyEphemeral(interaction, "Please make sure the index provided is in the bounds of the Queue size");
					return;
				}
				queue.skip(jumpTarget - 1);
				ResponseUtils.respondInstantlyEphemeral(interaction, "Jumped to Track " + jumpTarget + "!");
			}

			case UNEXPECTED -> ResponseUtils.respondInstantlyEphemeral(interaction, "Something unexpected happened!");
		}
	}
}