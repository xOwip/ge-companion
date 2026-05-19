/*
 *  ██████╗ ███████╗     ██████╗ ██████╗ ███╗   ███╗██████╗
 * ██╔════╝ ██╔════╝    ██╔════╝██╔═══██╗████╗ ████║██╔══██╗
 * ██║  ███╗█████╗      ██║     ██║   ██║██╔████╔██║██████╔╝
 * ██║   ██║██╔══╝      ██║     ██║   ██║██║╚██╔╝██║██╔═══╝
 * ╚██████╔╝███████╗    ╚██████╗╚██████╔╝██║ ╚═╝ ██║██║
 *  ╚═════╝ ╚══════╝     ╚═════╝ ╚═════╝ ╚═╝     ╚═╝╚═╝
 *
 *  Built by Owip | github.com/Owhip
 *  Live GE prices, bank value tracker, and item search.
 */

package gecompanion;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
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
	private ConfigManager configManager;

	private GECompanionPanel panel;
	private NavigationButton navButton;
	private ScheduledExecutorService scheduler;

	// Price data store — itemId -> PriceData
	private final Map<Integer, PriceData> priceCache = new HashMap<>();
	// 24h average prices — itemId -> avg price
	private final Map<Integer, Long> avgPrice24h = new HashMap<>();
	// Name -> itemId mapping
	private final Map<String, Integer> nameToId = new HashMap<>();
	// Item GE limits — itemId -> limit
	private final Map<Integer, Integer> itemLimits = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		panel = new GECompanionPanel(config, this);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/ge_companion_icon.png");

		navButton = NavigationButton.builder()
				.tooltip("GE Companion")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);

		// Start price refresh scheduler
		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(this::fetchPrices, 0, config.refreshInterval(), TimeUnit.SECONDS);

		log.debug("GE Companion started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
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
				// Fetch 24h averages for delta calculation
				fetch24hAverages();

				// Update panel on EDT
				javax.swing.SwingUtilities.invokeLater(() -> panel.onPricesUpdated(priceCache, nameToId, avgPrice24h, itemLimits));
			}
		}
		catch (Exception e)
		{
			log.warn("Error fetching prices", e);
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
	public void saveConfig(String key, String value)
	{
		configManager.setConfiguration("gecompanion", key, value);
	}

	@Provides
	GECompanionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GECompanionConfig.class);
	}
}