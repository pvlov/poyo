package org.pvlov;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
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
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberJoinListener;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberLeaveListener;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Bot implements ServerVoiceChannelMemberJoinListener, ServerVoiceChannelMemberLeaveListener,
        SlashCommandCreateListener {

    public static final Logger LOG = LogManager.getLogger();

    private static final String BOT_NAME = "Poyo";
    private static final String NEVER_GONNA_GIVE_YOU_UP = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

    public DiscordApi api;

    private final AudioPlayerManager playerManager;
    private final AudioQueue queue;
    private final Cache audioCache;
    private final List<Long> VIPs;

    // TODO: Multiple connections at once
    private AudioConnection currConnection;

    public Bot() {
        this.api = new DiscordApiBuilder().setToken(Config.INSTANCE.getString("DISCORD_TOKEN").orElseGet(() -> {
            LOG.error("No DISCORD_TOKEN set. Abort.");
            return null;
        })).login().join();

        this.playerManager = new DefaultAudioPlayerManager();
        this.queue = AudioQueue.buildQueue(this.playerManager, api);
        this.audioCache = new Cache(playerManager);
        this.VIPs = new ArrayList<>();

        createSlashCommands();
        api.addServerVoiceChannelMemberJoinListener(this);
        api.addServerVoiceChannelMemberLeaveListener(this);
        api.addSlashCommandCreateListener(this);

        this.audioCache.store(Config.INSTANCE.getStringArray("VIP_TRACKS").orElseGet(() -> {
            LOG.warn("No VIP tracks set.");
            return new ArrayList<>();
        }));

        this.VIPs.addAll(Config.INSTANCE.getLongArray("VIPS").orElseGet(() -> {
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
        builders.add(new SlashCommandBuilder().setName("ping").setDescription("For testing purposes."));
        builders.add(new SlashCommandBuilder()
                .setName("play").addOption(new SlashCommandOptionBuilder().setRequired(false)
                        .setType(SlashCommandOptionType.STRING).setName("link").setDescription("The link to the song.")
                        .build())
                .setDescription("Play a song."));
        builders.add(new SlashCommandBuilder().setName("skip").setDescription("Skip the current song."));
        builders.add(new SlashCommandBuilder().setName("playlist").setDescription("Print the current playlist."));
        builders.add(new SlashCommandBuilder().setName("stop").setDescription("Stop playing."));
        builders.add(new SlashCommandBuilder()
                .setName("volume").addOption(new SlashCommandOptionBuilder().setRequired(true)
                        .setType(SlashCommandOptionType.LONG).setLongMinValue(0).setLongMaxValue(100).setName("value")
                        .setDescription("The volume.")
                        .build())
                .setDescription("Set the volume of the bot."));
        builders.add(new SlashCommandBuilder()
                .setName("jump").addOption(new SlashCommandOptionBuilder()
                        .setRequired(true)
                        .setType(SlashCommandOptionType.LONG)
                        .setMinLength(1)
                        .setName("index")
                        .setDescription("the target index")
                        .build()
                )
                .setDescription("Jump to a specific Song in the Playlist"));
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

            queue.playNowAll(this.audioCache.iter());
            api.getYourself().updateNickname(audioConnection.getServer(),
                    Config.INSTANCE.getString("PLAY_NICKNAME").orElse(BOT_NAME));
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    @Override
    public void onServerVoiceChannelMemberLeave(ServerVoiceChannelMemberLeaveEvent event) {
        if (currConnection != null) {
            if (currConnection.getChannel().equals(event.getChannel())) {
                if (!event.getChannel().getConnectedUserIds().stream().anyMatch(userid -> this.VIPs.contains(userid))) {
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
        var args = interaction.getArguments();

        switch (Utils.parseCommandName(interaction.getCommandName())) {
            case PING -> Utils.sendQuickEphemeralResponse(interaction, "Pong!");

            case PLAY -> {

                final String link;

                if (args.size() >= 1 && args.get(0).getStringValue().isPresent()) {
                    link = args.get(0).getStringValue().get();
                } else {
                    link = NEVER_GONNA_GIVE_YOU_UP;
                }

                if (queue.isRunning()) {
                    playerManager.loadItem(link, queue);
                    Utils.sendQuickEphemeralResponse(interaction, "Track successfully added to Queue! :D");
                    return;
                }

                interaction.getUser().getConnectedVoiceChannel(interaction.getServer().get())
                        .ifPresentOrElse(
                                targetVoiceChannel -> {
                                    targetVoiceChannel.connect().thenAccept(audioConnection -> {
                                        queue.registerAudioDestination(audioConnection);
                                        playerManager.loadItem(link, queue);
                                    });

                                    Utils.sendQuickEphemeralResponse(interaction, new EmbedBuilder()
                                            .setAuthor(interaction.getUser())
                                            .addField("Playing: ", link));
                                },
                                () -> {
                                    Utils.sendQuickEphemeralResponse(interaction,
                                            "You need to be in a Voice-Channel in order to use the /play command");
                                });
            }

            case SKIP -> {
                if (!queue.isRunning()) {
                    Utils.sendQuickEphemeralResponse(interaction, "The Bot is not playing Music, skip ignored");
                    return;
                }
                Utils.sendQuickEphemeralResponse(interaction, "Skipped Track!");
                queue.skip();
            }

            case PLAYLIST -> {
                var embedBuilder = new EmbedBuilder();

                for (Pair<Integer, AudioTrack> entry : queue) {
                    embedBuilder.addField(String.valueOf(entry.getValue0() + 1), entry.getValue1().getInfo().title, true);
                }
                Utils.sendQuickEphemeralResponse(interaction, embedBuilder);
            }

            case STOP -> {
                queue.clear();
                api.getYourself().getConnectedVoiceChannel(interaction.getServer().get())
                        .ifPresent(ServerVoiceChannel::disconnect);
                Utils.sendQuickEphemeralResponse(interaction, "Bot was stopped");
            }

            case VOLUME -> {
                if (!queue.isRunning()) {
                    Utils.sendQuickEphemeralResponse(interaction, "Bot is not currently playing!");
                    return;
                }
                long arg = interaction.getArguments().get(0).getLongValue().get();

                Utils.sendQuickEphemeralResponse(interaction, "Adjusted Volume!");
                queue.setVolume((int) arg);
            }

            case JUMP -> {

                final long jumpTarget;
                if (args.size() >= 1 && args.get(0).getLongValue().isPresent()) {
                    jumpTarget = args.get(0).getLongValue().get();
                } else {
                    Utils.sendQuickEphemeralResponse(interaction, "Please only use a number as the target for the /jump command");
                    return;
                }
                // Be aware that for easier use and compatibility with /playlist, jump will be 1-indexed
                if (jumpTarget > queue.getSize()) {
                    Utils.sendQuickEphemeralResponse(interaction, "Please make sure the index provided is in the bounds of the Queue size");
                    return;
                }
                queue.skip(jumpTarget - 1);
                Utils.sendQuickEphemeralResponse(interaction, "Jumped to Track " + jumpTarget + "!");
            }

            case UNEXPECTED -> Utils.sendQuickEphemeralResponse(interaction, "Something unexpected happened!");
        }
    }
}
