package org.pvlov.audio;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AudioTrackLoadResultHandler {
    public static AudioTrackLoadResult await(Future<AudioTrackLoadResult> future) {
        ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            return service.submit(() -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }  finally {
            service.shutdown();
        }
    }
}
