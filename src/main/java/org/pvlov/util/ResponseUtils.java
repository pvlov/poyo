package org.pvlov.util;

import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

public class ResponseUtils {
    public static void respondInstantlyEphemeral(SlashCommandInteraction interaction, String response) {
        interaction.createImmediateResponder()
                .setContent(response)
                .setFlags(MessageFlag.EPHEMERAL)
                .respond();
    }

    public static void respondInstantlyEphemeral(SlashCommandInteraction interaction, EmbedBuilder embed) {
        interaction.createImmediateResponder()
                .addEmbed(embed)
                .setFlags(MessageFlag.EPHEMERAL)
                .respond();
    }

    public static void respondLaterEphemeral(SlashCommandInteraction interaction, String response) {
        interaction
                .respondLater(true)
                .join()
                .setContent(response)
                .update().join();
    }

    public static void respondLaterEphemeral(SlashCommandInteraction interaction, EmbedBuilder embed) {
        interaction
                .respondLater(true)
                .join()
                .addEmbed(embed)
                .update().join();
    }
    public static void respondLaterPublic(SlashCommandInteraction interaction, String response) {
        interaction
                .respondLater()
                .join()
                .setContent(response)
                .update().join();
    }

    public static void respondLaterPublic(SlashCommandInteraction interaction, EmbedBuilder embed) {
        interaction
                .respondLater()
                .join()
                .addEmbed(embed)
                .update().join();
    }
}
