package nexutils;

import com.google.inject.Provides;

import javax.annotation.Nullable;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
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
	private ClientThread clientThread;

	@Inject
	private NexUtilsConfig config;

	@Inject
	private AltarOverlay altarOverlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ItemManager itemManager;

	@Nullable
	public GameObject altarObject;
	@Getter
	private BufferedImage altarOverlayImage;
	@Getter
	private boolean zarosItemEquipped = false;
	private boolean emptyInventorySlots = false;
	private boolean isNexFightActive = false;
	private int lastNexBarrierValue = 0; // default value for the NEX_BARRIER varbit

	private static String menuEntryColor(String optionText) {
		return ColorUtil.wrapWithColorTag(optionText, new Color(255,106,213,255).brighter());
	}

	private final String ALTAR_DUMMY_OPTION_TEXT = menuEntryColor("Equip a zaros item!");
	private final String ENTRANCE_DUMMY_OPTION_TEXT = menuEntryColor("Fill your inventory!");
	private final Comparator<MenuEntry> ALTAR_TP_OPTION =
		Comparator.comparing(me -> me.getIdentifier() == ObjectID.NEX_ZAROS_ALTAR && me.getOption().equals("Teleport"));

	@Override
	protected void startUp() throws Exception {
		log.debug("Nex Utils started!");
		altarOverlayImage = combineItemImages(
			resize(ImageUtil.loadImageResource(getClass(), "zaros_logo.png"), 36, 32), // use same dimensions as an item sprite
			itemManager.getImage(ItemID.BANK_FILLER)
		);
		overlayManager.add(altarOverlay);

		clientThread.invoke(() -> {
			final ItemContainer wornItems = client.getItemContainer(InventoryID.WORN);
			if (wornItems != null) {
				zarosItemEquipped = checkAnyZarosItemEquipped(wornItems);
			}

			final ItemContainer inventoryItems = client.getItemContainer(InventoryID.INV);
			if (inventoryItems != null) {
				emptyInventorySlots = checkAnyEmptyInvSlots(inventoryItems);
			}
		});
	}

	@Override
	protected void shutDown() throws Exception {
		isNexFightActive = false;
		altarObject = null;
		altarOverlayImage = null;
		overlayManager.remove(altarOverlay);
		log.debug("Nex Utils stopped!");
	}

	@Provides
	NexUtilsConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(NexUtilsConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e) {
		if (e.getGameState() == GameState.LOGGING_IN || e.getGameState() == GameState.LOGIN_SCREEN || e.getGameState() == GameState.HOPPING) {
			altarObject = null;
		}
		if (e.getGameState() == GameState.LOGGED_IN && (!client.getTopLevelWorldView().isInstance() && client.getLocalPlayer().getWorldLocation().getRegionID() != Constants.NEX_ARENA_REGION)) {
			log.debug("altar is being set to null! {} {} {}", client.getLocalPlayer().getWorldLocation(), client.getLocalPlayer().getWorldLocation().getRegionID(), client.getTopLevelWorldView().isInstance());
			altarObject = null;
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event) {
		if (event.getGameObject().getId() == ObjectID.NEX_ZAROS_ALTAR) {
			altarObject = event.getGameObject();
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event) {
		if (event.getGameObject() != null && event.getGameObject().getId() == ObjectID.NEX_ZAROS_ALTAR) {
			altarObject = null;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		if (event.getContainerId() == InventoryID.WORN) {
			zarosItemEquipped = checkAnyZarosItemEquipped(event.getItemContainer());
		}

		if (event.getContainerId() == InventoryID.INV) {
			emptyInventorySlots = checkAnyEmptyInvSlots(event.getItemContainer());
		}
	}

	@Subscribe
	public void onPostMenuSort(PostMenuSort event) {
		if (altarObject == null) {
			return;
		}

		MenuEntry[] entries = client.getMenu().getMenuEntries();
		if (entries.length < 1) {
			return;
		}
		// Entries are always returned in "reverse" - top option is last
		MenuEntry topEntry = entries[entries.length - 1];

		if (topEntry.getIdentifier() == ObjectID.NEX_FIGHT_BARRIER && topEntry.getOption().startsWith("Pass (")) {
			if (emptyInventorySlots && config.entrancePreventEmptyInv()) {
				deprioritizeMenuEntry(entries, ENTRANCE_DUMMY_OPTION_TEXT);
			}
		} else if (topEntry.getIdentifier() == ObjectID.NEX_ZAROS_ALTAR) {
			if (!zarosItemEquipped && config.altarPreventNoZarosItem()) {
				deprioritizeMenuEntry(entries, ALTAR_DUMMY_OPTION_TEXT);
			} else if (!isNexFightActive && config.altarLeftClickTp()) {
				// Sort entries by putting teleport option on top
				Menu menu = client.getMenu();
				menu.setMenuEntries(Arrays.stream(menu.getMenuEntries()).sorted(ALTAR_TP_OPTION).toArray(MenuEntry[]::new));
			}
		}
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
		int newValue = event.getValue();

		// Nex fight has either just ended or the player has left the fight
		if (lastNexBarrierValue == 3) {
			isNexFightActive = false;
			if (newValue == 2 && config.reopenChatOnFinish()) {
				// (3 -> 2) Nex died and fight was completed successfully, reopen chatbox if desired
				// Only toggle/open chat if it's not set to 'Game'
				if (client.getVarcIntValue(VarClientID.CHAT_VIEW) != 1) {
					// Script ID 175 - clientscript,chat_button_onop
					// argument #1 - operation to perform on selected chat tab (1 = opens the tab)
					// argument #2 - value of CHAT_VIEW / chat tab to open (1 = 'Game')
					clientThread.invokeLater(() -> client.runScript(175, 1, 1));
				}
			}
		}

		lastNexBarrierValue = newValue;
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

	@Subscribe
	public void onCommandExecuted(CommandExecuted event) {
		if (!event.getCommand().equals("nu")) {
			return;
		}
		log.debug("empty slots? {} item {} altar obj {} fight active {} loc {} regID {} instance? {}", emptyInventorySlots, zarosItemEquipped, altarObject, isNexFightActive,
			client.getLocalPlayer().getWorldLocation(), client.getLocalPlayer().getWorldLocation().getRegionID(), client.getTopLevelWorldView().isInstance());
	}

	// Check whether player is within bounds of room with ancient minions - the one before Nex bank
	private boolean isInKcRoom(WorldPoint position) {
		if (position.getX() >= 2849 && position.getX() <= 2899) {
			return position.getY() >= 5194 && position.getY() <= 5228;
		}
		return false;
	}

	// Check every item slot to see if we have god protection
	private boolean checkAnyZarosItemEquipped(ItemContainer container) {
		final Item weapon = container.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		if (weapon != null && ZarosItems.Weapon.contains(weapon.getId())) {
			return true;
		}

		final Item offhand = container.getItem(EquipmentInventorySlot.SHIELD.getSlotIdx());
		if (offhand != null && ZarosItems.Offhand.contains(offhand.getId())) {
			return true;
		}

		final Item head = container.getItem(EquipmentInventorySlot.HEAD.getSlotIdx());
		if (head != null && ZarosItems.Head.contains(head.getId())) {
			return true;
		}

		final Item cape = container.getItem(EquipmentInventorySlot.CAPE.getSlotIdx());
		if (cape != null && ZarosItems.Cape.contains(cape.getId())) {
			return true;
		}

		final Item body = container.getItem(EquipmentInventorySlot.BODY.getSlotIdx());
		if (body != null && ZarosItems.Body.contains(body.getId())) {
			return true;
		}

		final Item legs = container.getItem(EquipmentInventorySlot.LEGS.getSlotIdx());
		if (legs != null && ZarosItems.Legs.contains(legs.getId())) {
			return true;
		}

		final Item hands = container.getItem(EquipmentInventorySlot.GLOVES.getSlotIdx());
		if (hands != null && ZarosItems.Gloves.contains(hands.getId())) {
			return true;
		}

		final Item feet = container.getItem(EquipmentInventorySlot.BOOTS.getSlotIdx());
		if (feet != null && ZarosItems.Boots.contains(feet.getId())) {
			return true;
		}

		final Item ammo = container.getItem(EquipmentInventorySlot.AMMO.getSlotIdx());
		if (ammo != null && ZarosItems.Ammo.contains(ammo.getId())) {
			return true;
		}

		final Item neck = container.getItem(EquipmentInventorySlot.AMULET.getSlotIdx());
		return neck != null && ZarosItems.Amulet.contains(neck.getId());
	}

	private boolean checkAnyEmptyInvSlots(ItemContainer inventoryItems) {
		for (Item invItem : inventoryItems.getItems()) {
			if (invItem.getId() == -1) {
				return true;
			}
		}
		return false;
	}

	// Insert a dummy menu option and put rest of the options down
	private void deprioritizeMenuEntry(MenuEntry[] entries, String textToShow) {
		// Create list of new entries which will include new dummy option on top
		MenuEntry[] newEntries = new MenuEntry[entries.length + 1];
		System.arraycopy(entries, 0, newEntries, 0, entries.length);
		// Insert new dummy entry; putting it at the end makes it the top option
		newEntries[newEntries.length - 1] = client.getMenu().createMenuEntry(0).setType(MenuAction.CANCEL).setOption(textToShow).setTarget("");
		client.getMenu().setMenuEntries(newEntries);
	}

	public static BufferedImage resize(BufferedImage img, int newW, int newH) {
		BufferedImage resizedImg = new BufferedImage(newW, newH, img.getType());
		Graphics2D g = resizedImg.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(img, 0, 0, newW, newH, null);
		g.dispose();
		return resizedImg;
	}

	// Taken from escape crystal notify plugin: https://github.com/Xylot/escape-crystal-notify/blob/7038395a1c95d0a9ea0b083fd2648f2b90fab510/src/main/java/com/escapecrystalnotify/EscapeCrystalNotifyPlugin.java#L952-L970
	private BufferedImage combineItemImages(BufferedImage... images) {
		BufferedImage backgroundImage = images[0];

		BufferedImage result = new BufferedImage(backgroundImage.getWidth(), backgroundImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

		Graphics2D graphics = result.createGraphics();

		graphics.setComposite(AlphaComposite.SrcOver);
		graphics.drawImage(backgroundImage, 0, 0, null);

		for (int i = 1; i < images.length; i++) {
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
			graphics.drawImage(images[i], 0, 0, null);
		}

		graphics.dispose();

		return result;
	}
}
