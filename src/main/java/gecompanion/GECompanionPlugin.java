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
import org.json.JSONObject;
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
				JSONObject json = new JSONObject(body);
				JSONObject data = json.getJSONObject("data");

				priceCache.clear();
				for (String key : data.keySet())
				{
					try
					{
						int id = Integer.parseInt(key);
						JSONObject item = data.getJSONObject(key);

						long high = item.optLong("high", 0);
						long low = item.optLong("low", 0);
						long highTime = item.optLong("highTime", 0);
						long lowTime = item.optLong("lowTime", 0);

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
				javax.swing.SwingUtilities.invokeLater(() -> panel.onPricesUpdated(priceCache, nameToId, avgPrice24h, avgPrice1h, avgPrice6h, itemLimits, buyVolume1h, sellVolume1h, volumeCache));
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
            JSONObject json = new JSONObject(body);
            JSONObject data = json.getJSONObject("data");

            cache.clear();
            for (String key : data.keySet())
            {
				try
				{
					int id = Integer.parseInt(key);
					JSONObject item = data.getJSONObject(key);
					long avgHigh = item.optLong("avgHighPrice", 0);
					long avgLow = item.optLong("avgLowPrice", 0);
					if (avgHigh > 0 && avgLow > 0)
						cache.put(id, (avgHigh + avgLow) / 2);
					else if (avgHigh > 0)
						cache.put(id, avgHigh);
					else if (avgLow > 0)
						cache.put(id, avgLow);
					// Store buy/sell volume for 1h timeframe
					if (interval.equals("1h"))
					{
						long buyVol = item.optLong("highPriceVolume", 0);
						long sellVol = item.optLong("lowPriceVolume", 0);
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
            JSONObject json = new JSONObject(body);
            JSONObject data = json.getJSONObject("data");

            avgPrice24h.clear();
            for (String key : data.keySet())
            {
                try
                {
                    int id = Integer.parseInt(key);
                    JSONObject item = data.getJSONObject(key);
                    long avgHigh = item.optLong("avgHighPrice", 0);
                    long avgLow = item.optLong("avgLowPrice", 0);
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
				JSONObject json = new JSONObject(body);
				JSONObject data = json.getJSONObject("data");

				volumeCache.clear();
				for (String key : data.keySet())
				{
					try
					{
						int id = Integer.parseInt(key);
						long volume = data.optLong(key, 0);
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
				org.json.JSONArray arr = new org.json.JSONArray(body);

				for (int i = 0; i < arr.length(); i++)
				{
					JSONObject item = arr.getJSONObject(i);
					int id = item.getInt("id");
					String name = item.getString("name");
					nameToId.put(name.toLowerCase(), id);
					int limit = item.optInt("limit", 0);
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
			int lookupId = panel.getItemVariantMap().getOrDefault(item.getId(), item.getId());
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
				newBankItems.add(displayName);
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
			}
		}

		final long finalBankOnly = bankOnlyValue;
		final long finalTotalWealth = totalWealthValue;
		javax.swing.SwingUtilities.invokeLater(() -> {
			panel.updateBankItems(newBankItems, newBankQuantities, finalBankOnly, finalTotalWealth);
		});
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