package org.pvlov;

import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PermissionSystem {
	public enum PermissionLevel {
		IGNORED(0),
		BASIC(1),
		ALL(2);

		PermissionLevel(int level) {
			this.level = level;
		}

		private final int level;

		public boolean isHigherEqual(PermissionLevel comp) {
			return this.level >= comp.level;
		}
	}

	private final Map<String, PermissionLevel> permissionNeeded;
	private final Map<Long, Map<Long, PermissionLevel>> permissionMapping;

	public PermissionSystem() {
		permissionNeeded = new HashMap<>();
		permissionMapping = new HashMap<>();
	}

	public PermissionSystem(Map<Long, Map<Long, String>> configMap) {
		permissionNeeded = new HashMap<>();
		permissionMapping = configMap.entrySet().stream()
				.collect(
						Collectors.toMap(
								Map.Entry::getKey,
								s -> s.getValue().entrySet().stream()
										.collect(
												Collectors.toMap(
														Map.Entry::getKey,
														u -> PermissionLevel.valueOf(u.getValue().toUpperCase())
												)
										)
						)
				);
	}

	public void registerCommand(String command, PermissionLevel permissionLevel) {
		permissionNeeded.put(command.toUpperCase(), permissionLevel);
	}

	public boolean checkPermissions(SlashCommandInteraction interaction) {
		PermissionLevel neededLevel = permissionNeeded.get(interaction.getFullCommandName().toUpperCase());
		if (neededLevel == null) {
			return true;
		}

		Server server = interaction.getServer().orElse(null);

		// DM interaction with bot
		if (server == null) {
			return true;
		}

		User user = interaction.getUser();

		if (server.isAdmin(user)) {
			return true;
		}

		PermissionLevel userLevel;
		var userMapping = permissionMapping.get(server.getId());
		if (userMapping == null) {
			userLevel = PermissionLevel.BASIC;
		} else {
			userLevel = userMapping.get(interaction.getUser().getId());
			if (userLevel == null) {
				userLevel = PermissionLevel.BASIC;
			}
		}
		return userLevel.isHigherEqual(neededLevel);
	}

	public void setOrUpdatePermission(Server server, User user, PermissionLevel level) {
		if (permissionMapping.containsKey(server.getId())) {
			var userMapping = permissionMapping.get(server.getId());
			userMapping.put(user.getId(), level);
		} else {
			Map<Long, PermissionLevel> userMapping = new HashMap<>();
			userMapping.put(user.getId(), level);
			permissionMapping.put(server.getId(), userMapping);
		}

		var configMap = permissionMapping.entrySet().stream()
				.collect(
						Collectors.toMap(
								Map.Entry::getKey,
								s -> s.getValue().entrySet().stream()
										.collect(
												Collectors.toMap(
														Map.Entry::getKey,
														u -> u.getValue().toString().toUpperCase())
												)
										)
						);

		Config.INSTANCE.setConfig("PERMISSIONS", configMap);
		Config.INSTANCE.saveConfig();
	}
}
