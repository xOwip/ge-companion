/*
 *  ██████╗ ███████╗     ██████╗ ██████╗ ███╗   ███╗██████╗
 * ██╔════╝ ██╔════╝    ██╔════╝██╔═══██╗████╗ ████║██╔══██╗
 * ██║  ███╗█████╗      ██║     ██║   ██║██╔████╔██║██████╔╝
 * ██║   ██║██╔══╝      ██║     ██║   ██║██║╚██╔╝██║██╔═══╝
 * ╚██████╔╝███████╗    ╚██████╗╚██████╔╝██║ ╚═╝ ██║██║
 *  ╚═════╝ ╚══════╝     ╚═════╝ ╚═════╝ ╚═╝     ╚═╝╚═╝
 *
 *  Built by Owip | github.com/xOwip
 *  Live GE prices, interactive price charts, watchlist, bank value tracker, and item search.
 */

package gecompanion;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuAction;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
		name = "GE Companion",
		description = "Live GE prices, bank value tracker, and item search — without tabbing out",
		tags = {"ge", "grand exchange", "price", "bank", "flipping", "merching", "ge tracker"}
)
public class GECompanionPlugin extends Plugin
{
	private static final String PRICES_URL = "https://prices.runescape.wiki/api/v1/osrs/latest";
	private static final String MAPPING_URL = "https://prices.runescape.wiki/api/v1/osrs/mapping";

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private GECompanionConfig config;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private EventBus eventBus;

	@Inject
	private ConfigManager configManager;

	@Inject
	private net.runelite.client.Notifier notifier;

	@Inject
	private net.runelite.client.chat.ChatMessageManager chatMessageManager;

	@Inject
	private net.runelite.client.chat.ChatCommandManager chatCommandManager;

	@Inject
	private net.runelite.client.callback.ClientThread clientThread;

	@Inject
	private net.runelite.client.game.ItemManager itemManager;

	public net.runelite.client.game.ItemManager getItemManager() { return itemManager; }

	private GECompanionPanel panel;
	private NavigationButton navButton;
	private ScheduledExecutorService scheduler;

	// Price data store — itemId -> PriceData
	private final Map<Integer, PriceData> priceCache = new HashMap<>();
	// 24h average prices — itemId -> avg price
	private final Map<Integer, Long> avgPrice24h = new HashMap<>();
	// 1h average prices — itemId -> avg price
	private final Map<Integer, Long> avgPrice1h = new HashMap<>();
	// 6h average prices — itemId -> avg price
	private final Map<Integer, Long> avgPrice6h = new HashMap<>();
	// 1h buy/sell volume — itemId -> volume
	private final Map<Integer, Long> buyVolume1h = new HashMap<>();
	private final Map<Integer, Long> sellVolume1h = new HashMap<>();
	private final Map<Integer, Long> volumeCache = new HashMap<>();
	// Name -> itemId mapping
	private final Map<String, Integer> nameToId = new HashMap<>();
	// Item GE limits — itemId -> limit
	private final Map<Integer, Integer> itemLimits = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		panel = new GECompanionPanel(config, this, configManager);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/ge_companion_icon_32_v2.png");

		navButton = NavigationButton.builder()
				.tooltip("GE Companion")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
		eventBus.register(this);

		// Start price refresh scheduler
		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(this::fetchPrices, 0, 60, TimeUnit.SECONDS);

		// Register !bank chat command
		chatCommandManager.registerCommandAsync("!bank", this::handleBankCommand);
		chatCommandManager.registerCommandAsync("!wealth", this::handleBankCommand);

		log.debug("GE Companion started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		eventBus.unregister(this);
		if (scheduler != null)
		{
			scheduler.shutdown();
		}
		chatCommandManager.unregisterCommand("!bank");
		chatCommandManager.unregisterCommand("!wealth");
		log.debug("GE Companion stopped!");
	}

