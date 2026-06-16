package gecompanion;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("gecompanion")
public interface GECompanionConfig extends Config
{

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
	default int gainersCount() { return 5; }

	@ConfigItem(
			keyName = "losersCount",
			name = "Default losers shown",
			description = "How many Top Losers to show by default (1-10)",
			section = bankSection,
			position = 2
	)
	@Range(min = 1, max = 10)
	default int losersCount() { return 5; }

	// ── WATCHLIST ──
	@ConfigSection(
			name = "Watchlist",
			description = "Watchlist tab settings",
			position = 2
	)
	String watchlistSection = "watchlist";

	@ConfigItem(
			keyName = "defaultTab",
			name = "Default tab",
			description = "Which tab opens when the plugin loads",
			position = 0
	)
	default DefaultTab defaultTab() { return DefaultTab.SEARCH; }

	@ConfigItem(
			keyName = "defaultChartRange",
			name = "Default chart range",
			description = "Which time range the price chart opens on",
			section = watchlistSection,
			position = 0
	)
	default ChartRange defaultChartRange() { return ChartRange.MONTH; }
	@ConfigItem(
			keyName = "chartZoomMode",
			name = "Chart zoom mode",
			description = "Choose between Drag Select zoom or Magnifier for the price chart",
			section = watchlistSection,
			position = 1
	)
	default ChartZoomMode chartZoomMode() { return ChartZoomMode.DRAG_SELECT; }
	@ConfigItem(
			keyName = "gameUpdateMode",
			name = "Game update markers",
			description = "Show game update markers on the price chart",
			section = watchlistSection,
			position = 2
	)
	default GameUpdateMode gameUpdateMode() { return GameUpdateMode.ALL; }
	@ConfigItem(
			keyName = "defaultTimeFrame",
			name = "Default timeframe",
			description = "Which timeframe the price display opens on",
			section = watchlistSection,
			position = 1
	)
	default DefaultTimeFrame defaultTimeFrame() { return DefaultTimeFrame.TWENTY_FOUR_HOUR; }

    @ConfigItem(
          keyName = "minBankItemValue",
          name = "Min bank item value (gp)",
          description = "Minimum total stack value to appear in Top Gainers/Losers",
          section = bankSection,
          position = 3
    )
    @Range(min = 0, max = 10000000)
    default int minBankItemValue() { return 500000; }
	@ConfigItem(
			keyName = "sortMode",
			name = "Sort Gainers/Losers by",
			description = "Sort Top Gainers and Top Losers by GP change or % change",
			section = bankSection,
			position = 4
	)
	default SortMode sortMode() { return SortMode.GP_CHANGE; }

	@ConfigItem(
			keyName = "showBankValueChange",
			name = "Show bank value change",
			description = "Show how much your bank value has changed over time in the bank tab",
			section = bankSection,
			position = 5
	)
	default boolean showBankValueChange() { return true; }

	@ConfigItem(
			keyName = "resetBankHistory",
			name = "Reset Bank Value History",
			description = "Permanently deletes all saved bank value history. Cannot be undone. Your data will rebuild automatically on your next bank scan.",
			section = bankSection,
			position = 6
	)
	default boolean resetBankHistory() { return false; }

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