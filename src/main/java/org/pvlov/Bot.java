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

import java.util.Optional;

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

        api.addServerVoiceChannelMemberJoinListener(this);
        api.addServerVoiceChannelMemberLeaveListener(this);
        api.addSlashCommandCreateListener(this);
        this.derPate = Utils.decodeTrack(this.playerManager, Bot.DERPATE);
    }

    @Override
    public void onServerVoiceChannelMemberJoin(ServerVoiceChannelMemberJoinEvent event) {
        // For now, ignore all Non-Enrico joins
        if (event.getUser().getId() != ENRICO_ID) {
            return;
        }
        event.getChannel().connect().thenAccept(audioConnection -> {
            //Only update connection if the Voice Channel has changed, unneeded reconnection makes the bot leave and then rejoin the same channel
            if (!currConnection.getChannel().equals(audioConnection)) {
                currConnection = audioConnection;
                queue.registerAudioDestination(audioConnection);
            }
            derPate.ifPresent(audioTrack -> queue.playNow(audioTrack.makeClone()));
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
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
            Utils.sendQuickEphemeralResponse(interaction, "Pong!");
        } else if (interaction.getCommandName().equals("play")) {
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
                                Utils.sendQuickEphemeralResponse(interaction, "You need to be in a Voice-Channel to use the /play command");
                            }
                    );

        } else if (interaction.getCommandName().equals("skip")) {
            if (!queue.isRunning()) {
                Utils.sendQuickEphemeralResponse(interaction, "The Bot is not playing Music, skip ignored");
                return;
            }
            Utils.sendQuickEphemeralResponse(interaction, "Skipped Track!");
            queue.skip();

        } else if (interaction.getCommandName().equals("playlist")) {
            var embedBuilder = new EmbedBuilder();

            // why not let it be a simple for loop?
            // get() should still be O(1), since it's based on Array
            AudioTrack curr = null;
            int counter = 0;
            for (var it = queue.iter(); it.hasNext(); curr = it.next()) {
                embedBuilder.addField(String.valueOf(counter + 1), curr.getInfo().title, true);
                counter++;
            }
            Utils.sendQuickEphemeralResponse(interaction, embedBuilder);

        } else if (interaction.getCommandName().equals("stop")) {
            queue.clear();
            api.getYourself().getConnectedVoiceChannel(interaction.getServer().get())
                    .ifPresent(ServerVoiceChannel::disconnect);

        } else if (interaction.getCommandName().equals("volume")) {
            if (!queue.isRunning()) {
                Utils.sendQuickEphemeralResponse(interaction, "Bot is not currently playing!");
                return;
            }
            long arg = interaction.getArguments().get(0).getLongValue().get();

            if (arg < 0 || arg > 100) {
                Utils.sendQuickEphemeralResponse(interaction, "Make sure to only specify a value between 0 and 100");
                return;
            }
            Utils.sendQuickEphemeralResponse(interaction, "Adjusted Volume!");
            queue.setVolume((int) arg);
        }
    }
}
