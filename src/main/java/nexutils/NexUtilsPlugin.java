package nexutils;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.CommandExecuted;
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

@Slf4j
@PluginDescriptor(
	name = "Nex Utilities",
	description = "Miscellaneous edits/additions related to Nex.",
	tags = { "nex", "nexling", "utilities", "additions", "blood reavers", "altar" }
)
public class NexUtilsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private NexUtilsConfig config;

	private boolean isNexFightActive = false;
	private int lastNexBarrierValue = 0; // default value for the varbit

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Nex Utils started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		isNexFightActive = false;
		log.debug("Nex Utils stopped!");
	}

	@Provides
	NexUtilsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NexUtilsConfig.class);
	}

	// TODO: Test if Nex detection actually works
	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		if (!config.altarLeftClickTp() || isNexFightActive)
		{
			return;
		}

		MenuEntry[] menuEntries = client.getMenu().getMenuEntries();
		int teleportIndex = -1;
		int topIndex = menuEntries.length - 1; // top option always has the last index
		// Check if Altar's 'Teleport' menu entry is present
        for (int i = 0; i < menuEntries.length; i++)
		{
			var entry = menuEntries[i];
			//log.debug("me debug: {} {} {} {}", entry.getType(), entry.getOption(), entry.getTarget(), entry.getIdentifier());
			if (entry.getIdentifier() == ObjectID.NEX_ZAROS_ALTAR && entry.getOption().equals("Teleport"))
			{
				teleportIndex = i;
				break;
			}
        }

		// Altar's 'Teleport' menu entry was not found, early out
		if (teleportIndex == -1)
		{
			return;
		}

		// TODO: This works, but needs improving
		MenuEntry entry1 = menuEntries[teleportIndex];
		MenuEntry entry2 = menuEntries[topIndex];

		menuEntries[teleportIndex] = entry2;
		menuEntries[topIndex] = entry1;
		client.getMenu().setMenuEntries(menuEntries);
	}

	@Subscribe
	public void onSoundEffectPlayed(SoundEffectPlayed event)
	{
		if (!config.muteBloodReavers() || !Constants.BLOOD_REAVER_ATTACK_SOUNDS.contains(event.getSoundId()))
		{
			return;
		}

		if (isInKcRoom(client.getLocalPlayer().getWorldLocation()))
		{
			event.consume();
		}
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		// for debugging - type ::nu 1 or ::nu 0 to enable/disable fight state
		if (!"nu".equals(event.getCommand()) || event.getArguments().length < 1)
		{
			return;
		}

		if (event.getArguments()[0].equals("1"))
		{
			isNexFightActive = true;
		}
		else if (event.getArguments()[0].equals("0"))
		{
			isNexFightActive = false;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() != VarbitID.NEX_BARRIER)
		{
			return;
		}

		// Nex fight has either just ended or the player has left the fight
		if (lastNexBarrierValue == 3)
		{
			isNexFightActive = false;
		}

		lastNexBarrierValue = event.getValue();
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		// Only proceed if we need to start a fight
		if (isNexFightActive || client.getVarbitValue(VarbitID.NEX_BARRIER) != 3)
		{
			return;
		}

		Actor actor = event.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}

		// When a hitsplat is applied to Nex, this means the fight is active
		// Checking for hitsplat works better in this case because if the intent is to leave, team will usually not attack her
		if ("Nex".equals(actor.getName()))
		{
			isNexFightActive = true;
		}
	}

	// Check whether player is within bounds of room with ancient minions - the one before Nex bank
	private boolean isInKcRoom(WorldPoint position)
	{
		if (position.getX() >= 2849 && position.getX() <= 2899)
		{
			return position.getY() >= 5194 && position.getY() <= 5228;
		}
		return false;
	}
}