	public void fetchPrices()
	{
		try
		{
			// Fetch item mapping first if empty
			if (nameToId.isEmpty())
			{
				fetchMapping();
			}

			// Fetch latest prices
			Request request = new Request.Builder()
					.url(PRICES_URL)
					.header("User-Agent", "GE Companion RuneLite Plugin")
					.build();

			try (Response response = okHttpClient.newCall(request).execute())
			{
				if (!response.isSuccessful() || response.body() == null)
				{
					log.warn("Failed to fetch prices: {}", response.code());
					return;
				}

				String body = response.body().string();
				JsonObject json = new JsonParser().parse(body).getAsJsonObject();
				JsonObject data = json.getAsJsonObject("data");

				priceCache.clear();
				for (String key : data.keySet())
				{
					try
					{
						int id = Integer.parseInt(key);
						JsonObject item = data.getAsJsonObject(key);

						long high = item.has("high") && !item.get("high").isJsonNull() ? item.get("high").getAsLong() : 0;
						long low = item.has("low") && !item.get("low").isJsonNull() ? item.get("low").getAsLong() : 0;
						long highTime = item.has("highTime") && !item.get("highTime").isJsonNull() ? item.get("highTime").getAsLong() : 0;
						long lowTime = item.has("lowTime") && !item.get("lowTime").isJsonNull() ? item.get("lowTime").getAsLong() : 0;

						priceCache.put(id, new PriceData(id, high, low, highTime, lowTime));
					}
					catch (NumberFormatException e)
					{
						// skip
					}
				}

				log.debug("Fetched prices for {} items", priceCache.size());
// Fetch all timeframe averages
				fetch24hAverages();
				fetchVolumes();
				fetchTimeframeAverages("1h", avgPrice1h);
				fetchTimeframeAverages("6h", avgPrice6h);

				// Update panel on EDT
				javax.swing.SwingUtilities.invokeLater(() -> {
					panel.onPricesUpdated(priceCache, nameToId, avgPrice24h, avgPrice1h, avgPrice6h, itemLimits, buyVolume1h, sellVolume1h, volumeCache);
					panel.checkPriceAlerts();
				});
			}
		}
		catch (Exception e)
		{
			log.warn("Error fetching prices", e);
		}
	}

	private void fetchTimeframeAverages(String interval, Map<Integer, Long> cache)
{
    try
    {
        Request request = new Request.Builder()
            .url("https://prices.runescape.wiki/api/v1/osrs/" + interval)
            .header("User-Agent", "GE Companion RuneLite Plugin")
            .build();

        try (Response response = okHttpClient.newCall(request).execute())
        {
            if (!response.isSuccessful() || response.body() == null) return;

            String body = response.body().string();
			JsonObject json = new JsonParser().parse(body).getAsJsonObject();
			JsonObject data = json.getAsJsonObject("data");

			cache.clear();
			for (String key : data.keySet())
			{
				try
				{
					int id = Integer.parseInt(key);
					JsonObject item = data.getAsJsonObject(key);
					long avgHigh = item.has("avgHighPrice") && !item.get("avgHighPrice").isJsonNull() ? item.get("avgHighPrice").getAsLong() : 0;
					long avgLow = item.has("avgLowPrice") && !item.get("avgLowPrice").isJsonNull() ? item.get("avgLowPrice").getAsLong() : 0;
					if (avgHigh > 0 && avgLow > 0)
						cache.put(id, (avgHigh + avgLow) / 2);
					else if (avgHigh > 0)
						cache.put(id, avgHigh);
					else if (avgLow > 0)
						cache.put(id, avgLow);
					// Store buy/sell volume for 1h timeframe
					if (interval.equals("1h"))
					{
						long buyVol = item.has("highPriceVolume") && !item.get("highPriceVolume").isJsonNull() ? item.get("highPriceVolume").getAsLong() : 0;
						long sellVol = item.has("lowPriceVolume") && !item.get("lowPriceVolume").isJsonNull() ? item.get("lowPriceVolume").getAsLong() : 0;
						if (buyVol > 0) buyVolume1h.put(id, buyVol);
						if (sellVol > 0) sellVolume1h.put(id, sellVol);
					}
				}
				catch (NumberFormatException e) { }
			}
			log.debug("Fetched {} averages for {} items", interval, cache.size());
		}
	}
	catch (Exception e)
	{
		log.warn("Error fetching {} averages", interval, e);
	}
}


private void fetch24hAverages()
{
    try
    {
        Request request = new Request.Builder()
            .url("https://prices.runescape.wiki/api/v1/osrs/24h")
            .header("User-Agent", "GE Companion RuneLite Plugin")
            .build();

        try (Response response = okHttpClient.newCall(request).execute())
        {
            if (!response.isSuccessful() || response.body() == null) return;

            String body = response.body().string();
			JsonObject json = new JsonParser().parse(body).getAsJsonObject();
			JsonObject data = json.getAsJsonObject("data");

			avgPrice24h.clear();
			for (String key : data.keySet())
			{
				try
				{
					int id = Integer.parseInt(key);
					JsonObject item = data.getAsJsonObject(key);
					long avgHigh = item.has("avgHighPrice") && !item.get("avgHighPrice").isJsonNull() ? item.get("avgHighPrice").getAsLong() : 0;
					long avgLow = item.has("avgLowPrice") && !item.get("avgLowPrice").isJsonNull() ? item.get("avgLowPrice").getAsLong() : 0;
					if (avgHigh > 0 && avgLow > 0)
						avgPrice24h.put(id, (avgHigh + avgLow) / 2);
					else if (avgHigh > 0)
						avgPrice24h.put(id, avgHigh);
					else if (avgLow > 0)
						avgPrice24h.put(id, avgLow);
				}
				catch (NumberFormatException e) { }
			}
            log.debug("Fetched 24h averages for {} items", avgPrice24h.size());
        }
    }
    catch (Exception e)
    {
		log.warn("Error fetching 24h averages", e);
	}
}

