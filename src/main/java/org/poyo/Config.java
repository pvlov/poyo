package org.poyo;

import java.util.*;

public class Config {
    private String DISCORD_TOKEN;
    private Map<Long, String> VIP_TRACKS;
    private String PLAY_NICKNAME;
    private Map<String, List<Long>> BLACK_LIST;

    public Config() {
    }

    public static Config createTemplate() {
        Config config = new Config();
        config.DISCORD_TOKEN = "<Your Discord Bot Token>";
        config.VIP_TRACKS = Map.of(16514721914L, "<a youtube link>");
        config.PLAY_NICKNAME = "<a funny Nickname>";
        config.BLACK_LIST = Map.of("ping", List.of(5919218L));
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

    public boolean isBlackListed(Long ID, String command) {
        // if a command is not set in the blacklist, its assumed everyone gets access
        if (!BLACK_LIST.containsKey(command)) {
            return false;
        }
        return BLACK_LIST.get(command).contains(ID);
    }
}
