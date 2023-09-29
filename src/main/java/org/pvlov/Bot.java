package org.pvlov;

import java.util.Optional;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberJoinListener;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberLeaveListener;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class Bot implements ServerVoiceChannelMemberJoinListener, ServerVoiceChannelMemberLeaveListener,
        SlashCommandCreateListener {
    private static final String BOT_NAME = "Poyo";
    private static final long ENRICO_ID = 625040314117128192L;
    private static final String NEVER_GONNA_GIVE_YOU_UP = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    private static final String DERPATE = "https://www.youtube.com/watch?v=HWqKPWO5T4o";

    DiscordApi api;
    AudioPlayerManager playerManager;
    AudioQueue queue;
    Optional<AudioTrack> derPate;

    // TODO: Multiple connections at once
    AudioConnection currConnection;

    public Bot(String token) {
        this.api = new DiscordApiBuilder().setToken(token).login().join();
        this.playerManager = new DefaultAudioPlayerManager();
        this.queue = Utils.buildQueue(this.playerManager, api);
        SlashCommand.with("volume", "Adjust the Volume between 0 and 100")
                .addOption(SlashCommandOption.createLongOption("volume", "the new volume value", true))
                .createGlobal(api).join();

        api.addServerVoiceChannelMemberJoinListener(this);
        api.addServerVoiceChannelMemberLeaveListener(this);
        api.addSlashCommandCreateListener(this);
        this.derPate = Utils.decodeTrack(this.playerManager, Bot.DERPATE);
    }

    @Override
    public void onServerVoiceChannelMemberJoin(ServerVoiceChannelMemberJoinEvent event) {
        for (ServerVoiceChannel voiceChannel : event.getServer().getVoiceChannels()) {
            if (voiceChannel.getConnectedUsers().stream().anyMatch(user -> user.getId() == ENRICO_ID)) {
                voiceChannel.connect().thenAccept(audioConnection -> {
                    currConnection = audioConnection;
                    queue.registerAudioDestination(audioConnection);

                    derPate.ifPresent(audioTrack -> queue.playNow(audioTrack.makeClone()));
                }).exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
            }
        }
    }

    @Override
    public void onServerVoiceChannelMemberLeave(ServerVoiceChannelMemberLeaveEvent event) {
        if (event.getUser().getId() == ENRICO_ID) {
            if (currConnection != null) {
                currConnection.close().join();
                queue.clear();
                currConnection = null;
            }
        }
    }

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();
        var args = interaction.getArguments();

        if (interaction.getCommandName().equals("ping")) {
            interaction.createImmediateResponder()
                    .setContent("Pong!")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();
        } else if (interaction.getCommandName().equals("play")) {
            String link = args.get(0).getStringValue().orElse(NEVER_GONNA_GIVE_YOU_UP);

            if (queue.isRunning()) {
                playerManager.loadItem(link, queue);
                interaction.createImmediateResponder()
                        .setContent("Track successfully added to Queue! :D")
                        .setFlags(MessageFlag.EPHEMERAL)
                        .respond();
                return;
            }

            interaction.createImmediateResponder()
                    .addEmbed(
                            new EmbedBuilder()
                                    .setAuthor(interaction.getUser())
                                    .addField("Playing: ", link))
                    .respond();
            var voiceChannels = interaction.getUser().getConnectedVoiceChannels();
            ServerVoiceChannel targetVoiceChannel = null;

            for (var voiceChannel : voiceChannels) {
                if (!interaction.getUser().isConnected(voiceChannel)) {
                    continue;
                }
                targetVoiceChannel = voiceChannel;
            }

            if (targetVoiceChannel == null) {
                interaction.createImmediateResponder()
                        .setContent("You need to be in a Voice-Channel to use the /play command")
                        .setFlags(MessageFlag.EPHEMERAL)
                        .respond();
                return;
            }

            targetVoiceChannel.connect().thenAccept(audioConnection -> {
                queue.registerAudioDestination(audioConnection);
                playerManager.loadItem(link, queue);
            });
        } else if (interaction.getCommandName().equals("skip")) {
            if (!queue.isRunning()) {
                interaction.createImmediateResponder()
                        .setContent("The Bot is not playing Music, skip ignored")
                        .setFlags(MessageFlag.EPHEMERAL)
                        .respond();
                return;
            }
            interaction.createImmediateResponder()
                    .setContent("Skipped Track!")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();
            queue.skip();
        } else if (interaction.getCommandName().equals("playlist")) {
            var embedBuilder = new EmbedBuilder();

            AudioTrack curr = null;
            int counter = 0;
            for (var it = queue.iter(); it.hasNext(); curr = it.next()) {
                embedBuilder.addField(String.valueOf(counter + 1), curr.getInfo().title, true);
                counter++;
            }

            interaction.createImmediateResponder()
                    .addEmbed(embedBuilder)
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();
        } else if (interaction.getCommandName().equals("stop")) {
            queue.clear();
            interaction.getServer().get().getVoiceChannels().stream()
                    .filter(serverVoiceChannel -> api.getYourself().isConnected(serverVoiceChannel))
                    .findFirst().get()
                    .disconnect();
        }
    }
}
