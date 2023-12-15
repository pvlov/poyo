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
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.inspector.TagInspector;
import org.yaml.snakeyaml.introspector.BeanAccess;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Bot implements ServerVoiceChannelMemberJoinListener, ServerVoiceChannelMemberLeaveListener {

    public static final Logger LOG = LogManager.getLogger();
    private static final String BOT_NAME = "Poyo";
    private final CustomAudioPlayerManager playerManager;
    private final AudioQueue queue;
    private final Cache audioCache;
    private final List<Long> VIPs;
    private final SlashCommandHandler slashCommandHandler;
    private final Config config;
    public DiscordApi api;
    // TODO: Multiple connections at once
    private AudioConnection currConnection;

    public Bot() {

        this.config = readConfigFile();
        this.api = new DiscordApiBuilder()
                .setToken(config.getToken())
                .login()
                .join();

        this.playerManager = new CustomAudioPlayerManager();
        this.queue = AudioQueue.buildQueue(this.playerManager, api);
        this.audioCache = new Cache(playerManager);
        this.VIPs = new ArrayList<>();
        this.slashCommandHandler = new SlashCommandHandler(this);

        createSlashCommands();
        api.addServerVoiceChannelMemberJoinListener(this);
        api.addServerVoiceChannelMemberLeaveListener(this);
        api.addSlashCommandCreateListener(slashCommandHandler);

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

    private Config readConfigFile() {
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

    private String getWorkingDir() {
        String folder = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        folder = folder.replace("\\", "/");
        folder = folder.substring(0, folder.lastIndexOf("/") + 1);
        return folder;
    }

    public Config getConfig() {
        return this.config;
    }
    public AudioQueue getAudioQueue() {
        return this.queue;
    }
    public DiscordApi getApi() {
        return this.api;
    }
    public CustomAudioPlayerManager getPlayerManager() {
        return this.playerManager;
    }
}
