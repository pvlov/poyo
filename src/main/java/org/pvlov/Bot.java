package org.pvlov;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberJoinListener;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberLeaveListener;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.javatuples.Pair;

import java.util.Optional;

public class Bot implements ServerVoiceChannelMemberJoinListener, ServerVoiceChannelMemberLeaveListener,
        SlashCommandCreateListener {
    private static final String BOT_NAME = "Poyo";
    private static final String NEVER_GONNA_GIVE_YOU_UP = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    private static final String DERPATE = "https://www.youtube.com/watch?v=HWqKPWO5T4o";

    private final long PATE_ID;

    DiscordApi api;
    AudioPlayerManager playerManager;
    AudioQueue queue;
    Optional<AudioTrack> derPate;

    // TODO: Multiple connections at once
    AudioConnection currConnection;

    // 625040314117128192
    public Bot(String token, Long pate) {
        this.api = new DiscordApiBuilder().setToken(token).login().join();
        this.playerManager = new DefaultAudioPlayerManager();
        this.queue = AudioQueue.buildQueue(this.playerManager, api);

        api.addServerVoiceChannelMemberJoinListener(this);
        api.addServerVoiceChannelMemberLeaveListener(this);
        api.addSlashCommandCreateListener(this);
        this.derPate = Utils.decodeTrack(this.playerManager, Bot.DERPATE);
        this.PATE_ID = pate;
    }

    @Override
    public void onServerVoiceChannelMemberJoin(ServerVoiceChannelMemberJoinEvent event) {
        // For now, ignore all Non-Enrico joins
        if (event.getUser().getId() != PATE_ID) {
            return;
        }
        event.getChannel().connect().thenAccept(audioConnection -> {
            // Only update connection if the Voice Channel has changed, unneeded
            // reconnection makes the bot leave and then rejoin the same channel
            if (currConnection == null || !currConnection.getChannel().equals(audioConnection.getChannel())) {
                currConnection = audioConnection;
                queue.registerAudioDestination(audioConnection);
            }

            derPate.ifPresent(audioTrack -> queue.playNow(audioTrack));
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    @Override
    public void onServerVoiceChannelMemberLeave(ServerVoiceChannelMemberLeaveEvent event) {
        if (event.getUser().getId() == PATE_ID) {
            if (currConnection != null) {
                currConnection.close().join();
                queue.clear();
                currConnection = null;

                // Preload
                derPate.ifPresent(audioTrack -> this.derPate = Optional.of(audioTrack.makeClone()));
            }
        }
    }

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();
        var args = interaction.getArguments();

        switch (Utils.parseCommandName(interaction.getCommandName())) {
            case PING -> {
                Utils.sendQuickEphemeralResponse(interaction, "Pong!");
            }

            case PLAY -> {
                String link = args.get(0).getStringValue().orElse(NEVER_GONNA_GIVE_YOU_UP);

                if (queue.isRunning()) {
                    playerManager.loadItem(link, queue);
                    Utils.sendQuickEphemeralResponse(interaction, "Track successfully added to Queue! :D");
                    return;
                }

                Utils.sendQuickEphemeralResponse(interaction, new EmbedBuilder()
                        .setAuthor(interaction.getUser())
                        .addField("Playing: ", link));

                interaction.getUser().getConnectedVoiceChannel(interaction.getServer().get())
                        .ifPresentOrElse(
                                targetVoiceChannel -> {
                                    targetVoiceChannel.connect().thenAccept(audioConnection -> {
                                        queue.registerAudioDestination(audioConnection);
                                        playerManager.loadItem(link, queue);
                                    });
                                },
                                () -> {
                                    Utils.sendQuickEphemeralResponse(interaction,
                                            "You need to be in a Voice-Channel to use the /play command");
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

                for (Pair<Integer, AudioTrack> entry : queue.enumerate()) {
                    embedBuilder.addField(String.valueOf(entry.getValue0()), entry.getValue1().getInfo().title, true);
                }
                Utils.sendQuickEphemeralResponse(interaction, embedBuilder);
            }

            case STOP -> {
                queue.clear();
                api.getYourself().getConnectedVoiceChannel(interaction.getServer().get())
                        .ifPresent(ServerVoiceChannel::disconnect);
            }

            case VOLUME -> {
                if (!queue.isRunning()) {
                    Utils.sendQuickEphemeralResponse(interaction, "Bot is not currently playing!");
                    return;
                }
                long arg = interaction.getArguments().get(0).getLongValue().get();

                if (arg < 0 || arg > 100) {
                    Utils.sendQuickEphemeralResponse(interaction,
                            "Make sure to only specify a value between 0 and 100");
                    return;
                }
                Utils.sendQuickEphemeralResponse(interaction, "Adjusted Volume!");
                queue.setVolume((int) arg);
            }

            case UNEXPECTED -> Utils.sendQuickEphemeralResponse(interaction, "Something unexpected happened!");
        }
    }
}
