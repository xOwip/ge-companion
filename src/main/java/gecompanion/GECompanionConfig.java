package gecompanion;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("gecompanion")
public interface GECompanionConfig extends Config
{
	// ── DATA ──
	@ConfigSection(
			name = "Data",
			description = "Price data settings",
			position = 0
	)
	String dataSection = "data";

	@ConfigItem(
			keyName = "refreshInterval",
			name = "Refresh interval (seconds)",
			description = "How often to fetch new prices from the OSRS Wiki API",
			section = dataSection,
			position = 0
	)
	@Range(min = 30, max = 300)
	default int refreshInterval() { return 60; }

	// ── BANK ──
	@ConfigSection(
			name = "Bank",
			description = "Bank tab settings",
			position = 1
	)
	String bankSection = "bank";

	@ConfigItem(
			keyName = "autoScanBank",
			name = "Auto-scan on bank open",
			description = "Automatically read your bank when you open it in-game",
			section = bankSection,
			position = 0
	)
	default boolean autoScanBank() { return true; }

	@ConfigItem(
			keyName = "gainersCount",
			name = "Default gainers shown",
			description = "How many Top Gainers to show by default (1-10)",
			section = bankSection,
			position = 1
	)
	@Range(min = 1, max = 10)
	default int gainersCount() { return 3; }

	@ConfigItem(
			keyName = "losersCount",
			name = "Default losers shown",
			description = "How many Top Losers to show by default (1-10)",
			section = bankSection,
			position = 2
	)
	@Range(min = 1, max = 10)
	default int losersCount() { return 3; }

	// ── WATCHLIST ──
	@ConfigSection(
			name = "Watchlist",
			description = "Watchlist tab settings",
			position = 2
	)
	String watchlistSection = "watchlist";

	@ConfigItem(
			keyName = "defaultChartRange",
			name = "Default chart range",
			description = "Which time range the price chart opens on",
			section = watchlistSection,
			position = 0
	)
	default ChartRange defaultChartRange() { return ChartRange.MONTH; }

    @ConfigItem(
          keyName = "minBankItemValue",
          name = "Min bank item value (gp)",
          description = "Minimum total stack value to appear in Top Gainers/Losers",
          section = bankSection,
          position = 3
    )
    @Range(min = 0, max = 10000000)
    default int minBankItemValue() { return 500000; }

    // ── DISPLAY ──
    @ConfigSection(
          name = "Display",
          description = "Display settings",
          position = 3
    )
    String displaySection = "display";

    @ConfigItem(
          keyName = "showLookupButton",
          name = "Show lookup button",
          description = "Show the GE Companion lookup button on the minimap",
          section = displaySection,
          position = 0
    )
    default boolean showLookupButton() { return true; }

    @ConfigItem(
          keyName = "pinnedItems",
          name = "Pinned items",
          description = "Comma separated list of pinned item names",
          section = watchlistSection,
          position = 1,
          hidden = true
    )
    default String pinnedItems() { return ""; }
}