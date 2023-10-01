package org.pvlov.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.audio.AudioSourceBase;

// A dedicated class to playing music to declutter the Bot class
public class LavaAudioPlayer extends AudioSourceBase {
    private final AudioPlayer audioPlayer;
    private AudioFrame lastFrame;

    private boolean isRunning;

    public LavaAudioPlayer(DiscordApi api, AudioPlayer audioPlayer) {
        super(api);
        this.audioPlayer = audioPlayer;
    }

    public void playAudio(AudioTrack track) {
        audioPlayer.playTrack(track);
        isRunning = true;
    }

    public void addListener(AudioEventListener listener) {
        this.audioPlayer.addListener(listener);
    }

    public void pauseAudioPlayer() {
        this.audioPlayer.setPaused(true);
    }

    public void stopAudioPlayer() {
        this.audioPlayer.stopTrack();
        this.isRunning = false;
    }

    public void setVolume(int volume) {
        audioPlayer.setVolume(volume);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setIsRunning(boolean bool) {
        this.isRunning = bool;
    }

    @Override
    public byte[] getNextFrame() {
        if (lastFrame == null) {
            return null;
        }
        return applyTransformers(lastFrame.getData());
    }

    @Override
    public boolean hasNextFrame() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public boolean hasFinished() {
        return false;
    }

    @Override
    public AudioSource copy() {
        return new LavaAudioPlayer(getApi(), audioPlayer);
    }
}
