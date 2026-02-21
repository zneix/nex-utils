package nexutils;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(NexUtilsConfig.GROUP_KEY)
public interface NexUtilsConfig extends Config {
	String GROUP_KEY = "nex-utils";
	String ALTAR_LEFT_CLICK_TP = "altar-left-click-tp";
	String ALTAR_PREVENT_NO_ZAROS_ITEM = "altar-prevent-no-zaros-item";
	String MUTE_BLOOD_REAVERS = "mute-blood-reavers";

	@ConfigSection(
		name = "Altar",
		description = "Changes related to the zarosian altar",
		position = 0,
		closedByDefault = false
	)
	String altarSection = "Altar";

	@ConfigItem(
		keyName = ALTAR_LEFT_CLICK_TP,
		name = "Left click tp when not in fight",
		description = "Swaps left click on the altar to 'Teleport' when Nex isn't present.<br>"
			+ "NOTE: This setting will still take effect if Nex spawns but hasn't been attacked yet (e.g. when leaving late)",
		position = 0,
		section = altarSection
	)
	default boolean altarLeftClickTp() {
		return false;
	}

	@ConfigItem(
		keyName = ALTAR_PREVENT_NO_ZAROS_ITEM,
		name = "Prevent use with no zaros item",
		description = "Deprioritizes usage of altar options when no zaros items are equipped.",
		position = 10,
		section = altarSection
	)
	default boolean altarPreventNoZarosItem() {
		return true;
	}

	@ConfigItem(
		keyName = MUTE_BLOOD_REAVERS,
		name = "Mute Blood Reavers in kc room",
		description = "Mutes Blood Reaver attacks in the minion room before bank.",
		position = 0
	)
	default boolean muteBloodReavers() {
		return false;
	}
}
