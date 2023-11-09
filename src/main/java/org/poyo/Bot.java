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
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberJoinListener;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberLeaveListener;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.javatuples.Pair;
import org.poyo.audio.AudioQueue;
import org.poyo.audio.CustomAudioPlayerManager;
import org.poyo.util.ResponseUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.inspector.TagInspector;
import org.yaml.snakeyaml.introspector.BeanAccess;

import java.io.*;
import java.util.*;

public class Bot implements ServerVoiceChannelMemberJoinListener, ServerVoiceChannelMemberLeaveListener,
        SlashCommandCreateListener {

    public static final Logger LOG = LogManager.getLogger();

    private static final String BOT_NAME = "Poyo";
    private static final String NEVER_GONNA_GIVE_YOU_UP = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    private final CustomAudioPlayerManager playerManager;
    private final AudioQueue queue;
    private final Cache audioCache;
    private final List<Long> VIPs;
    public DiscordApi api;
    // TODO: Multiple connections at once
    private AudioConnection currConnection;

    private final Config config;

    public Bot()  {

        this.config = readConfigFile();
        this.api = new DiscordApiBuilder()
                .setToken(config.getToken())
                .login()
                .join();

        this.playerManager = new CustomAudioPlayerManager();
        this.queue = AudioQueue.buildQueue(this.playerManager, api);
        this.audioCache = new Cache(playerManager);
        this.VIPs = new ArrayList<>();

        createSlashCommands();
        api.addServerVoiceChannelMemberJoinListener(this);
        api.addServerVoiceChannelMemberLeaveListener(this);
        api.addSlashCommandCreateListener(this);

        var VIPTracks = config.getAllVIPTrackLinks();

        if (VIPTracks.isEmpty()) {
            LOG.warn("No VIP tracks set.");
        }
        this.audioCache.store(VIPTracks);

        var VIPIDs = config.getAllVIPIDs();
        if (VIPIDs.isEmpty()) {
            LOG.warn("No VIP's set.");
        }
        this.VIPs.addAll(VIPIDs);
        startupCheck();
    }

    private Config readConfigFile()  {
        final String configFilePath = getWorkingDir() + "config.yaml";
        File configFile = new File(configFilePath);

        try (FileReader fileReader = new FileReader(configFile)) {
            var options = new LoaderOptions();
            TagInspector tagInspector = tag -> tag.getClassName().equals(Config.class.getName());
            options.setTagInspector(tagInspector);

            Yaml yaml = new Yaml(options);
            yaml.setBeanAccess(BeanAccess.FIELD);

            return yaml.loadAs(fileReader, Config.class);

        } catch (IOException ignored) {
            LOG.error("Could not find config.yaml at " + configFilePath);
            LOG.info("Creating template config.yaml at " + configFilePath);

            Config template = Config.createTemplate();

            DumperOptions options = new DumperOptions();
            options.setExplicitStart(false);

            Yaml yaml = new Yaml();
            yaml.setBeanAccess(BeanAccess.FIELD);
            try (Writer writer = new FileWriter(configFilePath)) {
                yaml.dump(template, writer);
            } catch (IOException lol) {
                LOG.error("Could not write Template config.yaml to " + configFilePath);
            }
            LOG.info("Please fill out the field 'DISCORD_TOKEN' in the config.yaml at " + configFilePath);
            System.exit(0);
            return null;
        }
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
                        .setLongMinValue(1)
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
            long VIP_ID = event.getUser().getId();
            var audioTracks = audioCache.retrieve(config.getVIPTrackLink(VIP_ID));

            audioTracks.ifPresent(tracks -> {
                api.getYourself().updateNickname(audioConnection.getServer(), config.getNickname());
                queue.playNowAll(audioTracks.orElseThrow());
            });
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
        var args = interaction.getArguments();

        switch (Utils.parseCommandName(interaction.getCommandName())) {
            case PING -> ResponseUtils.respondInstantlyEphemeral(interaction, "Pong!");

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

    private String getWorkingDir() {
        String folder = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        folder = folder.replace("\\", "/");
        folder = folder.substring(0, folder.lastIndexOf("/") + 1);
        return folder;
    }
}
