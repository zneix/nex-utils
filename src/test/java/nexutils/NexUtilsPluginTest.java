package nexutils;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class NexUtilsPluginTest {
	public static void main(String[] args) throws Exception {
		ExternalPluginManager.loadBuiltin(NexUtilsPlugin.class);
		RuneLite.main(args);
	}
}