	private void fetchVolumes()
	{
		try
		{
			Request request = new Request.Builder()
					.url("https://prices.runescape.wiki/api/v1/osrs/volumes")
					.header("User-Agent", "GE Companion RuneLite Plugin")
					.build();

			try (Response response = okHttpClient.newCall(request).execute())
			{
				if (!response.isSuccessful() || response.body() == null) return;

				String body = response.body().string();
				JsonObject json = new JsonParser().parse(body).getAsJsonObject();
				JsonObject data = json.getAsJsonObject("data");

				volumeCache.clear();
				for (String key : data.keySet())
				{
					try
					{
						int id = Integer.parseInt(key);
						long volume = data.has(key) && !data.get(key).isJsonNull() ? data.get(key).getAsLong() : 0;
						if (volume > 0) volumeCache.put(id, volume);
					}
					catch (NumberFormatException e) { }
				}
				log.debug("Fetched volumes for {} items", volumeCache.size());
			}
		}
		catch (Exception e)
		{
			log.warn("Error fetching volumes", e);
		}
	}

private void fetchMapping()
	{
		try
		{
			Request request = new Request.Builder()
					.url(MAPPING_URL)
					.header("User-Agent", "GE Companion RuneLite Plugin")
					.build();

			try (Response response = okHttpClient.newCall(request).execute())
			{
				if (!response.isSuccessful() || response.body() == null)
				{
					log.warn("Failed to fetch mapping: {}", response.code());
					return;
				}

				String body = response.body().string();
				JsonArray arr = new JsonParser().parse(body).getAsJsonArray();

				for (int i = 0; i < arr.size(); i++)
				{
					JsonObject item = arr.get(i).getAsJsonObject();
					int id = item.get("id").getAsInt();
					String name = item.get("name").getAsString();
					nameToId.put(name.toLowerCase(), id);
					int limit = item.has("limit") ? item.get("limit").getAsInt() : 0;
					itemLimits.put(id, limit);
				}

				log.debug("Loaded mapping for {} items", nameToId.size());
			}
		}
		catch (Exception e)
		{
			log.warn("Error fetching mapping", e);
		}
	}

	public Map<Integer, PriceData> getPriceCache()
	{
		return priceCache;
	}

