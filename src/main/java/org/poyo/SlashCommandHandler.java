package org.poyo;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.javatuples.Pair;
import org.poyo.util.ResponseUtils;

public class SlashCommandHandler implements SlashCommandCreateListener {

    Bot bot;
    private static final String NEVER_GONNA_GIVE_YOU_UP = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    public SlashCommandHandler(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();
        var args = interaction.getArguments();
        String commandName = interaction.getCommandName();

        if (bot.getConfig().isBlackListed(interaction.getUser().getId(), commandName)) {
            ResponseUtils.respondInstantlyEphemeral(interaction, "You are not allowed to use the command: " + commandName);
            return;
        }

        switch (parseCommand(commandName)) {
            case PING -> ResponseUtils.respondInstantlyEphemeral(interaction, "Pong!");

            case PLAY -> {

                if (interaction.getServer().isEmpty()) {
                    ResponseUtils.respondInstantlyEphemeral(interaction, "You need to be in a Voice-Channel in order to use the /play command");
                    return;
                }

                final String link = args.get(0).getStringValue().orElse(NEVER_GONNA_GIVE_YOU_UP);

                if (bot.getAudioQueue().isRunning()) {
                    var future = bot.getPlayerManager().loadItem(link);
                    future.thenAccept(result -> {
                        if (result.isOk()) {
                            bot.getAudioQueue().enqeue(result.orElseThrow());
                            ResponseUtils.respondLaterPublic(interaction, "Successfully added: " + result);
                        } else {
                            ResponseUtils.respondLaterEphemeral(interaction, "Something went wrong while loading tracks");
                        }
                    }).exceptionally(err -> {
                        ResponseUtils.respondLaterEphemeral(interaction, "Bot is busy, try again later!");
                        return null;
                    });
                    return;
                }

                interaction.getUser().getConnectedVoiceChannel(interaction.getServer().orElseThrow())
                        .ifPresentOrElse(
                                targetVoiceChannel -> targetVoiceChannel.connect().thenAccept(audioConnection -> {
                                    bot.getAudioQueue().registerAudioDestination(audioConnection);
                                    var future = bot.getPlayerManager().loadItem(link);

                                    future.thenAccept(result -> {
                                        if (result.isOk()) {
                                            bot.getAudioQueue().enqeue(result.orElseThrow());
                                            bot.getAudioQueue().start();
                                            ResponseUtils.respondLaterPublic(interaction,
                                                    new EmbedBuilder()
                                                            .setAuthor(interaction.getUser())
                                                            .addField("Playing: ", bot.getAudioQueue().getNowPlaying().getInfo().title));
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
                if (!bot.getAudioQueue().isRunning()) {
                    ResponseUtils.respondInstantlyEphemeral(interaction, "The Bot is not playing Music, skip ignored");
                    return;
                }
                ResponseUtils.respondInstantlyEphemeral(interaction, "Skipped Track!");
                bot.getAudioQueue().skip();
            }

            case PLAYLIST -> {
                // TODO: Fails with empty bot.getAudioQueue(), add if-check for empty queue
                var embedBuilder = new EmbedBuilder();

                for (Pair<Integer, AudioTrack> entry : bot.getAudioQueue()) {
                    embedBuilder.addField(String.valueOf(entry.getValue0() + 1), entry.getValue1().getInfo().title, true);
                }
                ResponseUtils.respondInstantlyEphemeral(interaction, embedBuilder);
            }

            case STOP -> {
                bot.getAudioQueue().clear();
                bot.getApi().getYourself().getConnectedVoiceChannel(interaction.getServer().orElseThrow())
                        .ifPresent(ServerVoiceChannel::disconnect);
                ResponseUtils.respondInstantlyEphemeral(interaction, "Bot was stopped");
            }

            case VOLUME -> {
                if (!bot.getAudioQueue().isRunning()) {
                    ResponseUtils.respondInstantlyEphemeral(interaction, "Bot is not currently playing!");
                    return;
                }
                long arg = interaction.getArguments().get(0).getLongValue().orElseThrow();

                ResponseUtils.respondInstantlyEphemeral(interaction, "Adjusted Volume!");
                bot.getAudioQueue().setVolume((int) arg);
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
                if (jumpTarget > bot.getAudioQueue().getSize()) {
                    ResponseUtils.respondInstantlyEphemeral(interaction, "Please make sure the index provided is in the bounds of the Queue size");
                    return;
                }
                bot.getAudioQueue().skip(jumpTarget - 1);
                ResponseUtils.respondInstantlyEphemeral(interaction, "Jumped to Track " + jumpTarget + "!");
            }

            case UNKNOWN -> ResponseUtils.respondInstantlyEphemeral(interaction, "Something unexpected happened!");
        }
    }

    public enum SlashCommand {
        PING,
        PLAY,
        PLAYLIST,
        SKIP,
        STOP,
        VOLUME,
        JUMP,
        UNKNOWN,
    }
    public SlashCommand parseCommand(String commandName) {
        try {
            return SlashCommand.valueOf(commandName.toUpperCase().strip());
        } catch (IllegalArgumentException ignored) {
            return SlashCommand.UNKNOWN;
        }
    }
}
