package org.poyo;

import java.util.*;

public class Config {


	private String DISCORD_TOKEN;
	private Map<Long, String> VIP_TRACKS;
	private String PLAY_NICKNAME;

	public Config() {}

	public static Config createTemplate() {
		Config config = new Config();
		config.DISCORD_TOKEN = "<Your Discord Bot Token>";
		config.VIP_TRACKS = new HashMap<>();
		config.PLAY_NICKNAME = "<a funny Nickname>";
		return config;
	}

	public String getToken() {
		return this.DISCORD_TOKEN;
	}
	public String getNickname() {
		return this.PLAY_NICKNAME;
	}

	public Collection<String> getAllVIPTrackLinks() {
		return Collections.unmodifiableCollection(this.VIP_TRACKS.values());
	}

	public Set<Long> getAllVIPIDs() {
		return Collections.unmodifiableSet(this.VIP_TRACKS.keySet());
	}

	public String getVIPTrackLink(Long ID) {
		return this.VIP_TRACKS.get(ID);
	}
}
