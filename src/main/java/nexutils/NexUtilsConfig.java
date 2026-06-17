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

	String ENTRANCE_PREVENT_EMPTY_INV = "entrance-prevent-empty-inv";
	String ENTRANCE_PREVENT_CHUGGING_BARREL = "entrance-prevent-chugging-barrel";
	String ENTRANCE_PREVENT_SATURATED_HEART = "entrance-prevent-saturated-heart";
	String ENTRANCE_PREVENT_NO_BOTD = "entrance-prevent-no-botd";

	String MUTE_BLOOD_REAVERS = "mute-blood-reavers";
	String REOPEN_CHAT_ON_FINISH = "reopen-chat-on-finish";

	@ConfigSection(
		name = "Altar",
		description = "Changes related to the zarosian altar",
		position = 0,
		closedByDefault = false
	)
	String altarSection = "Altar";

	@ConfigSection(
		name = "Entrance prevention",
		description = "Changes helping to avoid entering with wrong inventory",
		position = 5,
		closedByDefault = false
	)
	String entrancePreventSection = "Entrance prevention";

	@ConfigSection(
		name = "Miscellaneous",
		description = "Uncategorized changes",
		position = 10,
		closedByDefault = false
	)
	String miscSection = "Miscellaneous";

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
		keyName = ENTRANCE_PREVENT_EMPTY_INV,
		name = "Non-full inventory",
		description = "Deprioritizes usage of entrance barrier to Nex arena when your inventory isn't completely full.<br>"
			+ "Helps if you accidentally don't withdraw e.g. food items after pre-potting.",
		position = 0,
		section = entrancePreventSection
	)
	default boolean entrancePreventEmptyInv() {
		return true;
	}

	@ConfigItem(
		keyName = ENTRANCE_PREVENT_CHUGGING_BARREL,
		name = "Chugging barrel",
		description = "Deprioritizes usage of entrance barrier to Nex arena when<br>"
			+ "Chugging barrel is present in your inventory.",
		position = 10,
		section = entrancePreventSection
	)
	default boolean entrancePreventChuggingBarrel() {
		return true;
	}

	@ConfigItem(
		keyName = ENTRANCE_PREVENT_SATURATED_HEART,
		name = "Saturated/Imbued heart",
		description = "Deprioritizes usage of entrance barrier to Nex arena when<br>"
			+ "Saturated or Imbued heart is present in your inventory.",
		position = 20,
		section = entrancePreventSection
	)
	default boolean entrancePreventSaturatedHeart() {
		return true;
	}

	@ConfigItem(
		keyName = ENTRANCE_PREVENT_NO_BOTD,
		name = "No Book of the dead",
		description = "Deprioritizes usage of entrance barrier to Nex arena when<br>"
			+ "Book of the dead is NOT present in your inventory.",
		position = 30,
		section = entrancePreventSection
	)
	default boolean entrancePreventNoBOTD() {
		return true;
	}

	@ConfigItem(
		keyName = MUTE_BLOOD_REAVERS,
		name = "Mute Blood Reavers in kc room",
		description = "Mutes Blood Reaver attacks in the minion room before bank.",
		position = 0,
		section = miscSection
	)
	default boolean muteBloodReavers() {
		return false;
	}

	@ConfigItem(
		keyName = REOPEN_CHAT_ON_FINISH,
		name = "Open game chat when Nex dies",
		description = "Opens game chat upon a successful kill.<br>"
			+ "Aims to help to include kill time & mvp status for loot screenshots.",
		position = 20,
		section = miscSection
	)
	default boolean reopenChatOnFinish() {
		return false;
	}
}
