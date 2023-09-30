package org.pvlov;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioConnection;
import org.javatuples.Pair;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

public class AudioQueue extends AudioEventAdapter implements AudioLoadResultHandler, Iterable<Pair<Integer, AudioTrack>> {
    private final Deque<AudioTrack> audioQueue;
    private final LavaAudioPlayer audioPlayer;

    public static AudioQueue buildQueue(AudioPlayerManager playerManager, DiscordApi api) {
        AudioSourceManagers.registerRemoteSources(playerManager);
        return new AudioQueue(new LavaAudioPlayer(api, playerManager.createPlayer()));
    }

    public AudioQueue(LavaAudioPlayer audioPlayer) {
        this.audioQueue = new ArrayDeque<>();
        this.audioPlayer = audioPlayer;
        this.audioPlayer.addListener(this);
    }

    public void registerAudioDestination(AudioConnection connection) {
        connection.setAudioSource(audioPlayer);
    }

    public boolean isRunning() {
        return this.audioPlayer.isRunning();
    }

    public void setVolume(int volume) {
        audioPlayer.setVolume(volume);
    }

    public void playNow(AudioTrack track) {
        if (!audioQueue.isEmpty()) {
            this.audioQueue.pop();
        }

        this.audioQueue.addFirst(track);
        audioPlayer.playAudio(audioQueue.peek());
    }

    public void playNowAll(List<AudioTrack> tracks) {
        if (!audioQueue.isEmpty()) {
            this.audioQueue.pop();
        }

        for (int i = tracks.size() - 1; i >= 0; i--) {
            this.audioQueue.addFirst(tracks.get(i).makeClone());
        }

        audioPlayer.playAudio(audioQueue.peek());
    }

    public void playNowAll(Iterable<AudioTrack> tracks) {
        if (!audioQueue.isEmpty()) {
            this.audioQueue.pop();
        }

        Stack<AudioTrack> stack = new Stack<AudioTrack>();

        for (AudioTrack track : tracks) {
            stack.push(track);
        }

        while (!stack.empty()) {
            this.audioQueue.addFirst(stack.pop().makeClone());
        }

        audioPlayer.playAudio(audioQueue.peek());
    }

    public void skip() {
        audioQueue.pop();
        if (audioQueue.isEmpty()) {
            if (audioPlayer.isRunning()) {
                audioPlayer.stopAudioPlayer();
            }
            return;
        }
        audioPlayer.playAudio(audioQueue.peek());
    }

    public Iterable<AudioTrack> iter() {
        return this.audioQueue;
    }

    public void clear() {
        if (!audioQueue.isEmpty()) {
            if (audioPlayer.isRunning()) {
                audioPlayer.stopAudioPlayer();
            }
        }
        audioQueue.clear();
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
            audioPlayer.playAudio(audioQueue.peek());
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
        audioQueue.pop();

        if (audioQueue.isEmpty()) {
            audioPlayer.setIsRunning(false);
            return;
        }
        this.audioPlayer.playAudio(audioQueue.peek());
    }

    @Override
    public Iterator<Pair<Integer, AudioTrack>> iterator() {
        return new Iterator<>() {
            private int counter = 0;
            private final Iterator<AudioTrack> iter = audioQueue.iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Pair<Integer, AudioTrack> next() {
                return new Pair<Integer, AudioTrack>(counter++, iter.next());
            }
        };
    }
}
