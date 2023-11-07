package org.poyo;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.poyo.audio.AudioLoadError;
import org.poyo.audio.CustomAudioPlayerManager;

public class Cache {

	private HashMap<String, List<AudioTrack>> cache;
	private CustomAudioPlayerManager playerManager;

	public Cache(CustomAudioPlayerManager playerManager) {
		this.cache = new HashMap<>();
		this.playerManager = playerManager;
	}

	public Optional<List<AudioTrack>> retrieve(String link) {
		if (cache.containsKey(link)) {
			return Optional.of(cache.get(link));
		}
		return Optional.empty();
	}

	public Iterable<List<AudioTrack>> iter() {
		return cache.values();
	}

	public boolean store(String link) {
		if (!cache.containsKey(link)) {
			var result = playerManager.loadItemSync(new AudioReference(link, null));
			if (result.isOk()) {
				try {
					cache.put(link, result.orElseThrow());
				} catch (AudioLoadError e) {
					throw new RuntimeException(e);
				}
			}
			return true;
		}
		return false;
	}

	public boolean store(List<String> links) {
		if (links == null)
			return true;

		boolean success = true;
		for (String link : links) {
			if (!store(link)) {
				success = false;
			}
		}
		return success;
	}

	public boolean empty() {
		return this.cache.isEmpty();
	}

	public boolean free(String link) {
		if (cache.containsKey(link)) {
			cache.remove(link);
			return true;
		}
		return false;
	}

}