	public Map<String, Integer> getNameToId()
	{
		return nameToId;
	}
	@Subscribe
	public void onConfigChanged(net.runelite.client.events.ConfigChanged event)
	{
		if (!event.getGroup().equals("gecompanion")) return;
        if (event.getKey().equals("resetBankHistory"))
        {
            if ("true".equals(event.getNewValue()))
            {
                configManager.setConfiguration("gecompanion", "resetBankHistory", false);
                int result = javax.swing.JOptionPane.showOptionDialog(
                        panel,
                        "This will permanently delete all saved bank value history.\nThis cannot be undone. Continue?",
                        "Reset Bank Value History",
                        javax.swing.JOptionPane.YES_NO_OPTION,
                        javax.swing.JOptionPane.WARNING_MESSAGE,
                        null,
                        new Object[]{"Reset All Data", "Cancel"},
                        "Cancel"
                );
                if (result == 0)
                {
                    saveConfig("bankValueLog", "");
                    javax.swing.SwingUtilities.invokeLater(() -> panel.onBankHistoryReset());
                }
            }
            return;
        }
        if (!event.getKey().equals("recentSearches") && !event.getKey().equals("bankValueLog") && !event.getKey().equals("bankValue") && !event.getKey().equals("bankValueHidden") && !event.getKey().equals("pinnedItems"))
		{
			javax.swing.SwingUtilities.invokeLater(() -> panel.showTab(panel.getActiveTab()));
		}
	}
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.rightClickLookup()) return;

		String option = event.getOption();
		if (option == null || !option.equals("Examine")) return;

		MenuEntry[] entries = client.getMenuEntries();
		MenuEntry lastEntry = entries[entries.length - 1];
		int itemId = lastEntry.getItemId();
		if (itemId <= 0) return;
		int canonicalId = itemManager.canonicalize(itemId);
		if (!panel.isItemPriceable(canonicalId)) return;

		final int finalItemId = canonicalId;
		client.createMenuEntry(-1)
				.setOption("Price Check")
				.setTarget(event.getTarget())
				.setType(MenuAction.RUNELITE)
				.onClick(e -> {
					int lookupId = panel.getItemVariantMap().getOrDefault(finalItemId, finalItemId);
					javax.swing.SwingUtilities.invokeLater(() -> {
						clientToolbar.openPanel(navButton);
						panel.openItemLookup(lookupId);
					});
				});
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.BANK.getId()) return;

		ItemContainer bankContainer = event.getItemContainer();
		if (bankContainer == null) return;

		java.util.List<String> newBankItems = new java.util.ArrayList<>();
		java.util.Map<String, Integer> newBankQuantities = new java.util.HashMap<>();

		for (Item item : bankContainer.getItems())
		{
			if (item.getId() <= 0 || item.getQuantity() <= 0) continue;

// Check if item ID exists directly in nameToId
			int originalId = item.getId();
			int lookupId = panel.getItemVariantMap().getOrDefault(originalId, originalId);
			boolean isVariantItem = (originalId != lookupId);

			// Get original item name for variant indicator
			String originalName = null;
			if (isVariantItem) {
				ItemComposition originalComp = itemManager.getItemComposition(originalId);
				if (originalComp != null) originalName = originalComp.getName();
			}

			String foundName = null;
			for (java.util.Map.Entry<String, Integer> entry : nameToId.entrySet())
			{
				if (entry.getValue() == lookupId)
				{
					foundName = entry.getKey();
					break;
				}
			}
			if (foundName != null)
			{
				String[] words = foundName.split(" ");
				StringBuilder sb = new StringBuilder();
				for (String word : words)
				{
					if (word.length() > 0)
						sb.append(Character.toUpperCase(word.charAt(0)))
								.append(word.substring(1)).append(" ");
				}
				String displayName = sb.toString().trim();
				// Append original name as suffix if variant (separated by |)
				if (isVariantItem && originalName != null) {
					newBankItems.add(displayName + "|" + originalName);
				} else {
					newBankItems.add(displayName);
				}
				newBankQuantities.put(displayName, item.getQuantity());
			}
		}

		// Bank-only value (unchanged calculation — matches RuneLite Bank plugin)
		long bankOnlyValue = 0;
		for (Item item : bankContainer.getItems())
		{
			if (item.getId() <= 0 || item.getQuantity() <= 0) continue;
			long price = itemManager.getItemPrice(item.getId());
			bankOnlyValue += price * item.getQuantity();
		}

