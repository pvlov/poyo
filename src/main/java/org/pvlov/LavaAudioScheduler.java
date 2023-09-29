package org.pvlov;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.javacord.api.DiscordApi;

import java.util.ArrayList;

public class LavaAudioScheduler extends AudioEventAdapter implements AudioLoadResultHandler {
    private final ArrayList<AudioTrack> audioQueue;
    private final AudioPlayerManager playerManager;
    private final LavaAudioPlayer audioPlayer;

    public LavaAudioScheduler(DiscordApi api) {
        this.audioQueue = new ArrayList<>();
        playerManager = new DefaultAudioPlayerManager();
        //playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        AudioSourceManagers.registerRemoteSources(playerManager);
        audioPlayer = new LavaAudioPlayer(api, playerManager.createPlayer());
        audioPlayer.addListener(this);
    }

    public void enqueue(String link) {
        playerManager.loadItem(link, this);
    }

    public void skipTrack() {
        audioQueue.remove(0);
        if (audioQueue.isEmpty()) {
            if (audioPlayer.isRunning()) {
                audioPlayer.stopAudioPlayer();
            }
            return;
        }
        audioQueue.remove(0);
        audioPlayer.playAudio(audioQueue.get(0));
    }

    public void clearQueue() {
        audioQueue.clear();
    }

    public LavaAudioPlayer getAudioPlayer() {
        return this.audioPlayer;
    }

    public ArrayList<AudioTrack> getQueue() {
        return this.audioQueue;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        boolean wasEmpty = audioQueue.isEmpty();
        audioQueue.add(track);
        if (wasEmpty) {
            audioPlayer.playAudio(track);
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        boolean wasEmpty = audioQueue.isEmpty();
        audioQueue.addAll(playlist.getTracks());

        if (wasEmpty) {
            audioPlayer.playAudio(audioQueue.get(0));
        }
    }

    @Override
    public void noMatches() {
        System.out.println("noMatches");
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        System.out.println("failed");
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // An AudioTrack gets replaced when the last one was skipped, so we ignore this
        if (endReason.equals(AudioTrackEndReason.REPLACED)) {
            return;
        }
        audioQueue.remove(0);

        if (audioQueue.isEmpty()) {
            audioPlayer.setIsRunning(false);
            return;
        }
        this.audioPlayer.playAudio(audioQueue.get(0));
    }
}
