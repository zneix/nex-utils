package nexutils;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.Arrays;
import java.util.Comparator;

@Slf4j
@PluginDescriptor(
	name = "Nex Utilities",
	description = "Miscellaneous edits/additions related to Nex.",
	tags = {"nex", "nexling", "utilities", "additions", "blood reavers", "altar"}
)
public class NexUtilsPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private NexUtilsConfig config;

	private boolean isNexFightActive = false;
	private int lastNexBarrierValue = 0; // default value for the NEX_BARRIER varbit

	private final Comparator<MenuEntry> ALTAR_TP_OPTION =
		Comparator.comparing(me -> me.getIdentifier() == ObjectID.NEX_ZAROS_ALTAR && me.getOption().equals("Teleport"));

	@Override
	protected void startUp() throws Exception {
		log.debug("Nex Utils started!");
	}

	@Override
	protected void shutDown() throws Exception {
		isNexFightActive = false;
		log.debug("Nex Utils stopped!");
	}

	@Provides
	NexUtilsConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(NexUtilsConfig.class);
	}

	@Subscribe
	public void onPostMenuSort(PostMenuSort event) {
		if (!config.altarLeftClickTp() || isNexFightActive) {
			return;
		}

		Menu menu = client.getMenu();
		menu.setMenuEntries(Arrays.stream(menu.getMenuEntries()).sorted(ALTAR_TP_OPTION).toArray(MenuEntry[]::new));
	}

	@Subscribe
	public void onSoundEffectPlayed(SoundEffectPlayed event) {
		if (!config.muteBloodReavers() || !Constants.BLOOD_REAVER_ATTACK_SOUNDS.contains(event.getSoundId())) {
			return;
		}

		if (isInKcRoom(client.getLocalPlayer().getWorldLocation())) {
			event.consume();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event) {
		if (event.getVarbitId() != VarbitID.NEX_BARRIER) {
			return;
		}

		// Nex fight has either just ended or the player has left the fight
		if (lastNexBarrierValue == 3) {
			isNexFightActive = false;
		}

		lastNexBarrierValue = event.getValue();
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event) {
		// Only proceed if we need to start a fight
		if (isNexFightActive || client.getVarbitValue(VarbitID.NEX_BARRIER) != 3) {
			return;
		}

		Actor actor = event.getActor();
		// When a hitsplat is applied to Nex, this means the fight is active
		// Checking for hitsplat works better in this case because if the intent is to leave, team will usually not attack her
		if (actor instanceof NPC && "Nex".equals(actor.getName())) {
			isNexFightActive = true;
		}
	}

	// Check whether player is within bounds of room with ancient minions - the one before Nex bank
	private boolean isInKcRoom(WorldPoint position) {
		if (position.getX() >= 2849 && position.getX() <= 2899) {
			return position.getY() >= 5194 && position.getY() <= 5228;
		}
		return false;
	}
}