// Total wealth = bank + inventory + equipment
		long totalWealthValue = bankOnlyValue;

		ItemContainer invContainer = client.getItemContainer(InventoryID.INVENTORY);
		if (invContainer != null)
		{
			for (Item item : invContainer.getItems())
			{
				if (item.getId() <= 0 || item.getQuantity() <= 0) continue;
				long price = itemManager.getItemPrice(item.getId());
				totalWealthValue += price * item.getQuantity();
				// Add inventory items to bank items list for Top Gainers/Losers
				addItemToList(item, newBankItems, newBankQuantities);
			}
		}

		ItemContainer equipContainer = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipContainer != null)
		{
			for (Item item : equipContainer.getItems())
			{
				if (item.getId() <= 0 || item.getQuantity() <= 0) continue;
				long price = itemManager.getItemPrice(item.getId());
				totalWealthValue += price * item.getQuantity();
				// Add equipment items to bank items list for Top Gainers/Losers
				addItemToList(item, newBankItems, newBankQuantities);
			}
		}

		final long finalBankOnly = bankOnlyValue;
		final long finalTotalWealth = totalWealthValue;
		javax.swing.SwingUtilities.invokeLater(() -> {
			panel.updateBankItems(newBankItems, newBankQuantities, finalBankOnly, finalTotalWealth);
		});
	}

	private void handleBankCommand(net.runelite.api.events.ChatMessage chatMessage, String message)
	{
		if (client.getGameState() != net.runelite.api.GameState.LOGGED_IN) return;

		String[] parts = message.trim().split("\\s+");
		String timeframe = parts.length > 1 ? parts[1].toLowerCase() : null;
// Normalize shorthand timeframes
		if (timeframe != null) {
			switch (timeframe) {
				case "1": case "1h": timeframe = "1h"; break;
				case "6": case "6h": timeframe = "6h"; break;
				case "24": case "24h": timeframe = "24h"; break;
				case "7": case "7d": timeframe = "7d"; break;
				case "30": case "30d": timeframe = "30d"; break;
				case "3": case "3m": timeframe = "3m"; break;
				case "1y": case "365": timeframe = "1y"; break;
				case "all": timeframe = "all"; break;
			}
		}

		long wealth = panel.getTotalWealthValue();
		String prefix = "";

		if (wealth == 0) {
			sendLocalMessage("No bank data — open your bank first!");
			return;
		}

		String wealthStr = "<col=ffffff>" + String.format("%,d", wealth) + "</col> gp";

		if (timeframe == null) {
			updateChatMessage(chatMessage, prefix + "Total Wealth: " + wealthStr);
			return;
		}

		java.util.List<long[]> log = panel.getBankValueLog();
		if (log == null || log.isEmpty()) {
			sendLocalMessage("No bank history available — open your bank a few times to build history!");
			return;
		}

		long nowMs = System.currentTimeMillis();
		long cutoffMs = 0;
		String label = "";
		switch (timeframe) {
			case "1h": cutoffMs = nowMs - 3600_000L; label = "1H"; break;
			case "6h": cutoffMs = nowMs - 21600_000L; label = "6H"; break;
			case "24h": cutoffMs = nowMs - 86400_000L; label = "24H"; break;
			case "7d": cutoffMs = nowMs - 604800_000L; label = "7D"; break;
			case "30d": cutoffMs = nowMs - 2592000_000L; label = "30D"; break;
			case "3m": cutoffMs = nowMs - 7776000_000L; label = "3M"; break;
			case "1y": cutoffMs = nowMs - 31536000_000L; label = "1Y"; break;
			case "all": cutoffMs = Long.MAX_VALUE; label = "All Time"; break;
			default:
				sendLocalMessage("Unknown timeframe: " + timeframe + ". Try: !bank 1h, 6h, 24h, 7d, 30d, 3m, 1y, all");
				return;
		}

		final long cutoff = cutoffMs;
// For "all time" find the oldest entry; otherwise find the entry closest to cutoff
		long[] oldEntry = null;
		if (cutoffMs == Long.MAX_VALUE) {
// All time — find oldest entry
			oldEntry = log.stream()
					.min(java.util.Comparator.comparingLong(e -> e[0]))
					.orElse(null);
		} else {
			long oldWealth2 = log.stream()
					.filter(e -> e[0] * 1000 <= cutoff)
					.mapToLong(e -> e.length > 2 ? e[2] : e[1])
					.reduce((a, b) -> b)
					.orElse(0);
			if (oldWealth2 > 0) {
				oldEntry = new long[]{cutoff / 1000, 0, oldWealth2};
			}
		}

		if (oldEntry == null || (oldEntry.length > 2 ? oldEntry[2] : oldEntry[1]) == 0) {
			sendLocalMessage("No " + label + " history yet — bank value is recorded each time you open your bank. Open your bank more frequently to build history!");
			return;
		}

		long oldWealth = oldEntry.length > 2 ? oldEntry[2] : oldEntry[1];
		long change = wealth - oldWealth;
		double pct = ((double) change / oldWealth) * 100.0;

// For all time, add how long ago the oldest entry was
		String timeAgo = "";
		if (cutoffMs == Long.MAX_VALUE && oldEntry[0] > 0) {
			long elapsed = (System.currentTimeMillis() / 1000) - oldEntry[0];
			long days = elapsed / 86400;
			long hours = (elapsed % 86400) / 3600;
			timeAgo = ", " + (days > 0 ? days + "d " : "") + hours + "h ago";
		}
		String changeColor = change >= 0 ? "00ff00" : "ff0000";
		String changeStr = (change >= 0 ? "+" : "-") + net.runelite.client.util.QuantityFormatter.formatNumber(Math.abs(change)) + " gp";
		String pctStr = String.format("%+.2f%%", pct);
		updateChatMessage(chatMessage, prefix + "Total Wealth (" + label + "): " + wealthStr +
				" (<col=" + changeColor + ">" + changeStr + ", " + pctStr + "</col>" + timeAgo + ")");
	}

	private void sendLocalMessage(String msg)
	{
		clientThread.invokeLater(() ->
				chatMessageManager.queue(net.runelite.client.chat.QueuedMessage.builder()
						.type(net.runelite.api.ChatMessageType.CONSOLE)
						.runeLiteFormattedMessage(msg)
						.build())
		);
	}

	private void updateChatMessage(net.runelite.api.events.ChatMessage chatMessage, String msg)
	{
		clientThread.invokeLater(() -> {
			chatMessage.getMessageNode().setValue(msg);
			client.refreshChat();
		});
	}

	private void sendBankChatMessage(String msg)
	{
		clientThread.invokeLater(() ->
				chatMessageManager.queue(net.runelite.client.chat.QueuedMessage.builder()
						.type(net.runelite.api.ChatMessageType.GAMEMESSAGE)
						.runeLiteFormattedMessage(msg)
						.build())
		);
	}

	private void addItemToList(Item item, java.util.List<String> itemList, java.util.Map<String, Integer> quantities)
	{
		int originalId = item.getId();
		int lookupId = panel.getItemVariantMap().getOrDefault(originalId, originalId);
		boolean isVariantItem = (originalId != lookupId);

		String originalName = null;
		if (isVariantItem) {
			ItemComposition originalComp = itemManager.getItemComposition(originalId);
			if (originalComp != null) originalName = originalComp.getName();
		}

		String foundName = null;
		for (java.util.Map.Entry<String, Integer> entry : nameToId.entrySet())
		{
			if (entry.getValue() == lookupId)
			{
				foundName = entry.getKey();
				break;
			}
		}
		if (foundName != null)
		{
			String[] words = foundName.split(" ");
			StringBuilder sb = new StringBuilder();
			for (String word : words)
			{
				if (word.length() > 0)
					sb.append(Character.toUpperCase(word.charAt(0)))
							.append(word.substring(1)).append(" ");
			}
			String displayName = sb.toString().trim();
			if (isVariantItem && originalName != null) {
				displayName = displayName + "|" + originalName;
			}
			if (!itemList.contains(displayName)) {
				itemList.add(displayName);
			}
			quantities.merge(displayName, item.getQuantity(), Integer::sum);
		}
	}

	public void saveConfig(String key, String value)
	{
		configManager.setConfiguration("gecompanion", key, value);
	}

	public String loadConfig(String key)
	{
		return configManager.getConfiguration("gecompanion", key);
	}

	public boolean isBankValueHidden()
	{
		String val = loadConfig("bankValueHidden");
		return "true".equals(val);
	}

	public OkHttpClient getOkHttpClient() { return okHttpClient; }

	public void fireAlert(String itemName, long currentPrice, boolean isAbove, long targetPrice)
	{
		String msg = "GE Companion: " + itemName + " has reached your price target of " +
				net.runelite.client.util.QuantityFormatter.formatNumber(targetPrice) + " gp! Current: " +
				net.runelite.client.util.QuantityFormatter.formatNumber(currentPrice) + " gp";
		notifier.notify(msg);
		if (client.getGameState() == net.runelite.api.GameState.LOGGED_IN)
		{
			String prefixColor = String.format("%06X", config.chatPrefixColor().getRGB() & 0xFFFFFF);
			final String chatMsg = "<col=" + prefixColor + ">[GE Companion]</col> " +
					itemName + " has reached your price target of " +
					net.runelite.client.util.QuantityFormatter.formatNumber(targetPrice) + " gp! Current: " +
					net.runelite.client.util.QuantityFormatter.formatNumber(currentPrice) + " gp";
			clientThread.invokeLater(() ->
					chatMessageManager.queue(net.runelite.client.chat.QueuedMessage.builder()
							.type(net.runelite.api.ChatMessageType.GAMEMESSAGE)
							.runeLiteFormattedMessage(chatMsg)
							.build())
			);
		}
	}
	public java.util.Map<Integer, Long> getAvgPrice1h() { return avgPrice1h; }
	public java.util.Map<Integer, Long> getAvgPrice6h() { return avgPrice6h; }
	public java.util.Map<Integer, Long> getAvgPrice24h() { return avgPrice24h; }

	public void setBankValueHidden(boolean hidden)
	{
		saveConfig("bankValueHidden", String.valueOf(hidden));
	}
	@Provides
	GECompanionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GECompanionConfig.class);
	}
}