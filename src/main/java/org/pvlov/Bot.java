package org.pvlov;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberJoinListener;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

public class Bot implements ServerVoiceChannelMemberJoinListener, SlashCommandCreateListener {
    private final long ENRICO_ID = 625040314117128192L;
    private final String NEVER_GONNA_GIVE_YOU_UP = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    private final String GODFATHER = "https://www.youtube.com/watch?v=HWqKPWO5T4o";
    DiscordApi api;

    LavaAudioScheduler audioScheduler;

    public Bot(String token) {
        this.api = new DiscordApiBuilder().setToken(token).login().join();
        audioScheduler = new LavaAudioScheduler(api);
        SlashCommand.with("volume", "Adjust the Volume between 0 and 100")
                .addOption(SlashCommandOption.createLongOption("volume", "the new volume value", true)).createGlobal(api).join();

        api.addServerVoiceChannelMemberJoinListener(this);
        api.addSlashCommandCreateListener(this);
    }

    @Override
    public void onServerVoiceChannelMemberJoin(ServerVoiceChannelMemberJoinEvent event) {
        for (ServerVoiceChannel voiceChannel : event.getServer().getVoiceChannels()) {
            if (voiceChannel.getConnectedUsers().stream().anyMatch(user -> user.getId() == ENRICO_ID)) {
                voiceChannel.connect().thenAccept(audioConnection -> {
                    audioConnection.setAudioSource(audioScheduler.getAudioPlayer());
                    audioScheduler.clearQueue();
                    audioScheduler.skipTrack();
                    audioScheduler.enqueue(GODFATHER);
                }).exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
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

            if (audioScheduler.getAudioPlayer().isRunning()) {
                audioScheduler.enqueue(link);
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
                                    .addField("Playing: ", link)
                    )
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
                audioConnection.setAudioSource(audioScheduler.getAudioPlayer());
                audioScheduler.enqueue(link);
            });
        } else if (interaction.getCommandName().equals("skip")) {
            if (!audioScheduler.getAudioPlayer().isRunning()) {
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
            audioScheduler.skipTrack();
        } else if (interaction.getCommandName().equals("playlist")) {
            var embedBuilder = new EmbedBuilder();
            var queue = audioScheduler.getQueue();

            for (int i = 0; i < audioScheduler.getQueue().size(); ++i) {
                embedBuilder.addField(String.valueOf(i + 1), queue.get(i).getInfo().title, true);
            }

            interaction.createImmediateResponder()
                    .addEmbed(embedBuilder)
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();
        } else if (interaction.getCommandName().equals("stop")) {
            audioScheduler.clearQueue();
            interaction.getServer().get().getVoiceChannels().stream()
                    .filter(serverVoiceChannel -> api.getYourself().isConnected(serverVoiceChannel))
                    .findFirst().get()
                    .disconnect();
        }
    }
}
