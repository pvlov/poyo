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

public class AudioQueue extends AudioEventAdapter
        implements AudioLoadResultHandler, Iterable<Pair<Integer, AudioTrack>> {
    private final Deque<AudioTrack> audioQueue;
    private final LavaAudioPlayer audioPlayer;

    /**
     * Build an audio queue using an player manager and a discord api.
     * 
     * @param playerManager
     * @param api
     * @return
     */
    public static AudioQueue buildQueue(AudioPlayerManager playerManager, DiscordApi api) {
        AudioSourceManagers.registerRemoteSources(playerManager);
        return new AudioQueue(new LavaAudioPlayer(api, playerManager.createPlayer()));
    }

    /**
     * Create a new Audio queue with a given audio player. (Most of the time you
     * want to use buildQueue)
     * 
     * @param audioPlayer
     */
    public AudioQueue(LavaAudioPlayer audioPlayer) {
        this.audioQueue = new ArrayDeque<>();
        this.audioPlayer = audioPlayer;
        this.audioPlayer.addListener(this);
    }

    /**
     * Returns the size of the queue (including the currently playing track)
     * 
     * @return
     */
    public int getSize() {
        return this.audioQueue.size();
    }

    /**
     * This will set this AudioQueue as the source of an AudioConnection.
     * The connection can be safely destroyed without unregistering the source.
     * 
     * @param connection
     */
    public void registerAudioDestination(AudioConnection connection) {
        connection.setAudioSource(audioPlayer);
    }

    /**
     * Tells whether the player is currently playing music.
     * 
     * @return
     */
    public boolean isRunning() {
        return this.audioPlayer.isRunning();
    }

    /**
     * Sets the volume of the audio player.
     * 
     * @param volume
     */
    public void setVolume(int volume) {
        audioPlayer.setVolume(volume);
    }

    /**
     * Moves a track to the front of the queue and pops the currently playing track.
     * 
     * @param track
     */
    public void playNow(AudioTrack track) {
        if (!audioQueue.isEmpty()) {
            this.audioQueue.pop();
        }

        this.audioQueue.addFirst(track);
        audioPlayer.playAudio(audioQueue.peek());
    }

    /**
     * Moves all tracks in order to the front of the queue and pops the currently
     * playing track.
     * 
     * @param tracks
     */
    public void playNowAll(List<AudioTrack> tracks) {
        if (!audioQueue.isEmpty()) {
            this.audioQueue.pop();
        }

        for (int i = tracks.size() - 1; i >= 0; i--) {
            this.audioQueue.addFirst(tracks.get(i).makeClone());
        }

        audioPlayer.playAudio(audioQueue.peek());
    }

    /**
     * Moves all tracks in order to the front of the queue and pops the currently
     * playing track.
     * 
     * @param tracks
     */
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

    /**
     * Skips the currently playing track (pops it from the queue).
     */
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

    /**
     * Skips a number of tracks in the queue.
     * 
     * @param n The number of tracks to be skipped (the currently playing track
     *          included)
     */
    public void skip(long n) {
        for (int i = 0; i < n; ++i) {
            skip();
        }
    }

    /**
     * 
     * @return Iterator over the queue.
     */
    public Iterable<AudioTrack> iter() {
        return this.audioQueue;
    }

    /**
     * Clears the queue (aswell as the currently playing song) and stops the audio
     * player.
     */
    public void clear() {
        if (!audioQueue.isEmpty()) {
            if (audioPlayer.isRunning()) {
                audioPlayer.stopAudioPlayer();
            }
        }
        audioQueue.clear();
    }

    /**
     * Plays the loaded track if the queue is empty. Otherwise enques it.
     */
    @Override
    public void trackLoaded(AudioTrack track) {
        boolean wasEmpty = audioQueue.isEmpty();
        audioQueue.add(track);
        if (wasEmpty) {
            audioPlayer.playAudio(track);
        }
    }

    /**
     * Adds all songs of the playlist to the queue and plays the first one.
     */
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

    /**
     * Plays the next track in the queue or stops the audio player when the queue is
     * empty.
     */
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

    /**
     * Returns an iterator with the track position in the queue as first value.
     */
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
