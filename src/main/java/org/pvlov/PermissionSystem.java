package org.pvlov;

import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionChoice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PermissionSystem {
	public enum PermissionLevel {
		IGNORED,
		BASIC,
		ALL
	}

	public static List<SlashCommandOptionChoice> getSlashCommandOptionChoices() {
		return Stream.of(PermissionLevel.values()).map(v -> SlashCommandOptionChoice.create(v.name(), v.ordinal())).toList();
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

	public void registerCommand(Utils.SlashCommand command, PermissionLevel permissionLevel) {
		permissionNeeded.put(command.name(), permissionLevel);
	}

	public boolean checkPermissions(SlashCommandInteraction interaction) {
		PermissionLevel neededLevel = permissionNeeded.get(Utils.SlashCommand.fromCommandName(interaction.getFullCommandName()).name());
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
		return userLevel.compareTo(neededLevel) >= 0;
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
