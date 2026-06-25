package gecompanion;

import net.runelite.client.ui.PluginPanel;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.Rectangle;
import java.util.*;
import java.util.List;

public class GECompanionPanel extends PluginPanel
{
    // Font sizes
    private static final int FONT_TITLE = 16;
    private static final int FONT_TAB = 13;
    private static final int FONT_ITEM_NAME = 14;
    private static final int FONT_PRICE = 13;
    private static final int FONT_DELTA = 13;
    private static final int FONT_SECTION = 13;
    private static final int FONT_STAT_LABEL = 11;
    private static final int FONT_STAT_VALUE = 14;
    private static final int FONT_BUTTON = 11;
    private static final int FONT_TIMEFRAME = 12;
    private static final int FONT_REFRESH = 17;
    private static final int FONT_META = 12;
    private static final int FONT_LIMIT = 12;

    // Colors
    private static final Color BG_DARK = new Color(35, 31, 32);
    private static final Color BG_HEADER = new Color(26, 23, 24);
    private static final Color BG_DETAIL = new Color(20, 18, 18);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color PRICE_GOLD = new Color(255, 210, 50);
    private static final Color STAT_GOLD = new Color(255, 210, 50);
    private static final Color TEXT_PRIMARY = new Color(239, 241, 243);
    private static final Color TEXT_DIM = new Color(110, 100, 90);
    private static final Color TAB_INACTIVE = new Color(74, 69, 64);
    private static final Color LIVE_GREEN = new Color(74, 154, 48);
    private static final Color GREEN_UP = new Color(74, 154, 48);
    private static final Color RED_DOWN = new Color(192, 57, 43);
    private static final Color BG_ROW_HOVER = new Color(45, 40, 38);
    private static final Color BG_ROW_SELECTED = new Color(26, 24, 20);
    private static final Color STAT_BLUE = new Color(74, 122, 191);

    private final GECompanionConfig config;
    private final GECompanionPlugin plugin;

    private int activeTab = 1;
    public int getActiveTab() { return activeTab; }
    public void openItemLookup(int itemId)
    {
        // Reverse-lookup item name from ID
        String foundName = null;
        for (java.util.Map.Entry<String, Integer> entry : nameToId.entrySet()) {
            if (entry.getValue() == itemId) {
                foundName = entry.getKey();
                break;
            }
        }
        if (foundName == null) return;

        // Capitalize first letter of each word
        String[] words = foundName.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0)
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1)).append(" ");
        }
        String displayName = sb.toString().trim();

        activeTab = 1;
        for (int j = 0; j < 3; j++)
            updateTabStyle(tabLabels[j], j == 1);
        showTab(1);

        javax.swing.SwingUtilities.invokeLater(() -> {
            if (searchField != null) {
                searchField.setText(displayName);
            }
        });
    }
    public java.util.Map<Integer, Integer> getItemVariantMap() { return itemVariantMap; }
    public boolean isItemPriceable(int itemId)
    {
        if (itemVariantMap.containsKey(itemId)) return true;
        return nameToId.containsValue(itemId);
    }
    private String activeTimeFrame = "24H";
    private boolean bankAllItemsCollapsed = true;
    private boolean graphWasOpen = false;
    private String graphActiveTimeframe = "1D";
    private JPanel tabContentPanel;
    private JLabel[] tabLabels = new JLabel[3];
    private JLabel timerLabel;
    private int secondsSinceRefresh = 0;
    private javax.swing.Timer liveTimer;

    // Search state
    private JPanel searchResultsPanel;
    private JPanel watchlistListPanel;
    private JPanel recentSearchesPanel;
    private JTextField searchField = new JTextField();
    private JLabel searchClearBtn;
    private boolean suppressSearchChange = false;
    private boolean isRefreshing = false;
    private Runnable watchlistReopenAction = null;
    private Runnable searchReopenAction = null;
    private Runnable bankReopenAction = null;
    private JPanel bankListPanel;
    private java.util.List<String> recentSearches = new java.util.ArrayList<>();
    private String selectedItemName = null;
    private String selectedWatchlistItemName = null;
    private String selectedBankItemName = null;
    private String[] currentOpenSearchItem = null;
    private String[] currentOpenWatchlistItem = null;
    private String[] currentOpenBankItem = null;
    // Live label references for zero-disruption refresh
    private JLabel liveHeaderPriceLabel = null;
    private javax.swing.JPanel activeStatsFloatPanel = null;
    private javax.swing.JLayeredPane activeStatsLayeredPane = null;
    private JLabel liveFloatVolumeLabel = null;
    private JLabel liveFloatMarginLabel = null;
    private JLabel liveFloatProfitLabel = null;
    private JLabel liveFloatRoiLabel = null;
    private JLabel liveBuyPriceValueLabel = null;
    private JLabel liveBuyPriceHeaderLabel = null;
    private JLabel liveSellPriceValueLabel = null;
    private JLabel liveSellPriceHeaderLabel = null;
    private JPanel liveStatGrid = null;
    private JLabel[] liveStatsLabels = null;
    private int liveOpenItemId = -1;
    private String[] liveOpenItemData = null;
    private javax.swing.JPanel liveGraphPanel = null;
    private javax.swing.JPanel liveUpdateCanvas = null;

    // Live price data
    private java.util.Map<Integer, PriceData> priceCache = new java.util.HashMap<>();
    private java.util.Map<String, Integer> nameToId = new java.util.HashMap<>();
    private java.util.Map<Integer, Long> avgPrice24h = new java.util.HashMap<>();
    private java.util.Map<Integer, Long> avgPrice1h = new java.util.HashMap<>();
    private java.util.Map<Integer, Long> avgPrice6h = new java.util.HashMap<>();
    private java.util.Map<Integer, Integer> itemLimits = new java.util.HashMap<>();
    private java.util.Map<Integer, Long> buyVolume1h = new java.util.HashMap<>();
    private java.util.Map<Integer, Long> sellVolume1h = new java.util.HashMap<>();
    private java.util.Map<Integer, Long> volumeCache = new java.util.HashMap<>();

    // Price graph cache — key: "itemId_timeframe" e.g. "20997_30d"
    private final java.util.Map<String, java.util.List<PricePoint>> timeseriesCache = new java.util.HashMap<>();

    // Price point for graph data
    static class PricePoint {
        long timestamp;
        long buyPrice;
        long sellPrice;
        int buyVolume;
        int sellVolume;
        PricePoint(long timestamp, long buyPrice, long sellPrice, int buyVolume, int sellVolume) {
            this.timestamp = timestamp;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.buyVolume = buyVolume;
            this.sellVolume = sellVolume;
        }
    }
    static class UpdateMarker {
        String title;
        long timestamp;
        String category;
        String wikiUrl;
        UpdateMarker(String title, long timestamp, String category, String wikiUrl) {
            this.title = title;
            this.timestamp = timestamp;
            this.category = category;
            this.wikiUrl = wikiUrl;
        }
    }

    // Game update markers cache
    private java.util.List<UpdateMarker> gameUpdates = null;
    private boolean gameUpdatesFetching = false;
    private boolean updateTooltipPinned = false;
    private boolean watchlistEditMode = false;
    private String bankWealthTimeFrame = "24H";
    private boolean bankMetadataExpanded = false;
    private JPanel bankCompPanel = null;
    private JSeparator bankMetaBottomSep = null;
    private javax.swing.Timer bankMetaTimer = null;
    private javax.swing.JViewport bankMetaViewport = null;

    // Pinned/watched items
    private final java.util.Map<Integer, javax.swing.ImageIcon> iconCache = new java.util.HashMap<>();
    private final java.util.List<String> pinnedItems = new java.util.ArrayList<>();
    // Item variant map: ornamented/charged item ID → base tradeable item ID
    private final java.util.Map<Integer, Integer> itemVariantMap = new java.util.HashMap<>();
    private final java.util.List<String> bankItems = new java.util.ArrayList<>();
    private final java.util.Map<String, Integer> bankQuantities = new java.util.HashMap<>();

    // Inline detail tracking
    private JPanel currentOpenSearchDetail = null;
    private JPanel currentOpenWatchlistDetail = null;
    private JPanel currentOpenBankDetail = null;
    private JPanel currentOpenSearchRow = null;
    private JPanel currentOpenSearchInfo = null;
    private JPanel currentOpenSearchIconWrapper = null;
    private JPanel currentOpenSearchDeltaRow = null;
    private JPanel currentOpenWatchlistRow = null;
    private JPanel currentOpenWatchlistInfo = null;
    private JPanel currentOpenWatchlistIconWrapper = null;
    private JPanel currentOpenWatchlistDeltaRow = null;
    private JPanel currentOpenBankRow = null;
    private Color currentOpenSearchRowColor = BG_DARK;
    private Color currentOpenWatchlistRowColor = BG_DARK;
    private Color currentOpenBankRowColor = BG_DARK;
    private Color currentOpenBankBorderColor = null;
private JPanel currentOpenBankInfo = null;
private JPanel currentOpenBankDeltaRow = null;
private String openBankItemName = null;

    public GECompanionPanel(GECompanionConfig config, GECompanionPlugin plugin)
    {
        this.config = config;
        this.plugin = plugin;
        loadBankData();
        activeTimeFrame = config.defaultTimeFrame().getValue();
        loadBankValueLog();
        loadPinnedItems();
        loadRecentSearches();
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
        {
            public void insertUpdate(javax.swing.event.DocumentEvent e)
            {
                onSearchChanged(searchField.getText());
                if (searchClearBtn != null) searchClearBtn.setVisible(!searchField.getText().isEmpty());
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e)
            {
                onSearchChanged(searchField.getText());
                if (searchClearBtn != null) searchClearBtn.setVisible(!searchField.getText().isEmpty());
            }
            public void changedUpdate(javax.swing.event.DocumentEvent e)
            {
                onSearchChanged(searchField.getText());
                if (searchClearBtn != null) searchClearBtn.setVisible(!searchField.getText().isEmpty());
            }
        });
        startLiveTimer();

        setLayout(new BorderLayout());
        setBackground(BG_DARK);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        build();
    }

    private long totalBankValue = 0;
    private java.util.List<long[]> bankValueLog = new java.util.ArrayList<>();

    public void updateBankItems(java.util.List<String> items, java.util.Map<String, Integer> quantities, long bankOnlyValue, long totalWealthValue)
    {
        boolean itemListChanged = !items.equals(this.bankItems);
        this.bankItems.clear();
        this.bankItems.addAll(items);
        this.bankQuantities.clear();
        this.bankQuantities.putAll(quantities);
        this.totalBankValue = bankOnlyValue;
        if (config.showBankValueChange()) saveBankValueLog(bankOnlyValue, totalWealthValue);
        saveBankData();
        if (activeTab == 2)
        {
            if (itemListChanged)
            {
                // close any open floating stats panel before rebuilding
                if (activeStatsFloatPanel != null && activeStatsLayeredPane != null) {
                    activeStatsLayeredPane.remove(activeStatsFloatPanel);
                    activeStatsLayeredPane.repaint();
                    activeStatsFloatPanel = null;
                    activeStatsLayeredPane = null;
                }
                isRefreshing = true;
                showTab(2);
                isRefreshing = false;
                if (bankReopenAction != null)
                {
                    javax.swing.Timer t = new javax.swing.Timer(150, e2 -> bankReopenAction.run());
                    t.setRepeats(false);
                    t.start();
                }
            }
        }
    }

    public void buildItemVariantMap()
    {
        itemVariantMap.clear();

// Master item variant map: untradeable/charged item ID → tradeable base item ID
        // Source: osrs_preferred_bank_value_remap.csv
        // Charge state variants
        itemVariantMap.put(21633, 21634); // Ancient wyvern shield (charged) → uncharged
        itemVariantMap.put(21816, 21817); // Bracelet of ethereum (charged) → uncharged
        itemVariantMap.put(22370, 22368); // Bryophyta's staff (charged) → uncharged
        itemVariantMap.put(25545, 25543); // Celestial signet (charged) → uncharged
        itemVariantMap.put(22550, 22547); // Craw's bow (charged) → uncharged
        itemVariantMap.put(11283, 11284); // Dragonfire shield (charged) → uncharged
        itemVariantMap.put(22002, 22003); // Dragonfire ward (charged) → uncharged
        itemVariantMap.put(31113, 31115); // Eye of ayak (charged) → uncharged
        itemVariantMap.put(26948, 26945); // Pharaoh's sceptre (charged) → uncharged
        itemVariantMap.put(26950, 26945); // Pharaoh's sceptre (charged) → uncharged
        itemVariantMap.put(24736, 24844); // Ring of endurance (charged) → uncharged
        itemVariantMap.put(26818, 26815); // Ring of the elements (charged) → uncharged
        itemVariantMap.put(22323, 22481); // Sanguinesti staff (charged) → uncharged
        itemVariantMap.put(22325, 22486); // Scythe of vitur (charged) → uncharged
        itemVariantMap.put(12931, 12929); // Serpentine helm (charged) → uncharged
        itemVariantMap.put(27788, 27785); // Thammaron's sceptre (a) (charged) → uncharged
        itemVariantMap.put(22555, 22552); // Thammaron's sceptre (charged) → uncharged
        itemVariantMap.put(28922, 28919); // Tonalztics of ralos (charged) → uncharged
        itemVariantMap.put(12904, 12902); // Toxic staff of the dead (charged) → uncharged
        itemVariantMap.put(22288, 22290); // Trident of the seas (e) (charged) → uncharged
        itemVariantMap.put(11905, 11908); // Trident of the seas (charged) → uncharged
        itemVariantMap.put(11907, 11908); // Trident of the seas (partially charged) → uncharged
        itemVariantMap.put(22292, 22294); // Trident of the swamp (e) (charged) → uncharged
        itemVariantMap.put(12899, 12900); // Trident of the swamp (charged) → uncharged
        itemVariantMap.put(27275, 27277); // Tumeken's shadow (charged) → uncharged
        itemVariantMap.put(27610, 27612); // Venator bow (charged) → uncharged
        itemVariantMap.put(22545, 22542); // Viggora's chainmace (charged) → uncharged
        itemVariantMap.put(28585, 28583); // Warped sceptre (charged) → uncharged
        itemVariantMap.put(26239, 26237); // Zaryte bow (charged) → uncharged
        // Charged → empty state
        itemVariantMap.put(30064, 30066); // Tome of earth (charged) → empty
        itemVariantMap.put(20714, 20716); // Tome of fire (charged) → empty
        itemVariantMap.put(25574, 25576); // Tome of water (charged) → empty
        itemVariantMap.put(12926, 12924); // Toxic blowpipe (charged) → empty
        itemVariantMap.put(28688, 12924); // Blazing blowpipe (charged) → Toxic blowpipe (empty)
        itemVariantMap.put(28687, 12924); // Blazing blowpipe (empty) → Toxic blowpipe (empty)
        // Cosmetic charge variants
        itemVariantMap.put(28543, 22486); // Corrupted scythe of vitur (charged) → Scythe of vitur (uncharged)
        itemVariantMap.put(28545, 22486); // Corrupted scythe of vitur (uncharged) → Scythe of vitur (uncharged)
        itemVariantMap.put(28547, 27277); // Corrupted tumeken's shadow (charged) → Tumeken's shadow (uncharged)
        itemVariantMap.put(28549, 27277); // Corrupted tumeken's shadow (uncharged) → Tumeken's shadow (uncharged)
        itemVariantMap.put(25731, 22481); // Holy sanguinesti staff (charged) → Sanguinesti staff (uncharged)
        itemVariantMap.put(25733, 22481); // Holy sanguinesti staff (uncharged) → Sanguinesti staff (uncharged)
        itemVariantMap.put(25736, 22486); // Holy scythe of vitur (charged) → Scythe of vitur (uncharged)
        itemVariantMap.put(25738, 22486); // Holy scythe of vitur (uncharged) → Scythe of vitur (uncharged)
        itemVariantMap.put(13199, 12929); // Magma helm (charged) → Serpentine helm (uncharged)
        itemVariantMap.put(13198, 12929); // Magma helm (uncharged) → Serpentine helm (uncharged)
        itemVariantMap.put(25739, 22486); // Sanguine scythe of vitur (charged) → Scythe of vitur (uncharged)
        itemVariantMap.put(25741, 22486); // Sanguine scythe of vitur (uncharged) → Scythe of vitur (uncharged)
        itemVariantMap.put(13197, 12929); // Tanzanite helm (charged) → Serpentine helm (uncharged)
        itemVariantMap.put(13196, 12929); // Tanzanite helm (uncharged) → Serpentine helm (uncharged)
        // Crystal blade/bow recolours → base inactive item (tradeable-style lookup targets)
        itemVariantMap.put(23995, 23997); // Blade of saeldor (charged) → inactive
        itemVariantMap.put(25865, 25862); // Bow of faerdhinen (charged) → inactive
        itemVariantMap.put(24551, 23997); // Blade of saeldor (c) → inactive
        itemVariantMap.put(25867, 25862); // Bow of faerdhinen (c) → inactive
        itemVariantMap.put(25870, 23997); // Blade of saeldor (c) (Ithell) → inactive
        itemVariantMap.put(25872, 23997); // Blade of saeldor (c) (Iorwerth) → inactive
        itemVariantMap.put(25874, 23997); // Blade of saeldor (c) (Trahaearn) → inactive
        itemVariantMap.put(25876, 23997); // Blade of saeldor (c) (Cadarn) → inactive
        itemVariantMap.put(25878, 23997); // Blade of saeldor (c) (Crwys) → inactive
        itemVariantMap.put(25880, 23997); // Blade of saeldor (c) (Meilyr) → inactive
        itemVariantMap.put(25882, 23997); // Blade of saeldor (c) (Amlodd) → inactive
        itemVariantMap.put(25884, 25862); // Bow of faerdhinen (c) (Ithell) → inactive
        itemVariantMap.put(25886, 25862); // Bow of faerdhinen (c) (Iorwerth) → inactive
        itemVariantMap.put(25888, 25862); // Bow of faerdhinen (c) (Trahaearn) → inactive
        itemVariantMap.put(25890, 25862); // Bow of faerdhinen (c) (Cadarn) → inactive
        itemVariantMap.put(25892, 25862); // Bow of faerdhinen (c) (Crwys) → inactive
        itemVariantMap.put(25894, 25862); // Bow of faerdhinen (c) (Meilyr) → inactive
        itemVariantMap.put(25896, 25862); // Bow of faerdhinen (c) (Amlodd) → inactive
        // Crystal tools → Crystal tool seed (23953)
        itemVariantMap.put(23673, 23953); itemVariantMap.put(23675, 23953); // Crystal axe
        itemVariantMap.put(23762, 23953); itemVariantMap.put(23764, 23953); // Crystal harpoon
        itemVariantMap.put(23680, 23953); itemVariantMap.put(23682, 23953); // Crystal pickaxe
        itemVariantMap.put(28220, 23953); itemVariantMap.put(28223, 23953); // Crystal felling axe
        // Crystal weapons → Crystal weapon seed (4207)
        itemVariantMap.put(23983, 4207); itemVariantMap.put(23985, 4207); // Crystal bow
        itemVariantMap.put(23987, 4207); itemVariantMap.put(23989, 4207); // Crystal halberd
        itemVariantMap.put(23991, 4207); itemVariantMap.put(23993, 4207); // Crystal shield
        // Crystal armor (base + all recolours) → Crystal armour seed (23956)
        itemVariantMap.put(23975, 23956); itemVariantMap.put(23977, 23956); // Crystal body
        itemVariantMap.put(23971, 23956); itemVariantMap.put(23973, 23956); // Crystal helm
        itemVariantMap.put(23979, 23956); itemVariantMap.put(23981, 23956); // Crystal legs
        itemVariantMap.put(23889, 23956); itemVariantMap.put(23890, 23956); itemVariantMap.put(23891, 23956); // Crystal body (basic/attuned/perfected)
        itemVariantMap.put(25496, 23956); // Crystal body (beta)
        itemVariantMap.put(33166, 23956); // Crystal body (Last Man Standing)
        itemVariantMap.put(27709, 23956); itemVariantMap.put(27711, 23956); // Crystal body (Ithell)
        itemVariantMap.put(27721, 23956); itemVariantMap.put(27723, 23956); // Crystal body (Iorwerth)
        itemVariantMap.put(27733, 23956); itemVariantMap.put(27735, 23956); // Crystal body (Trahaearn)
        itemVariantMap.put(27745, 23956); itemVariantMap.put(27747, 23956); // Crystal body (Cadarn)
        itemVariantMap.put(27757, 23956); itemVariantMap.put(27759, 23956); // Crystal body (Crwys)
        itemVariantMap.put(27697, 23956); itemVariantMap.put(27699, 23956); // Crystal body (Hefin)
        itemVariantMap.put(27769, 23956); itemVariantMap.put(27771, 23956); // Crystal body (Amlodd)
        itemVariantMap.put(33023, 23956); itemVariantMap.put(33025, 23956); // Crystal body (Deadman)
        itemVariantMap.put(27717, 23956); itemVariantMap.put(27719, 23956); // Crystal helm (Ithell)
        itemVariantMap.put(27729, 23956); itemVariantMap.put(27731, 23956); // Crystal helm (Iorwerth)
        itemVariantMap.put(27741, 23956); itemVariantMap.put(27743, 23956); // Crystal helm (Trahaearn)
        itemVariantMap.put(27753, 23956); itemVariantMap.put(27755, 23956); // Crystal helm (Cadarn)
        itemVariantMap.put(27765, 23956); itemVariantMap.put(27767, 23956); // Crystal helm (Crwys)
        itemVariantMap.put(27705, 23956); itemVariantMap.put(27707, 23956); // Crystal helm (Hefin)
        itemVariantMap.put(27777, 23956); itemVariantMap.put(27779, 23956); // Crystal helm (Amlodd)
        itemVariantMap.put(33031, 23956); itemVariantMap.put(33033, 23956); // Crystal helm (Deadman)
        itemVariantMap.put(27713, 23956); itemVariantMap.put(27715, 23956); // Crystal legs (Ithell)
        itemVariantMap.put(27725, 23956); itemVariantMap.put(27727, 23956); // Crystal legs (Iorwerth)
        itemVariantMap.put(27737, 23956); itemVariantMap.put(27739, 23956); // Crystal legs (Trahaearn)
        itemVariantMap.put(27749, 23956); itemVariantMap.put(27751, 23956); // Crystal legs (Cadarn)
        itemVariantMap.put(27761, 23956); itemVariantMap.put(27763, 23956); // Crystal legs (Crwys)
        itemVariantMap.put(27701, 23956); itemVariantMap.put(27703, 23956); // Crystal legs (Hefin)
        itemVariantMap.put(27773, 23956); itemVariantMap.put(27775, 23956); // Crystal legs (Amlodd)
        itemVariantMap.put(33027, 23956); itemVariantMap.put(33029, 23956); // Crystal legs (Deadman)
        // Ornament kit items
        itemVariantMap.put(26484, 4151);   // Abyssal tentacle (or) → Abyssal whip (tentacle is whip + kraken tentacle, not tradeable on its own)
        itemVariantMap.put(12006, 4151);   // Abyssal tentacle → Abyssal whip (tentacle is whip + kraken tentacle, not tradeable on its own)
        itemVariantMap.put(26482, 4151);   // Abyssal whip (or) → Abyssal whip
        itemVariantMap.put(12436, 6585);   // Amulet of fury (or) → Amulet of fury
        itemVariantMap.put(20366, 19553);  // Amulet of torture (or) → Amulet of torture
        itemVariantMap.put(29605, 11802);  // Armadyl godsword (deadman) → Armadyl godsword
        itemVariantMap.put(20368, 11802);  // Armadyl godsword (or) → Armadyl godsword
        itemVariantMap.put(20370, 11804);  // Bandos godsword (or) → Bandos godsword
        itemVariantMap.put(23240, 11128);  // Berserker necklace (or) → Berserker necklace
        itemVariantMap.put(33021, 25862);  // Bow of faerdhinen (c) (deadman) → Bow of faerdhinen (inactive)
        itemVariantMap.put(26524, 10);     // Cannon barrels (or) → Cannon barrels
        itemVariantMap.put(26520, 6);      // Cannon base (or) → Cannon base
        itemVariantMap.put(26526, 12);     // Cannon furnace (or) → Cannon furnace
        itemVariantMap.put(26522, 8);      // Cannon stand (or) → Cannon stand
        itemVariantMap.put(12459, 6924);   // Dark infinity bottoms → Infinity bottoms
        itemVariantMap.put(12457, 6918);   // Dark infinity hat → Infinity hat
        itemVariantMap.put(12458, 6916);   // Dark infinity top → Infinity top
        itemVariantMap.put(28682, 21015);  // Dinh's blazing bulwark → Dinh's bulwark
        itemVariantMap.put(28051, 7158);   // Dragon 2h sword (cr) → Dragon 2h sword
        itemVariantMap.put(28037, 1377);   // Dragon battleaxe (cr) → Dragon battleaxe
        itemVariantMap.put(28055, 11840);  // Dragon boots (cr) → Dragon boots
        itemVariantMap.put(22234, 11840);  // Dragon boots (g) → Dragon boots
        itemVariantMap.put(28065, 3140);   // Dragon chainbody (cr) → Dragon chainbody
        itemVariantMap.put(12414, 3140);   // Dragon chainbody (g) → Dragon chainbody
        itemVariantMap.put(28039, 13652);  // Dragon claws (cr) → Dragon claws
        itemVariantMap.put(28053, 21902);  // Dragon crossbow (cr) → Dragon crossbow
        itemVariantMap.put(12417, 11335);  // Dragon full helm (g) → Dragon full helm
        itemVariantMap.put(28049, 3204);   // Dragon halberd (cr) → Dragon halberd
        itemVariantMap.put(25918, 21012);  // Dragon hunter crossbow (b) → Dragon hunter crossbow
        itemVariantMap.put(25916, 21012);  // Dragon hunter crossbow (t) → Dragon hunter crossbow
        itemVariantMap.put(22244, 21895);  // Dragon kiteshield (g) → Dragon kiteshield
        itemVariantMap.put(28033, 1305);   // Dragon longsword (cr) → Dragon longsword
        itemVariantMap.put(28027, 1434);   // Dragon mace (cr) → Dragon mace
        itemVariantMap.put(28057, 1149);   // Dragon med helm (cr) → Dragon med helm
        itemVariantMap.put(23677, 11920);  // Dragon pickaxe (or) → Dragon pickaxe
        itemVariantMap.put(12797, 11920);  // Dragon pickaxe (upgraded) → Dragon pickaxe
        itemVariantMap.put(22242, 21892);  // Dragon platebody (g) → Dragon platebody
        itemVariantMap.put(28061, 4087);   // Dragon platelegs (cr) → Dragon platelegs
        itemVariantMap.put(12415, 4087);   // Dragon platelegs (g) → Dragon platelegs
        itemVariantMap.put(28063, 4585);   // Dragon plateskirt (cr) → Dragon plateskirt
        itemVariantMap.put(12416, 4585);   // Dragon plateskirt (g) → Dragon plateskirt
        itemVariantMap.put(28031, 4587);   // Dragon scimitar (cr) → Dragon scimitar
        itemVariantMap.put(20000, 4587);   // Dragon scimitar (or) → Dragon scimitar
        itemVariantMap.put(28059, 1187);   // Dragon sq shield (cr) → Dragon sq shield
        itemVariantMap.put(12418, 1187);   // Dragon sq shield (g) → Dragon sq shield
        itemVariantMap.put(28029, 21009);  // Dragon sword (cr) → Dragon sword
        itemVariantMap.put(28035, 13576);  // Dragon warhammer (cr) → Dragon warhammer
        itemVariantMap.put(30434, 27612);  // Echo venator bow (charged) → Venator bow (uncharged)
        itemVariantMap.put(30436, 27612);  // Echo venator bow (uncharged) → Venator bow (uncharged)
        itemVariantMap.put(30437, 26241);  // Echo virtus mask → Virtus mask
        itemVariantMap.put(30441, 26245);  // Echo virtus robe bottom → Virtus robe bottom
        itemVariantMap.put(30439, 26243);  // Echo virtus robe top → Virtus robe top
        itemVariantMap.put(28945, 28942);  // Echo boots → Echo crystal
        itemVariantMap.put(27119, 20595);  // Elder chaos hood (or) → Elder chaos hood
        itemVariantMap.put(27117, 20520);  // Elder chaos robe (or) → Elder chaos robe
        itemVariantMap.put(27115, 20517);  // Elder chaos top (or) → Elder chaos top
        itemVariantMap.put(27100, 21003);  // Elder maul (or) → Elder maul
        itemVariantMap.put(12774, 4151);   // Frozen abyssal whip → Abyssal whip
        itemVariantMap.put(26712, 19481);  // Heavy ballista (or) → Heavy ballista
        itemVariantMap.put(28070, 10828);  // Helm of neitiznot (or) → Helm of neitiznot
        itemVariantMap.put(25734, 22324);  // Holy ghrazi rapier → Ghrazi rapier
        itemVariantMap.put(21198, 3053);   // Lava battlestaff (or) → Lava battlestaff
        itemVariantMap.put(12421, 6924);   // Light infinity bottoms → Infinity bottoms
        itemVariantMap.put(12419, 6918);   // Light infinity hat → Infinity hat
        itemVariantMap.put(12420, 6916);   // Light infinity top → Infinity top
        itemVariantMap.put(12806, 11924);  // Malediction ward (or) → Malediction ward
        itemVariantMap.put(26539, 4097);   // Mystic boots (or) → Mystic boots
        itemVariantMap.put(26537, 4095);   // Mystic gloves (or) → Mystic gloves
        itemVariantMap.put(26531, 4089);   // Mystic hat (or) → Mystic hat
        itemVariantMap.put(26535, 4093);   // Mystic robe bottom (or) → Mystic robe bottom
        itemVariantMap.put(26533, 4091);   // Mystic robe top (or) → Mystic robe top
        itemVariantMap.put(22249, 19547);  // Necklace of anguish (or) → Necklace of anguish
        itemVariantMap.put(19720, 12002);  // Occult necklace (or) → Occult necklace
        itemVariantMap.put(12807, 11926);  // Odium ward (or) → Odium ward
        itemVariantMap.put(27246, 26219);  // Osmumten's fang (or) → Osmumten's fang
        itemVariantMap.put(30779, 30753);  // Radiant oathplate chest → Oathplate chest
        itemVariantMap.put(30777, 30750);  // Radiant oathplate helm → Oathplate helm
        itemVariantMap.put(30781, 30756);  // Radiant oathplate legs → Oathplate legs
        itemVariantMap.put(33340, 33338);  // Radiant slayer helmet → Oathplate slayer helmet
        itemVariantMap.put(26486, 9185);   // Rune crossbow (or) → Rune crossbow
        itemVariantMap.put(23330, 1333);   // Rune scimitar (guthix) → Rune scimitar
        itemVariantMap.put(23332, 1333);   // Rune scimitar (saradomin) → Rune scimitar
        itemVariantMap.put(23334, 1333);   // Rune scimitar (zamorak) → Rune scimitar
        itemVariantMap.put(28254, 26382);  // Sanguine torva full helm → Torva full helm
        itemVariantMap.put(28256, 26384);  // Sanguine torva platebody → Torva platebody
        itemVariantMap.put(28258, 26386);  // Sanguine torva platelegs → Torva platelegs
        itemVariantMap.put(20372, 11806);  // Saradomin godsword (or) → Saradomin godsword
        itemVariantMap.put(33335, 28338);  // Soulreaper axe (o) → Soulreaper axe
        itemVariantMap.put(12795, 11787);  // Steam battlestaff (or) → Steam battlestaff
        itemVariantMap.put(23444, 19544);  // Tormented bracelet (or) → Tormented bracelet
        itemVariantMap.put(33036, 12902);  // Toxic staff (deadman) charged → Toxic staff of the dead (uncharged)
        itemVariantMap.put(33035, 12902);  // Toxic staff (deadman) uncharged → Toxic staff of the dead (uncharged)
        itemVariantMap.put(33323, 33434);  // Trident of the seas (o) fully charged → uncharged
        itemVariantMap.put(33322, 33434);  // Trident of the seas (o) partially charged → uncharged
        itemVariantMap.put(33326, 22290);  // Trident of the seas (e) (o) charged → uncharged
        itemVariantMap.put(33328, 22290);  // Trident of the seas (e) (o) uncharged → uncharged
        itemVariantMap.put(33434, 11908);  // Trident of the seas (o) uncharged → Trident of the seas (uncharged)
        itemVariantMap.put(33318, 22294);  // Trident of the swamp (e) (o) charged → uncharged
        itemVariantMap.put(33320, 22294);  // Trident of the swamp (e) (o) uncharged → uncharged
        itemVariantMap.put(33314, 12900);  // Trident of the swamp (o) charged → uncharged
        itemVariantMap.put(33316, 12900);  // Trident of the swamp (o) uncharged → uncharged
        itemVariantMap.put(24664, 21018);  // Twisted ancestral hat → Ancestral hat
        itemVariantMap.put(24668, 21024);  // Twisted ancestral robe bottom → Ancestral robe bottom
        itemVariantMap.put(24666, 21021);  // Twisted ancestral robe top → Ancestral robe top
        itemVariantMap.put(23235, 6528);   // Tzhaar-ket-om (t) → Tzhaar-ket-om
        itemVariantMap.put(29607, 27690);  // Voidwaker (deadman) → Voidwaker
        itemVariantMap.put(29609, 24424);  // Volatile nightmare staff (deadman) → Volatile nightmare staff
        itemVariantMap.put(12773, 4151);   // Volcanic abyssal whip → Abyssal whip
        itemVariantMap.put(20374, 11808);  // Zamorak godsword (or) → Zamorak godsword
        // Wilderness upgraded variants
        itemVariantMap.put(27679, 22552);  // Accursed sceptre (a) charged → Thammaron's sceptre (uncharged)
        itemVariantMap.put(27676, 22552);  // Accursed sceptre (a) uncharged → Thammaron's sceptre (uncharged)
        itemVariantMap.put(27665, 22552);  // Accursed sceptre charged → Thammaron's sceptre (uncharged)
        itemVariantMap.put(27662, 22552);  // Accursed sceptre uncharged → Thammaron's sceptre (uncharged)
        itemVariantMap.put(27660, 22542);  // Ursine chainmace charged → Viggora's chainmace (uncharged)
        itemVariantMap.put(27657, 22542);  // Ursine chainmace uncharged → Viggora's chainmace (uncharged)
        itemVariantMap.put(27655, 22547);  // Webweaver bow charged → Craw's bow (uncharged)
        itemVariantMap.put(27652, 22547);  // Webweaver bow uncharged → Craw's bow (uncharged)
        // Additional simple remaps
        itemVariantMap.put(29804, 29801);  // Amulet of rancour (s) → Amulet of rancour
        itemVariantMap.put(22322, 22477);  // Avernic defender → Avernic defender hilt
        itemVariantMap.put(22441, 22477);  // Avernic defender (broken) → Avernic defender hilt
        itemVariantMap.put(24186, 22477);  // Avernic defender (locked) → Avernic defender hilt
        itemVariantMap.put(27550, 22477);  // Ghommal's avernic defender 5 → Avernic defender hilt
        itemVariantMap.put(27551, 22477);  // Ghommal's avernic defender 5 (locked) → Avernic defender hilt
        itemVariantMap.put(27552, 22477);  // Ghommal's avernic defender 6 → Avernic defender hilt
        itemVariantMap.put(27553, 22477);  // Ghommal's avernic defender 6 (locked) → Avernic defender hilt
        itemVariantMap.put(24271, 24268);  // Neitiznot faceguard → Basilisk jaw
        itemVariantMap.put(22981, 22983);  // Ferocious gloves → Hydra leather
        // Avernic treads variants (composite: base treads + boots)
        itemVariantMap.put(31091, 31088);  // Avernic treads (pr) → Avernic treads
        itemVariantMap.put(31092, 31088);  // Avernic treads (pe) → Avernic treads
        itemVariantMap.put(31093, 31088);  // Avernic treads (et) → Avernic treads
        itemVariantMap.put(31094, 31088);  // Avernic treads (pr)(pe) → Avernic treads
        itemVariantMap.put(31095, 31088);  // Avernic treads (pr)(et) → Avernic treads
        itemVariantMap.put(31096, 31088);  // Avernic treads (pe)(et) → Avernic treads
        itemVariantMap.put(31097, 31088);  // Avernic treads (max) → Avernic treads
        // Ring of suffering variants
        itemVariantMap.put(20655, 19550);  // Ring of suffering (recoil) → Ring of suffering (uncharged)
        itemVariantMap.put(19710, 19550);  // Ring of suffering (i) (uncharged) → Ring of suffering (uncharged)
        itemVariantMap.put(25246, 19550);  // Ring of suffering (i) (uncharged) variant → Ring of suffering (uncharged)
        itemVariantMap.put(26761, 19550);  // Ring of suffering (i) (uncharged) variant → Ring of suffering (uncharged)
        itemVariantMap.put(20657, 19550);  // Ring of suffering (i) (recoil) → Ring of suffering (uncharged)
        itemVariantMap.put(25248, 19550);  // Ring of suffering (i) (recoil) variant → Ring of suffering (uncharged)
        itemVariantMap.put(26762, 19550);  // Ring of suffering (i) (recoil) variant → Ring of suffering (uncharged)
        // Barrows armor degradation variants (all map to Undamaged tradeable ID)
        itemVariantMap.put(4856, 4708); itemVariantMap.put(4857, 4708); itemVariantMap.put(4858, 4708); itemVariantMap.put(4859, 4708); itemVariantMap.put(4860, 4708); // Ahrim's hood
        itemVariantMap.put(4862, 4710); itemVariantMap.put(4863, 4710); itemVariantMap.put(4864, 4710); itemVariantMap.put(4865, 4710); itemVariantMap.put(4866, 4710); // Ahrim's staff
        itemVariantMap.put(4868, 4712); itemVariantMap.put(4869, 4712); itemVariantMap.put(4870, 4712); itemVariantMap.put(4871, 4712); itemVariantMap.put(4872, 4712); // Ahrim's robetop
        itemVariantMap.put(4874, 4714); itemVariantMap.put(4875, 4714); itemVariantMap.put(4876, 4714); itemVariantMap.put(4877, 4714); itemVariantMap.put(4878, 4714); // Ahrim's robeskirt
        itemVariantMap.put(4880, 4716); itemVariantMap.put(4881, 4716); itemVariantMap.put(4882, 4716); itemVariantMap.put(4883, 4716); itemVariantMap.put(4884, 4716); // Dharok's helm
        itemVariantMap.put(4886, 4718); itemVariantMap.put(4887, 4718); itemVariantMap.put(4888, 4718); itemVariantMap.put(4889, 4718); itemVariantMap.put(4890, 4718); // Dharok's greataxe
        itemVariantMap.put(4892, 4720); itemVariantMap.put(4893, 4720); itemVariantMap.put(4894, 4720); itemVariantMap.put(4895, 4720); itemVariantMap.put(4896, 4720); // Dharok's platebody
        itemVariantMap.put(4898, 4722); itemVariantMap.put(4899, 4722); itemVariantMap.put(4900, 4722); itemVariantMap.put(4901, 4722); itemVariantMap.put(4902, 4722); // Dharok's platelegs
        itemVariantMap.put(4904, 4724); itemVariantMap.put(4905, 4724); itemVariantMap.put(4906, 4724); itemVariantMap.put(4907, 4724); itemVariantMap.put(4908, 4724); // Guthan's helm
        itemVariantMap.put(4910, 4726); itemVariantMap.put(4911, 4726); itemVariantMap.put(4912, 4726); itemVariantMap.put(4913, 4726); itemVariantMap.put(4914, 4726); // Guthan's warspear
        itemVariantMap.put(4916, 4728); itemVariantMap.put(4917, 4728); itemVariantMap.put(4918, 4728); itemVariantMap.put(4919, 4728); itemVariantMap.put(4920, 4728); // Guthan's platebody
        itemVariantMap.put(4922, 4730); itemVariantMap.put(4923, 4730); itemVariantMap.put(4924, 4730); itemVariantMap.put(4925, 4730); itemVariantMap.put(4926, 4730); // Guthan's chainskirt
        itemVariantMap.put(4928, 4732); itemVariantMap.put(4929, 4732); itemVariantMap.put(4930, 4732); itemVariantMap.put(4931, 4732); itemVariantMap.put(4932, 4732); // Karil's coif
        itemVariantMap.put(4934, 4734); itemVariantMap.put(4935, 4734); itemVariantMap.put(4936, 4734); itemVariantMap.put(4937, 4734); itemVariantMap.put(4938, 4734); // Karil's crossbow
        itemVariantMap.put(4940, 4736); itemVariantMap.put(4941, 4736); itemVariantMap.put(4942, 4736); itemVariantMap.put(4943, 4736); itemVariantMap.put(4944, 4736); // Karil's leathertop
        itemVariantMap.put(4946, 4738); itemVariantMap.put(4947, 4738); itemVariantMap.put(4948, 4738); itemVariantMap.put(4949, 4738); itemVariantMap.put(4950, 4738); // Karil's leatherskirt
        itemVariantMap.put(4952, 4745); itemVariantMap.put(4953, 4745); itemVariantMap.put(4954, 4745); itemVariantMap.put(4955, 4745); itemVariantMap.put(4956, 4745); // Torag's helm
        itemVariantMap.put(4958, 4747); itemVariantMap.put(4959, 4747); itemVariantMap.put(4960, 4747); itemVariantMap.put(4961, 4747); itemVariantMap.put(4962, 4747); // Torag's hammers
        itemVariantMap.put(4964, 4749); itemVariantMap.put(4965, 4749); itemVariantMap.put(4966, 4749); itemVariantMap.put(4967, 4749); itemVariantMap.put(4968, 4749); // Torag's platebody
        itemVariantMap.put(4970, 4751); itemVariantMap.put(4971, 4751); itemVariantMap.put(4972, 4751); itemVariantMap.put(4973, 4751); itemVariantMap.put(4974, 4751); // Torag's platelegs
        itemVariantMap.put(4976, 4753); itemVariantMap.put(4977, 4753); itemVariantMap.put(4978, 4753); itemVariantMap.put(4979, 4753); itemVariantMap.put(4980, 4753); // Verac's helm
        itemVariantMap.put(4982, 4755); itemVariantMap.put(4983, 4755); itemVariantMap.put(4984, 4755); itemVariantMap.put(4985, 4755); itemVariantMap.put(4986, 4755); // Verac's flail
        itemVariantMap.put(4988, 4757); itemVariantMap.put(4989, 4757); itemVariantMap.put(4990, 4757); itemVariantMap.put(4991, 4757); itemVariantMap.put(4992, 4757); // Verac's brassard
        itemVariantMap.put(4994, 4759); itemVariantMap.put(4995, 4759); itemVariantMap.put(4996, 4759); itemVariantMap.put(4997, 4759); itemVariantMap.put(4998, 4759); // Verac's plateskirt
        // Blue moon, Blood moon, Eclipse moon armor (Used/Broken/LMS → New tradeable ID)
        itemVariantMap.put(29037, 29013); itemVariantMap.put(29058, 29013); itemVariantMap.put(29843, 29013); // Blue moon chestplate
        itemVariantMap.put(29041, 29019); itemVariantMap.put(29064, 29019); itemVariantMap.put(29845, 29019); // Blue moon helm
        itemVariantMap.put(29039, 29016); itemVariantMap.put(29061, 29016); itemVariantMap.put(29844, 29016); // Blue moon tassets
        itemVariantMap.put(29849, 28988); // Blue moon spear (LMS)
        itemVariantMap.put(29043, 29022); itemVariantMap.put(29067, 29022); itemVariantMap.put(29846, 29022); // Blood moon chestplate
        itemVariantMap.put(29047, 29028); itemVariantMap.put(29073, 29028); itemVariantMap.put(29848, 29028); // Blood moon helm
        itemVariantMap.put(29045, 29025); itemVariantMap.put(29070, 29025); itemVariantMap.put(29847, 29025); // Blood moon tassets
        itemVariantMap.put(29851, 29000); // Eclipse atlatl (LMS)
        itemVariantMap.put(29031, 29004); itemVariantMap.put(29049, 29004); itemVariantMap.put(29840, 29004); // Eclipse moon chestplate
        itemVariantMap.put(29035, 29010); itemVariantMap.put(29055, 29010); itemVariantMap.put(29842, 29010); // Eclipse moon helm
        itemVariantMap.put(29033, 29007); itemVariantMap.put(29052, 29007); itemVariantMap.put(29841, 29007); // Eclipse moon tassets
        // Tormented synapse variants
        itemVariantMap.put(29589, 29580); // Emberlight → Tormented synapse
        itemVariantMap.put(29594, 29580); // Purging staff → Tormented synapse
        itemVariantMap.put(29591, 29580); // Scorching bow → Tormented synapse
    }

    public void onBankHistoryReset()
    {
        bankValueLog.clear();
        showTab(activeTab);
    }

    public void onPricesUpdated(java.util.Map<Integer, PriceData> priceCache, java.util.Map<String, Integer> nameToId, java.util.Map<Integer, Long> avgPrice24h, java.util.Map<Integer, Long> avgPrice1h, java.util.Map<Integer, Long> avgPrice6h, java.util.Map<Integer, Integer> itemLimits, java.util.Map<Integer, Long> buyVolume1h, java.util.Map<Integer, Long> sellVolume1h, java.util.Map<Integer, Long> volumeCache)
    {
        this.priceCache = priceCache;
        this.nameToId = nameToId;
        if (itemVariantMap.isEmpty()) buildItemVariantMap();
        this.avgPrice24h = avgPrice24h;
        this.avgPrice1h = avgPrice1h;
        this.avgPrice6h = avgPrice6h;
        this.itemLimits = itemLimits;
        this.buyVolume1h = buyVolume1h;
        this.sellVolume1h = sellVolume1h;
        this.volumeCache = volumeCache;
        secondsSinceRefresh = 0;

        // If a detail panel is open, update labels in place — no rebuild
        if (liveOpenItemId != -1)
        {
            refreshOpenDetail();
        }
        else
        {
            // No detail panel open — safe to rebuild the tab normally
            showTab(activeTab);
        }
    }
    private void loadBankData()
    {
        String savedValue = plugin.loadConfig("bankValue");
        if (savedValue != null && !savedValue.trim().isEmpty())
        {
            try { totalBankValue = Long.parseLong(savedValue.trim()); }
            catch (NumberFormatException e) { }
        }
    }

    private void saveBankData()
    {
        plugin.saveConfig("bankValue", String.valueOf(totalBankValue));
    }

    private void loadBankValueLog()
    {
        String raw = plugin.loadConfig("bankValueLog");
        if (raw == null || raw.isEmpty()) return;
        bankValueLog.clear();
        for (String entry : raw.split(","))
        {
            String[] parts = entry.split("\\|");
            if (parts.length == 3)
            {
                try
                {
                    long ts = Long.parseLong(parts[0]);
                    long bankOnly = Long.parseLong(parts[1]);
                    long totalWealth = Long.parseLong(parts[2]);
                    bankValueLog.add(new long[]{ts, bankOnly, totalWealth});
                }
                catch (NumberFormatException e) { }
            }
            // silently drop old format entries (ts:val) — migration
        }
    }

    private void saveBankValueLog(long bankOnlyValue, long totalWealthValue)
    {
        long nowSeconds = System.currentTimeMillis() / 1000;

        // Add new entry
        bankValueLog.add(new long[]{nowSeconds, bankOnlyValue, totalWealthValue});

        // Split into recent (within 24h) and older entries
        long cutoff24h = nowSeconds - (24 * 3600);
        java.util.List<long[]> recent = new java.util.ArrayList<>();
        java.util.List<long[]> older = new java.util.ArrayList<>();
        for (long[] entry : bankValueLog)
        {
            if (entry[0] >= cutoff24h) recent.add(entry);
            else older.add(entry);
        }

        // Group older entries by calendar day, keep only the last per day
        java.util.Map<String, long[]> dayMap = new java.util.LinkedHashMap<>();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        for (long[] entry : older)
        {
            cal.setTimeInMillis(entry[0] * 1000);
            String dayKey = cal.get(java.util.Calendar.YEAR) + "-"
                    + cal.get(java.util.Calendar.DAY_OF_YEAR);
            dayMap.put(dayKey, entry);
        }

        // Prune entries older than 1 year
        long oneYearAgo = nowSeconds - (365L * 24 * 3600);
        dayMap.entrySet().removeIf(e -> e.getValue()[0] < oneYearAgo);

        // Rebuild log
        bankValueLog.clear();
        bankValueLog.addAll(dayMap.values());
        bankValueLog.addAll(recent);

        // Serialize
        StringBuilder sb = new StringBuilder();
        for (long[] entry : bankValueLog)
        {
            if (sb.length() > 0) sb.append(",");
            sb.append(entry[0]).append("|").append(entry[1]).append("|").append(entry[2]);
        }
        plugin.saveConfig("bankValueLog", sb.toString());
    }

    private void loadRecentSearches()
    {
        String saved = plugin.loadConfig("recentSearches");
        recentSearches.clear();
        if (saved != null && !saved.trim().isEmpty())
        {
            for (String item : saved.split("\\|"))
            {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) recentSearches.add(trimmed);
            }
        }
    }

    private void saveRecentSearches()
    {
        plugin.saveConfig("recentSearches", String.join("|", recentSearches));
    }

    private void loadPinnedItems()
    {
        pinnedItems.clear();
        String saved = config.pinnedItems();
        if (saved != null && !saved.trim().isEmpty())
        {
            for (String item : saved.split(","))
            {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) pinnedItems.add(trimmed);
            }
        }
    }

    private void savePinnedItems()
    {
        String joined = String.join(",", pinnedItems);
        plugin.saveConfig("pinnedItems", joined);
    }

    private void startLiveTimer()
    {
        liveTimer = new javax.swing.Timer(1000, e ->
        {
            secondsSinceRefresh++;
            if (timerLabel != null)
            {
                String text;
                if (secondsSinceRefresh < 60)
                    text = secondsSinceRefresh + "s ago";
                else if (secondsSinceRefresh < 3600)
                    text = (secondsSinceRefresh / 60) + "m ago";
                else
                    text = (secondsSinceRefresh / 3600) + "h ago";
                timerLabel.setText(text);
            }
        });
        liveTimer.start();
    }

    private void build()
    {
        JPanel goldBar = new JPanel();
        goldBar.setPreferredSize(new Dimension(0, 2));
        goldBar.setBackground(GOLD);
        add(goldBar, BorderLayout.NORTH);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.add(buildHeader(), BorderLayout.NORTH);

        tabContentPanel = new JPanel(new BorderLayout());
        tabContentPanel.setBackground(BG_DARK);
        wrapper.add(tabContentPanel, BorderLayout.CENTER);
        int defaultTabIndex = 1;
        switch (config.defaultTab())
        {
            case SEARCH: defaultTabIndex = 1; break;
            case WATCHLIST: defaultTabIndex = 0; break;
            case BANK: defaultTabIndex = 2; break;
        }
        showTab(defaultTabIndex);
        activeTab = defaultTabIndex;
        for (int j = 0; j < 3; j++)
            updateTabStyle(tabLabels[j], j == defaultTabIndex);

        JPanel footer = new JPanel();
        footer.setBackground(BG_DARK);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(new EmptyBorder(4, 0, 4, 0));
        JSeparator footerSeparator = new JSeparator();
        footerSeparator.setForeground(new Color(50, 45, 42));
        footerSeparator.setBackground(BG_DARK);
        JLabel footerLabel = new JLabel("prices.runescape.wiki");
        footerLabel.setForeground(TEXT_DIM);
        footerLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
        footerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.add(footerSeparator);
        footer.add(footerLabel);
        add(wrapper, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel buildHeader()
    {
        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setBackground(BG_DARK);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(BG_HEADER);
        titleRow.setBorder(new EmptyBorder(6, 8, 6, 8));

        JLabel title = new JLabel("GE Companion");
        title.setForeground(GOLD);
        title.setFont(new Font("Monospaced", Font.PLAIN, FONT_TITLE));
        titleRow.add(title, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightPanel.setBackground(BG_HEADER);

        JLabel liveDot = new JLabel("●");
        liveDot.setForeground(LIVE_GREEN);
        liveDot.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));

        timerLabel = new JLabel("0s ago");
        timerLabel.setForeground(TAB_INACTIVE);
        timerLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
        JLabel timer = timerLabel;

        JLabel refresh = new JLabel("↻");
        refresh.setForeground(TAB_INACTIVE);
        refresh.setFont(new Font("Monospaced", Font.PLAIN, FONT_REFRESH));
        refresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refresh.addMouseListener(new MouseAdapter()
        {
            public void mouseEntered(MouseEvent e) { refresh.setForeground(GOLD); }
            public void mouseExited(MouseEvent e) { refresh.setForeground(TAB_INACTIVE); }
            public void mouseClicked(MouseEvent e)
            {
                refresh.setForeground(GOLD);
                new Thread(() -> plugin.fetchPrices()).start();
            }
        });

        rightPanel.add(liveDot);
        rightPanel.add(timer);
        rightPanel.add(refresh);
        titleRow.add(rightPanel, BorderLayout.EAST);

        JPanel tabRow = new JPanel(new GridLayout(1, 3));
        tabRow.setBackground(BG_HEADER);
        tabRow.setBorder(new MatteBorder(0, 0, 1, 0, new Color(42, 37, 40)));

        String[] tabNames = {"Watchlist", "Search", "Bank"};
        for (int i = 0; i < 3; i++)
        {
            final int idx = i;
            JLabel tab = new JLabel(tabNames[i], SwingConstants.CENTER);
            tab.setFont(new Font("Monospaced", Font.PLAIN, FONT_TAB));
            tab.setBorder(new EmptyBorder(5, 0, 5, 0));
            tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            updateTabStyle(tab, i == activeTab);
            tab.addMouseListener(new MouseAdapter()
            {
                public void mouseClicked(MouseEvent e)
                {
                    // Collapse bank metadata when leaving the bank tab
                    if (activeTab == 2 && idx != 2 && bankMetadataExpanded) {
                        bankMetadataExpanded = false;
                    }
                    activeTab = idx;
                    for (int j = 0; j < 3; j++)
                        updateTabStyle(tabLabels[j], j == idx);
                    showTab(idx);
                }
                public void mouseEntered(MouseEvent e)
                {
                    if (idx != activeTab) tab.setForeground(TEXT_PRIMARY);
                }
                public void mouseExited(MouseEvent e)
                {
                    if (idx != activeTab) tab.setForeground(TAB_INACTIVE);
                }
            });
            tabLabels[i] = tab;
            tabRow.add(tab);
        }

        headerWrap.add(titleRow, BorderLayout.NORTH);
        headerWrap.add(tabRow, BorderLayout.SOUTH);
        return headerWrap;
    }

    private void updateTabStyle(JLabel tab, boolean active)
    {
        if (active)
        {
            tab.setForeground(GOLD);
            tab.setBorder(new MatteBorder(0, 0, 2, 0, GOLD));
        }
        else
        {
            tab.setForeground(TAB_INACTIVE);
            tab.setBorder(new EmptyBorder(0, 0, 2, 0));
        }
    }

    public void showTab(int index)
    {
// Save search text before rebuild
        String savedSearch = (searchField != null && index == 1) ? searchField.getText() : "";

        tabContentPanel.removeAll();
        if (!isRefreshing)
        {
            currentOpenSearchDetail = null;
            currentOpenWatchlistDetail = null;
            currentOpenBankDetail = null;
            currentOpenSearchRow = null;
            currentOpenWatchlistRow = null;
            currentOpenBankRow = null;
            selectedItemName = null;
            selectedWatchlistItemName = null;
            selectedBankItemName = null;
            searchReopenAction = null;
            watchlistReopenAction = null;
            bankReopenAction = null;
            graphWasOpen = false;
        }
        switch (index)
        {
            case 0: tabContentPanel.add(buildWatchlistTab(), BorderLayout.CENTER); break;
            case 1: tabContentPanel.add(buildSearchTab(), BorderLayout.CENTER); break;
            case 2: tabContentPanel.add(buildBankTab(), BorderLayout.CENTER); break;
        }
        tabContentPanel.revalidate();
        tabContentPanel.repaint();

        // Restore search text after rebuild
        if (!savedSearch.isEmpty() && searchField != null && index == 1)
        {
            suppressSearchChange = true;
            searchField.setText(savedSearch);
            suppressSearchChange = false;
            onSearchChanged(savedSearch);
        }
    }
    private void refreshOpenDetail()
    {
        if (liveOpenItemId == -1 || liveOpenItemData == null) return;

        PriceData pd = priceCache.get(liveOpenItemId);
        if (pd == null) return;

        String[] item = liveOpenItemData;
        long mid = pd.getMid();

        // Update header price label
        if (liveHeaderPriceLabel != null)
        {
            liveHeaderPriceLabel.setText(formatFullPrice(String.valueOf(mid)) + " gp");
        }

// Update Buy Price box
        if (liveBuyPriceValueLabel != null)
        {
            String buyPriceDisplay = pd.high <= 0 ? "?" : formatFullPrice(String.valueOf(pd.high)) + " gp";
            liveBuyPriceValueLabel.setText(buyPriceDisplay);
        }
        if (liveBuyPriceHeaderLabel != null)
        {
            String buyTimeStr = pd.getBuyTimeSince();
            String buyPriceLabel = "BUY PRICE" + (buyTimeStr.equals("unknown") ? "" : "  ·  " + buyTimeStr);
            liveBuyPriceHeaderLabel.setText(buyPriceLabel);
        }

// Update Sell Price box
        if (liveSellPriceValueLabel != null)
        {
            String sellPriceDisplay = pd.low <= 0 ? "?" : formatFullPrice(String.valueOf(pd.low)) + " gp";
            liveSellPriceValueLabel.setText(sellPriceDisplay);
        }
        if (liveSellPriceHeaderLabel != null)
        {
            String sellTimeStr = pd.getSellTimeSince();
            String sellPriceLabel = "SELL PRICE" + (sellTimeStr.equals("unknown") ? "" : "  ·  " + sellTimeStr);
            liveSellPriceHeaderLabel.setText(sellPriceLabel);
        }

// Update floating stats panel live (Margin, Profit, ROI only — Volume is static)
        if (activeStatsFloatPanel != null && liveOpenItemId != -1) {
            long liveMargin = pd.high - pd.low;
            Integer liveBuyLimit = itemLimits.get(liveOpenItemId);
            long liveProfitAtLimit = liveBuyLimit != null ? liveMargin * liveBuyLimit : 0;
            double liveRoi = pd.low > 0 ? ((double) liveMargin / pd.low) * 100.0 : 0;
            if (liveFloatMarginLabel != null) {
                String mStr = (liveMargin >= 0 ? "+" : "") + formatFullPrice(String.valueOf(liveMargin)) + " gp";
                liveFloatMarginLabel.setText(mStr);
                liveFloatMarginLabel.setForeground(liveMargin >= 0 ? new Color(109, 184, 109) : new Color(192, 57, 43));
            }
            if (liveFloatProfitLabel != null) {
                String pStr = liveBuyLimit != null ? (liveProfitAtLimit >= 0 ? "+" : "") + formatFullPrice(String.valueOf(liveProfitAtLimit)) + " gp" : "?";
                liveFloatProfitLabel.setText(pStr);
                liveFloatProfitLabel.setForeground(liveProfitAtLimit >= 0 ? new Color(109, 184, 109) : new Color(192, 57, 43));
            }
            if (liveFloatRoiLabel != null) {
                liveFloatRoiLabel.setText(String.format("%.2f%%", liveRoi));
            }
        }

// Repaint graph if open
        if (liveGraphPanel != null)
        {
            liveGraphPanel.repaint();
        }
    }

    private void updateStatBox(JPanel box, String value, String tooltip)
    {
        if (box.getComponentCount() >= 2 && box.getComponent(1) instanceof JLabel)
        {
            JLabel valueLabel = (JLabel) box.getComponent(1);
            valueLabel.setText(value);
            if (tooltip != null) valueLabel.setToolTipText(tooltip);
        }
    }

    // ── SHARED DETAIL PANEL BUILDER ──
    private JPanel buildInlineDetail(String[] item, boolean isWatchlist)
    {
        // Auto-close any open floating stats panel instantly
        if (activeStatsFloatPanel != null && activeStatsLayeredPane != null) {
            activeStatsLayeredPane.remove(activeStatsFloatPanel);
            activeStatsLayeredPane.repaint();
            activeStatsFloatPanel = null;
            activeStatsLayeredPane = null;
        }
        String name = item[0];
        String price = item[1];
        String profit = item[2];
        String buyQty = item[3];
        String sellQty = item[4];

        boolean isUp = profit.startsWith("+");

        JPanel det = new JPanel(new BorderLayout());
        det.setBackground(BG_DETAIL);
        det.setBorder(new MatteBorder(0, 2, 0, 0, GOLD));
        det.setMaximumSize(new Dimension(230, Integer.MAX_VALUE));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(BG_DETAIL);
        inner.setBorder(new javax.swing.border.CompoundBorder(
                new MatteBorder(0, 0, 1, 0, GOLD),
                new EmptyBorder(6, 7, 6, 7)));
        inner.add(buildDetailHeader(name, price, item.length > 6 ? item[6] : "?"));
        inner.add(Box.createVerticalStrut(6));

// Buy Price stat box
        int detailItemId = item.length > 12 ? Integer.parseInt(item[12]) : -1;
        PriceData pdForDetail = priceCache.get(detailItemId);
        long buyPriceVal = pdForDetail != null ? pdForDetail.high : 0;
        String buyTimeStr = pdForDetail != null ? pdForDetail.getBuyTimeSince() : "unknown";
        String buyPriceLabel = "BUY PRICE" + (buyTimeStr.equals("unknown") ? "" : "  ·  " + buyTimeStr);
        String buyPriceDisplay = buyPriceVal <= 0 ? "?" : formatFullPrice(String.valueOf(buyPriceVal)) + " gp";

        JPanel buyPriceBox = new JPanel();
        buyPriceBox.setLayout(new BoxLayout(buyPriceBox, BoxLayout.Y_AXIS));
        buyPriceBox.setBackground(new Color(14, 12, 13));
        buyPriceBox.setBorder(new EmptyBorder(6, 5, 6, 5));
        buyPriceBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        buyPriceBox.setMaximumSize(new Dimension(225, 52));
        buyPriceBox.setMinimumSize(new Dimension(225, 52));
        buyPriceBox.setPreferredSize(new Dimension(225, 52));

        JLabel buyPriceLabelComp = new JLabel(buyPriceLabel, SwingConstants.CENTER);
        buyPriceLabelComp.setForeground(TEXT_DIM);
        buyPriceLabelComp.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
        buyPriceLabelComp.setAlignmentX(Component.CENTER_ALIGNMENT);
        buyPriceLabelComp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

        JLabel buyPriceValueComp = new JLabel(buyPriceDisplay, SwingConstants.CENTER);
        buyPriceValueComp.setForeground(STAT_GOLD);
        buyPriceValueComp.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_VALUE));
        buyPriceValueComp.setAlignmentX(Component.CENTER_ALIGNMENT);
        buyPriceValueComp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        liveBuyPriceValueLabel = buyPriceValueComp;
        liveBuyPriceHeaderLabel = buyPriceLabelComp;

        buyPriceBox.add(buyPriceLabelComp);
        buyPriceBox.add(buyPriceValueComp);
        inner.add(buyPriceBox);
        inner.add(Box.createVerticalStrut(4));

        // Sell Price stat box
        long sellPriceVal = pdForDetail != null ? pdForDetail.low : 0;
        String sellTimeStr = pdForDetail != null ? pdForDetail.getSellTimeSince() : "unknown";
        String sellPriceLabel = "SELL PRICE" + (sellTimeStr.equals("unknown") ? "" : "  ·  " + sellTimeStr);
        String sellPriceDisplay = sellPriceVal <= 0 ? "?" : formatFullPrice(String.valueOf(sellPriceVal)) + " gp";

        JPanel sellPriceBox = new JPanel();
        sellPriceBox.setLayout(new BoxLayout(sellPriceBox, BoxLayout.Y_AXIS));
        sellPriceBox.setBackground(new Color(14, 12, 13));
        sellPriceBox.setBorder(new EmptyBorder(6, 5, 6, 5));
        sellPriceBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        sellPriceBox.setMaximumSize(new Dimension(225, 52));
        sellPriceBox.setMinimumSize(new Dimension(225, 52));
        sellPriceBox.setPreferredSize(new Dimension(225, 52));

        JLabel sellPriceLabelComp = new JLabel(sellPriceLabel, SwingConstants.CENTER);
        sellPriceLabelComp.setForeground(TEXT_DIM);
        sellPriceLabelComp.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
        sellPriceLabelComp.setAlignmentX(Component.CENTER_ALIGNMENT);
        sellPriceLabelComp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

        JLabel sellPriceValueComp = new JLabel(sellPriceDisplay, SwingConstants.CENTER);
        sellPriceValueComp.setForeground(STAT_BLUE);
        sellPriceValueComp.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_VALUE));
        sellPriceValueComp.setAlignmentX(Component.CENTER_ALIGNMENT);
        sellPriceValueComp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        liveSellPriceValueLabel = sellPriceValueComp;
        liveSellPriceHeaderLabel = sellPriceLabelComp;

        sellPriceBox.add(sellPriceLabelComp);
        sellPriceBox.add(sellPriceValueComp);
        inner.add(sellPriceBox);
        inner.add(Box.createVerticalStrut(4));

        JPanel grid = new JPanel(new GridLayout(1, 2, 2, 2));
        grid.setBackground(BG_DETAIL);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(225, 45));

        grid.add(buildStatBox("Buy Qty/hr", item.length > 8 ? item[8] : "?", STAT_GOLD, null));
        grid.add(buildStatBox("Sell Qty/hr", item.length > 9 ? item[9] : "?", STAT_BLUE, null));
        liveStatGrid = grid;

        inner.add(grid);
        inner.add(Box.createVerticalStrut(6));

        JPanel footer = new JPanel(new GridLayout(1, 3, 2, 0));
        footer.setBackground(BG_DETAIL);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.setMaximumSize(new Dimension(220, 24));

        boolean isWatched = pinnedItems.contains(item[0]);
        JButton watchBtn = buildFooterBtn(isWatched ? "✓ Watch" : "+ Watch", isWatched);
        if (isWatchlist || isWatched)
            watchBtn.setBorder(BorderFactory.createLineBorder(GOLD));
        watchBtn.addActionListener(e -> {
            boolean currentlyWatched = pinnedItems.contains(item[0]);
            if (isWatchlist || currentlyWatched)
            {
                pinnedItems.remove(item[0]);
            }
            else
            {
                pinnedItems.add(item[0]);
            }
            savePinnedItems();
            if (activeTab == 0) showTab(0);
            // Update button live
            boolean nowWatched = pinnedItems.contains(item[0]);
            watchBtn.setText(isWatchlist ? "- Unwatch" : (nowWatched ? "✓ Watch" : "+ Watch"));
            watchBtn.setBorder(BorderFactory.createLineBorder(
                (isWatchlist || nowWatched) ? GOLD : new Color(58, 53, 48)
            ));
            watchBtn.setForeground(nowWatched || isWatchlist ? GOLD : TAB_INACTIVE);
        });
        footer.add(watchBtn);

        JButton trackerBtn = buildFooterBtn("Prices ↗", false);
        trackerBtn.addActionListener(e -> {
            try {
                int trackerItemId = item.length > 12 ? Integer.parseInt(item[12]) : -1;
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://prices.runescape.wiki/osrs/item/" + trackerItemId));
            } catch (Exception ex) { }
        });
        footer.add(trackerBtn);

        JButton wikiBtn = buildFooterBtn("Wiki ↗", false);
        wikiBtn.addActionListener(e -> {
            try {
                String wikiName = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase().replace(" ", "_");
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://oldschool.runescape.wiki/w/" + wikiName));
            } catch (Exception ex) { }
        });
        footer.add(wikiBtn);

// ── Show Price Chart button ────────────────────────────────────
        int graphItemId = -1;
        try { if (item.length > 12) graphItemId = Integer.parseInt(item[12]); } catch (NumberFormatException ignored) {}
        liveOpenItemId = graphItemId;
        liveOpenItemData = item;
        long currentMidPrice = 0;
        try { currentMidPrice = Long.parseLong(item[1]); } catch (NumberFormatException ignored) {}

        final boolean[] graphOpen = {false};
        final JPanel[] graphPanelHolder = {null};
        final JLabel[] statsLabels = new JLabel[4];
        final boolean[] statsOpen2 = {false};
        final javax.swing.JViewport[] statsViewportHolder = {null};
        final JLabel[] statsArrowHolder = {null};
        final JLabel[] statsLblHolder = {null};
        final JPanel[] statsHeaderHolder = {null};

        JButton chartBtn = new JButton("▼ Show Price Chart");
        chartBtn.setForeground(TAB_INACTIVE);
        chartBtn.setBackground(BG_DETAIL);
        chartBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 53, 48)));
        chartBtn.setFont(new Font("Monospaced", Font.PLAIN, FONT_TAB));
        chartBtn.setFocusPainted(false);
        chartBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chartBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        chartBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(Box.createVerticalStrut(4));
        inner.add(chartBtn);
        inner.add(Box.createVerticalStrut(4));

        // ── graph viewport (animated drawer) ──────────────────────────
        JViewport graphViewport = new JViewport();
        graphViewport.setBackground(new Color(14, 12, 13));
        graphViewport.setVisible(false);
        graphViewport.setPreferredSize(new Dimension(1, 0));
        graphViewport.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        inner.add(graphViewport);
        inner.add(Box.createVerticalStrut(4));

        final int graphItemIdFinal = graphItemId;
        final long currentMidPriceFinal = currentMidPrice;

        chartBtn.addActionListener(e -> {
            if (!graphOpen[0]) {
                // build graph panel on first open
                if (graphPanelHolder[0] == null) {
                    String tf = graphActiveTimeframe;
                    graphPanelHolder[0] = buildGraphPanel(graphItemIdFinal, currentMidPriceFinal, tf, statsLabels);
                    liveGraphPanel = graphPanelHolder[0];
                    fetchGameUpdates();
                }
                graphViewport.setView(graphPanelHolder[0]);
                graphViewport.setVisible(true);

                int fullH = graphPanelHolder[0].getPreferredSize().height;
                if (fullH <= 0) fullH = 200;
                final int targetH = fullH;
                graphViewport.setPreferredSize(new Dimension(1, 0));
                graphViewport.setViewPosition(new java.awt.Point(0, targetH));
                int[] curH = {0};
                javax.swing.Timer openTimer = new javax.swing.Timer(16, null);
                openTimer.addActionListener(ev -> {
                    curH[0] = Math.min(curH[0] + 30, targetH);
                    graphViewport.setPreferredSize(new Dimension(1, curH[0]));
                    graphViewport.setViewPosition(new java.awt.Point(0, targetH - curH[0]));
                    graphViewport.revalidate();
                    if (curH[0] >= targetH) {
                        openTimer.stop();
                        graphViewport.setPreferredSize(new Dimension(1, targetH));
                        graphViewport.revalidate();
                    }
                });
                openTimer.start();
                graphOpen[0] = true;
                graphWasOpen = true;
                if (statsHeaderHolder[0] != null) statsHeaderHolder[0].setVisible(true);
                chartBtn.setText("▲ Hide Price Chart");
                chartBtn.setForeground(GOLD);
                chartBtn.setBorder(BorderFactory.createLineBorder(GOLD));
            } else {
                // close animation
                int fullH = graphViewport.getView() != null
                        ? graphViewport.getView().getPreferredSize().height : 200;
                if (fullH <= 0) fullH = 200;
                final int targetH = fullH;
                int[] curH = {targetH};
                javax.swing.Timer closeTimer = new javax.swing.Timer(16, null);
                closeTimer.addActionListener(ev -> {
                    curH[0] = Math.max(curH[0] - 30, 0);
                    graphViewport.setPreferredSize(new Dimension(1, curH[0]));
                    graphViewport.setViewPosition(new java.awt.Point(0, targetH - curH[0]));
                    graphViewport.revalidate();
                    if (curH[0] <= 0) {
                        closeTimer.stop();
                        graphViewport.setVisible(false);
                        graphViewport.setPreferredSize(new Dimension(1, 0));
                        graphViewport.revalidate();
                    }
                });
                closeTimer.start();
                graphOpen[0] = false;
                graphWasOpen = false;
                if (statsHeaderHolder[0] != null) statsHeaderHolder[0].setVisible(false);
                // also close stats if open
                if (statsOpen2[0] && statsViewportHolder[0] != null) {
                    statsOpen2[0] = false;
                    statsViewportHolder[0].setVisible(false);
                    statsViewportHolder[0].setPreferredSize(new Dimension(1, 0));
                    if (statsArrowHolder[0] != null) statsArrowHolder[0].setText("▼");
                    if (statsLblHolder[0] != null) statsLblHolder[0].setForeground(new Color(110, 100, 90));
                    if (statsArrowHolder[0] != null) statsArrowHolder[0].setForeground(new Color(110, 100, 90));
                    if (statsHeaderHolder[0] != null) statsHeaderHolder[0].setBorder(BorderFactory.createLineBorder(new Color(42, 37, 32)));
                }
                chartBtn.setText("▼ Show Price Chart");
                chartBtn.setForeground(TAB_INACTIVE);
                chartBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 53, 48)));
            }
        });

// ── statistics section (at detail panel level, outside graph viewport) ──
        JPanel statsHeader = new JPanel(new BorderLayout());
        statsHeader.setBackground(BG_DETAIL);
        statsHeader.setBorder(BorderFactory.createLineBorder(new Color(42, 37, 32)));
        statsHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        statsHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsHeader.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel statsLbl = new JLabel("STATISTICS");
        statsLbl.setForeground(new Color(110, 100, 90));
        statsLbl.setFont(new Font("Monospaced", Font.PLAIN, 10));
        statsLbl.setBorder(new EmptyBorder(0, 8, 0, 0));

        JLabel statsArrow = new JLabel("▼");
        statsArrow.setForeground(new Color(110, 100, 90));
        statsArrow.setFont(new Font("Monospaced", Font.PLAIN, 10));
        statsArrow.setBorder(new EmptyBorder(0, 0, 0, 8));

        statsHeader.add(statsLbl, BorderLayout.WEST);
        statsHeader.add(statsArrow, BorderLayout.EAST);

        JPanel statsContent = new JPanel(new GridLayout(2, 2, 2, 2));
        statsContent.setBackground(BG_DETAIL);
        statsContent.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsContent.setPreferredSize(new Dimension(200, 110));
        statsContent.setMaximumSize(new Dimension(200, 200));

        String[] statNames = {"Buying High", "Buying Low", "Selling High", "Selling Low"};
        Color[] statColors = {GOLD, GOLD, STAT_BLUE, STAT_BLUE};
        for (int i = 0; i < 4; i++) {
            JPanel box = new JPanel();
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setBackground(new Color(14, 12, 13));
            box.setBorder(new EmptyBorder(8, 5, 0, 5));
            JLabel nameLabel = new JLabel(statNames[i].toUpperCase(), SwingConstants.CENTER);
            nameLabel.setForeground(TEXT_DIM);
            nameLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
            JLabel valLabel = new JLabel("—");
            valLabel.setForeground(statColors[i]);
            valLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_VALUE));
            valLabel.setHorizontalAlignment(SwingConstants.CENTER);
            valLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            nameLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
            statsLabels[i] = valLabel;
            box.add(nameLabel);
            box.add(valLabel);
            statsContent.add(box);
        }
        liveStatsLabels = statsLabels;

        JViewport statsViewport = new JViewport();
        statsViewport.setBackground(BG_DETAIL);
        statsViewport.setVisible(false);
        statsViewport.setPreferredSize(new Dimension(1, 0));
        statsViewport.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        statsHeader.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!graphOpen[0]) return; // don't open stats if chart is closed
                if (!statsOpen2[0]) {
                    statsViewport.setView(statsContent);
                    statsViewport.setVisible(true);
                    statsContent.doLayout();
                    int fullH = 110;
                    final int targetH = fullH;
                    statsViewport.setPreferredSize(new Dimension(1, 0));
                    statsViewport.setViewPosition(new java.awt.Point(0, targetH));
                    int[] curH = {0};
                    javax.swing.Timer t = new javax.swing.Timer(16, null);
                    t.addActionListener(ev -> {
                        curH[0] = Math.min(curH[0] + 15, targetH);
                        statsViewport.setPreferredSize(new Dimension(1, curH[0]));
                        statsViewport.setViewPosition(new java.awt.Point(0, targetH - curH[0]));
                        statsViewport.revalidate();
                        if (curH[0] >= targetH) {
                            t.stop();
                            statsViewport.setPreferredSize(new Dimension(1, targetH));
                            statsViewport.revalidate();
                        }
                    });
                    t.start();
                    statsOpen2[0] = true;
                    statsArrow.setText("▲");
                    statsLbl.setForeground(GOLD);
                    statsArrow.setForeground(GOLD);
                    statsHeader.setBorder(BorderFactory.createLineBorder(GOLD));
                } else {
                    int fullH = statsViewport.getView() != null ? statsViewport.getView().getPreferredSize().height : 100;
                    if (fullH <= 0) fullH = 100;
                    final int targetH = fullH;
                    int[] curH = {targetH};
                    javax.swing.Timer t = new javax.swing.Timer(16, null);
                    t.addActionListener(ev -> {
                        curH[0] = Math.max(curH[0] - 15, 0);
                        statsViewport.setPreferredSize(new Dimension(1, curH[0]));
                        statsViewport.setViewPosition(new java.awt.Point(0, targetH - curH[0]));
                        statsViewport.revalidate();
                        if (curH[0] <= 0) {
                            t.stop();
                            statsViewport.setVisible(false);
                            statsViewport.setPreferredSize(new Dimension(1, 0));
                            statsViewport.revalidate();
                        }
                    });
                    t.start();
                    statsOpen2[0] = false;
                    statsArrow.setText("▼");
                    statsLbl.setForeground(new Color(110, 100, 90));
                    statsArrow.setForeground(new Color(110, 100, 90));
                    statsHeader.setBorder(BorderFactory.createLineBorder(new Color(42, 37, 32)));
                }
            }
            public void mouseEntered(MouseEvent e) {
                if (!statsOpen2[0]) statsHeader.setBorder(BorderFactory.createLineBorder(new Color(80, 70, 60)));
            }
            public void mouseExited(MouseEvent e) {
                if (!statsOpen2[0]) statsHeader.setBorder(BorderFactory.createLineBorder(new Color(42, 37, 32)));
            }
        });

// wire up holders
        statsViewportHolder[0] = statsViewport;
        statsArrowHolder[0] = statsArrow;
        statsLblHolder[0] = statsLbl;
        statsHeaderHolder[0] = statsHeader;

        inner.add(Box.createVerticalStrut(4));
        statsHeader.setVisible(false);
        inner.add(statsHeader);
        inner.add(statsViewport);
        inner.add(Box.createVerticalStrut(4));
        inner.add(footer);
        det.add(inner, BorderLayout.CENTER);
        return det;
    }

    // ── SEARCH TAB ──
    private JPanel buildSearchTab()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);

        JPanel searchWrap = new JPanel(new BorderLayout());
        searchWrap.setBackground(new Color(20, 18, 19));
        searchWrap.setBorder(new EmptyBorder(4, 6, 4, 6));

        searchField.setBackground(new Color(14, 12, 13));
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setCaretColor(TEXT_PRIMARY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(58, 53, 48)),
                new EmptyBorder(3, 5, 3, 5)
        ));
        searchField.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));

        searchClearBtn = new JLabel("✕");
        searchClearBtn.setForeground(TEXT_DIM);
        searchClearBtn.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
        searchClearBtn.setBorder(new EmptyBorder(0, 4, 0, 4));
        searchClearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchClearBtn.setVisible(!searchField.getText().isEmpty());
        searchClearBtn.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e) { searchField.setText(""); }
            public void mouseEntered(MouseEvent e) { searchClearBtn.setForeground(TEXT_PRIMARY); }
            public void mouseExited(MouseEvent e) { searchClearBtn.setForeground(TEXT_DIM); }
        });

        searchWrap.add(searchField, BorderLayout.CENTER);
        searchWrap.add(searchClearBtn, BorderLayout.EAST);
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_DARK);
        topBar.add(buildTimeFrameBar(), BorderLayout.NORTH);
        topBar.add(searchWrap, BorderLayout.SOUTH);
        panel.add(topBar, BorderLayout.NORTH);

        recentSearchesPanel = new JPanel();
        recentSearchesPanel.setLayout(new BoxLayout(recentSearchesPanel, BoxLayout.Y_AXIS));
        recentSearchesPanel.setBackground(BG_DARK);

        recentSearchesPanel.setBackground(BG_DARK);

        if (!recentSearches.isEmpty())
        {
            JLabel recentLabel = new JLabel("Recent Searches");
            recentLabel.setForeground(TEXT_DIM);
            recentLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
            recentLabel.setBorder(new EmptyBorder(4, 7, 2, 7));
            recentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            recentSearchesPanel.add(recentLabel);

            for (String recentItem : recentSearches)
            {
                JPanel chipRow = new JPanel(new BorderLayout());
                chipRow.setBackground(BG_DARK);
                chipRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                chipRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

                JLabel chip = new JLabel("⟳ " + recentItem);
                chip.setForeground(TAB_INACTIVE);
                chip.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
                chip.setBorder(new EmptyBorder(3, 7, 3, 7));
                chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                JLabel removeBtn = new JLabel("✕");
                removeBtn.setForeground(TEXT_DIM);
                removeBtn.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
                removeBtn.setBorder(new EmptyBorder(3, 0, 3, 7));
                removeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                chipRow.add(chip, BorderLayout.CENTER);
                chipRow.add(removeBtn, BorderLayout.EAST);

                chip.addMouseListener(new MouseAdapter()
                {
                    public void mouseClicked(MouseEvent e) { searchField.setText(recentItem); }
                    public void mouseEntered(MouseEvent e) { chip.setForeground(TEXT_PRIMARY); chipRow.setBackground(BG_ROW_HOVER); }
                    public void mouseExited(MouseEvent e) { chip.setForeground(TAB_INACTIVE); chipRow.setBackground(BG_DARK); }
                });

                removeBtn.addMouseListener(new MouseAdapter()
                {
                    public void mouseClicked(MouseEvent e)
                    {
                        recentSearches.remove(recentItem);
                        saveRecentSearches();
                        showTab(activeTab);
                    }
                    public void mouseEntered(MouseEvent e) { removeBtn.setForeground(TEXT_PRIMARY); }
                    public void mouseExited(MouseEvent e) { removeBtn.setForeground(TEXT_DIM); }
                });

                recentSearchesPanel.add(chipRow);
            }
        }

        searchResultsPanel = new JPanel();
        searchResultsPanel.setLayout(new BoxLayout(searchResultsPanel, BoxLayout.Y_AXIS));
        searchResultsPanel.setBackground(new Color(20, 18, 19));
        searchResultsPanel.setVisible(false);
        searchResultsPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel listWrapper = new JPanel(new BorderLayout())
        {
            @Override
            public Dimension getPreferredSize()
            {
                Dimension d = super.getPreferredSize();
                d.height = Math.max(d.height, 400);
                return d;
            }
        };
        listWrapper.setBackground(BG_DARK);
        listWrapper.add(recentSearchesPanel, BorderLayout.NORTH);
        listWrapper.add(searchResultsPanel, BorderLayout.CENTER);

        panel.add(listWrapper, BorderLayout.CENTER);
        searchField.addFocusListener(new FocusAdapter()
        {
            public void focusGained(FocusEvent e)
            {
                searchField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(GOLD),
                        new EmptyBorder(3, 5, 3, 5)
                ));
            }
            public void focusLost(FocusEvent e)
            {
                searchField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(58, 53, 48)),
                        new EmptyBorder(3, 5, 3, 5)
                ));
            }
        });

        return panel;
    }

    private void onSearchChanged(String query)
    {
        if (suppressSearchChange) return;
        // Close any open detail when search changes
        if (currentOpenSearchDetail != null)
        {
            currentOpenSearchDetail.setVisible(false);
            currentOpenSearchDetail = null;
        }
        if (currentOpenSearchRow != null)
        {
            currentOpenSearchRow.setBackground(BG_DARK);
            currentOpenSearchRow = null;
        }
        selectedItemName = null;

        if (query.trim().isEmpty())
        {
            // Rebuild recent searches panel dynamically
            recentSearchesPanel.removeAll();
            if (!recentSearches.isEmpty())
            {
                JLabel recentLabel = new JLabel("Recent Searches");
                recentLabel.setForeground(TEXT_DIM);
                recentLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
                recentLabel.setBorder(new EmptyBorder(4, 7, 2, 7));
                recentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                recentSearchesPanel.add(recentLabel);
                for (String recentItem : recentSearches)
                {
                    JPanel chipRow = new JPanel(new BorderLayout());
                    chipRow.setBackground(BG_DARK);
                    chipRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                    chipRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
                    JLabel chip = new JLabel("⟳ " + recentItem);
                    chip.setForeground(TAB_INACTIVE);
                    chip.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
                    chip.setBorder(new EmptyBorder(3, 7, 3, 7));
                    chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    JLabel removeBtn = new JLabel("✕");
                    removeBtn.setForeground(TEXT_DIM);
                    removeBtn.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
                    removeBtn.setBorder(new EmptyBorder(3, 0, 3, 7));
                    removeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    chipRow.add(chip, BorderLayout.CENTER);
                    chipRow.add(removeBtn, BorderLayout.EAST);
                    chip.addMouseListener(new MouseAdapter()
                    {
                        public void mouseClicked(MouseEvent e) { searchField.setText(recentItem); }
                        public void mouseEntered(MouseEvent e) { chip.setForeground(TEXT_PRIMARY); chipRow.setBackground(BG_ROW_HOVER); }
                        public void mouseExited(MouseEvent e) { chip.setForeground(TAB_INACTIVE); chipRow.setBackground(BG_DARK); }
                    });
                    removeBtn.addMouseListener(new MouseAdapter()
                    {
                        public void mouseClicked(MouseEvent e)
                        {
                            recentSearches.remove(recentItem);
                            saveRecentSearches();
                            onSearchChanged("");
                        }
                        public void mouseEntered(MouseEvent e) { removeBtn.setForeground(TEXT_PRIMARY); }
                        public void mouseExited(MouseEvent e) { removeBtn.setForeground(TEXT_DIM); }
                    });
                    recentSearchesPanel.add(chipRow);
                }
            }
            recentSearchesPanel.revalidate();
            recentSearchesPanel.repaint();
            recentSearchesPanel.setVisible(true);
            searchResultsPanel.setVisible(false);
        }
        else if (query.trim().length() >= 2)
        {
            recentSearchesPanel.setVisible(false);
            searchResultsPanel.setVisible(true);
            updateSearchResults(query.trim().toLowerCase());
        }
        else
        {
            recentSearchesPanel.setVisible(true);
            searchResultsPanel.setVisible(false);
        }
        tabContentPanel.revalidate();
        tabContentPanel.repaint();
    }

    private void updateSearchResults(String query) {
        searchResultsPanel.removeAll();

        List<String[]> results = new ArrayList<>();

        if (!nameToId.isEmpty() && !priceCache.isEmpty()) {
            // Use live data
            for (Map.Entry<String, Integer> entry : nameToId.entrySet()) {
                if (entry.getKey().contains(query)) {
                    int id = entry.getValue();
                    PriceData pd = priceCache.get(id);
                    if (pd != null && pd.getMid() > 0) {
                        String name = entry.getKey();
// Capitalize first letter of each word
                        String[] words = name.split(" ");
                        StringBuilder sb = new StringBuilder();
                        for (String word : words) {
                            if (word.length() > 0)
                                sb.append(Character.toUpperCase(word.charAt(0)))
                                        .append(word.substring(1)).append(" ");
                        }
                        String displayName = sb.toString().trim();
                        String[] fullItem = buildItemDataFromCache(displayName);
                        if (fullItem != null) results.add(fullItem);
                    }
                }
            }

            // Sort alphabetically
            results.sort((a, b) -> a[0].compareTo(b[0]));
            // Limit to 50 results
            if (results.size() > 50) results = results.subList(0, 50);
        } else {
            // Data still loading
            searchResultsPanel.removeAll();
            JLabel loading = new JLabel("⟳ Loading prices...");
            loading.setForeground(TAB_INACTIVE);
            loading.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
            loading.setBorder(new EmptyBorder(8, 7, 8, 7));
            loading.setAlignmentX(Component.LEFT_ALIGNMENT);
            searchResultsPanel.add(loading);
            searchResultsPanel.revalidate();
            searchResultsPanel.repaint();
            return;
        }

        if (results.isEmpty()) {
            JLabel noResults = new JLabel("No results found");
            noResults.setForeground(TEXT_DIM);
            noResults.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
            noResults.setBorder(new EmptyBorder(8, 7, 8, 7));
            noResults.setAlignmentX(Component.LEFT_ALIGNMENT);
            searchResultsPanel.add(noResults);
        } else {
            int searchIndex = 0;
            for (String[] item : results) {
                searchResultsPanel.add(buildSearchItemBlock(item, searchIndex++));
            }
        }
        searchResultsPanel.add(Box.createVerticalGlue());
        searchResultsPanel.revalidate();
        searchResultsPanel.repaint();
        scheduleRepaint(searchResultsPanel);
    }

    private JPanel buildSearchItemBlock(String[] item, int index)
    {
        String name = item[0];
        String price = item[1];
        String buyQty = item[3];
        String sellQty = item[4];
        String delta = item[5];
        String limit = item.length > 6 ? item[6] : "?";
        String gpChange = item.length > 7 ? item[7] : "0";

        boolean isUp = delta.startsWith("+");
        boolean isDown = delta.startsWith("-");
        Color rowBg = (index % 2 == 0) ? BG_DARK : new Color(20, 18, 19);

        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBackground(rowBg);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, 800));
        block.setBorder(javax.swing.BorderFactory.createEmptyBorder());

        JPanel row = new JPanel(new BorderLayout())
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                if (name.equals(selectedItemName))
                {
                    Graphics2D g2 = (Graphics2D) g.create();
                    GradientPaint gp = new GradientPaint(0, 0, new Color(212, 175, 55, 180), getWidth(), 0, new Color(0, 0, 0, 0));
                    g2.setPaint(gp);
                    g2.fillRect(0, 0, getWidth(), 3);
                    g2.dispose();
                }
            }
        };
        row.setBackground(rowBg);
        row.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel iconPanel = new JPanel(new java.awt.GridBagLayout());
        iconPanel.setPreferredSize(new Dimension(42, 42));
        iconPanel.setBackground(new Color(14, 12, 13));
        iconPanel.setOpaque(true);
        iconPanel.setBorder(BorderFactory.createLineBorder(isUp ? new Color(0, 100, 0) : isDown ? new Color(100, 0, 0) : new Color(42, 37, 40)));

        Integer itemId = nameToId.get(name.toLowerCase()
                .replace('\u2019', '\'')
                .replace('\u2018', '\''));
        if (itemId == null) itemId = nameToId.get(name.toLowerCase());
        if (itemId != null)
        {
            loadIconAsync(itemId, iconPanel, rowBg);
        }

        final JPanel iconWrapper = new JPanel(new java.awt.GridBagLayout());
        iconWrapper.setBackground(rowBg);
        iconWrapper.add(iconPanel);
        iconWrapper.setToolTipText(name);
        row.add(iconWrapper, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(rowBg);
        info.setBorder(new EmptyBorder(6, 7, 6, 7));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_ITEM_NAME));
        nameLabel.setMaximumSize(new Dimension(190, 20));

        JLabel priceLabel = new JLabel(formatPrice(price) + " gp");
        priceLabel.setForeground(PRICE_GOLD);
        priceLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_PRICE));

        JLabel qtyLabel = new JLabel(buyQty.equals("0") ? "" : "Buy: " + buyQty + "/hr  Sell: " + sellQty + "/hr");
        qtyLabel.setForeground(TAB_INACTIVE);
        qtyLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));

        JLabel deltaLabel = new JLabel("(" + delta + "%)");
        deltaLabel.setForeground(isUp ? GREEN_UP : isDown ? RED_DOWN : TAB_INACTIVE);
        deltaLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
        deltaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel gpChangeLabel = new JLabel(gpChange);
        gpChangeLabel.setForeground(isUp ? GREEN_UP : isDown ? RED_DOWN : TAB_INACTIVE);
        gpChangeLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_DELTA));

        JPanel deltaLimitRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        deltaLimitRow.setBackground(rowBg);
        deltaLimitRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        deltaLimitRow.add(gpChangeLabel);
        deltaLimitRow.add(Box.createHorizontalStrut(4));
        deltaLimitRow.add(deltaLabel);

        info.add(nameLabel);
        info.add(priceLabel);
        info.add(deltaLimitRow);
        row.add(info, BorderLayout.CENTER);

// Inline detail panel for this row
        final JButton[] watchBtnRef = {null};
        JPanel detailSlot = new JPanel(new BorderLayout());
        detailSlot.setBackground(BG_DARK);
        detailSlot.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        detailSlot.setAlignmentX(Component.LEFT_ALIGNMENT);

        javax.swing.JViewport detailViewport = new javax.swing.JViewport();
        detailViewport.setView(detailSlot);
        detailViewport.setVisible(false);
        detailViewport.setBorder(null);
        detailViewport.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.addMouseListener(new MouseAdapter()
        {
            // Toggle — click same item to close
            public void mouseClicked(MouseEvent e)
            {
                if (javax.swing.SwingUtilities.isRightMouseButton(e))
                {
                    javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
                    boolean isWatched = pinnedItems.contains(item[0]);
                    javax.swing.JMenuItem watchItem = new javax.swing.JMenuItem(isWatched ? "- Unwatch" : "+ Watch");
                    watchItem.addActionListener(ev -> {
                        boolean nowWatched = !pinnedItems.contains(item[0]);
                        if (nowWatched) pinnedItems.add(item[0]);
                        else pinnedItems.remove(item[0]);
                        savePinnedItems();
                        if (activeTab == 0) showTab(0);
                        // update watch button live if detail panel is open
                        JButton wb = watchBtnRef[0];
                        if (wb != null) {
                            wb.setText(nowWatched ? "✓ Watch" : "+ Watch");
                            wb.setBorder(BorderFactory.createLineBorder(nowWatched ? GOLD : new Color(58, 53, 48)));
                            wb.setForeground(nowWatched ? GOLD : TAB_INACTIVE);
                        }
                        // update popup item text for next time
                        watchItem.setText(nowWatched ? "- Unwatch" : "+ Watch");
                    });
                    popup.add(watchItem);
                    javax.swing.JMenuItem pricesItem = new javax.swing.JMenuItem("Prices ↗");
                    pricesItem.addActionListener(ev -> {
                        try {
                            int trackerItemId = item.length > 12 ? Integer.parseInt(item[12]) : -1;
                            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://prices.runescape.wiki/osrs/item/" + trackerItemId));
                        } catch (Exception ex) { }
                    });
                    popup.add(pricesItem);
                    javax.swing.JMenuItem wikiItem = new javax.swing.JMenuItem("Wiki ↗");
                    wikiItem.addActionListener(ev -> {
                        try {
                            String wikiName = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase().replace(" ", "_");
                            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://oldschool.runescape.wiki/w/" + wikiName));
                        } catch (Exception ex) { }
                    });
                    popup.add(wikiItem);
                    popup.show(row, e.getX(), e.getY());
                    return;
                }
                if (name.equals(selectedItemName) && currentOpenSearchDetail != null)
                {
                    final JPanel closingDetail = currentOpenSearchDetail;
                    currentOpenSearchDetail = null;
                    final javax.swing.JViewport closingVP = (javax.swing.JViewport) closingDetail.getParent();
                    final int fullH = closingVP != null ? closingVP.getHeight() : 0;
                    int[] curH2 = {fullH};
                    javax.swing.Timer closeTimer = new javax.swing.Timer(16, null);
                    closeTimer.addActionListener(ev -> {
                        curH2[0] = Math.max(curH2[0] - 30, 0);
                        if (closingVP != null) {
                            closingVP.setPreferredSize(new Dimension(230, curH2[0]));
                            closingVP.setViewPosition(new java.awt.Point(0, fullH - curH2[0]));
                            closingVP.revalidate();
                        }
                        if (curH2[0] <= 0)
                        {
                            if (closingVP != null) { closingVP.setVisible(false); closingVP.setPreferredSize(null); }
                            if (currentOpenSearchRow != null) { currentOpenSearchRow.setBackground(currentOpenSearchRowColor); }
                            if (currentOpenSearchInfo != null) { currentOpenSearchInfo.setBackground(currentOpenSearchRowColor); currentOpenSearchInfo = null; }
                            if (currentOpenSearchIconWrapper != null) { currentOpenSearchIconWrapper.setBackground(currentOpenSearchRowColor); currentOpenSearchIconWrapper = null; }
                            if (currentOpenSearchDeltaRow != null) { currentOpenSearchDeltaRow.setBackground(currentOpenSearchRowColor); currentOpenSearchDeltaRow = null; }
                            searchResultsPanel.revalidate();
                            searchResultsPanel.repaint();
                            closeTimer.stop();
                        }
                    });
                    closeTimer.start();
                        if (currentOpenSearchRow != null)
                        {
                        currentOpenSearchRow.setBackground(currentOpenSearchRowColor);
                        currentOpenSearchRow.setBorder(new MatteBorder(0, 3, 0, 1, new Color(26, 24, 24)));
                        for (Component c : currentOpenSearchRow.getComponents())
                        {
                            if (c instanceof JPanel)
                            {
                                if (((JPanel)c).getComponentCount() > 0 && ((JPanel)c).getComponent(0) instanceof JPanel && ((JPanel)c).getComponent(0).getPreferredSize().width == 42)
                                {
                                    c.setBackground(currentOpenSearchRowColor);
                                    continue;
                                }
                                c.setBackground(currentOpenSearchRowColor);
                                for (Component cc : ((JPanel)c).getComponents())
                                    if (cc instanceof JPanel && cc.getPreferredSize().width != 42) cc.setBackground(currentOpenSearchRowColor);
                            }
                        }
                        currentOpenSearchRow = null;
                        currentOpenSearchRowColor = BG_DARK;
                        currentOpenSearchRow = null;
                    }
                    selectedItemName = null;
                    searchReopenAction = null;
                    graphWasOpen = false;
                    liveHeaderPriceLabel = null;
                    liveBuyPriceValueLabel = null;
                    liveBuyPriceHeaderLabel = null;
                    liveSellPriceValueLabel = null;
                    liveSellPriceHeaderLabel = null;
                    liveFloatVolumeLabel = null;
                    liveFloatMarginLabel = null;
                    liveFloatProfitLabel = null;
                    liveFloatRoiLabel = null;
                    liveStatGrid = null;
                    liveStatsLabels = null;
                    liveOpenItemId = -1;
                    liveOpenItemData = null;
                    liveGraphPanel = null;
                    searchResultsPanel.revalidate();
                    searchResultsPanel.repaint();
                    return;
                }

                // Close previous
                if (currentOpenSearchDetail != null)
                {
                    final JPanel closingDetail2 = currentOpenSearchDetail;
                    currentOpenSearchDetail = null;
                    final javax.swing.JViewport closingVP2 = (javax.swing.JViewport) closingDetail2.getParent();
                    final int fullH2 = closingVP2 != null ? closingVP2.getHeight() : 0;
                    int[] curH3 = {fullH2};
                    javax.swing.Timer closeTimer2 = new javax.swing.Timer(16, null);
                    closeTimer2.addActionListener(ev -> {
                        curH3[0] = Math.max(curH3[0] - 30, 0);
                        if (closingVP2 != null) {
                            closingVP2.setPreferredSize(new Dimension(230, curH3[0]));
                            closingVP2.setViewPosition(new java.awt.Point(0, fullH2 - curH3[0]));
                            closingVP2.revalidate();
                        }
                        if (curH3[0] <= 0)
                        {
                            if (closingVP2 != null) { closingVP2.setVisible(false); closingVP2.setPreferredSize(null); }
                            if (currentOpenSearchRow != null) { currentOpenSearchRow.setBackground(currentOpenSearchRowColor); }
                            if (currentOpenSearchInfo != null) { currentOpenSearchInfo.setBackground(currentOpenSearchRowColor); currentOpenSearchInfo = null; }
                            if (currentOpenSearchIconWrapper != null) { currentOpenSearchIconWrapper.setBackground(currentOpenSearchRowColor); currentOpenSearchIconWrapper = null; }
                            if (currentOpenSearchDeltaRow != null) { currentOpenSearchDeltaRow.setBackground(currentOpenSearchRowColor); currentOpenSearchDeltaRow = null; }
                            searchResultsPanel.revalidate();
                            searchResultsPanel.repaint();
                            closeTimer2.stop();
                        }
                    });
                    closeTimer2.start();
                    graphWasOpen = false;
                }
                if (currentOpenSearchRow != null)
                {
                    currentOpenSearchRow.setBackground(BG_DARK);
                    currentOpenSearchRow.setBorder(new MatteBorder(0, 3, 0, 1, new Color(26, 24, 24)));
                    for (Component c : currentOpenSearchRow.getComponents())
                    {
                        if (c instanceof JPanel)
                        {
                            if (((JPanel)c).getComponentCount() > 0 && ((JPanel)c).getComponent(0) instanceof JPanel && ((JPanel)c).getComponent(0).getPreferredSize().width == 42)
                            {
                                c.setBackground(BG_DARK);
                                continue;
                            }
                            c.setBackground(BG_DARK);
                        }
                    }
                }

// Open this one
                selectedItemName = name;
// Add to recent searches and auto-fill search field
                String lowerName = name.toLowerCase();
                recentSearches.remove(lowerName);
                recentSearches.add(0, lowerName);
                if (recentSearches.size() > 5) recentSearches = new java.util.ArrayList<>(recentSearches.subList(0, 5));
                javax.swing.SwingUtilities.invokeLater(() -> saveRecentSearches());
                currentOpenSearchRow = row;
                currentOpenSearchRowColor = rowBg;
                currentOpenSearchDetail = detailSlot;
                currentOpenSearchInfo = info;
                currentOpenSearchIconWrapper = iconWrapper;
                currentOpenSearchDeltaRow = deltaLimitRow;
                graphWasOpen = false;
                suppressSearchChange = true;
                searchField.setText(name);
                searchField.selectAll();
                suppressSearchChange = false;

                detailSlot.removeAll();
                detailSlot.add(buildInlineDetail(item, false), BorderLayout.CENTER);
// find and store watch button reference for right-click menu
                watchBtnRef[0] = findWatchButton(detailSlot);
                detailSlot.revalidate();
                javax.swing.SwingUtilities.invokeLater(() -> {
                    int fullHeight = detailSlot.getPreferredSize().height;
                    if (fullHeight <= 0) fullHeight = 150;
                    final int targetFinal = fullHeight;
                    detailViewport.setPreferredSize(new Dimension(230, 0));
                    detailViewport.setViewPosition(new java.awt.Point(0, targetFinal));
                    detailViewport.setVisible(true);
                    detailViewport.revalidate();
                    int[] curH = {0};
                    javax.swing.Timer openTimer = new javax.swing.Timer(16, null);
                    openTimer.addActionListener(ev -> {
                        curH[0] = Math.min(curH[0] + 30, targetFinal);
                        detailViewport.setPreferredSize(new Dimension(230, curH[0]));
                        detailViewport.setViewPosition(new java.awt.Point(0, targetFinal - curH[0]));
                        detailViewport.revalidate();
                        if (curH[0] >= targetFinal)
                        {
                            detailViewport.setPreferredSize(null);
                            detailViewport.setViewPosition(new java.awt.Point(0, 0));
                            openTimer.stop();
                        }
                    });
                    openTimer.start();
                });
                scheduleRepaint(detailSlot);
                final String[] itemData = item;
                searchReopenAction = () -> {
                    for (java.awt.Component c : searchResultsPanel.getComponents())
                    {
                        if (!(c instanceof JPanel)) continue;
                        JPanel block = (JPanel) c;
                        if (block.getComponentCount() < 2) continue;
                        java.awt.Component second = block.getComponent(1);
                        if (!(second instanceof javax.swing.JViewport)) continue;
                        javax.swing.JViewport newViewport = (javax.swing.JViewport) second;
                        if (!(newViewport.getView() instanceof JPanel)) continue;
                        JPanel newDetailSlot = (JPanel) newViewport.getView();
                        java.awt.Component first = block.getComponent(0);
                        if (!(first instanceof JPanel)) continue;
                        JPanel newRow = (JPanel) first;
                        for (java.awt.Component child : newRow.getComponents())
                        {
                            if (!(child instanceof JPanel)) continue;
                            for (java.awt.Component rowChild : ((JPanel)child).getComponents())
                            {
                                if (!(rowChild instanceof JLabel)) continue;
                                JLabel label = (JLabel) rowChild;
                                if (itemData[0].equalsIgnoreCase(label.getText()))
                                {
                                    newDetailSlot.removeAll();
                                    newDetailSlot.add(buildInlineDetail(itemData, false), BorderLayout.CENTER);
                                    newViewport.setVisible(true);
                                    scheduleRepaint(newDetailSlot);
                                    if (graphWasOpen) reopenGraph(newDetailSlot);
                                    currentOpenSearchDetail = newDetailSlot;
                                    selectedItemName = itemData[0];
                                    newRow.setBorder(BorderFactory.createCompoundBorder(
                                            new MatteBorder(1, 3, 0, 0, GOLD),
                                            new MatteBorder(0, 0, 1, 0, new Color(26, 24, 24))
                                    ));
                                    currentOpenSearchRow = newRow;
                                    return;
                                }
                            }
                        }
                    }
                };

                row.setBackground(BG_ROW_SELECTED);
                info.setBackground(BG_ROW_SELECTED);
                iconWrapper.setBackground(BG_ROW_SELECTED);
                deltaLimitRow.setBackground(BG_ROW_SELECTED);
                row.setBorder(BorderFactory.createCompoundBorder(
                        new MatteBorder(1, 3, 0, 0, GOLD),
                        new MatteBorder(0, 0, 1, 0, new Color(26, 24, 24))
                ));

                searchResultsPanel.revalidate();
                searchResultsPanel.repaint();

                SwingUtilities.invokeLater(() -> {
                    JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, row);
                    if (sp != null)
                    {
                        sp.getViewport().revalidate();
                        Rectangle slotBounds = SwingUtilities.convertRectangle(
                                detailSlot.getParent(), detailSlot.getBounds(), sp.getViewport().getView()
                        );
                        sp.getViewport().scrollRectToVisible(slotBounds);
                    }
                });
            }
            public void mouseEntered(MouseEvent e)
            {
                if (!name.equals(selectedItemName))
                {
                    row.setBackground(BG_ROW_HOVER);
                    info.setBackground(BG_ROW_HOVER);
                    iconWrapper.setBackground(BG_ROW_HOVER);
                    deltaLimitRow.setBackground(BG_ROW_HOVER);
                }
            }
            public void mouseExited(MouseEvent e)
            {
                if (!name.equals(selectedItemName))
                {
                    row.setBackground(rowBg);
                    info.setBackground(rowBg);
                    iconWrapper.setBackground(rowBg);
                    deltaLimitRow.setBackground(rowBg);
                }
            }
        });

        block.add(row);
        block.add(detailViewport);
        return block;
    }

    // ── WATCHLIST TAB ──
    private JPanel buildWatchlistTab()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);

        JPanel listPanel = new JPanel();
        watchlistListPanel = listPanel;
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(new Color(20, 18, 19));
        listPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder());

        JPanel pinnedHeader = new JPanel(new BorderLayout());
        pinnedHeader.setBackground(new Color(20, 18, 19));
        pinnedHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        pinnedHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel pinnedLabel = new JLabel("Pinned Items");
        pinnedLabel.setForeground(TEXT_DIM);
        pinnedLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
        pinnedLabel.setBorder(new EmptyBorder(4, 7, 2, 7));

        JLabel editBtn = new JLabel(watchlistEditMode ? "Done" : "Edit");
        editBtn.setForeground(TAB_INACTIVE);
        editBtn.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
        editBtn.setBorder(watchlistEditMode ?
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(GOLD), new EmptyBorder(3, 6, 1, 6)) :
                new EmptyBorder(4, 7, 2, 7));
        editBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editBtn.setVisible(watchlistEditMode);
        editBtn.setForeground(watchlistEditMode ? GOLD : TAB_INACTIVE);

        pinnedHeader.add(pinnedLabel, BorderLayout.WEST);
        pinnedHeader.add(editBtn, BorderLayout.EAST);

        pinnedHeader.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { if (currentOpenWatchlistDetail == null) editBtn.setVisible(true); }
            @Override
            public void mouseExited(MouseEvent e) { if (!watchlistEditMode) editBtn.setVisible(false); }
        });

        editBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                editBtn.setVisible(true);
                editBtn.setForeground(watchlistEditMode ? GOLD : TAB_INACTIVE);
                editBtn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(watchlistEditMode ? GOLD : TAB_INACTIVE),
                        new EmptyBorder(3, 6, 1, 6)));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                editBtn.setForeground(watchlistEditMode ? GOLD : TAB_INACTIVE);
                editBtn.setBorder(watchlistEditMode ?
                        BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(GOLD), new EmptyBorder(3, 6, 1, 6)) :
                        new EmptyBorder(4, 7, 2, 7));
                if (!watchlistEditMode) editBtn.setVisible(false);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentOpenWatchlistDetail != null) return;
                watchlistEditMode = !watchlistEditMode;
                showTab(0);
            }
        });

        listPanel.add(pinnedHeader);

        int watchlistIndex = 0;
        for (String pinnedName : pinnedItems)
        {
            // Pre-warm icon cache for watchlist items
            String normalizedName = pinnedName.toLowerCase()
                    .replace('\u2019', '\'')
                    .replace('\u2018', '\'');
            Integer preWarmId = nameToId.get(normalizedName);
            if (preWarmId == null) preWarmId = nameToId.get(pinnedName.toLowerCase());
            if (preWarmId != null && !iconCache.containsKey(preWarmId))
            {
                final int fid = preWarmId;
                new Thread(() -> {
                    java.awt.image.BufferedImage img = plugin.getItemManager().getImage(fid);
                    if (img != null) iconCache.put(fid, new ImageIcon(img));
                }).start();
            }
            String[] item = buildItemDataFromCache(pinnedName);
            if (item != null) listPanel.add(buildWatchlistItemBlock(item, watchlistIndex++));
        }
        if (pinnedItems.isEmpty())
        {
            JLabel emptyLabel = new JLabel("No items pinned yet");
            emptyLabel.setForeground(TAB_INACTIVE);
            emptyLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
            emptyLabel.setBorder(new EmptyBorder(8, 7, 4, 7));
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(emptyLabel);

            JLabel emptyLabel2 = new JLabel("Search and click + Watch to add");
            emptyLabel2.setForeground(TAB_INACTIVE);
            emptyLabel2.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
            emptyLabel2.setBorder(new EmptyBorder(2, 7, 4, 7));
            emptyLabel2.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(emptyLabel2);
        }

        panel.add(buildTimeFrameBar(), BorderLayout.NORTH);
        panel.add(listPanel, BorderLayout.CENTER);
        javax.swing.SwingUtilities.invokeLater(() -> {
            listPanel.revalidate();
            listPanel.repaint();
        });
        scheduleRepaint(listPanel);
        return panel;
    }

    private JPanel buildWatchlistItemBlock(String[] item, int index)
    {
        String name = item[0];
        String price = item[1];
        String delta = item[5];
        String limit = item[6];
        String gpChange = item.length > 7 ? item[7] : "0";

        boolean isUp = delta.startsWith("+");
        boolean isDown = delta.startsWith("-");

        Color rowBg = (index % 2 == 0) ? BG_DARK : new Color(20, 18, 19);

        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBackground(rowBg);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, 800));

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(rowBg);
        row.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel iconPanel = new JPanel(new java.awt.GridBagLayout());
        iconPanel.setPreferredSize(new Dimension(42, 42));
        iconPanel.setBackground(new Color(14, 12, 13));
        iconPanel.setOpaque(true);
        iconPanel.setBorder(BorderFactory.createLineBorder(isUp ? new Color(0, 100, 0) : isDown ? new Color(100, 0, 0) : new Color(42, 37, 40)));

        Integer itemId = nameToId.get(name.toLowerCase()
                .replace('\u2019', '\'')
                .replace('\u2018', '\''));
        if (itemId == null) itemId = nameToId.get(name.toLowerCase());
        if (itemId != null)
        {
            loadIconAsync(itemId, iconPanel, rowBg);
        }

        final JPanel iconWrapper = new JPanel(new java.awt.GridBagLayout());
        iconWrapper.setBackground(rowBg);
        iconWrapper.add(iconPanel);
        iconWrapper.setToolTipText(name);
        row.add(iconWrapper, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(rowBg);
        info.setBorder(new EmptyBorder(5, 7, 8, 0));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_ITEM_NAME));
        nameLabel.setMaximumSize(new Dimension(190, 20));

        JLabel priceLabel = new JLabel(formatPrice(price) + " gp");
        priceLabel.setForeground(PRICE_GOLD);
        priceLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_PRICE));

        JLabel deltaLabel = new JLabel("(" + delta + "%)");
        deltaLabel.setForeground(isUp ? GREEN_UP : isDown ? RED_DOWN : TEXT_PRIMARY);
        deltaLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
        deltaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel gpChangeLabel = new JLabel(gpChange);
        gpChangeLabel.setForeground(isUp ? GREEN_UP : isDown ? RED_DOWN : TAB_INACTIVE);
        gpChangeLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_DELTA));

        JPanel deltaLimitRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        deltaLimitRow.setBackground(rowBg);
        deltaLimitRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        deltaLimitRow.add(gpChangeLabel);
        deltaLimitRow.add(Box.createHorizontalStrut(4));
        deltaLimitRow.add(deltaLabel);

        info.add(nameLabel);
        info.add(priceLabel);
        info.add(deltaLimitRow);
        row.add(info, BorderLayout.CENTER);
        // ── rearrange arrows (edit mode) ──────────────────────────────
        if (watchlistEditMode) {
            final int totalItems = pinnedItems.size();
            JPanel arrowPanel = new JPanel();
            arrowPanel.setLayout(new BoxLayout(arrowPanel, BoxLayout.Y_AXIS));
            arrowPanel.setBackground(rowBg);
            arrowPanel.setBorder(new EmptyBorder(0, 4, 0, 8));
            arrowPanel.setPreferredSize(new Dimension(24, 68));

            JLabel upArrow = new JLabel(index > 0 ? "▲" : " ");
            upArrow.setForeground(TEXT_DIM);
            upArrow.setFont(new Font("Monospaced", Font.BOLD, FONT_ITEM_NAME));
            upArrow.setAlignmentX(Component.CENTER_ALIGNMENT);
            upArrow.setCursor(index > 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());

            JLabel downArrow = new JLabel(index < totalItems - 1 ? "▼" : " ");
            downArrow.setForeground(TEXT_DIM);
            downArrow.setFont(new Font("Monospaced", Font.BOLD, FONT_ITEM_NAME));
            downArrow.setAlignmentX(Component.CENTER_ALIGNMENT);
            downArrow.setCursor(index < totalItems - 1 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());

            arrowPanel.add(Box.createVerticalGlue());
            arrowPanel.add(upArrow);
            arrowPanel.add(Box.createVerticalStrut(4));
            arrowPanel.add(downArrow);
            arrowPanel.add(Box.createVerticalGlue());

            upArrow.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    e.consume();
                    if (index > 0) {
                        java.util.Collections.swap(pinnedItems, index, index - 1);
                        savePinnedItems();
                        showTab(0);
                    }
                }
                @Override
                public void mouseEntered(MouseEvent e) { if (index > 0) upArrow.setForeground(GOLD); }
                @Override
                public void mouseExited(MouseEvent e) { upArrow.setForeground(TEXT_DIM); }
            });

            downArrow.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    e.consume();
                    if (index < totalItems - 1) {
                        java.util.Collections.swap(pinnedItems, index, index + 1);
                        savePinnedItems();
                        showTab(0);
                    }
                }
                @Override
                public void mouseEntered(MouseEvent e) { if (index < totalItems - 1) downArrow.setForeground(GOLD); }
                @Override
                public void mouseExited(MouseEvent e) { downArrow.setForeground(TEXT_DIM); }
            });

            row.add(arrowPanel, BorderLayout.EAST);
        }
// Inline detail slot
        final JButton[] watchBtnRef = {null};
        JPanel detailSlot = new JPanel(new BorderLayout());
        detailSlot.setBackground(BG_DARK);
        detailSlot.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        detailSlot.setAlignmentX(Component.LEFT_ALIGNMENT);

        javax.swing.JViewport detailViewport = new javax.swing.JViewport();
        detailViewport.setView(detailSlot);
        detailViewport.setVisible(false);
        detailViewport.setBorder(null);
        detailViewport.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if (watchlistEditMode) return;
                if (javax.swing.SwingUtilities.isRightMouseButton(e))
                {
                    javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
                    boolean isWatched = pinnedItems.contains(item[0]);
                    javax.swing.JMenuItem watchItem = new javax.swing.JMenuItem(isWatched ? "- Unwatch" : "+ Watch");
                    watchItem.addActionListener(ev -> {
                        boolean nowWatched = !pinnedItems.contains(item[0]);
                        if (nowWatched) pinnedItems.add(item[0]);
                        else pinnedItems.remove(item[0]);
                        savePinnedItems();
                        if (activeTab == 0) showTab(0);
                        JButton wb = watchBtnRef[0];
                        if (wb != null) {
                            wb.setText(nowWatched ? "✓ Watch" : "+ Watch");
                            wb.setBorder(BorderFactory.createLineBorder(nowWatched ? GOLD : new Color(58, 53, 48)));
                            wb.setForeground(nowWatched ? GOLD : TAB_INACTIVE);
                        }
                        watchItem.setText(nowWatched ? "- Unwatch" : "+ Watch");
                    });
                    popup.add(watchItem);
                    javax.swing.JMenuItem pricesItem = new javax.swing.JMenuItem("Prices ↗");
                    pricesItem.addActionListener(ev -> {
                        try {
                            int trackerItemId = item.length > 12 ? Integer.parseInt(item[12]) : -1;
                            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://prices.runescape.wiki/osrs/item/" + trackerItemId));
                        } catch (Exception ex) { }
                    });
                    popup.add(pricesItem);
                    javax.swing.JMenuItem wikiItem = new javax.swing.JMenuItem("Wiki ↗");
                    wikiItem.addActionListener(ev -> {
                        try {
                            String wikiName = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase().replace(" ", "_");
                            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://oldschool.runescape.wiki/w/" + wikiName));
                        } catch (Exception ex) { }
                    });
                    popup.add(wikiItem);
                    popup.show(row, e.getX(), e.getY());
                    return;
                }
                // Toggle
                if (name.equals(selectedWatchlistItemName) && currentOpenWatchlistDetail != null)
                {
                    final JPanel closingDetail = currentOpenWatchlistDetail;
                    currentOpenWatchlistDetail = null;
                    final javax.swing.JViewport closingVP = (javax.swing.JViewport) closingDetail.getParent();
                    final int fullH = closingVP != null ? closingVP.getHeight() : 0;
                    int[] curH2 = {fullH};
                    javax.swing.Timer closeTimer = new javax.swing.Timer(16, null);
                    closeTimer.addActionListener(ev -> {
                        curH2[0] = Math.max(curH2[0] - 30, 0);
                        if (closingVP != null) {
                            closingVP.setPreferredSize(new Dimension(230, curH2[0]));
                            closingVP.setViewPosition(new java.awt.Point(0, fullH - curH2[0]));
                            closingVP.revalidate();
                        }
                        if (curH2[0] <= 0)
                        {
                            if (closingVP != null) { closingVP.setVisible(false); closingVP.setPreferredSize(null); }
                            if (currentOpenWatchlistRow != null) { currentOpenWatchlistRow.setBackground(currentOpenWatchlistRowColor); }
                            if (currentOpenWatchlistInfo != null) { currentOpenWatchlistInfo.setBackground(currentOpenWatchlistRowColor); currentOpenWatchlistInfo = null; }
                            if (currentOpenWatchlistIconWrapper != null) { currentOpenWatchlistIconWrapper.setBackground(currentOpenWatchlistRowColor); currentOpenWatchlistIconWrapper = null; }
                            if (currentOpenWatchlistDeltaRow != null) { currentOpenWatchlistDeltaRow.setBackground(currentOpenWatchlistRowColor); currentOpenWatchlistDeltaRow = null; }
                            watchlistListPanel.revalidate();
                            watchlistListPanel.repaint();
                            closeTimer.stop();
                        }
                    });
                    closeTimer.start();
                    if (currentOpenWatchlistRow != null)
                    {
                        currentOpenWatchlistRow.setBackground(BG_DARK);
                        currentOpenWatchlistRow.setBorder(javax.swing.BorderFactory.createEmptyBorder());
                        for (Component c : currentOpenWatchlistRow.getComponents())
                        {
                            if (c instanceof JPanel)
                            {
                                if (((JPanel)c).getComponentCount() > 0 && ((JPanel)c).getComponent(0) instanceof JPanel && ((JPanel)c).getComponent(0).getPreferredSize().width == 42)
                                {
                                    c.setBackground(BG_DARK);
                                    continue;
                                }
                                c.setBackground(BG_DARK);
                                for (Component cc : ((JPanel)c).getComponents())
                                    if (cc instanceof JPanel && cc.getPreferredSize().width != 42) cc.setBackground(BG_DARK);
                            }
                        }
                        currentOpenWatchlistRow = null;
                    }
                    selectedWatchlistItemName = null;
                    watchlistReopenAction = null;
                    graphWasOpen = false;
                    liveHeaderPriceLabel = null;
                    liveBuyPriceValueLabel = null;
                    liveBuyPriceHeaderLabel = null;
                    liveSellPriceValueLabel = null;
                    liveSellPriceHeaderLabel = null;
                    liveFloatVolumeLabel = null;
                    liveFloatMarginLabel = null;
                    liveFloatProfitLabel = null;
                    liveFloatRoiLabel = null;
                    liveStatGrid = null;
                    liveStatsLabels = null;
                    liveOpenItemId = -1;
                    liveOpenItemData = null;
                    liveGraphPanel = null;
                    return;
                }

                // Close previous
                if (currentOpenWatchlistDetail != null)
                {
                    final JPanel closingDetail2 = currentOpenWatchlistDetail;
                    currentOpenWatchlistDetail = null;
                    final javax.swing.JViewport closingVP2 = (javax.swing.JViewport) closingDetail2.getParent();
                    final int fullH2 = closingVP2 != null ? closingVP2.getHeight() : 0;
                    int[] curH3 = {fullH2};
                    javax.swing.Timer closeTimer2 = new javax.swing.Timer(16, null);
                    closeTimer2.addActionListener(ev -> {
                        curH3[0] = Math.max(curH3[0] - 30, 0);
                        if (closingVP2 != null) {
                            closingVP2.setPreferredSize(new Dimension(230, curH3[0]));
                            closingVP2.setViewPosition(new java.awt.Point(0, fullH2 - curH3[0]));
                            closingVP2.revalidate();
                        }
                        if (curH3[0] <= 0)
                        {
                            if (closingVP2 != null) { closingVP2.setVisible(false); closingVP2.setPreferredSize(null); }
                            if (currentOpenWatchlistRow != null) { currentOpenWatchlistRow.setBackground(currentOpenWatchlistRowColor); }
                            if (currentOpenWatchlistInfo != null) { currentOpenWatchlistInfo.setBackground(currentOpenWatchlistRowColor); currentOpenWatchlistInfo = null; }
                            if (currentOpenWatchlistIconWrapper != null) { currentOpenWatchlistIconWrapper.setBackground(currentOpenWatchlistRowColor); currentOpenWatchlistIconWrapper = null; }
                            if (currentOpenWatchlistDeltaRow != null) { currentOpenWatchlistDeltaRow.setBackground(currentOpenWatchlistRowColor); currentOpenWatchlistDeltaRow = null; }
                            watchlistListPanel.revalidate();
                            watchlistListPanel.repaint();
                            closeTimer2.stop();
                        }
                    });
                    closeTimer2.start();
                    graphWasOpen = false;
                }
                if (currentOpenWatchlistRow != null)
                {
                    currentOpenWatchlistRow.setBackground(currentOpenWatchlistRowColor);
                    currentOpenWatchlistRow.setBorder(javax.swing.BorderFactory.createEmptyBorder());
                    for (Component c : currentOpenWatchlistRow.getComponents())
                    {
                        if (c instanceof JPanel)
                        {
                            if (((JPanel)c).getComponentCount() > 0 && ((JPanel)c).getComponent(0) instanceof JPanel && ((JPanel)c).getComponent(0).getPreferredSize().width == 42)
                            {
                                c.setBackground(currentOpenWatchlistRowColor);
                                continue;
                            }
                            c.setBackground(currentOpenWatchlistRowColor);
                        }
                    }
                }

                // Open this one
                selectedWatchlistItemName = name;
                currentOpenWatchlistRow = row;
                currentOpenWatchlistRowColor = rowBg;
                currentOpenWatchlistDetail = detailSlot;
                currentOpenWatchlistInfo = info;
                currentOpenWatchlistIconWrapper = iconWrapper;
                currentOpenWatchlistDeltaRow = deltaLimitRow;

                detailSlot.removeAll();
                detailSlot.add(buildInlineDetail(item, true), BorderLayout.CENTER);
                watchBtnRef[0] = findWatchButton(detailSlot);
                detailSlot.revalidate();
                javax.swing.SwingUtilities.invokeLater(() -> {
                    int fullHeight = detailSlot.getPreferredSize().height;
                    if (fullHeight <= 0) fullHeight = 150;
                    final int targetFinal = fullHeight;
                    detailViewport.setPreferredSize(new Dimension(230, 0));
                    detailViewport.setViewPosition(new java.awt.Point(0, targetFinal));
                    detailViewport.setVisible(true);
                    detailViewport.revalidate();
                    int[] curH = {0};
                    javax.swing.Timer openTimer = new javax.swing.Timer(16, null);
                    openTimer.addActionListener(ev -> {
                        curH[0] = Math.min(curH[0] + 30, targetFinal);
                        detailViewport.setPreferredSize(new Dimension(230, curH[0]));
                        detailViewport.setViewPosition(new java.awt.Point(0, targetFinal - curH[0]));
                        detailViewport.revalidate();
                        if (curH[0] >= targetFinal)
                        {
                            detailViewport.setPreferredSize(null);
                            detailViewport.setViewPosition(new java.awt.Point(0, 0));
                            openTimer.stop();
                        }
                    });
                    openTimer.start();
                });
                scheduleRepaint(detailSlot);
                final String[] itemData = item;
                watchlistReopenAction = () -> {
                    for (java.awt.Component c : watchlistListPanel.getComponents())
                    {
                        if (!(c instanceof JPanel)) continue;
                        JPanel block = (JPanel) c;
                        if (block.getComponentCount() < 2) continue;
                        java.awt.Component second = block.getComponent(1);
                        if (!(second instanceof javax.swing.JViewport)) continue;
                        javax.swing.JViewport newViewport = (javax.swing.JViewport) second;
                        if (!(newViewport.getView() instanceof JPanel)) continue;
                        JPanel newDetailSlot = (JPanel) newViewport.getView();
                        java.awt.Component first = block.getComponent(0);
                        if (!(first instanceof JPanel)) continue;
                        JPanel newRow = (JPanel) first;
                        for (java.awt.Component child : newRow.getComponents())
                        {
                            if (!(child instanceof JPanel)) continue;
                            for (java.awt.Component rowChild : ((JPanel)child).getComponents())
                            {
                                if (!(rowChild instanceof JLabel)) continue;
                                JLabel label = (JLabel) rowChild;
                                if (itemData[0].equalsIgnoreCase(label.getText()))
                                {
                                    newDetailSlot.removeAll();
                                    newDetailSlot.add(buildInlineDetail(itemData, true), BorderLayout.CENTER);
                                    newViewport.setVisible(true);
                                    scheduleRepaint(newDetailSlot);
                                    if (graphWasOpen) reopenGraph(newDetailSlot);
                                    currentOpenWatchlistDetail = newDetailSlot;
                                    selectedWatchlistItemName = itemData[0];
                                    newRow.setBorder(new MatteBorder(1, 3, 0, 0, GOLD));
                                    currentOpenWatchlistRow = newRow;
                                    return;
                                }
                            }
                        }
                    }
                };

                row.setBackground(BG_ROW_SELECTED);
                info.setBackground(BG_ROW_SELECTED);
                iconWrapper.setBackground(BG_ROW_SELECTED);
                deltaLimitRow.setBackground(BG_ROW_SELECTED);
                row.setBorder(BorderFactory.createEmptyBorder());
                row.setBorder(new MatteBorder(1, 3, 0, 0, GOLD));
                tabContentPanel.revalidate();
                tabContentPanel.repaint();
            }
            public void mouseEntered(MouseEvent e)
            {
                if (!name.equals(selectedWatchlistItemName))
                {
                    row.setBackground(BG_ROW_HOVER);
                    info.setBackground(BG_ROW_HOVER);
                    iconWrapper.setBackground(BG_ROW_HOVER);
                    deltaLimitRow.setBackground(BG_ROW_HOVER);
                    if (watchlistEditMode) row.getComponent(row.getComponentCount()-1).setBackground(BG_ROW_HOVER);
                }
            }
            public void mouseExited(MouseEvent e)
            {
                if (!name.equals(selectedWatchlistItemName))
                {
                    row.setBackground(rowBg);
                    info.setBackground(rowBg);
                    iconWrapper.setBackground(rowBg);
                    deltaLimitRow.setBackground(rowBg);
                    if (watchlistEditMode) row.getComponent(row.getComponentCount()-1).setBackground(rowBg);
                }
            }
        });

        block.add(row);
        block.add(detailViewport);
        return block;
    }

    // ── BANK TAB ──
    private JPanel buildBankTab()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);

        JPanel hero = new JPanel();
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setBackground(BG_DARK);
        hero.setBorder(new EmptyBorder(6, 6, 4, 6));

// OSRS-style bordered wealth section
        JPanel borderedSection = new JPanel(new GridBagLayout());
        borderedSection.setBackground(BG_DARK);
        borderedSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(58, 53, 48), 1),
                BorderFactory.createLineBorder(new Color(15, 13, 12), 3)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new java.awt.Insets(0, 0, 0, 0);

        // Row 1: Timeframe buttons row 1
        JPanel wealthTfRow1 = new JPanel(new GridLayout(1, 5, 3, 0));
        wealthTfRow1.setBackground(BG_DARK);
        wealthTfRow1.setBorder(new EmptyBorder(6, 6, 2, 6));
        String[] wealthFrames1 = {"1H", "6H", "24H", "7D", "30D"};
        for (String frame : wealthFrames1)
        {
            JButton btn = new JButton(frame);
            btn.setFont(new Font("Monospaced", Font.PLAIN, FONT_TIMEFRAME));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBackground(frame.equals(bankWealthTimeFrame) ? new Color(26, 21, 0) : new Color(20, 16, 10));
            btn.setForeground(frame.equals(bankWealthTimeFrame) ? GOLD : TAB_INACTIVE);
            btn.setBorder(BorderFactory.createLineBorder(frame.equals(bankWealthTimeFrame) ? GOLD : new Color(58, 53, 48)));
            btn.addActionListener(e -> {
                bankWealthTimeFrame = frame;
                isRefreshing = true;
                showTab(activeTab);
                isRefreshing = false;
            });
            wealthTfRow1.add(btn);
        }
        gbc.gridy = 0;
        borderedSection.add(wealthTfRow1, gbc);

        // Row 2: Timeframe buttons row 2
        JPanel wealthTfRow2 = new JPanel(new GridLayout(1, 3, 3, 0));
        wealthTfRow2.setBackground(BG_DARK);
        wealthTfRow2.setBorder(new EmptyBorder(0, 6, 4, 6));
        String[] wealthFrames2 = {"3M", "1Y", "All"};
        for (String frame : wealthFrames2)
        {
            JButton btn = new JButton(frame);
            btn.setFont(new Font("Monospaced", Font.PLAIN, FONT_TIMEFRAME));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBackground(frame.equals(bankWealthTimeFrame) ? new Color(26, 21, 0) : new Color(20, 16, 10));
            btn.setForeground(frame.equals(bankWealthTimeFrame) ? GOLD : TAB_INACTIVE);
            btn.setBorder(BorderFactory.createLineBorder(frame.equals(bankWealthTimeFrame) ? GOLD : new Color(58, 53, 48)));
            btn.addActionListener(e -> {
                bankWealthTimeFrame = frame;
                isRefreshing = true;
                showTab(activeTab);
                isRefreshing = false;
            });
            wealthTfRow2.add(btn);
        }
        gbc.gridy = 1;
        borderedSection.add(wealthTfRow2, gbc);

        // Row 3: Separator
        JSeparator wealthSep = new JSeparator();
        wealthSep.setForeground(new Color(65, 55, 38));
        wealthSep.setBackground(new Color(65, 55, 38));
        gbc.gridy = 2;
        gbc.insets = new java.awt.Insets(0, 6, 0, 6);
        borderedSection.add(wealthSep, gbc);
        gbc.insets = new java.awt.Insets(0, 0, 0, 0);

        // Row 4: "TOTAL BANK VALUE" label
        JLabel heroLabel = new JLabel("TOTAL BANK VALUE", SwingConstants.CENTER);
        heroLabel.setForeground(TEXT_DIM);
        heroLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
        gbc.gridy = 3;
        gbc.insets = new java.awt.Insets(8, 6, 2, 6);
        borderedSection.add(heroLabel, gbc);
        gbc.insets = new java.awt.Insets(0, 0, 0, 0);

        // Calculate last updated time
        long lastScanTime = 0;
        for (long[] entry : bankValueLog)
        {
            if (entry[0] > lastScanTime) lastScanTime = entry[0];
        }
        long nowSeconds = System.currentTimeMillis() / 1000;
        long secondsAgo = nowSeconds - lastScanTime;
        String lastUpdatedStr;
        if (lastScanTime == 0)
            lastUpdatedStr = "Bank not yet scanned";
        else if (secondsAgo < 60)
            lastUpdatedStr = "Last updated just now";
        else if (secondsAgo < 3600)
            lastUpdatedStr = "Last updated " + (secondsAgo / 60) + "m ago";
        else
            lastUpdatedStr = "Last updated " + (secondsAgo / 3600) + "h ago";

        // Row 5: Bank value
        String bankValueStr = totalBankValue == 0 ? "No bank data" : formatFullPrice(String.valueOf(totalBankValue)) + " gp";
        boolean bankHidden = plugin.isBankValueHidden();
        String heroText = bankHidden ? "Click to reveal" : bankValueStr;
        JLabel heroValue = new JLabel(heroText, SwingConstants.CENTER);
        heroValue.setForeground(bankHidden ? TEXT_DIM : PRICE_GOLD);
        int heroFontSize = 20;
        int availableWidth = 190;
        java.awt.FontMetrics fm;
        do {
            heroValue.setFont(new Font("Monospaced", Font.BOLD, heroFontSize));
            fm = heroValue.getFontMetrics(heroValue.getFont());
            if (fm.stringWidth(heroText) <= availableWidth) break;
            heroFontSize--;
        } while (heroFontSize > 10);
        heroValue.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        heroValue.setToolTipText(bankHidden ? "Click to reveal bank value" : "Click to hide bank value");
        heroValue.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                plugin.setBankValueHidden(!plugin.isBankValueHidden());
                showTab(activeTab);
            }
            public void mouseEntered(MouseEvent e) { heroValue.setForeground(TEXT_PRIMARY); }
            public void mouseExited(MouseEvent e) { heroValue.setForeground(bankHidden ? TEXT_DIM : PRICE_GOLD); }
        });
        gbc.gridy = 4;
        gbc.insets = new java.awt.Insets(0, 6, 2, 6);
        borderedSection.add(heroValue, gbc);
        gbc.insets = new java.awt.Insets(0, 0, 0, 0);

        // Calculate change data
        long targetSeconds = nowSeconds;
        if (bankWealthTimeFrame.equals("1H")) targetSeconds = nowSeconds - 3600;
        else if (bankWealthTimeFrame.equals("6H")) targetSeconds = nowSeconds - 21600;
        else if (bankWealthTimeFrame.equals("24H")) targetSeconds = nowSeconds - 86400;
        else if (bankWealthTimeFrame.equals("7D")) targetSeconds = nowSeconds - 7 * 86400L;
        else if (bankWealthTimeFrame.equals("30D")) targetSeconds = nowSeconds - 30 * 86400L;
        else if (bankWealthTimeFrame.equals("3M")) targetSeconds = nowSeconds - 90 * 86400L;
        else if (bankWealthTimeFrame.equals("1Y")) targetSeconds = nowSeconds - 365 * 86400L;

        long tolerance = 1800;
        if (bankWealthTimeFrame.equals("6H")) tolerance = 7200;
        else if (bankWealthTimeFrame.equals("24H")) tolerance = 14400;
        else if (bankWealthTimeFrame.equals("7D")) tolerance = 86400;
        else if (bankWealthTimeFrame.equals("30D")) tolerance = 86400 * 3L;
        else if (bankWealthTimeFrame.equals("3M")) tolerance = 86400 * 7L;
        else if (bankWealthTimeFrame.equals("1Y")) tolerance = 86400 * 14L;
        else if (bankWealthTimeFrame.equals("All")) tolerance = Long.MAX_VALUE;

        long[] closestEntry = null;
        long closestDiff = Long.MAX_VALUE;
        for (long[] entry : bankValueLog)
        {
            if (entry.length < 3) continue;
            long diff = Math.abs(entry[0] - targetSeconds);
            if (diff < closestDiff && diff <= tolerance)
            {
                closestDiff = diff;
                closestEntry = entry;
            }
        }
        if (bankWealthTimeFrame.equals("All"))
        {
            closestEntry = null;
            for (long[] entry : bankValueLog)
            {
                if (entry.length < 3) continue;
                if (closestEntry == null || entry[0] < closestEntry[0])
                    closestEntry = entry;
            }
        }

        String bankChangeStr;
        Color bankChangeColor;
        String noDataMessage;
        if (bankWealthTimeFrame.equals("1H")) noDataMessage = "Open bank again in ~1H";
        else if (bankWealthTimeFrame.equals("6H")) noDataMessage = "Open bank again in ~6H";
        else if (bankWealthTimeFrame.equals("24H")) noDataMessage = "Open bank again in ~24H";
        else if (bankWealthTimeFrame.equals("7D")) noDataMessage = "Open bank again in ~7D";
        else if (bankWealthTimeFrame.equals("30D")) noDataMessage = "Open bank again in ~30D";
        else if (bankWealthTimeFrame.equals("3M")) noDataMessage = "Open bank again in ~3M";
        else if (bankWealthTimeFrame.equals("1Y")) noDataMessage = "Open bank again in ~1Y";
        else noDataMessage = "No bank history yet";

        if (closestEntry == null || bankItems.isEmpty())
        {
            bankChangeStr = noDataMessage;
            bankChangeColor = TEXT_DIM;
        }
        else
        {
            long historicalWealth = closestEntry[2];
            long currentWealth = bankValueLog.isEmpty() ? totalBankValue :
                    bankValueLog.get(bankValueLog.size() - 1)[2];
            long bankGpChange = currentWealth - historicalWealth;
            double bankPctChange = historicalWealth > 0 ?
                    ((double) bankGpChange / historicalWealth) * 100.0 : 0;
            String gpStr = bankGpChange >= 0 ?
                    "+" + formatPrice(String.valueOf(Math.abs(bankGpChange))) + " gp" :
                    "-" + formatPrice(String.valueOf(Math.abs(bankGpChange))) + " gp";
            String pctStr = String.format("%+.2f%%", bankPctChange);
            bankChangeStr = gpStr + "  (" + pctStr + ")";
            bankChangeColor = bankGpChange > 0 ? GREEN_UP :
                    bankGpChange < 0 ? RED_DOWN : TEXT_DIM;
        }

        // Row 6: Change label (always present, empty when no data)
        boolean showChangeData = config.showBankValueChange() && !bankHidden
                && closestEntry != null && !bankItems.isEmpty();
        String changeDisplayStr = showChangeData ? bankChangeStr : " ";
        JLabel bankChangeLabel = new JLabel(changeDisplayStr, SwingConstants.CENTER);
        bankChangeLabel.setForeground(showChangeData ? bankChangeColor : TEXT_DIM);
        bankChangeLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
        gbc.gridy = 5;
        gbc.insets = new java.awt.Insets(0, 6, 0, 6);
        if (!showChangeData)
        {
            gbc.ipady = 0;
            bankChangeLabel.setPreferredSize(new Dimension(0, 0));
        }
        borderedSection.add(bankChangeLabel, gbc);
        gbc.ipady = 0;
        bankChangeLabel.setPreferredSize(null);

        // Row 7: Context label (always present)
// Shorten lastUpdatedStr for context label
        String shortUpdatedStr;
        if (lastScanTime == 0)
            shortUpdatedStr = "not yet scanned";
        else if (secondsAgo < 60)
            shortUpdatedStr = "just now";
        else if (secondsAgo < 3600)
            shortUpdatedStr = (secondsAgo / 60) + "min ago";
        else
            shortUpdatedStr = (secondsAgo / 3600) + "h ago";
        String contextStr = config.showBankValueChange() && !bankHidden ?
                "· " + bankWealthTimeFrame + " · Last updated " + shortUpdatedStr : "Last updated " + shortUpdatedStr;
        JLabel contextLabel = new JLabel(contextStr, SwingConstants.CENTER);
        contextLabel.setForeground(TEXT_DIM);
        contextLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
        gbc.gridy = 6;
        gbc.insets = new java.awt.Insets(0, 6, 4, 6);
        borderedSection.add(contextLabel, gbc);

// Row 8: Clickable toggle separator
        JPanel toggleSepRow = new JPanel(new BorderLayout());
        toggleSepRow.setBackground(BG_DARK);
        JSeparator compSep = new JSeparator();
        compSep.setForeground(new Color(65, 55, 38));
        compSep.setBackground(new Color(65, 55, 38));
        JLabel toggleArrow = new JLabel(bankMetadataExpanded ? " ▲" : " ▼");
        toggleArrow.setForeground(new Color(65, 55, 38));
        toggleArrow.setFont(new Font("SansSerif", Font.PLAIN, 9));
        toggleSepRow.add(compSep, BorderLayout.CENTER);
        toggleSepRow.add(toggleArrow, BorderLayout.EAST);
        toggleSepRow.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        toggleSepRow.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (bankMetaTimer != null && bankMetaTimer.isRunning()) return;
                if (!bankMetadataExpanded) {
                    // Expand animation
                    bankMetadataExpanded = true;
                    toggleArrow.setText(" ▲");
                    if (bankMetaViewport != null) {
                        if (bankMetaBottomSep != null) bankMetaBottomSep.setVisible(true);
                        int targetH = bankCompPanel.getPreferredSize().height;
                        int[] curH = {0};
                        bankMetaViewport.setPreferredSize(new java.awt.Dimension(1, 0));
                        bankMetaViewport.setViewPosition(new java.awt.Point(0, targetH));
                        bankMetaViewport.revalidate();
                        bankMetaTimer = new javax.swing.Timer(16, null);
                        bankMetaTimer.addActionListener(ev -> {
                            curH[0] = Math.min(curH[0] + 12, targetH);
                            bankMetaViewport.setPreferredSize(new java.awt.Dimension(1, curH[0]));
                            bankMetaViewport.setViewPosition(new java.awt.Point(0, targetH - curH[0]));
                            bankMetaViewport.revalidate();
                            if (curH[0] >= targetH) {
                                bankMetaTimer.stop();
                                isRefreshing = true;
                                showTab(activeTab);
                                isRefreshing = false;
                            }
                        });
                        bankMetaTimer.start();
                    }
                } else {
                    // Collapse animation
                    bankMetadataExpanded = false;
                    toggleArrow.setText(" ▼");
                    if (bankMetaViewport != null) {
                        if (bankMetaBottomSep != null) bankMetaBottomSep.setVisible(false);
                        int targetH = bankCompPanel.getPreferredSize().height;
                        int[] curH = {targetH};
                        bankMetaViewport.setPreferredSize(new java.awt.Dimension(1, targetH));
                        bankMetaViewport.setViewPosition(new java.awt.Point(0, 0));
                        bankMetaViewport.revalidate();
                        bankMetaTimer = new javax.swing.Timer(16, null);
                        bankMetaTimer.addActionListener(ev -> {
                            curH[0] = Math.max(curH[0] - 12, 0);
                            bankMetaViewport.setPreferredSize(new java.awt.Dimension(1, curH[0]));
                            bankMetaViewport.setViewPosition(new java.awt.Point(0, targetH - curH[0]));
                            bankMetaViewport.revalidate();
                            if (curH[0] <= 0) {
                                bankMetaTimer.stop();
                                isRefreshing = true;
                                showTab(activeTab);
                                isRefreshing = false;
                            }
                        });
                        bankMetaTimer.start();
                    }
                }
            }
        });
        gbc.gridy = 8;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(4, 6, 4, 6);
        borderedSection.add(toggleSepRow, gbc);

        // Row 9: Comparison data (always present, dim when no data)
        JPanel compPanel = new JPanel(new GridBagLayout());
        compPanel.setBackground(BG_DARK);
        compPanel.setPreferredSize(new Dimension(218, 52));
        GridBagConstraints cgbc = new GridBagConstraints();
        cgbc.gridx = 0;
        cgbc.fill = GridBagConstraints.HORIZONTAL;
        cgbc.weightx = 1.0;

        if (showChangeData && closestEntry != null)
        {
            long entryTs = closestEntry[0];
            long entryWealth = closestEntry[2];
            long nowSec = System.currentTimeMillis() / 1000;
            long diffSec = nowSec - entryTs;
            long diffH = diffSec / 3600;
            long diffM = (diffSec % 3600) / 60;
            String timeAgoStr;
            if (diffH >= 24) {
                long diffD = diffH / 24;
                long remH = diffH % 24;
                timeAgoStr = remH > 0 ? diffD + "d " + remH + "h ago" : diffD + "d ago";
            } else if (diffH > 0) {
                timeAgoStr = diffH + "h " + diffM + "min ago";
            } else {
                timeAgoStr = diffM + "min ago";
            }
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d 'at' h:mm a");
            String dateStr = sdf.format(new java.util.Date(entryTs * 1000));
            String wealthStr = formatFullPrice(String.valueOf(entryWealth)) + " gp";

            JLabel compLine1 = new JLabel("Compared:  " + timeAgoStr, SwingConstants.LEFT);
            compLine1.setForeground(TEXT_DIM);
            compLine1.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
            cgbc.gridy = 0;
            cgbc.insets = new java.awt.Insets(0, 6, 1, 6);
            compPanel.add(compLine1, cgbc);

            JLabel compLine2 = new JLabel("Date:      " + dateStr, SwingConstants.LEFT);
            compLine2.setForeground(TEXT_DIM);
            compLine2.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
            cgbc.gridy = 1;
            compPanel.add(compLine2, cgbc);

            JLabel compLine3 = new JLabel("Wealth:    " + wealthStr, SwingConstants.LEFT);
            compLine3.setForeground(TEXT_DIM);
            compLine3.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
            cgbc.gridy = 2;
            cgbc.insets = new java.awt.Insets(0, 6, 4, 6);
            compPanel.add(compLine3, cgbc);
        }
        else
        {
            JLabel noDataLabel = new JLabel("No comparison data yet", SwingConstants.CENTER);
            noDataLabel.setForeground(TEXT_DIM);
            noDataLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
            cgbc.gridy = 0;
            cgbc.insets = new java.awt.Insets(4, 6, 4, 6);
            compPanel.add(noDataLabel, cgbc);
        }

        gbc.gridy = 9;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(0, 0, 0, 0);
        javax.swing.JViewport metaVp = new javax.swing.JViewport();
        metaVp.setView(compPanel);
        int compH = compPanel.getPreferredSize().height;
        if (bankMetadataExpanded) {
            metaVp.setPreferredSize(new java.awt.Dimension(1, compH));
            metaVp.setViewPosition(new java.awt.Point(0, 0));
        } else {
            metaVp.setPreferredSize(new java.awt.Dimension(1, 0));
            metaVp.setViewPosition(new java.awt.Point(0, compH));
        }
        borderedSection.add(metaVp, gbc);
        bankCompPanel = compPanel;
        bankMetaViewport = metaVp;

        JSeparator metaBottomSep = new JSeparator();
        metaBottomSep.setForeground(new Color(65, 55, 38));
        metaBottomSep.setBackground(new Color(65, 55, 38));
        gbc.gridy = 10;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(4, 6, 4, 6);
        borderedSection.add(metaBottomSep, gbc);
        metaBottomSep.setVisible(bankMetadataExpanded);
        bankMetaBottomSep = metaBottomSep;

        hero.add(borderedSection);
        hero.add(Box.createVerticalStrut(4));
        hero.add(buildTimeFrameBar());
        panel.add(hero, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        bankListPanel = listPanel;
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(new Color(20, 18, 19));
        listPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder());

        if (!bankItems.isEmpty())
        {
            JPanel gainersHeader = new JPanel(new BorderLayout());
            gainersHeader.setBackground(new Color(28, 28, 28));
            gainersHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            gainersHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            JLabel gainersLabel = new JLabel("▲ Top Gainers (by " + config.sortMode().getLabel() + ")");
            gainersLabel.setForeground(GREEN_UP);
            gainersLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
            gainersLabel.setToolTipText("Based on items in your bank at last scan");
            gainersHeader.add(gainersLabel, BorderLayout.WEST);
            JLabel gCountLabel = new JLabel(String.valueOf(config.gainersCount()));
            gCountLabel.setForeground(TEXT_DIM);
            gCountLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
            JLabel gMinusBtn = new JLabel("[-]");
            gMinusBtn.setForeground(TEXT_DIM);
            gMinusBtn.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
            gMinusBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            gMinusBtn.addMouseListener(new MouseAdapter()
            {
                public void mouseEntered(MouseEvent e) { gMinusBtn.setForeground(TEXT_PRIMARY); }
                public void mouseExited(MouseEvent e) { gMinusBtn.setForeground(TEXT_DIM); }
                public void mouseClicked(MouseEvent e)
                {
                    if (config.gainersCount() > 1)
                        {
                            int newCount = config.gainersCount() - 1;
                            plugin.saveConfig("gainersCount", String.valueOf(newCount));
                            gCountLabel.setText(String.valueOf(newCount));
                            showTab(activeTab);
                        }
                }
            });
            JLabel gPlusBtn = new JLabel("[+]");
            gPlusBtn.setForeground(TEXT_DIM);
            gPlusBtn.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
            gPlusBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            gPlusBtn.addMouseListener(new MouseAdapter()
            {
                public void mouseEntered(MouseEvent e) { gPlusBtn.setForeground(TEXT_PRIMARY); }
                public void mouseExited(MouseEvent e) { gPlusBtn.setForeground(TEXT_DIM); }
                public void mouseClicked(MouseEvent e)
                {
                    if (config.gainersCount() < 10)
                    {
                        int newCount = config.gainersCount() + 1;
                        plugin.saveConfig("gainersCount", String.valueOf(newCount));
                        gCountLabel.setText(String.valueOf(newCount));
                        showTab(activeTab);
                    }
                }
            });
            JPanel gBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            gBtnPanel.setBackground(new Color(28, 28, 28));
            gBtnPanel.setVisible(false);
            gBtnPanel.add(gMinusBtn);
            gBtnPanel.add(gCountLabel);
            gBtnPanel.add(gPlusBtn);
            gainersHeader.add(gBtnPanel, BorderLayout.EAST);
            MouseAdapter gHoverListener = new MouseAdapter()
            {
                public void mouseEntered(MouseEvent e) { gBtnPanel.setVisible(true); }
                public void mouseExited(MouseEvent e) { gBtnPanel.setVisible(false); }
            };
            gainersHeader.addMouseListener(gHoverListener);
            gBtnPanel.addMouseListener(gHoverListener);
            gMinusBtn.addMouseListener(gHoverListener);
            gCountLabel.addMouseListener(gHoverListener);
            gPlusBtn.addMouseListener(gHoverListener);
            listPanel.add(gainersHeader);
            JPanel gainersAccent = new JPanel();
            gainersAccent.setBackground(new Color(47, 95, 47));
            gainersAccent.setPreferredSize(new Dimension(0, 2));
            gainersAccent.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
            gainersAccent.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(gainersAccent);
        }
        if (bankItems.isEmpty())
        {
            JLabel emptyLabel = new JLabel("Open your bank to scan items");
            emptyLabel.setForeground(TEXT_DIM);
            emptyLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
            emptyLabel.setBorder(new EmptyBorder(12, 7, 4, 7));
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(emptyLabel);

            JLabel emptyLabel2 = new JLabel("Prices will update automatically");
            emptyLabel2.setForeground(TEXT_DIM);
            emptyLabel2.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
            emptyLabel2.setBorder(new EmptyBorder(2, 7, 4, 7));
            emptyLabel2.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(emptyLabel2);
        }
        else
        {
            // Sort by delta for gainers/losers
            java.util.List<String[]> allBankItems = new java.util.ArrayList<>();
            java.util.Set<String> seenNames = new java.util.HashSet<>();
            for (String name : bankItems)
            {
                if (seenNames.contains(name)) continue;
                seenNames.add(name);
                String[] item = buildItemDataFromCache(name);
                if (item != null) allBankItems.add(item);
            }

// Filter by minimum value threshold
            int minStackValue = config.minBankItemValue();
            allBankItems.removeIf(bankItem -> {
                try {
                    long price = Long.parseLong(bankItem[1]);
                    int qty = bankQuantities.getOrDefault(bankItem[0], 0);
                    long stackValue = price * qty;
                    return price < 50000 || stackValue < minStackValue;
                } catch (NumberFormatException e) { return true; }
            });

            // Split into gainers and losers
            java.util.List<String[]> gainers = new java.util.ArrayList<>();
            java.util.List<String[]> losers = new java.util.ArrayList<>();
            for (String[] bankItem : allBankItems)
            {
                double delta = Double.parseDouble(bankItem[5].replace("+", ""));
                if (delta > 0) gainers.add(bankItem);
                else if (delta < 0) losers.add(bankItem);
            }

            // Sort gainers and losers
            if (config.sortMode() == SortMode.GP_CHANGE)
            {
                gainers.sort((a, b) -> Long.compare(parseGpChange(b.length > 7 ? b[7] : "0 gp"), parseGpChange(a.length > 7 ? a[7] : "0 gp")));
                losers.sort((a, b) -> Long.compare(parseGpChange(a.length > 7 ? a[7] : "0 gp"), parseGpChange(b.length > 7 ? b[7] : "0 gp")));
            }
            else
            {
                gainers.sort((a, b) -> Double.compare(Double.parseDouble(b[5].replace("+", "")), Double.parseDouble(a[5].replace("+", ""))));
                losers.sort((a, b) -> Double.compare(Double.parseDouble(a[5].replace("+", "")), Double.parseDouble(b[5].replace("+", ""))));
            }

            // Top Gainers
            int gainersCount = Math.min(config.gainersCount(), gainers.size());
            for (int i = 0; i < gainersCount; i++)
                listPanel.add(buildBankItemBlock(gainers.get(i), true, i));

            // Top Losers
            JPanel losersHeader = new JPanel(new BorderLayout());
            losersHeader.setBackground(new Color(28, 28, 28));
            losersHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            losersHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            JLabel losersLabel = new JLabel("▼ Top Losers (by " + config.sortMode().getLabel() + ")");
            losersLabel.setForeground(RED_DOWN);
            losersLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
            losersLabel.setToolTipText("Based on items in your bank at last scan");
            losersHeader.add(losersLabel, BorderLayout.WEST);
            JLabel lCountLabel = new JLabel(String.valueOf(config.losersCount()));
            lCountLabel.setForeground(TEXT_DIM);
            lCountLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
            JLabel lMinusBtn = new JLabel("[-]");
            lMinusBtn.setForeground(TEXT_DIM);
            lMinusBtn.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
            lMinusBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lMinusBtn.addMouseListener(new MouseAdapter()
            {
                public void mouseEntered(MouseEvent e) { lMinusBtn.setForeground(TEXT_PRIMARY); }
                public void mouseExited(MouseEvent e) { lMinusBtn.setForeground(TEXT_DIM); }
                public void mouseClicked(MouseEvent e)
                {
                    if (config.losersCount() > 1)
                        {
                            int newCount = config.losersCount() - 1;
                            plugin.saveConfig("losersCount", String.valueOf(newCount));
                            lCountLabel.setText(String.valueOf(newCount));
                            showTab(activeTab);
                        }
                }
            });
            JLabel lPlusBtn = new JLabel("[+]");
            lPlusBtn.setForeground(TEXT_DIM);
            lPlusBtn.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
            lPlusBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lPlusBtn.addMouseListener(new MouseAdapter()
            {
                public void mouseEntered(MouseEvent e) { lPlusBtn.setForeground(TEXT_PRIMARY); }
                public void mouseExited(MouseEvent e) { lPlusBtn.setForeground(TEXT_DIM); }
                public void mouseClicked(MouseEvent e)
                {
                    if (config.losersCount() < 10)
                    {
                        int newCount = config.losersCount() + 1;
                        plugin.saveConfig("losersCount", String.valueOf(newCount));
                        lCountLabel.setText(String.valueOf(newCount));
                        showTab(activeTab);
                    }
                }
            });
            JPanel lBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            lBtnPanel.setBackground(new Color(28, 28, 28));
            lBtnPanel.setVisible(false);
            lBtnPanel.add(lMinusBtn);
            lBtnPanel.add(lCountLabel);
            lBtnPanel.add(lPlusBtn);
            losersHeader.add(lBtnPanel, BorderLayout.EAST);
            MouseAdapter lHoverListener = new MouseAdapter()
            {
                public void mouseEntered(MouseEvent e) { lBtnPanel.setVisible(true); }
                public void mouseExited(MouseEvent e) { lBtnPanel.setVisible(false); }
            };
            losersHeader.addMouseListener(lHoverListener);
            lBtnPanel.addMouseListener(lHoverListener);
            lMinusBtn.addMouseListener(lHoverListener);
            lCountLabel.addMouseListener(lHoverListener);
            lPlusBtn.addMouseListener(lHoverListener);
            listPanel.add(losersHeader);
            JPanel losersAccent = new JPanel();
            losersAccent.setBackground(new Color(95, 47, 47));
            losersAccent.setPreferredSize(new Dimension(0, 2));
            losersAccent.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
            losersAccent.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(losersAccent);

            int losersCount = Math.min(config.losersCount(), losers.size());
            for (int i = 0; i < losersCount; i++)
                listPanel.add(buildBankItemBlock(losers.get(i), true, i));

// All Items — collapsible
            JLabel allLabel = new JLabel((bankAllItemsCollapsed ? "≡ All Bank Items ▶" : "≡ All Bank Items ▼"));
            allLabel.setForeground(TAB_INACTIVE);
            allLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
            allLabel.setBorder(new EmptyBorder(6, 7, 2, 7));
            allLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            allLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            allLabel.addMouseListener(new MouseAdapter()
            {
                public void mouseClicked(MouseEvent e)
                {
                    bankAllItemsCollapsed = !bankAllItemsCollapsed;
                    showTab(2);
                }
                public void mouseEntered(MouseEvent e) { allLabel.setForeground(TEXT_PRIMARY); }
                public void mouseExited(MouseEvent e) { allLabel.setForeground(TAB_INACTIVE); }
            });
            listPanel.add(allLabel);
// Bank search bar — always visible
            JPanel bankSearchWrap = new JPanel(new BorderLayout());
            bankSearchWrap.setBackground(new Color(20, 18, 19));
            bankSearchWrap.setBorder(new EmptyBorder(4, 6, 4, 6));
            bankSearchWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
            bankSearchWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            bankSearchWrap.setMinimumSize(new Dimension(0, 34));
            bankSearchWrap.setPreferredSize(new Dimension(214, 34));

            JTextField bankSearchField = new JTextField();
            bankSearchField.setBackground(new Color(14, 12, 13));
            bankSearchField.setForeground(TEXT_PRIMARY);
            bankSearchField.setCaretColor(TEXT_PRIMARY);
            bankSearchField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(58, 53, 48)),
                    new EmptyBorder(3, 5, 3, 5)
            ));
            bankSearchField.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
            bankSearchField.setPreferredSize(new Dimension(190, 24));
            bankSearchField.setMaximumSize(new Dimension(190, 24));
            bankSearchField.setMinimumSize(new Dimension(190, 24));
            bankSearchWrap.add(bankSearchField, BorderLayout.CENTER);
            listPanel.add(bankSearchWrap);

            JPanel bankResultsPanel = new JPanel();
            bankResultsPanel.setLayout(new BoxLayout(bankResultsPanel, BoxLayout.Y_AXIS));
            bankResultsPanel.setBackground(BG_DARK);
            bankResultsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bankResultsPanel.setMinimumSize(new java.awt.Dimension(0, 0));
            bankResultsPanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            final java.util.List<String[]> finalBankItems = new java.util.ArrayList<>(allBankItems);

            Runnable updateBankSearch = () -> {
                bankResultsPanel.removeAll();
                String query = bankSearchField.getText().trim().toLowerCase();
                boolean hasQuery = !query.isEmpty();

                if (!bankAllItemsCollapsed || hasQuery) {
                    java.util.List<String[]> filtered = hasQuery ?
                            finalBankItems.stream()
                            .filter(i -> i[0].toLowerCase().contains(query))
                            .collect(java.util.stream.Collectors.toList()) :
                            finalBankItems;

                    if (filtered.isEmpty() && hasQuery) {
                        JLabel noResults = new JLabel("No items found in your bank");
                        noResults.setForeground(TEXT_DIM);
                        noResults.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
                        noResults.setBorder(new EmptyBorder(8, 7, 4, 7));
                        noResults.setAlignmentX(Component.LEFT_ALIGNMENT);
                        bankResultsPanel.add(noResults);
                    } else {
                        int bankAllIndex = 0;
                        for (String[] i : filtered)
                            bankResultsPanel.add(buildBankItemBlock(i, false, bankAllIndex++));
                    }
                }
                bankResultsPanel.revalidate();
                bankResultsPanel.repaint();
            };

            bankSearchField.addFocusListener(new FocusAdapter()
            {
                public void focusGained(FocusEvent e)
                {
                    bankSearchField.setBorder(BorderFactory.createCompoundBorder(
                            new MatteBorder(1, 1, 1, 1, GOLD),
                            new EmptyBorder(3, 5, 3, 5)
                    ));
                }
                public void focusLost(FocusEvent e)
                {
                    bankSearchField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(58, 53, 48)),
                            new EmptyBorder(3, 5, 3, 5)
                    ));
                }
            });

            bankSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
            {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { SwingUtilities.invokeLater(updateBankSearch); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { SwingUtilities.invokeLater(updateBankSearch); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { SwingUtilities.invokeLater(updateBankSearch); }
            });

            updateBankSearch.run();
            listPanel.add(bankResultsPanel);
        }

        panel.add(listPanel, BorderLayout.CENTER);
        scheduleRepaint(listPanel);
        return panel;
    }

    private JPanel buildBankItemBlock(String[] item, boolean colorCode, int index)
    {
        String name = item[0];
        String price = item[1];
        String delta = item[5];
        String gpChange = item.length > 7 ? item[7] : "0 gp";

        boolean isUp = delta.startsWith("+");
        boolean isDown = delta.startsWith("-");

        Color borderColor = colorCode && isUp ? new Color(0, 180, 0) : colorCode && isDown ? new Color(200, 0, 0) : new Color(80, 75, 70);
        Color bgColor = (index % 2 == 0) ? BG_DARK : new Color(20, 18, 19);

        JPanel block = new JPanel()
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                if (colorCode && (isUp || isDown))
                {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Color gradColor = isUp ? new Color(0, 180, 0, 220) : new Color(200, 0, 0, 220);
                    GradientPaint gp = new GradientPaint(0, 0, gradColor, getWidth() * 0.75f, 0, new Color(0, 0, 0, 0));
                    g2.setPaint(gp);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            }
        };
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBackground(bgColor);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, 800));

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(bgColor);
        row.setBorder(new MatteBorder(0, 4, 0, 0, borderColor));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel iconPanel = new JPanel();
        iconPanel.setPreferredSize(new Dimension(42, 42));
        iconPanel.setBackground(new Color(14, 12, 13));
        iconPanel.setOpaque(true);
        iconPanel.setBorder(BorderFactory.createLineBorder(colorCode && isUp ? new Color(0, 100, 0) : colorCode && isDown ? new Color(100, 0, 0) : new Color(42, 37, 40)));

        Integer bankItemId = nameToId.get(name.toLowerCase()
                .replace('\u2019', '\'')
                .replace('\u2018', '\''));
        if (bankItemId == null) bankItemId = nameToId.get(name.toLowerCase());
        if (bankItemId != null)
        {
            loadIconAsync(bankItemId, iconPanel, bgColor);
        }

        JPanel iconWrapper = new JPanel(new java.awt.GridBagLayout());
        iconWrapper.setBackground(bgColor);
        iconWrapper.add(iconPanel);
        iconWrapper.setToolTipText(name);
        row.add(iconWrapper, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(bgColor);
        info.setBorder(new EmptyBorder(5, 7, 8, 0));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_ITEM_NAME));
        nameLabel.setMaximumSize(new Dimension(175, 20));

        JLabel priceLabel = new JLabel(formatPrice(price) + " gp");
        priceLabel.setForeground(PRICE_GOLD);
        priceLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_PRICE));

        JLabel deltaLabel = new JLabel("(" + delta + "%)");
        deltaLabel.setForeground(isUp ? GREEN_UP : isDown ? RED_DOWN : TAB_INACTIVE);
        deltaLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
        deltaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel gpChangeLabel = new JLabel(gpChange);
        gpChangeLabel.setForeground(isUp ? GREEN_UP : isDown ? RED_DOWN : TAB_INACTIVE);
        gpChangeLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_DELTA));

        JPanel deltaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        deltaRow.setBackground(bgColor);
        deltaRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        deltaRow.add(gpChangeLabel);
        deltaRow.add(Box.createHorizontalStrut(4));
        deltaRow.add(deltaLabel);

        info.add(nameLabel);
        info.add(priceLabel);
        info.add(deltaRow);
        row.add(info, BorderLayout.CENTER);

// Inline detail slot
        final JButton[] watchBtnRef = {null};
        JPanel detailSlot = new JPanel(new BorderLayout());
        detailSlot.setBackground(BG_DARK);
        detailSlot.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        detailSlot.setAlignmentX(Component.LEFT_ALIGNMENT);

        javax.swing.JViewport detailViewport = new javax.swing.JViewport();
        detailViewport.setView(detailSlot);
        detailViewport.setVisible(false);
        detailViewport.setBorder(null);
        detailViewport.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if (javax.swing.SwingUtilities.isRightMouseButton(e))
                {
                    javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
                    boolean isWatched = pinnedItems.contains(item[0]);
                    javax.swing.JMenuItem watchItem = new javax.swing.JMenuItem(isWatched ? "- Unwatch" : "+ Watch");
                    watchItem.addActionListener(ev -> {
                        boolean nowWatched = !pinnedItems.contains(item[0]);
                        if (nowWatched) pinnedItems.add(item[0]);
                        else pinnedItems.remove(item[0]);
                        savePinnedItems();
                        if (activeTab == 0) showTab(0);
                        JButton wb = watchBtnRef[0];
                        if (wb != null) {
                            wb.setText(nowWatched ? "✓ Watch" : "+ Watch");
                            wb.setBorder(BorderFactory.createLineBorder(nowWatched ? GOLD : new Color(58, 53, 48)));
                            wb.setForeground(nowWatched ? GOLD : TAB_INACTIVE);
                        }
                        watchItem.setText(nowWatched ? "- Unwatch" : "+ Watch");
                    });
                    popup.add(watchItem);
                    javax.swing.JMenuItem pricesItem = new javax.swing.JMenuItem("Prices ↗");
                    pricesItem.addActionListener(ev -> {
                        try {
                            int trackerItemId = item.length > 12 ? Integer.parseInt(item[12]) : -1;
                            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://prices.runescape.wiki/osrs/item/" + trackerItemId));
                        } catch (Exception ex) { }
                    });
                    popup.add(pricesItem);
                    javax.swing.JMenuItem wikiItem = new javax.swing.JMenuItem("Wiki ↗");
                    wikiItem.addActionListener(ev -> {
                        try {
                            String wikiName = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase().replace(" ", "_");
                            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://oldschool.runescape.wiki/w/" + wikiName));
                        } catch (Exception ex) { }
                    });
                    popup.add(wikiItem);
                    popup.show(row, e.getX(), e.getY());
                    return;
                }
                if (name.equals(selectedBankItemName) && currentOpenBankDetail != null)
                {
                    final JPanel closingDetail = currentOpenBankDetail;
                    currentOpenBankDetail = null;
                    final javax.swing.JViewport closingVP = (javax.swing.JViewport) closingDetail.getParent();
                    final int fullH = closingVP != null ? closingVP.getHeight() : 0;
                    int[] curH2 = {fullH};
                    javax.swing.Timer closeTimer = new javax.swing.Timer(16, null);
                    closeTimer.addActionListener(ev -> {
                        curH2[0] = Math.max(curH2[0] - 30, 0);
                        if (closingVP != null) {
                            closingVP.setPreferredSize(new Dimension(230, curH2[0]));
                            closingVP.setViewPosition(new java.awt.Point(0, fullH - curH2[0]));
                            closingVP.revalidate();
                        }
                        if (curH2[0] <= 0)
                        {
                            if (closingVP != null) { closingVP.setVisible(false); closingVP.setPreferredSize(null); }
                            closeTimer.stop();
                        }
                    });
                    closeTimer.start();
                    if (currentOpenBankRow != null)
                    {
                        currentOpenBankRow.setBackground(bgColor);
                        currentOpenBankRow.setBorder(new MatteBorder(0, 4, 0, 0, borderColor));
                        info.setBackground(bgColor);
                        deltaRow.setBackground(bgColor);
                        for (Component c : currentOpenBankRow.getComponents())
                            if (c instanceof JPanel) c.setBackground(bgColor);
                        currentOpenBankRow = null;
                        currentOpenBankInfo = null;
                        currentOpenBankDeltaRow = null;
                        currentOpenBankRowColor = BG_DARK;
                    }
                    selectedBankItemName = null;
                    bankReopenAction = null;
                    graphWasOpen = false;
                    liveHeaderPriceLabel = null;
                    liveBuyPriceValueLabel = null;
                    liveBuyPriceHeaderLabel = null;
                    liveSellPriceValueLabel = null;
                    liveSellPriceHeaderLabel = null;
                    liveFloatVolumeLabel = null;
                    liveFloatMarginLabel = null;
                    liveFloatProfitLabel = null;
                    liveFloatRoiLabel = null;
                    liveStatGrid = null;
                    liveStatsLabels = null;
                    liveOpenItemId = -1;
                    liveOpenItemData = null;
                    liveGraphPanel = null;
                    tabContentPanel.revalidate();
                    tabContentPanel.repaint();
                    return;
                }
// Close previous
                if (currentOpenBankDetail != null)
                {
                    final JPanel closingDetail2 = currentOpenBankDetail;
                    currentOpenBankDetail = null;
                    final javax.swing.JViewport closingVP2 = (javax.swing.JViewport) closingDetail2.getParent();
                    final int fullH2 = closingVP2 != null ? closingVP2.getHeight() : 0;
                    int[] curH3 = {fullH2};
                    javax.swing.Timer closeTimer2 = new javax.swing.Timer(16, null);
                    closeTimer2.addActionListener(ev -> {
                        curH3[0] = Math.max(curH3[0] - 30, 0);
                        if (closingVP2 != null) {
                            closingVP2.setPreferredSize(new Dimension(230, curH3[0]));
                            closingVP2.setViewPosition(new java.awt.Point(0, fullH2 - curH3[0]));
                            closingVP2.revalidate();
                        }
                        if (curH3[0] <= 0)
                        {
                            if (closingVP2 != null) { closingVP2.setVisible(false); closingVP2.setPreferredSize(null); }
                            closeTimer2.stop();
                        }
                    });
                    closeTimer2.start();
                    graphWasOpen = false;
                }
                if (currentOpenBankRow != null)
                {
                    currentOpenBankRow.setBackground(currentOpenBankRowColor);
                    currentOpenBankRow.setBorder(new MatteBorder(0, 4, 0, 0, currentOpenBankBorderColor != null ? currentOpenBankBorderColor : borderColor));
                    currentOpenBankBorderColor = null;
                    if (currentOpenBankInfo != null) currentOpenBankInfo.setBackground(currentOpenBankRowColor);
                    if (currentOpenBankDeltaRow != null) currentOpenBankDeltaRow.setBackground(currentOpenBankRowColor);
                    for (Component c : currentOpenBankRow.getComponents())
                        if (c instanceof JPanel) c.setBackground(currentOpenBankRowColor);
                }

                // Open this one
                selectedBankItemName = name;
                currentOpenBankRow = row;
                currentOpenBankRowColor = bgColor;
                currentOpenBankInfo = info;
                currentOpenBankDeltaRow = deltaRow;
                currentOpenBankDetail = detailSlot;
                currentOpenBankBorderColor = borderColor;
                row.setBorder(new MatteBorder(1, 4, 0, 0, GOLD));

                detailSlot.removeAll();
                detailSlot.add(buildInlineDetail(item, false), BorderLayout.CENTER);
                watchBtnRef[0] = findWatchButton(detailSlot);
                detailSlot.revalidate();
                javax.swing.SwingUtilities.invokeLater(() -> {
                    int fullHeight = detailSlot.getPreferredSize().height;
                    if (fullHeight <= 0) fullHeight = 150;
                    final int targetFinal = fullHeight;
                    detailViewport.setPreferredSize(new Dimension(230, 0));
                    detailViewport.setViewPosition(new java.awt.Point(0, targetFinal));
                    detailViewport.setVisible(true);
                    detailViewport.revalidate();
                    int[] curH = {0};
                    javax.swing.Timer openTimer = new javax.swing.Timer(16, null);
                    openTimer.addActionListener(ev -> {
                        curH[0] = Math.min(curH[0] + 30, targetFinal);
                        detailViewport.setPreferredSize(new Dimension(230, curH[0]));
                        detailViewport.setViewPosition(new java.awt.Point(0, targetFinal - curH[0]));
                        detailViewport.revalidate();
                        if (curH[0] >= targetFinal)
                        {
                            detailViewport.setPreferredSize(null);
                            detailViewport.setViewPosition(new java.awt.Point(0, 0));
                            openTimer.stop();
                        }
                    });
                    openTimer.start();
                });
                scheduleRepaint(detailSlot);
                final String[] itemData = item;
                bankReopenAction = () -> {
                    for (java.awt.Component c : bankListPanel.getComponents())
                    {
                        if (!(c instanceof JPanel)) continue;
                        JPanel block = (JPanel) c;
                        if (block.getComponentCount() < 2) continue;
                        java.awt.Component second = block.getComponent(1);
                        if (!(second instanceof javax.swing.JViewport)) continue;
                        javax.swing.JViewport newViewport = (javax.swing.JViewport) second;
                        if (!(newViewport.getView() instanceof JPanel)) continue;
                        JPanel newDetailSlot = (JPanel) newViewport.getView();
                        java.awt.Component first = block.getComponent(0);
                        if (!(first instanceof JPanel)) continue;
                        JPanel newRow = (JPanel) first;
                        for (java.awt.Component child : newRow.getComponents())
                        {
                            if (!(child instanceof JPanel)) continue;
                            for (java.awt.Component rowChild : ((JPanel)child).getComponents())
                            {
                                if (!(rowChild instanceof JLabel)) continue;
                                JLabel label = (JLabel) rowChild;
                                if (itemData[0].equalsIgnoreCase(label.getText()))
                                {
                                    newDetailSlot.removeAll();
                                    newDetailSlot.add(buildInlineDetail(itemData, false), BorderLayout.CENTER);
                                    newViewport.setVisible(true);                                    ;
                                    scheduleRepaint(newDetailSlot);
                                    if (graphWasOpen) reopenGraph(newDetailSlot);
                                    currentOpenBankDetail = newDetailSlot;
                                    selectedBankItemName = itemData[0];
                                    newRow.setBorder(new MatteBorder(1, 4, 0, 0, GOLD));
                                    currentOpenBankRow = newRow;
                                    return;
                                }
                            }
                        }
                    }
                };

                row.setBackground(BG_ROW_SELECTED);
                info.setBackground(BG_ROW_SELECTED);
                iconWrapper.setBackground(BG_ROW_SELECTED);
                deltaRow.setBackground(BG_ROW_SELECTED);

                tabContentPanel.revalidate();
                tabContentPanel.repaint();
            }
            public void mouseEntered(MouseEvent e)
            {
                if (!name.equals(selectedBankItemName))
                {
                    row.setBackground(BG_ROW_HOVER);
                    info.setBackground(BG_ROW_HOVER);
                    iconWrapper.setBackground(BG_ROW_HOVER);
                    deltaRow.setBackground(BG_ROW_HOVER);
                }
            }
            public void mouseExited(MouseEvent e)
            {
                if (!name.equals(selectedBankItemName))
                {
                    row.setBackground(bgColor);
                    info.setBackground(bgColor);
                    deltaRow.setBackground(bgColor);
                    iconWrapper.setBackground(bgColor);
                }
            }
        });

        block.add(row);
        block.add(detailViewport);
        return block;
    }

    private void addScrollForwarding(JComponent component, JScrollPane scrollPane)
    {
        component.addMouseWheelListener(e -> {
            javax.swing.JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getValue() + (int)(e.getUnitsToScroll() * 8));
        });
    }

    private void scheduleRepaint(JPanel panel)
    {
        int[] delays = {300, 600, 1000};
        for (int delay : delays)
        {
            javax.swing.Timer t = new javax.swing.Timer(delay, e -> {
                panel.revalidate();
                panel.repaint();
            });
            t.setRepeats(false);
            t.start();
        }
    }

    private void loadIconAsync(int itemId, JPanel iconPanel, Color rowBg)
{
    ImageIcon cached = iconCache.get(itemId);
    if (cached != null)
    {
        JLabel iconLabel = new JLabel(cached);
        iconPanel.add(iconLabel);
        iconPanel.revalidate();
        iconPanel.repaint();
        return;
    }
    new Thread(() -> {
        java.awt.image.BufferedImage img = plugin.getItemManager().getImage(itemId);
        if (img != null)
        {
            ImageIcon icon = new ImageIcon(img);
            iconCache.put(itemId, icon);
            javax.swing.SwingUtilities.invokeLater(() -> {
                iconPanel.add(new JLabel(icon));
                iconPanel.revalidate();
                iconPanel.repaint();
                java.awt.Container parent = iconPanel.getParent();
                while (parent != null)
                {
                    parent.revalidate();
                    parent.repaint();
                    if (parent instanceof JScrollPane) break;
                    parent = parent.getParent();
                }
            });
        }
    }).start();
}

    private void reopenGraph(JPanel detailSlot)
    {
        // Find the chart button inside the rebuilt detail panel and click it
        findAndClickChartButton(detailSlot);
    }

    private void findAndClickChartButton(java.awt.Container container)
    {
        for (java.awt.Component c : container.getComponents())
        {
            if (c instanceof JButton)
            {
                JButton btn = (JButton) c;
                if (btn.getText().contains("Show Price Chart"))
                {
                    btn.doClick();
                    return;
                }
            }
            if (c instanceof java.awt.Container)
            {
                findAndClickChartButton((java.awt.Container) c);
            }
        }
    }

    private void fetchGameUpdates()
    {
        if (gameUpdatesFetching || gameUpdates != null) return;
        gameUpdatesFetching = true;

        new Thread(() ->
        {
            try
            {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                java.util.List<UpdateMarker> updates = new java.util.ArrayList<>();
                long oneYearAgo = System.currentTimeMillis() / 1000 - 365L * 24 * 3600;

                // Step 2: fetch Game_updates page to get accurate dates
                String pageUrl = "https://oldschool.runescape.wiki/api.php?action=parse&page=Game_updates&prop=text&format=json";
                okhttp3.Request pageRequest = new okhttp3.Request.Builder()
                        .url(pageUrl).header("User-Agent", "GE Companion RuneLite Plugin").build();
                try (okhttp3.Response response = client.newCall(pageRequest).execute())
                {
                    if (response.isSuccessful() && response.body() != null)
                    {
                        String body = response.body().string();
                        org.json.JSONObject json = new org.json.JSONObject(body);
                        String wikitext = json.getJSONObject("parse").getJSONObject("text").getString("*");

                        // Parse year sections and bullet entries
                        // Format: ==2026== ... * [[link|Title]] or * [[Update:Title|Title]]
                        int currentYear = java.time.LocalDate.now().getYear();
// Parse year sections: <h3 id="2026">
                        java.util.regex.Pattern yearPattern = java.util.regex.Pattern.compile(
                                "<h3[^>]*id=\"(\\d{4})\"");
// Parse entries: <li><a ...>3 June</a> û <a href="/w/Update:Title" ...>
                        java.util.regex.Pattern entryPattern = java.util.regex.Pattern.compile(
                                "<li>[^<]*<a[^>]*>([^<]+)</a>\\s*[û\\-–]\\s*<a href=\"/w/Update:([^\"]+)\"[^>]*>([^<]+)</a>");

                        java.util.regex.Matcher yearM = yearPattern.matcher(wikitext);
                        java.util.regex.Matcher entryM = entryPattern.matcher(wikitext);

// Build year position map
                        java.util.TreeMap<Integer, Integer> yearPositions = new java.util.TreeMap<>();
                        while (yearM.find()) {
                            try {
                                int y = Integer.parseInt(yearM.group(1));
                                yearPositions.put(yearM.start(), y);
                            } catch (Exception ex) {}
                        }

                        while (entryM.find()) {
                            String dateStr = entryM.group(1).trim();
                            String titleEncoded = entryM.group(2).trim();
                            String titleDisplay = entryM.group(3).trim();

                            // Find which year this entry belongs to
                            int pos = entryM.start();
                            int year = currentYear;
                            for (java.util.Map.Entry<Integer, Integer> entry : yearPositions.entrySet()) {
                                if (entry.getKey() <= pos) year = entry.getValue();
                                else break;
                            }

                            // Decode URL encoding in title
                            String titleRaw;
                            try {
                                titleRaw = java.net.URLDecoder.decode(titleEncoded.replace("_", " "), "UTF-8");
                            } catch (Exception ex) { titleRaw = titleDisplay; }
                            if (titleRaw.startsWith("Update:")) titleRaw = titleRaw.substring(7);

                            long ts = parseWikiDate(dateStr + " " + year);
                            if (ts == 0 || ts < oneYearAgo) continue;

                            String cat = inferUpdateCategory(titleRaw);
                            String wikiUrl = "https://oldschool.runescape.wiki/w/Update:" + titleEncoded;
                            updates.add(new UpdateMarker(titleRaw, ts, cat, wikiUrl));
                        }
                    }
                }
                catch (Exception ex) {}

                // Sort by timestamp ascending
                updates.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));

                gameUpdates = updates;
                gameUpdatesFetching = false;
                System.out.println("GE Companion: fetched " + gameUpdates.size() + " game updates");
                javax.swing.SwingUtilities.invokeLater(() -> { if (liveUpdateCanvas != null) liveUpdateCanvas.repaint(); });
            }
            catch (Exception e)
            {
                gameUpdatesFetching = false;
                gameUpdates = new java.util.ArrayList<>();
                System.out.println("GE Companion: game updates fetch failed: " + e.getMessage());
            }
        }).start();
    }

    private String inferUpdateCategory(String title)
    {
        if (title == null) return "game";
        String t = title.toLowerCase();
        if (t.contains("poll")) return "poll";
        if (t.contains("patch") || t.contains("hotfix") || t.contains("fix") ||
                t.contains("fixes") || t.contains("tweak") || t.contains("tweaks") ||
                t.contains("bug") || t.contains("qol") || t.contains("changes"))
            return "patch";
        if (t.contains("event") || t.contains("christmas") || t.contains("easter") ||
                t.contains("halloween") || t.contains("leagues") || t.contains("deadman") ||
                t.contains("holiday") || t.contains("anniversary"))
            return "event";
        return "game";
    }

    private Color getUpdateColor(String category)
    {
        if (category == null) return new Color(120, 120, 120);
        String cat = category.toLowerCase();
        if (cat.contains("game")) return new Color(212, 175, 55);      // gold
        if (cat.contains("patch")) return new Color(74, 122, 191);     // blue
        if (cat.contains("event")) return new Color(75, 153, 75);      // green
        if (cat.contains("poll")) return new Color(150, 80, 200);      // purple
        return new Color(120, 120, 120);                                // gray
    }

    private String extractParam(String params, String key)
    {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                key + "\\s*=\\s*([^|\\}]+)",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher m = p.matcher(params);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private long parseWikiDate(String date)
    {
        try
        {
            java.time.LocalDate ld = java.time.LocalDate.parse(date,
                    java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH));
            return ld.atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond();
        }
        catch (Exception e)
        {
            try
            {
                java.time.LocalDate ld = java.time.LocalDate.parse(date,
                        java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", java.util.Locale.ENGLISH));
                return ld.atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond();
            }
            catch (Exception e2) { return 0; }
        }
    }

    private void fetchTimeseries(int itemId, String timeframe, java.util.function.Consumer<java.util.List<PricePoint>> callback)
    {
        String cacheKey = itemId + "_" + timeframe;
        if (timeseriesCache.containsKey(cacheKey))
        {
            callback.accept(timeseriesCache.get(cacheKey));
            return;
        }

        new Thread(() ->
        {
            try
            {
                String timestep;
                switch (timeframe)
                {
                    case "1D":  timestep = "5m";  break;
                    case "7D":  timestep = "1h";  break;
                    case "30D": timestep = "6h";  break;
                    case "3M":
                    case "1Y":  timestep = "24h"; break;
                    default:    timestep = "6h";  break;
                }

                String url = "https://prices.runescape.wiki/api/v1/osrs/timeseries?timestep=" + timestep + "&id=" + itemId;
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url)
                        .header("User-Agent", "GE Companion RuneLite Plugin")
                        .build();

                try (okhttp3.Response response = client.newCall(request).execute())
                {
                    if (!response.isSuccessful() || response.body() == null) return;
                    String body = response.body().string();
                    org.json.JSONObject json = new org.json.JSONObject(body);
                    org.json.JSONArray data = json.getJSONArray("data");

                    java.util.List<PricePoint> points = new java.util.ArrayList<>();
                    for (int i = 0; i < data.length(); i++)
                    {
                        org.json.JSONObject obj = data.getJSONObject(i);
                        long timestamp  = obj.optLong("timestamp", 0);
                        long buyPrice   = obj.optLong("avgHighPrice", 0);
                        long sellPrice  = obj.optLong("avgLowPrice", 0);
                        int  buyVolume  = obj.optInt("highPriceVolume", 0);
                        int  sellVolume = obj.optInt("lowPriceVolume", 0);
                        if (timestamp > 0)
                            points.add(new PricePoint(timestamp, buyPrice, sellPrice, buyVolume, sellVolume));
                    }

                    // Filter to correct date range
                    long now = System.currentTimeMillis() / 1000L;
                    long cutoff = 0;
                    switch (timeframe)
                    {
                        case "1D":  cutoff = now - 1L   * 86400; break;
                        case "7D":  cutoff = now - 7L   * 86400; break;
                        case "30D": cutoff = now - 30L  * 86400; break;
                        case "3M":  cutoff = now - 90L  * 86400; break;
                        case "1Y":  cutoff = now - 365L * 86400; break;
                        default:    cutoff = 0; break;
                    }
                    final long fc = cutoff;
                    java.util.List<PricePoint> filtered = new java.util.ArrayList<>();
                    for (PricePoint p : points)
                        if (p.timestamp >= fc) filtered.add(p);

                    // forward-fill zero prices with last known value
                    long lastBuy = 0, lastSell = 0;
                    for (PricePoint p : filtered) {
                        if (p.buyPrice > 0) lastBuy = p.buyPrice;
                        else p.buyPrice = lastBuy;
                        if (p.sellPrice > 0) lastSell = p.sellPrice;
                        else p.sellPrice = lastSell;
                    }

                    timeseriesCache.put(cacheKey, filtered);
                    javax.swing.SwingUtilities.invokeLater(() -> callback.accept(filtered));
                }
            }
            catch (Exception e)
            {
                // silently fail — graph just won't render
            }
        }).start();
    }
private String[] buildItemDataFromCache(String name)
    {
        String normalizedName = name.toLowerCase()
            .replace('\u2019', '\'')
            .replace('\u2018', '\'');
        Integer id = nameToId.get(normalizedName);
        if (id == null) id = nameToId.get(name.toLowerCase());
        if (id == null) return null;
        // Remap ornamented/charged items to their base tradeable item for price lookup
        int lookupId = itemVariantMap.getOrDefault(id, id);
        PriceData pd = priceCache.get(lookupId);
        if (pd == null || pd.getMid() == 0) return null;

        String price = String.valueOf(pd.getMid());
        String delta = "0.00";
        java.util.Map<Integer, Long> avgCache = avgPrice24h;
        if (activeTimeFrame.equals("1H")) avgCache = avgPrice1h;
        else if (activeTimeFrame.equals("6H")) avgCache = avgPrice6h;
        Long avg = avgCache.get(lookupId);
        if (avg != null && avg > 0)
        {
            double pct = ((double)(pd.getMid() - avg) / avg) * 100.0;
            delta = String.format("%+.2f", pct);
        }
        long gpChange = (avg != null && avg > 0) ? (pd.getMid() - avg) : 0;
        String gpChangeStr = gpChange > 0 ? "+" + formatPrice(String.valueOf(Math.abs(gpChange))) + " gp" :
                gpChange < 0 ? "-" + formatPrice(String.valueOf(Math.abs(gpChange))) + " gp" : "0 gp";
        Integer limit = itemLimits.get(lookupId);
        String limitStr = (limit != null && limit > 0) ? String.format("%,d", limit) : "?";
        String buyPrice = pd.high > 0 ? String.valueOf(pd.high) : pd.getMid() > 0 ? String.valueOf(pd.getMid()) : "0";
        String sellPrice = pd.low > 0 ? String.valueOf(pd.low) : pd.getMid() > 0 ? String.valueOf(pd.getMid()) : "0";
        String buyQty = buyVolume1h.containsKey(lookupId) ? String.valueOf(buyVolume1h.get(lookupId)) : "?";
        String sellQty = sellVolume1h.containsKey(lookupId) ? String.valueOf(sellVolume1h.get(lookupId)) : "?";
        long lastTraded = (pd.highTime >= pd.lowTime) ? pd.high : pd.low;
        String lastTradedStr = lastTraded > 0 ? String.valueOf(lastTraded) : "0";
        String lastTradedTime = pd.getTimeSince();
        return new String[]{name, price, buyPrice, sellPrice, "0", delta, limitStr, gpChangeStr, buyQty, sellQty, lastTradedStr, lastTradedTime, String.valueOf(id)};
    }


    private JPanel buildTimeFrameBar()
    {
        JPanel bar = new JPanel(new GridLayout(1, 3, 3, 0));
        bar.setBackground(new Color(26, 23, 24));
        bar.setBorder(new EmptyBorder(5, 6, 5, 6));

        String[] frames = {"1H", "6H", "24H"};
        for (String frame : frames)
        {
            JButton btn = new JButton(frame);
            btn.setFont(new Font("Monospaced", Font.PLAIN, FONT_TIMEFRAME));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBackground(frame.equals(activeTimeFrame) ? new Color(26, 21, 0) : new Color(14, 12, 13));
            btn.setForeground(frame.equals(activeTimeFrame) ? GOLD : TAB_INACTIVE);
            btn.setBorder(BorderFactory.createLineBorder(frame.equals(activeTimeFrame) ? GOLD : new Color(58, 53, 48)));
            btn.addActionListener(e -> {
                activeTimeFrame = frame;
                // Close floating stats panel if open
                if (activeStatsFloatPanel != null && activeStatsLayeredPane != null) {
                    activeStatsLayeredPane.remove(activeStatsFloatPanel);
                    activeStatsLayeredPane.repaint();
                    activeStatsFloatPanel = null;
                    activeStatsLayeredPane = null;
                }
                isRefreshing = true;
                showTab(activeTab);
                isRefreshing = false;
                if (searchReopenAction != null && activeTab == 1)
                {
                    javax.swing.SwingUtilities.invokeLater(searchReopenAction);
                }
                else if (watchlistReopenAction != null && activeTab == 0)
                {
                    javax.swing.SwingUtilities.invokeLater(watchlistReopenAction);
                }
            });
            bar.add(btn);
        }
        return bar;
    }

    private JPanel buildDetailHeader(String name, String price, String limit)
    {
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(BG_DETAIL);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        Integer detailItemId = nameToId.get(name.toLowerCase()
                .replace('\u2019', '\'')
                .replace('\u2018', '\''));
        if (detailItemId == null) detailItemId = nameToId.get(name.toLowerCase());

        // Build tooltip for second icon
        String tooltipHtml = "<html><table width='230' cellpadding='2'>";
        if (detailItemId != null) {
            PriceData tooltipPd = priceCache.get(detailItemId);
            Long dailyVol = volumeCache.get(detailItemId);
            Integer buyLimit = itemLimits.get(detailItemId);
            if (tooltipPd != null) {
                long margin = tooltipPd.high - tooltipPd.low;
                long profitAtLimit = buyLimit != null ? margin * buyLimit : 0;
                double roi = tooltipPd.low > 0 ? ((double) margin / tooltipPd.low) * 100.0 : 0;
                String volStr = dailyVol != null ? String.format("%,d", dailyVol) : "?";
                String marginColor = margin >= 0 ? "#6db86d" : "#c0392b";
                String profitColor = profitAtLimit >= 0 ? "#6db86d" : "#c0392b";
                String marginStr = (margin >= 0 ? "+" : "") + formatFullPrice(String.valueOf(margin)) + " gp";
                String profitStr = buyLimit != null ? (profitAtLimit >= 0 ? "+" : "") + formatFullPrice(String.valueOf(profitAtLimit)) + " gp" : "?";
                String roiStr = String.format("%.2f%%", roi);
                tooltipHtml += "<tr><td><font color='#8a8680'>Daily Volume</font></td><td>&nbsp;&nbsp;</td><td><font color='#d4af37'>" + volStr + "</font></td></tr>";
                tooltipHtml += "<tr><td><font color='#8a8680'>Margin</font></td><td>&nbsp;&nbsp;</td><td><font color='" + marginColor + "'>" + marginStr + "</font></td></tr>";
                tooltipHtml += "<tr><td><font color='#8a8680'>Profit (at limit)</font></td><td>&nbsp;&nbsp;</td><td><font color='" + profitColor + "'>" + profitStr + "</font></td></tr>";
                tooltipHtml += "<tr><td><font color='#8a8680'>ROI</font></td><td>&nbsp;&nbsp;</td><td><font color='#e8e3d8'>" + roiStr + "</font></td></tr>";
                tooltipHtml += "<tr><td colspan='3'><hr></td></tr>";
                tooltipHtml += "<tr><td colspan='3'><font color='#6b6660'><i>Updated when panel opened &middot; reopen for latest</i></font></td></tr>";
            }
        }
        tooltipHtml += "</table></html>";

        // Override getToolTipLocation to position tooltip above the icon
        final String finalTooltipHtml = tooltipHtml;
        javax.swing.JPanel iconBoxWithTooltip = new javax.swing.JPanel(new java.awt.GridBagLayout()) {
            @Override
            public java.awt.Point getToolTipLocation(java.awt.event.MouseEvent e) {
                return new java.awt.Point(0, -160);
            }
        };
        iconBoxWithTooltip.setPreferredSize(new Dimension(42, 42));
        iconBoxWithTooltip.setMaximumSize(new Dimension(42, 42));
        iconBoxWithTooltip.setMinimumSize(new Dimension(42, 42));
        iconBoxWithTooltip.setBackground(new Color(14, 12, 13));
        iconBoxWithTooltip.setBorder(BorderFactory.createLineBorder(new Color(42, 37, 40)));
        iconBoxWithTooltip.setToolTipText(finalTooltipHtml);

        // Load icon directly into iconBoxWithTooltip
        if (detailItemId != null) {
            final int finalDetailId = detailItemId;
            new Thread(() -> {
                java.awt.image.BufferedImage detailIcon = plugin.getItemManager().getImage(finalDetailId);
                if (detailIcon != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        javax.swing.JLabel iconLabel = new javax.swing.JLabel(new javax.swing.ImageIcon(detailIcon));
                        iconBoxWithTooltip.add(iconLabel);
                        iconBoxWithTooltip.revalidate();
                        iconBoxWithTooltip.repaint();
                        if (iconBoxWithTooltip.getParent() != null) iconBoxWithTooltip.getParent().repaint();
                    });
                }
            }).start();
        }

// Build floating stats panel content
        JPanel statsFloatPanel = new JPanel();
        statsFloatPanel.setLayout(new BoxLayout(statsFloatPanel, BoxLayout.Y_AXIS));
        statsFloatPanel.setBackground(new Color(14, 12, 13));
        statsFloatPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 65, 20), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        // Build stat rows
        if (detailItemId != null) {
            PriceData fpd = priceCache.get(detailItemId);
            Long fdailyVol = volumeCache.get(detailItemId);
            Integer fbuyLimit = itemLimits.get(detailItemId);
            if (fpd != null) {
                long fmargin = fpd.high - fpd.low;
                long fprofitAtLimit = fbuyLimit != null ? fmargin * fbuyLimit : 0;
                double froi = fpd.low > 0 ? ((double) fmargin / fpd.low) * 100.0 : 0;
                String fvolStr = fdailyVol != null ? String.format("%,d", fdailyVol) : "?";
                String fmarginColor = fmargin >= 0 ? "#6db86d" : "#c0392b";
                String fprofitColor = fprofitAtLimit >= 0 ? "#6db86d" : "#c0392b";
                String fmarginStr = (fmargin >= 0 ? "+" : "") + formatFullPrice(String.valueOf(fmargin)) + " gp";
                String fprofitStr = fbuyLimit != null ? (fprofitAtLimit >= 0 ? "+" : "") + formatFullPrice(String.valueOf(fprofitAtLimit)) + " gp" : "?";
                String froiStr = String.format("%.2f%%", froi);

                JPanel volRow = buildFloatStatRow("Daily Volume", fvolStr, new Color(212, 175, 55));
                liveFloatVolumeLabel = (JLabel) ((java.awt.BorderLayout) volRow.getLayout() == null ? null : volRow.getComponent(1));
                statsFloatPanel.add(volRow);
                statsFloatPanel.add(Box.createVerticalStrut(3));
                JPanel marginRow = buildFloatStatRow("Margin", fmarginStr, fmargin >= 0 ? new Color(109, 184, 109) : new Color(192, 57, 43));
                liveFloatMarginLabel = (JLabel) marginRow.getComponent(1);
                statsFloatPanel.add(marginRow);
                statsFloatPanel.add(Box.createVerticalStrut(3));
                JPanel profitRow = buildFloatStatRow("Profit (at limit)", fprofitStr, fprofitAtLimit >= 0 ? new Color(109, 184, 109) : new Color(192, 57, 43));
                liveFloatProfitLabel = (JLabel) profitRow.getComponent(1);
                statsFloatPanel.add(profitRow);
                statsFloatPanel.add(Box.createVerticalStrut(3));
                JPanel roiRow = buildFloatStatRow("ROI", froiStr, new Color(232, 227, 216));
                liveFloatRoiLabel = (JLabel) roiRow.getComponent(1);
                statsFloatPanel.add(roiRow);
                statsFloatPanel.add(Box.createVerticalStrut(5));
                JSeparator sep = new JSeparator();
                sep.setForeground(new Color(60, 50, 30));
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                statsFloatPanel.add(sep);
                statsFloatPanel.add(Box.createVerticalStrut(4));
                JLabel footnote = new JLabel("<html><center>Margin, Profit & ROI auto-refresh every 60s<br>Volume updates on startup</center></html>", SwingConstants.CENTER);
                footnote.setForeground(new Color(107, 102, 96));
                footnote.setFont(new Font("Monospaced", Font.PLAIN, 9));
                footnote.setAlignmentX(Component.CENTER_ALIGNMENT);
                footnote.setHorizontalAlignment(SwingConstants.CENTER);
                footnote.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                statsFloatPanel.add(footnote);
            }
        }

        final boolean[] statsOpen = {false};
        final javax.swing.Timer[] closeTimer = {null};
        // Declare arrowIndicator early so it can be referenced in mouse listener
        final JLabel[] arrowIndicatorRef = {null};

        // Hover border highlight + ▲ indicator
        iconBoxWithTooltip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        iconBoxWithTooltip.setToolTipText(null); // remove old tooltip — replaced by floating panel
        iconBoxWithTooltip.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                iconBoxWithTooltip.setBorder(BorderFactory.createLineBorder(new Color(100, 80, 20)));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!statsOpen[0])
                    iconBoxWithTooltip.setBorder(BorderFactory.createLineBorder(new Color(42, 37, 40)));
            }
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                javax.swing.JRootPane root = javax.swing.SwingUtilities.getRootPane(iconBoxWithTooltip);
                if (root == null) return;
                javax.swing.JLayeredPane layered = root.getLayeredPane();

                if (statsOpen[0]) {
                    // Close
// Slide down animation
                    statsOpen[0] = false;
                    iconBoxWithTooltip.setBorder(BorderFactory.createLineBorder(new Color(42, 37, 40)));
                    int[] curH2 = {statsFloatPanel.getHeight()};
                    final int closingY = statsFloatPanel.getY();
                    final int closingX = statsFloatPanel.getX();
                    final int closingW = statsFloatPanel.getWidth();
                    javax.swing.Timer closeTimer2 = new javax.swing.Timer(12, null);
                    closeTimer2.addActionListener(ev -> {
                        curH2[0] = Math.max(curH2[0] - 15, 0);
                        // Keep Y fixed at bottom, shrink from top down
                        int newY = closingY + (statsFloatPanel.getPreferredSize().height - curH2[0]);
                        statsFloatPanel.setBounds(closingX, newY, closingW, curH2[0]);
                        layered.repaint();
                        if (curH2[0] <= 0) {
                            closeTimer2.stop();
                            layered.remove(statsFloatPanel);
                            layered.repaint();
                        }
                    });
                    closeTimer2.start();
                } else {
                    // Calculate position above icon
                    java.awt.Point arrowLoc = arrowIndicatorRef[0] != null ?
                            javax.swing.SwingUtilities.convertPoint(arrowIndicatorRef[0], 0, 0, layered) :
                            javax.swing.SwingUtilities.convertPoint(iconBoxWithTooltip, 0, 0, layered);
                    java.awt.Point iconLoc = javax.swing.SwingUtilities.convertPoint(iconBoxWithTooltip, 0, 0, layered);
                    statsFloatPanel.setSize(statsFloatPanel.getPreferredSize());
                    int pw = statsFloatPanel.getPreferredSize().width;
                    int ph = statsFloatPanel.getPreferredSize().height;
                    int px = iconLoc.x;
                    int py = arrowLoc.y - ph;
// Slide up animation — starts at tip of ▲, grows upward
                    statsFloatPanel.setBounds(px, arrowLoc.y, pw, 0);
                    layered.add(statsFloatPanel, javax.swing.JLayeredPane.POPUP_LAYER);
                    layered.repaint();
                    statsOpen[0] = true;
                    activeStatsFloatPanel = statsFloatPanel;
                    activeStatsLayeredPane = layered;
                    final int targetY = py;
                    final int targetH = ph;
                    int[] curH = {0};
                    javax.swing.Timer openTimer = new javax.swing.Timer(12, null);
                    openTimer.addActionListener(ev -> {
                        curH[0] = Math.min(curH[0] + 15, targetH);
                        int newY = arrowLoc.y - curH[0];
                        statsFloatPanel.setBounds(px, newY, pw, curH[0]);
                        layered.repaint();
                        if (curH[0] >= targetH) {
                            openTimer.stop();
                        }
                    });
                    openTimer.start();
                    iconBoxWithTooltip.setBorder(BorderFactory.createLineBorder(new Color(140, 110, 30)));
                }
            }
        });

// ▲ indicator — shows on hover above the icon
        JLabel arrowIndicator = new JLabel("▲", SwingConstants.LEFT);
        arrowIndicatorRef[0] = arrowIndicator;
        arrowIndicator.setForeground(new Color(100, 80, 20));
        arrowIndicator.setFont(new Font("Monospaced", Font.PLAIN, 8));
        arrowIndicator.setAlignmentX(Component.CENTER_ALIGNMENT);
        arrowIndicator.setForeground(new Color(0, 0, 0, 0)); // transparent when not hovered

        // Show/hide ▲ on icon hover
        iconBoxWithTooltip.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                arrowIndicator.setForeground(new Color(100, 80, 20));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!statsOpen[0]) arrowIndicator.setForeground(new Color(0, 0, 0, 0));
            }
        });

        JPanel detailIconWrapper = new JPanel(new java.awt.GridBagLayout());
        detailIconWrapper.setBackground(BG_DETAIL);
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = java.awt.GridBagConstraints.SOUTH;
        detailIconWrapper.add(arrowIndicator, gbc);
        gbc.gridy = 1;
        gbc.anchor = java.awt.GridBagConstraints.CENTER;
        detailIconWrapper.add(iconBoxWithTooltip, gbc);
        headerRow.add(detailIconWrapper, BorderLayout.WEST);

        JPanel namePrice = new JPanel();
        namePrice.setLayout(new BoxLayout(namePrice, BoxLayout.Y_AXIS));
        namePrice.setBackground(BG_DETAIL);
        namePrice.setBorder(new EmptyBorder(5, 7, 5, 0));
        namePrice.setMaximumSize(new Dimension(175, 80));

        String detailName = name.length() > 20 ? name.substring(0, 17) + "..." : name;
        JLabel nameLabel = new JLabel(detailName);
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_ITEM_NAME));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (name.length() > 20) nameLabel.setToolTipText(name);

        JLabel priceLabel = new JLabel(formatFullPrice(price) + " gp");
        priceLabel.setForeground(PRICE_GOLD);
        priceLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_PRICE));
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        liveHeaderPriceLabel = priceLabel;

        JLabel limitLabel = new JLabel("Lmt: " + limit);
        limitLabel.setForeground(TEXT_PRIMARY);
        limitLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
        limitLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        namePrice.add(nameLabel);
        namePrice.add(priceLabel);
        namePrice.add(limitLabel);
        headerRow.add(namePrice, BorderLayout.CENTER);

        return headerRow;
    }

    private JPanel buildGraphPanel(int itemId, long currentPrice, String initialTimeframe, JLabel[] statsLabels)
    {
        final String[] activeFrame = {initialTimeframe};
        final java.util.List<PricePoint>[] pointsHolder = new java.util.List[]{null};
        final boolean[] animating = {false};
        final boolean[] currentLineHighlighted = {false};
        final int[] crosshairIdx = {-1};
        final int[] revealW = {0};
        final int[] zoomStart = {0};
        final int[] zoomEnd = {-1}; // -1 means full range
        final long[] zoomBoxMinY = {-1}; // -1 means auto-scale
        final long[] zoomBoxMaxY = {-1}; // -1 means auto-scale
        final int[] dragStartY = {-1};
        final int[] dragEndY = {-1};
        final int[] panStartX = {-1};
        final int[] panStartY = {-1};
        final int[] panZoomStart = {-1};
        final int[] panZoomEnd = {-1};
        final long[] panBoxMinY = {-1};
        final long[] panBoxMaxY = {-1};
        final boolean[] isPanning = {false};
        final boolean[] magnifying = {false};
        final int[] magnifyIdx = {-1};

        // ── outer wrapper ──────────────────────────────────────────────
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(new Color(14, 12, 13));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // ── timeframe buttons ──────────────────────────────────────────
        String[] frames = {"1D", "7D", "30D", "3M", "1Y"};
        JPanel tfBar = new JPanel(new GridLayout(1, 5, 2, 0));
        tfBar.setBackground(new Color(14, 12, 13));
        tfBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        tfBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton[] tfBtns = new JButton[5];
        for (int i = 0; i < frames.length; i++)
        {
            JButton b = new JButton(frames[i]);
            b.setFont(new Font("Monospaced", Font.PLAIN, FONT_TIMEFRAME));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setBackground(new Color(14, 12, 13));
            boolean active = frames[i].equals(initialTimeframe);
            b.setForeground(active ? GOLD : TAB_INACTIVE);
            b.setBorder(active
                    ? BorderFactory.createLineBorder(GOLD)
                    : BorderFactory.createLineBorder(new Color(58, 53, 48)));
            tfBtns[i] = b;
            tfBar.add(b);
        }
        wrapper.add(tfBar);
        wrapper.add(Box.createVerticalStrut(4));

        // ── legend ─────────────────────────────────────────────────────
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        legend.setBackground(new Color(14, 12, 13));
        legend.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        legend.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel buyLeg  = new JLabel("— Buy");
        buyLeg.setForeground(GOLD);
        buyLeg.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
        JLabel sellLeg = new JLabel("— Sell");
        sellLeg.setForeground(new Color(74, 122, 191));
        sellLeg.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
        JLabel curLeg  = new JLabel("--- Current");
        curLeg.setForeground(new Color(155, 89, 182));
        curLeg.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
// Item name label — right-aligned, above legend
        String itemName = "";
        for (java.util.Map.Entry<String, Integer> entry : nameToId.entrySet()) {
            if (entry.getValue() == itemId) {
                String[] words = entry.getKey().split(" ");
                StringBuilder sb = new StringBuilder();
                for (String word : words) {
                    if (word.length() > 0)
                        sb.append(Character.toUpperCase(word.charAt(0)))
                                .append(word.substring(1)).append(" ");
                }
                itemName = sb.toString().trim();
                break;
            }
        }
        JLabel itemNameLabel = new JLabel(itemName, SwingConstants.LEFT);
        itemNameLabel.setForeground(TEXT_DIM);
        itemNameLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
        itemNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        itemNameLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        wrapper.add(itemNameLabel);

        legend.add(buyLeg);
        legend.add(sellLeg);
        legend.add(curLeg);

// Current price value on second line
        JLabel curPriceLabel = new JLabel(currentPrice > 0 ? String.format("%,d gp", currentPrice) : "");
        curPriceLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
        curPriceLabel.setBorder(new EmptyBorder(0, 105, 0, 0));
        curPriceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        curPriceLabel.setForeground(new Color(0, 0, 0, 0));
        curPriceLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        curLeg.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final JPanel[] priceCanvasHolder = {null};
        final JPanel[] volCanvasHolder = {null};

        // ↺ reset zoom button (only for DRAG_SELECT mode)
        if (config.chartZoomMode() == ChartZoomMode.DRAG_SELECT) {
            JLabel resetBtn = new JLabel("↺");
            resetBtn.setForeground(TEXT_DIM);
            resetBtn.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
            resetBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            resetBtn.setToolTipText("Reset zoom");
            resetBtn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { resetBtn.setForeground(GOLD); }
                public void mouseExited(MouseEvent e)  { resetBtn.setForeground(TEXT_DIM); }
                public void mouseClicked(MouseEvent e) {
                    zoomStart[0] = 0;
                    zoomEnd[0] = -1;
                    if (priceCanvasHolder[0] != null) priceCanvasHolder[0].repaint();
                    if (volCanvasHolder[0] != null) volCanvasHolder[0].repaint();
                }
            });
            legend.add(resetBtn);
        }
        curLeg.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { currentLineHighlighted[0] = true; curPriceLabel.setForeground(new Color(185, 109, 222)); if (priceCanvasHolder[0] != null) priceCanvasHolder[0].repaint(); }
            public void mouseExited(MouseEvent e)  { currentLineHighlighted[0] = false; curPriceLabel.setForeground(new Color(0, 0, 0, 0)); if (priceCanvasHolder[0] != null) priceCanvasHolder[0].repaint(); }
        });
        curPriceLabel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { currentLineHighlighted[0] = true; curPriceLabel.setForeground(new Color(185, 109, 222)); if (priceCanvasHolder[0] != null) priceCanvasHolder[0].repaint(); }
            public void mouseExited(MouseEvent e)  { currentLineHighlighted[0] = false; curPriceLabel.setForeground(new Color(0, 0, 0, 0)); if (priceCanvasHolder[0] != null) priceCanvasHolder[0].repaint(); }
        });
        wrapper.add(legend);
        wrapper.add(curPriceLabel);
        wrapper.add(Box.createVerticalStrut(3));

        // ── price canvas ───────────────────────────────────────────────
        JPanel priceCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                if (animating[0] && revealW[0] < 100) {
                    g2.setClip(0, 0, (int)(w * revealW[0] / 100.0), h);
                }
                g2.setColor(new Color(14, 12, 13));
                g2.fillRect(0, 0, w, h);

                java.util.List<PricePoint> pts = pointsHolder[0];
                if (pts == null || pts.size() < 2) {
                    g2.setColor(new Color(110, 100, 90));
                    g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
                    g2.drawString("Loading...", w / 2 - 25, h / 2);
                    g2.dispose();
                    return;
                }

// price range — use shared zoom state directly
                int visStart = zoomStart[0];
                int visEnd = zoomEnd[0] >= 0 ? zoomEnd[0] : pts.size() - 1;
                visStart = Math.max(0, Math.min(visStart, pts.size()-1));
                visEnd = Math.max(visStart+1, Math.min(visEnd, pts.size()-1));
                java.util.List<PricePoint> visPts = pts.subList(visStart, visEnd+1);

                long minP = Long.MAX_VALUE, maxP = Long.MIN_VALUE;
                for (PricePoint p : visPts) {
                    if (p.buyPrice  > 0) { minP = Math.min(minP, p.buyPrice);  maxP = Math.max(maxP, p.buyPrice); }
                    if (p.sellPrice > 0) { minP = Math.min(minP, p.sellPrice); maxP = Math.max(maxP, p.sellPrice); }
                }
                if (currentPrice > 0) { minP = Math.min(minP, currentPrice); maxP = Math.max(maxP, currentPrice); }
                if (minP == Long.MAX_VALUE) { g2.dispose(); return; }
                long pad = Math.max((maxP - minP) / 3, 1);
                minP -= pad; maxP += pad;
// box zoom: override Y axis if explicit bounds are set
                final long fMin = zoomBoxMinY[0] >= 0 ? zoomBoxMinY[0] : minP;
                final long fMax = zoomBoxMaxY[0] >= 0 ? zoomBoxMaxY[0] : maxP;

// grid lines
                g2.setColor(new Color(40, 36, 34));
                for (float pct : new float[]{0.25f, 0.5f, 0.75f}) {
                    int y = (int)(h * pct);
                    g2.drawLine(0, y, w, y);
                }

                int n = visPts.size();

// intersect clip with chart bounds so lines are clipped at Y boundaries
                g2.clip(new java.awt.Rectangle(0, 0, w, h));

// buy line
                g2.setStroke(new java.awt.BasicStroke(1.4f));
                g2.setColor(GOLD);
                int[] bx = new int[n], by = new int[n];
                boolean[] bHas = new boolean[n];
                for (int i = 0; i < n; i++) {
                    bx[i] = (int)(i * (w - 1.0) / (n - 1));
                    bHas[i] = visPts.get(i).buyPrice > 0;
                    by[i] = bHas[i]
                            ? h - (int)((visPts.get(i).buyPrice - fMin) * h / Math.max(fMax - fMin, 1))
                            : 0;
                }
                for (int i = 0; i < n - 1; i++)
                    if (bHas[i] && bHas[i+1])
                        g2.drawLine(bx[i], by[i], bx[i+1], by[i+1]);

// sell line
                g2.setColor(new Color(74, 122, 191));
                int[] sx = new int[n], sy = new int[n];
                boolean[] sHas = new boolean[n];
                for (int i = 0; i < n; i++) {
                    sx[i] = bx[i];
                    sHas[i] = visPts.get(i).sellPrice > 0;
                    sy[i] = sHas[i]
                            ? h - (int)((visPts.get(i).sellPrice - fMin) * h / Math.max(fMax - fMin, 1))
                            : 0;
                }
                for (int i = 0; i < n - 1; i++)
                    if (sHas[i] && sHas[i+1])
                        g2.drawLine(sx[i], sy[i], sx[i+1], sy[i+1]);

// dots
                g2.setStroke(new java.awt.BasicStroke(1f));
                for (int i = 0; i < n; i++) {
                    if (bHas[i]) { g2.setColor(GOLD); g2.fillOval(bx[i]-1, by[i]-1, 2, 2); }
                    if (sHas[i]) { g2.setColor(new Color(74, 122, 191)); g2.fillOval(sx[i]-1, sy[i]-1, 2, 2); }
                }

// current price dashed line (drawn after lines/dots so label is on top)
                if (currentPrice > 0 && currentPrice >= fMin && currentPrice <= fMax) {
                    int cy = h - (int)((currentPrice - fMin) * h / Math.max(fMax - fMin, 1));
                    boolean highlighted = currentLineHighlighted[0];
                    g2.setColor(highlighted ? new Color(185, 109, 222) : new Color(155, 89, 182));
                    if (highlighted) {
                        g2.setStroke(new java.awt.BasicStroke(1.8f));
                        g2.drawLine(0, cy, w, cy);
                        g2.setStroke(new java.awt.BasicStroke(1f));
                    } else {
                        g2.setStroke(new java.awt.BasicStroke(1f, java.awt.BasicStroke.CAP_BUTT,
                                java.awt.BasicStroke.JOIN_MITER, 10f, new float[]{4f, 3f}, 0f));
                        g2.drawLine(0, cy, w, cy);
                        g2.setStroke(new java.awt.BasicStroke(1f));
                    }
                }

                // crosshair
                int ci = crosshairIdx[0];
                if (!animating[0] && ci >= 0 && ci < n) {
                    int cx = bx[ci];
                    // vertical line
                    g2.setColor(new Color(200, 200, 200, 160));
                    g2.setStroke(new java.awt.BasicStroke(1f, java.awt.BasicStroke.CAP_BUTT,
                            java.awt.BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0f));
                    g2.drawLine(cx, 0, cx, h);
                    g2.setStroke(new java.awt.BasicStroke(1f));

                    PricePoint cp = visPts.get(ci);
                    // buy dot + label
                    if (bHas[ci]) {
                        g2.setColor(new Color(14, 12, 13));
                        g2.fillOval(bx[ci]-4, by[ci]-4, 8, 8);
                        g2.setColor(GOLD);
                        g2.drawOval(bx[ci]-4, by[ci]-4, 8, 8);
                        g2.fillOval(bx[ci]-2, by[ci]-2, 4, 4);
                    }
                    if (sHas[ci]) {
                        g2.setColor(new Color(14, 12, 13));
                        g2.fillOval(sx[ci]-4, sy[ci]-4, 8, 8);
                        g2.setColor(new Color(74, 122, 191));
                        g2.drawOval(sx[ci]-4, sy[ci]-4, 8, 8);
                        g2.fillOval(sx[ci]-2, sy[ci]-2, 4, 4);
                    }

// floating price labels (suppressed when magnifier is active)
                    Object dsObj2 = getClientProperty("isDragging");
                    boolean draggingNow2 = dsObj2 instanceof boolean[] && ((boolean[])dsObj2)[0];
                    if (!magnifying[0] && !draggingNow2) {
                    g2.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
                    FontMetrics fm = g2.getFontMetrics();
                        PricePoint cp2 = visPts.get(ci);
                    String buyStr2 = cp2.buyPrice > 0 ? String.format("%,d", cp2.buyPrice) : "";
                    String sellStr2 = cp2.sellPrice > 0 ? String.format("%,d", cp2.sellPrice) : "";
                    int labelW = Math.max(fm.stringWidth(buyStr2), fm.stringWidth(sellStr2)) + 6, labelH = 14;
                    int gap = 3;

                    int buyLabelY  = by[ci] >= 0 ? Math.min(h - labelH, Math.max(0, by[ci] - labelH / 2)) : -999;
                    int sellLabelY = sy[ci] >= 0 ? Math.min(h - labelH, Math.max(0, sy[ci] - labelH / 2)) : -999;

                    // collision avoidance
                    if (by[ci] >= 0 && sy[ci] >= 0 && Math.abs(by[ci] - sy[ci]) < labelH + gap) {
                        int mid = (by[ci] + sy[ci]) / 2;
                        buyLabelY  = mid - labelH - gap / 2;
                        sellLabelY = mid + gap / 2;
                    }

                    boolean nearRight = cx > w - labelW - 6;
                    int lx = nearRight ? cx - labelW - 6 : cx + 6;

                        if (bHas[ci]) {

                            g2.setColor(new Color(30, 25, 10));
                            g2.fillRect(lx, buyLabelY, labelW, labelH);
                            g2.setColor(GOLD);
                            g2.drawRect(lx, buyLabelY, labelW, labelH);
                            g2.drawString(buyStr2, lx + 3, buyLabelY + labelH - 3);
                        }
                        if (sHas[ci]) {

                            g2.setColor(new Color(10, 15, 30));
                            g2.fillRect(lx, sellLabelY, labelW, labelH);
                            g2.setColor(new Color(74, 122, 191));
                            g2.drawRect(lx, sellLabelY, labelW, labelH);
                            g2.drawString(sellStr2, lx + 3, sellLabelY + labelH - 3);
                        }
                    } // end if (!magnifying[0])
                } // end crosshair if block

            // ── drag selection rectangle overlay ──────────────────────────
                if (config.chartZoomMode() == ChartZoomMode.DRAG_SELECT) {
                Object dsObj = getClientProperty("isDragging");
                Object dx1Obj = getClientProperty("dragStart");
                Object dx2Obj = getClientProperty("dragEnd");
                boolean draggingNow = dsObj instanceof boolean[] && ((boolean[])dsObj)[0];
                    if (draggingNow && dx1Obj instanceof int[] && dx2Obj instanceof int[]) {
                        int dx1 = Math.min(((int[])dx1Obj)[0], ((int[])dx2Obj)[0]);
                        int dx2 = Math.max(((int[])dx1Obj)[0], ((int[])dx2Obj)[0]);
                        int ddx = Math.abs(((int[])dx2Obj)[0] - ((int[])dx1Obj)[0]);
                        int ddy = Math.abs(dragEndY[0] - dragStartY[0]);
                        boolean isBoxZoom = dragStartY[0] >= 0 && ddy > ddx * 0.25;
                        int dy1 = Math.min(dragStartY[0], dragEndY[0]);
                        int dy2 = Math.max(dragStartY[0], dragEndY[0]);
                        if (isBoxZoom) {
                            // box zoom — draw actual rectangle
                            g2.setColor(new Color(0, 0, 0, 90));
                            g2.fillRect(0, 0, dx1, h);
                            g2.fillRect(dx2, 0, w - dx2, h);
                            g2.fillRect(dx1, 0, dx2 - dx1, dy1);
                            g2.fillRect(dx1, dy2, dx2 - dx1, h - dy2);
                            // selection fill
                            g2.setColor(new Color(74, 122, 191, 30));
                            g2.fillRect(dx1, dy1, dx2 - dx1, dy2 - dy1);
                            // selection border
                            g2.setColor(new Color(100, 160, 255, 200));
                            g2.setStroke(new java.awt.BasicStroke(1.5f));
                            g2.drawRect(dx1, dy1, dx2 - dx1, dy2 - dy1);
                            g2.setStroke(new java.awt.BasicStroke(1f));
                        } else {
                            // range zoom — original horizontal behavior
                            g2.setColor(new Color(0, 0, 0, 90));
                            g2.fillRect(0, 0, dx1, h);
                            g2.fillRect(dx2, 0, w - dx2, h);
                            // selection fill
                            g2.setColor(new Color(74, 122, 191, 30));
                            g2.fillRect(dx1, 0, dx2 - dx1, h);
                            // selection border
                            g2.setColor(new Color(100, 160, 255, 200));
                            g2.setStroke(new java.awt.BasicStroke(1.5f));
                            g2.drawLine(dx1, 0, dx1, h);
                            g2.drawLine(dx2, 0, dx2, h);
                            g2.setStroke(new java.awt.BasicStroke(1f));
                        }
                    }
            }

            // ── magnifier / loupe ──────────────────────────────────────────
                if (config.chartZoomMode() == ChartZoomMode.MAGNIFIER && magnifying[0] && magnifyIdx[0] >= 0 && pts != null && pts.size() >= 2) {
                    int mci = magnifyIdx[0];
                    int mn = pts.size();
                    int magW = 160, magH = 70, magPad = 5;
                    int half = 3;

                    // position: above crosshair, flip left/right near edges
                    int cx = (int)(mci * (w - 1.0) / (mn - 1));
                    int magX = cx - magW / 2;
                    if (magX < 2) magX = 2;
                    if (magX + magW > w - 2) magX = w - magW - 2;
                    int magY = 4;

// background + border
                    g2.setColor(new Color(14, 12, 13));
                    g2.fillRect(magX, magY, magW, magH);
                    g2.setColor(GOLD);
                    g2.drawRect(magX, magY, magW, magH);

                    // compute zoom window — ±3 points around mci
                    int zStart = Math.max(0, mci - half);
                    int zEnd   = Math.min(mn - 1, mci + half);
                    int zCount = zEnd - zStart + 1;
                    if (zCount < 2) { g2.dispose(); return; }

                    // price range for zoomed window
                    long zMinP = Long.MAX_VALUE, zMaxP = Long.MIN_VALUE;
                    for (int i = zStart; i <= zEnd; i++) {
                        PricePoint zp = pts.get(i);
                        if (zp.buyPrice  > 0) { zMinP = Math.min(zMinP, zp.buyPrice);  zMaxP = Math.max(zMaxP, zp.buyPrice); }
                        if (zp.sellPrice > 0) { zMinP = Math.min(zMinP, zp.sellPrice); zMaxP = Math.max(zMaxP, zp.sellPrice); }
                    }
                    if (zMinP == Long.MAX_VALUE) { g2.dispose(); return; }
                    long zPad = Math.max((zMaxP - zMinP) / 4, 1);
                    zMinP -= zPad; zMaxP += zPad;

                    // draw buy line in zoomed window
                    g2.setStroke(new java.awt.BasicStroke(1.4f));
                    g2.setColor(GOLD);
                    int[] zbx = new int[zCount], zby = new int[zCount];
                    for (int i = 0; i < zCount; i++) {
                        zbx[i] = magX + magPad + (int)(i * (magW - magPad * 2 - 1.0) / (zCount - 1));
                        long bp = pts.get(zStart + i).buyPrice;
                        zby[i] = bp > 0 ? magY + magH - magPad - (int)((bp - zMinP) * (magH - magPad * 2) / Math.max(zMaxP - zMinP, 1)) : -1;
                    }
                    for (int i = 0; i < zCount - 1; i++)
                        if (zby[i] >= 0 && zby[i+1] >= 0)
                            g2.drawLine(zbx[i], zby[i], zbx[i+1], zby[i+1]);

                    // draw sell line in zoomed window
                    g2.setColor(new Color(74, 122, 191));
                    int[] zsx = new int[zCount], zsy = new int[zCount];
                    for (int i = 0; i < zCount; i++) {
                        zsx[i] = zbx[i];
                        long sp = pts.get(zStart + i).sellPrice;
                        zsy[i] = sp > 0 ? magY + magH - magPad - (int)((sp - zMinP) * (magH - magPad * 2) / Math.max(zMaxP - zMinP, 1)) : -1;
                    }
                    for (int i = 0; i < zCount - 1; i++)
                        if (zsy[i] >= 0 && zsy[i+1] >= 0)
                            g2.drawLine(zsx[i], zsy[i], zsx[i+1], zsy[i+1]);

                    // center point dots
                    int zci = mci - zStart;
                    if (zci >= 0 && zci < zCount) {
                        if (zby[zci] >= 0) { g2.setColor(GOLD); g2.fillOval(zbx[zci]-3, zby[zci]-3, 6, 6); }
                        if (zsy[zci] >= 0) { g2.setColor(new Color(74, 122, 191)); g2.fillOval(zsx[zci]-3, zsy[zci]-3, 6, 6); }
                    }

// crosshair vertical line at center point
                    int zcx = magX + magPad + (int)(zci * (magW - magPad * 2 - 1.0) / Math.max(zCount - 1, 1));
                    g2.setColor(new Color(200, 200, 200, 120));
                    g2.setStroke(new java.awt.BasicStroke(1f, java.awt.BasicStroke.CAP_BUTT,
                            java.awt.BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0f));
                    g2.drawLine(zcx, magY + 1, zcx, magY + magH - 1);
                    g2.setStroke(new java.awt.BasicStroke(1f));

                    // price labels for center point
                    PricePoint mcp = pts.get(mci);
                    g2.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
                    FontMetrics mfm = g2.getFontMetrics();
                    if (zby[zci] >= 0 && mcp.buyPrice > 0) {
                        String mBuyStr = String.format("%,d", mcp.buyPrice);
                        int mlw = mfm.stringWidth(mBuyStr) + 6;
                        boolean mNearRight = zcx > magX + magW - mlw - 6;
                        int mlx = mNearRight ? zcx - mlw - 3 : zcx + 3;
                        int mly = Math.max(magY + 2, zby[zci] - 18);
                        g2.setColor(new Color(30, 25, 10));
                        g2.fillRect(mlx, mly, mlw, 12);
                        g2.setColor(GOLD);
                        g2.drawRect(mlx, mly, mlw, 12);
                        g2.drawString(mBuyStr, mlx + 3, mly + 10);
                    }
                    if (zsy[zci] >= 0 && mcp.sellPrice > 0) {
                        String mSellStr = String.format("%,d", mcp.sellPrice);
                        int mlw = mfm.stringWidth(mSellStr) + 6;
                        boolean mNearRight = zcx > magX + magW - mlw - 6;
                        int mlx = mNearRight ? zcx - mlw - 3 : zcx + 3;
                        int mly = Math.min(magY + magH - 13, zsy[zci] + 8);
                        if (zby[zci] >= 0 && Math.abs(zby[zci] - zsy[zci]) < 14) mly = zby[zci] + 14;
                        mly = Math.max(magY + 1, mly);
                        g2.setColor(new Color(10, 15, 30));
                        g2.fillRect(mlx, mly, mlw, 12);
                        g2.setColor(new Color(74, 122, 191));
                        g2.drawRect(mlx, mly, mlw, 12);
                        g2.drawString(mSellStr, mlx + 3, mly + 10);
                    }

                    g2.setStroke(new java.awt.BasicStroke(1f));
                }

                g2.dispose();
            }
        };

        priceCanvas.setPreferredSize(new Dimension(1, 80));
        priceCanvasHolder[0] = priceCanvas;
        priceCanvas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        priceCanvas.setMinimumSize(new Dimension(0, 80));
        priceCanvas.setBackground(new Color(14, 12, 13));
        priceCanvas.setAlignmentX(Component.LEFT_ALIGNMENT);
        priceCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        wrapper.add(priceCanvas);
        wrapper.add(Box.createVerticalStrut(3));

        // ── volume label ───────────────────────────────────────────────
        JPanel volLabelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        volLabelRow.setBackground(new Color(14, 12, 13));
        volLabelRow.setMaximumSize(new Dimension(225, 14));
        volLabelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel volLbl  = new JLabel("Volume");
        volLbl.setForeground(new Color(110, 100, 90));
        volLbl.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
        JLabel volBuyL = new JLabel("■ Buy");
        volBuyL.setForeground(GOLD);
        volBuyL.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
        JLabel volSelL = new JLabel("■ Sell");
        volSelL.setForeground(new Color(74, 122, 191));
        volSelL.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
        volLabelRow.add(volLbl);
        volLabelRow.add(volBuyL);
        volLabelRow.add(volSelL);
        wrapper.add(volLabelRow);

        // ── volume canvas ──────────────────────────────────────────────
        JPanel volCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(14, 12, 13));
                g2.fillRect(0, 0, w, h);

                java.util.List<PricePoint> pts = pointsHolder[0];
                if (pts == null || pts.size() < 2) { g2.dispose(); return; }

// sync zoom with price canvas — use shared zoom state directly
                int visStart = zoomStart[0];
                int visEnd = zoomEnd[0] >= 0 ? zoomEnd[0] : pts.size() - 1;
                visStart = Math.max(0, Math.min(visStart, pts.size()-1));
                visEnd = Math.max(visStart+1, Math.min(visEnd, pts.size()-1));
                java.util.List<PricePoint> visPts = pts.subList(visStart, visEnd+1);
                int n = visPts.size();

                long maxVol = 1;
                for (PricePoint p : visPts)
                    maxVol = Math.max(maxVol, Math.max(p.buyVolume, p.sellVolume));

                int ci = crosshairIdx[0];
                float barW = (float)(w) / (n * 2 + n + 1);
                if (barW < 1) barW = 1;

                for (int i = 0; i < n; i++) {
                    PricePoint p = visPts.get(i);
                    int x = (int)(i * (w - 1.0) / (n - 1));
                    boolean hovered = (i == ci);
                    float alpha = (!animating[0] && ci >= 0 && !hovered) ? 0.15f : 1.0f;

                    // buy bar
                    int bh = (int)((double)p.buyVolume / maxVol * h);
                    g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), (int)(255 * alpha)));
                    int bx2 = Math.max(0, x - (int)barW - 1);
                    g2.fillRect(bx2, h - bh, (int)barW, bh);

// sell bar
                    int sh = (int)((double)p.sellVolume / maxVol * h);
                    g2.setColor(new Color(74, 122, 191, (int)(255 * alpha)));
                    g2.fillRect(x + 1, h - sh, (int)barW, sh);
                }

                // draw drag-select rectangle on volCanvas
                boolean[] volIsDragging = (boolean[]) getClientProperty("isDragging");
                int[] volDragStart = (int[]) getClientProperty("dragStart");
                int[] volDragEnd = (int[]) getClientProperty("dragEnd");
                String volDragSource = (String) priceCanvas.getClientProperty("dragSource");
                if (volIsDragging != null && volIsDragging[0] && volDragStart != null && volDragEnd != null
                        && volDragStart[0] >= 0 && volDragEnd[0] >= 0 && "vol".equals(volDragSource)) {
                    int x1 = Math.min(volDragStart[0], volDragEnd[0]);
                    int x2 = Math.max(volDragStart[0], volDragEnd[0]);
                    g2.setColor(new Color(100, 140, 255, 60));
                    g2.fillRect(x1, 0, x2 - x1, h);
                    g2.setColor(new Color(100, 140, 255, 180));
                    g2.drawRect(x1, 0, x2 - x1, h);
                }

                // volume tooltips on hovered bar
                if (!animating[0] && ci >= 0 && ci < n) {
                    PricePoint cp = visPts.get(ci);
                    int x = (int)(ci * (w - 1.0) / (n - 1));
                    g2.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
                    FontMetrics fm = g2.getFontMetrics();

                    String buyStr = String.format("%,d", cp.buyVolume);
                    String sellStr = String.format("%,d", cp.sellVolume);
                    int buyLabelW = fm.stringWidth(buyStr) + 6;
                    int sellLabelW = fm.stringWidth(sellStr) + 6;
                    int labelH = 14;
                    int gap = 3;

                    boolean nearRight = x > w - buyLabelW - 6;
                    int lx = nearRight ? x - buyLabelW - 6 : x + 6;

                    int buyLabelY = 1;
                    int sellLabelY = buyLabelY + labelH + gap;

                    // buy volume label
                    g2.setColor(new Color(30, 25, 10));
                    g2.fillRect(lx, buyLabelY, buyLabelW, labelH);
                    g2.setColor(GOLD);
                    g2.drawRect(lx, buyLabelY, buyLabelW, labelH);
                    g2.drawString(buyStr, lx + 3, buyLabelY + labelH - 3);

                    // sell volume label
                    int sellLx = nearRight ? x - sellLabelW - 6 : x + 6;
                    g2.setColor(new Color(10, 15, 30));
                    g2.fillRect(sellLx, sellLabelY, sellLabelW, labelH);
                    g2.setColor(new Color(74, 122, 191));
                    g2.drawRect(sellLx, sellLabelY, sellLabelW, labelH);
                    g2.drawString(sellStr, sellLx + 3, sellLabelY + labelH - 3);
                }

                g2.dispose();
            }
        };
        volCanvas.setPreferredSize(new Dimension(1, 35));
        volCanvas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        volCanvasHolder[0] = volCanvas;
        volCanvas.setBackground(new Color(14, 12, 13));
        volCanvas.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(volCanvas);

// ── update markers canvas ──────────────────────────────────────
        final JPanel[] updateCanvasHolder = {null};
        final JPanel[] dateCanvasHolder = {null};
        JPanel updateCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(14, 12, 13));
                g2.fillRect(0, 0, w, h);

                if (config.gameUpdateMode() == GameUpdateMode.OFF || gameUpdates == null || gameUpdates.isEmpty())
                { g2.dispose(); return; }

                java.util.List<PricePoint> pts = pointsHolder[0];
                if (pts == null || pts.size() < 2) { g2.dispose(); return; }

                int visStart = zoomStart[0];
                int visEnd = zoomEnd[0] >= 0 ? zoomEnd[0] : pts.size() - 1;
                visStart = Math.max(0, Math.min(visStart, pts.size() - 1));
                visEnd = Math.max(visStart + 1, Math.min(visEnd, pts.size() - 1));
                java.util.List<PricePoint> visPts = pts.subList(visStart, visEnd + 1);

                long tMin = visPts.get(0).timestamp;
                long tMax = visPts.get(visPts.size() - 1).timestamp;
                if (tMax <= tMin) { g2.dispose(); return; }

                Integer hoveredIdx = (Integer) getClientProperty("hoveredUpdate");
                int dotY = 5; // center of dot strip (top 10px)

                for (int i = 0; i < gameUpdates.size(); i++) {
                    UpdateMarker u = gameUpdates.get(i);
                    if (config.gameUpdateMode() == GameUpdateMode.MAJOR_ONLY &&
                            !u.category.contains("game")) continue;
                    if (u.timestamp < tMin || u.timestamp > tMax) continue;

                    int x = (int)((double)(u.timestamp - tMin) / (tMax - tMin) * (w - 1));
                    Color dotColor = getUpdateColor(u.category);
                    boolean hovered = hoveredIdx != null && hoveredIdx == i;
                    int r = hovered ? 5 : 3;
                    g2.setColor(dotColor);
                    g2.fillOval(x - r, dotY - r, r * 2, r * 2);
                    if (hovered) {
                        g2.setColor(Color.WHITE);
                        g2.drawOval(x - r - 1, dotY - r - 1, r * 2 + 2, r * 2 + 2);

                        // draw tooltip in bottom 48px strip
                        g2.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
                        FontMetrics fm = g2.getFontMetrics();
                        java.time.LocalDate ld = java.time.Instant.ofEpochSecond(u.timestamp)
                                .atZone(java.time.ZoneOffset.UTC).toLocalDate();
                        String catLabel = u.category.equals("patch") ? "Patch Notes" :
                                u.category.equals("event") ? "Event" :
                                u.category.equals("poll") ? "Poll" : "Game Update";
                        String dateStr = ld.getDayOfMonth() + " "
                                + ld.getMonth().toString().substring(0,1)
                                + ld.getMonth().toString().substring(1,3).toLowerCase()
                                + " " + ld.getYear() + "  (" + catLabel + ")";
// wrap title across up to 2 lines instead of truncating
                        int pad = 5;
                        int lineH = fm.getHeight();
                        int maxBoxW = w - 8;
                        String titleLine1, titleLine2;
                        if (fm.stringWidth(u.title) <= maxBoxW - pad * 2) {
                            titleLine1 = u.title;
                            titleLine2 = null;
                        } else {
                            // find wrap point
                            int wrapAt = u.title.length();
                            for (int ci = u.title.length(); ci > 0; ci--) {
                                if (u.title.charAt(ci-1) == ' ' && fm.stringWidth(u.title.substring(0, ci-1)) <= maxBoxW - pad * 2) {
                                    wrapAt = ci - 1;
                                    break;
                                }
                            }
                            titleLine1 = u.title.substring(0, wrapAt);
                            titleLine2 = u.title.substring(wrapAt).trim();
                            if (fm.stringWidth(titleLine2) > maxBoxW - pad * 2)
                                titleLine2 = titleLine2.substring(0, Math.min(titleLine2.length(), 25)) + "...";
                        }
                        int line1W = fm.stringWidth(dateStr);
                        int line2W = fm.stringWidth(titleLine1);
                        int line3W = titleLine2 != null ? fm.stringWidth(titleLine2) : 0;
                        int maxTextW = Math.max(line1W, Math.max(line2W, line3W));
                        int boxW = Math.min(maxTextW + pad * 2, maxBoxW);
                        int boxH = titleLine2 != null ? lineH * 3 + 4 : lineH * 2 + 4;
                        int arrowH = 5;
                        int arrowW = 7;
                        int tooltipY = 10; // start of tooltip area
                        int cx = Math.max(arrowW, Math.min(w - arrowW, x));
                        int boxX = Math.max(2, Math.min(w - boxW - 2, cx - boxW / 2));
                        int boxY = tooltipY + arrowH;
                        // arrow
                        int[] ax = {cx - arrowW/2, cx, cx + arrowW/2};
                        int[] ay = {tooltipY + arrowH, tooltipY, tooltipY + arrowH};
                        g2.setColor(new Color(50, 45, 40));
                        g2.fillPolygon(ax, ay, 3);
                        // box background tinted
                        Color dc = dotColor;
                        g2.setColor(new Color(
                                Math.min(255, dc.getRed() / 4 + 20),
                                Math.min(255, dc.getGreen() / 4 + 15),
                                Math.min(255, dc.getBlue() / 4 + 15)));
                        g2.fillRect(boxX, boxY, boxW, boxH);
                        // box border
                        g2.setColor(dc.darker());
                        g2.drawRect(boxX, boxY, boxW, boxH);
                        // date (white)
                        g2.setColor(Color.WHITE);
                        g2.drawString(dateStr, boxX + pad, boxY + lineH - 2);
                        // title (tan)
                        g2.setColor(new Color(180, 165, 140));
                        g2.drawString(titleLine1, boxX + pad, boxY + lineH * 2 - 2);
                        if (titleLine2 != null)
                            g2.drawString(titleLine2, boxX + pad, boxY + lineH * 3 - 2);
                    }
                }
                g2.dispose();
            }
        };
        updateCanvas.setPreferredSize(new Dimension(1, 10));
        updateCanvas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        updateCanvas.setMinimumSize(new Dimension(0, 10));
        updateCanvas.setBackground(new Color(14, 12, 13));
        updateCanvas.setAlignmentX(Component.LEFT_ALIGNMENT);
        updateCanvasHolder[0] = updateCanvas;
        liveUpdateCanvas = updateCanvas;
        wrapper.add(updateCanvas);
        updateCanvas.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                if (gameUpdates == null || gameUpdates.isEmpty()) return;
                java.util.List<PricePoint> pts = pointsHolder[0];
                if (pts == null || pts.size() < 2) return;

                int visStart = zoomStart[0];
                int visEnd = zoomEnd[0] >= 0 ? zoomEnd[0] : pts.size() - 1;
                visStart = Math.max(0, Math.min(visStart, pts.size() - 1));
                visEnd = Math.max(visStart + 1, Math.min(visEnd, pts.size() - 1));
                java.util.List<PricePoint> visPts = pts.subList(visStart, visEnd + 1);
                long tMin = visPts.get(0).timestamp;
                long tMax = visPts.get(visPts.size() - 1).timestamp;
                if (tMax <= tMin) return;

                int w = updateCanvas.getWidth();
                int mx = e.getX();
                int my = e.getY();
                int threshold = 6;
                int found = -1;

// check if hovering existing pinned dot first
                Integer currentHovered = (Integer) updateCanvas.getClientProperty("hoveredUpdate");
                if (currentHovered != null && my > 10) {
                    // cursor is in tooltip area — keep current hovered dot
                    found = currentHovered;
                } else {
                    for (int i = 0; i < gameUpdates.size(); i++) {
                        UpdateMarker u = gameUpdates.get(i);
                        if (config.gameUpdateMode() == GameUpdateMode.MAJOR_ONLY && !u.category.contains("game")) continue;
                        if (u.timestamp < tMin || u.timestamp > tMax) continue;
                        int x = (int)((double)(u.timestamp - tMin) / (tMax - tMin) * (w - 1));
                        if (Math.abs(mx - x) <= threshold && my <= 12) { found = i; break; }
                    }
                }

                updateCanvas.putClientProperty("hoveredUpdate", found >= 0 ? found : null);
// expand/collapse canvas based on hover state
                int targetH = found >= 0 ? 72 : 10;
                if (updateCanvas.getPreferredSize().height != targetH) {
                    updateCanvas.setPreferredSize(new Dimension(1, targetH));
                    updateCanvas.setMaximumSize(new Dimension(Integer.MAX_VALUE, targetH));
                    updateCanvas.revalidate();
                }
                updateCanvas.repaint();

                if (found >= 0) {
                    UpdateMarker u = gameUpdates.get(found);
                    // find nearest visPts index for crosshair
                    int nearest = 0;
                    long minDiff = Long.MAX_VALUE;
                    for (int i = 0; i < visPts.size(); i++) {
                        long diff = Math.abs(visPts.get(i).timestamp - u.timestamp);
                        if (diff < minDiff) { minDiff = diff; nearest = i; }
                    }
                    crosshairIdx[0] = nearest;
                    if (priceCanvasHolder[0] != null) priceCanvasHolder[0].repaint();
                    if (volCanvasHolder[0] != null) volCanvasHolder[0].repaint();
                } else {
                    crosshairIdx[0] = -1;
                    if (priceCanvasHolder[0] != null) priceCanvasHolder[0].repaint();
                    if (volCanvasHolder[0] != null) volCanvasHolder[0].repaint();
                }
            }
        });

        updateCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!updateTooltipPinned) {
                    updateCanvas.putClientProperty("hoveredUpdate", null);
                    updateCanvas.setPreferredSize(new Dimension(1, 10));
                    updateCanvas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
                    updateCanvas.revalidate();
                    updateCanvas.repaint();
                    crosshairIdx[0] = -1;
                    dateCanvasHolder[0].putClientProperty("dateText", "");
                    dateCanvasHolder[0].putClientProperty("dateColor", null);
                    dateCanvasHolder[0].repaint();
                    if (priceCanvasHolder[0] != null) priceCanvasHolder[0].repaint();
                    if (volCanvasHolder[0] != null) volCanvasHolder[0].repaint();
                }
            }
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getButton() != java.awt.event.MouseEvent.BUTTON3) return;
                if (gameUpdates == null) return;
                Integer idx = (Integer) updateCanvas.getClientProperty("hoveredUpdate");
                if (idx == null) return;
                UpdateMarker u = gameUpdates.get(idx);
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(u.wikiUrl));
                } catch (Exception ex) {}
            }
        });

        // ── date label ─────────────────────────────────────────────────
        JPanel dateCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(14, 12, 13));
                g2.fillRect(0, 0, w, h);
                String text = (String) getClientProperty("dateText");
                if (text != null && !text.trim().isEmpty()) {
                    g2.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
                    FontMetrics fm = g2.getFontMetrics();
                    int textW = fm.stringWidth(text);
                    int pad = 5;
                    int boxW = textW + pad * 2;
                    int lineH = fm.getHeight();
                    int boxH = lineH;
                    int arrowH = 5;
                    int arrowW = 7;
                    Integer xPos = (Integer) getClientProperty("dateX");
                    int cx = xPos != null ? xPos : w / 2;
                    int boxX = Math.max(2, Math.min(w - boxW - 2, cx - boxW / 2));
                    int boxY = arrowH;
                    cx = Math.max(arrowW, Math.min(w - arrowW, cx));
                    // arrow
                    int[] ax = {cx - arrowW/2, cx, cx + arrowW/2};
                    int[] ay = {arrowH, 0, arrowH};
                    g2.setColor(new Color(50, 45, 40));
                    g2.fillPolygon(ax, ay, 3);
                    // box background
                    g2.setColor(new Color(30, 27, 25));
                    g2.fillRect(boxX, boxY, boxW, boxH);
                    // box border
                    g2.setColor(new Color(80, 72, 60));
                    g2.drawRect(boxX, boxY, boxW, boxH);
                    // date text (white)
                    g2.setColor(Color.WHITE);
                    g2.drawString(text, boxX + pad, boxY + lineH - 2);
                }
                g2.dispose();
            }
        };
        dateCanvas.setPreferredSize(new Dimension(1, 22));
        dateCanvas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        dateCanvas.setMinimumSize(new Dimension(0, 22));
        dateCanvas.setBackground(new Color(14, 12, 13));
        dateCanvas.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(dateCanvas);
        dateCanvasHolder[0] = dateCanvas;
        dateCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                updateTooltipPinned = true;
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                updateTooltipPinned = false;
                updateCanvas.putClientProperty("hoveredUpdate", null);
                updateCanvas.setPreferredSize(new Dimension(1, 10));
                updateCanvas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
                updateCanvas.revalidate();
                updateCanvas.repaint();
                crosshairIdx[0] = -1;
                dateCanvasHolder[0].putClientProperty("dateText", "");
                dateCanvasHolder[0].putClientProperty("dateColor", null);
                dateCanvasHolder[0].repaint();
                if (priceCanvasHolder[0] != null) priceCanvasHolder[0].repaint();
                if (volCanvasHolder[0] != null) volCanvasHolder[0].repaint();
            }
        });

// ── zoom hint text ─────────────────────────────────────────────
        JLabel zoomHint = new JLabel(
                config.chartZoomMode() == ChartZoomMode.DRAG_SELECT
                        ? "Drag to zoom · dbl-click resets"
                        : "Click & hold to zoom",
                SwingConstants.CENTER);
        zoomHint.setForeground(TEXT_DIM);
        zoomHint.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
        zoomHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        zoomHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(zoomHint);
        if (config.gameUpdateMode() != GameUpdateMode.OFF) {
            JLabel updateHint = new JLabel("Right-click update dot → Wiki ↗", SwingConstants.CENTER);
            updateHint.setForeground(TEXT_DIM);
            updateHint.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
            updateHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
            updateHint.setAlignmentX(Component.LEFT_ALIGNMENT);
            wrapper.add(updateHint);
        }
        wrapper.add(Box.createVerticalStrut(4));

// ── mouse interaction ──────────────────────────────────────────
        if (config.chartZoomMode() != ChartZoomMode.DRAG_SELECT) priceCanvas.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                java.util.List<PricePoint> pts = pointsHolder[0];
                if (animating[0] || pts == null || pts.size() < 2) return;
                int curStart = zoomStart[0];
                int curEnd = zoomEnd[0] >= 0 ? zoomEnd[0] : pts.size()-1;
                int visN = curEnd - curStart + 1;
                int w = priceCanvas.getWidth();
                int nearest = 0; double minDist = Double.MAX_VALUE;
                for (int i = 0; i < visN; i++) {
                    int px = (int)(i * (w - 1.0) / (visN - 1));
                    double dist = Math.abs(e.getX() - px);
                    if (dist < minDist) { minDist = dist; nearest = i; }
                }
                crosshairIdx[0] = nearest;
                PricePoint cp = pts.get(curStart + nearest);
                java.time.Instant inst = java.time.Instant.ofEpochSecond(cp.timestamp);
                java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(inst, java.time.ZoneId.of("UTC"));
                String fmt = (activeFrame[0].equals("1D") || activeFrame[0].equals("7D"))
                        ? String.format("%s %d %s %02d:%02d", ldt.getDayOfWeek().toString().substring(0,3), ldt.getDayOfMonth(), ldt.getMonth().toString().substring(0,3), ldt.getHour(), ldt.getMinute())
                        : String.format("%d %s %d", ldt.getDayOfMonth(),
                        ldt.getMonth().toString().substring(0,3), ldt.getYear());
                dateCanvas.putClientProperty("dateText", fmt);
                dateCanvas.putClientProperty("dateX", (int)(crosshairIdx[0] * (priceCanvas.getWidth()-1.0) / Math.max(zoomEnd[0] >= 0 ? zoomEnd[0]-zoomStart[0] : pointsHolder[0].size()-1, 1)));
                dateCanvas.repaint();
                priceCanvas.repaint();
                volCanvas.repaint();
                if (updateCanvasHolder[0] != null) updateCanvasHolder[0].repaint();
            }
        }); // end non-DRAG_SELECT mouseMoved
        if (config.chartZoomMode() == ChartZoomMode.MAGNIFIER) {
            priceCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                crosshairIdx[0] = -1;
                magnifying[0] = false;
                magnifyIdx[0] = -1;
                dateCanvas.putClientProperty("dateText", "");
                dateCanvas.repaint();
                priceCanvas.repaint();
                volCanvas.repaint();
                if (updateCanvasHolder[0] != null) updateCanvasHolder[0].repaint();
            }
                @Override
                public void mousePressed(MouseEvent e) {
                java.util.List<PricePoint> pts = pointsHolder[0];
                if (animating[0] || pts == null || pts.size() < 2) return;
                int n = pts.size();
                int w = priceCanvas.getWidth();
                int nearest = 0;
                double minDist = Double.MAX_VALUE;
                for (int i = 0; i < n; i++) {
                    int px = (int)(i * (w - 1.0) / (n - 1));
                    double dist = Math.abs(e.getX() - px);
                    if (dist < minDist) { minDist = dist; nearest = i; }
                }
                magnifyIdx[0] = nearest;
                crosshairIdx[0] = nearest;
                magnifying[0] = true;
                priceCanvas.repaint();
                volCanvas.repaint();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                magnifying[0] = false;
                magnifyIdx[0] = -1;
                priceCanvas.repaint();
            }
        });
        priceCanvas.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                java.util.List<PricePoint> pts = pointsHolder[0];
                if (!magnifying[0] || pts == null || pts.size() < 2) return;
                int n = pts.size();
                int w = priceCanvas.getWidth();
                int nearest = 0;
                double minDist = Double.MAX_VALUE;
                for (int i = 0; i < n; i++) {
                    int px = (int) (i * (w - 1.0) / (n - 1));
                    double dist = Math.abs(e.getX() - px);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = i;
                    }
                }
                magnifyIdx[0] = nearest;
                crosshairIdx[0] = nearest;
                PricePoint cp = pts.get(nearest);
                java.time.Instant inst = java.time.Instant.ofEpochSecond(cp.timestamp);
                java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(inst, java.time.ZoneId.of("UTC"));
                String fmt = (activeFrame[0].equals("1D") || activeFrame[0].equals("7D"))
                        ? String.format("%s %d %s %02d:%02d", ldt.getDayOfWeek().toString().substring(0,3), ldt.getDayOfMonth(), ldt.getMonth().toString().substring(0,3), ldt.getHour(), ldt.getMinute())
                        : String.format("%d %s %d", ldt.getDayOfMonth(),
                        ldt.getMonth().toString().substring(0,3), ldt.getYear());
                dateCanvas.putClientProperty("dateText", fmt);
                dateCanvas.putClientProperty("dateX", (int)(crosshairIdx[0] * (priceCanvas.getWidth()-1.0) / Math.max(zoomEnd[0] >= 0 ? zoomEnd[0]-zoomStart[0] : pointsHolder[0].size()-1, 1)));
                dateCanvas.repaint();
                priceCanvas.repaint();
                volCanvas.repaint();
                if (updateCanvasHolder[0] != null) updateCanvasHolder[0].repaint();
            }
        });
        } // end MAGNIFIER mode

// ── drag-to-select zoom ────────────────────────────────────────
        if (config.chartZoomMode() == ChartZoomMode.DRAG_SELECT) {
            final int[] dragStart = {-1};
            final int[] dragEnd = {-1};
            final boolean[] isDragging = {false};

            priceCanvas.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    crosshairIdx[0] = -1;
                    dateCanvas.putClientProperty("dateText", "");
                    dateCanvas.repaint();
                    priceCanvas.repaint();
                    volCanvas.repaint();
                    if (updateCanvasHolder[0] != null) updateCanvasHolder[0].repaint();
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == java.awt.event.MouseEvent.BUTTON2) {
                        isPanning[0] = true;
                        panStartX[0] = e.getX();
                        panStartY[0] = e.getY();
                        panZoomStart[0] = zoomStart[0];
                        panZoomEnd[0] = zoomEnd[0];
                        panBoxMinY[0] = zoomBoxMinY[0];
                        panBoxMaxY[0] = zoomBoxMaxY[0];
                        return;
                    }
                    isDragging[0] = true;
                    dragStart[0] = e.getX();
                    dragEnd[0] = e.getX();
                    dragStartY[0] = e.getY();
                    dragEndY[0] = e.getY();
                    priceCanvas.putClientProperty("dragSource", "price");
                    priceCanvas.repaint();
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (isPanning[0]) {
                        isPanning[0] = false;
                        panStartX[0] = -1; panStartY[0] = -1;
                        return;
                    }
                    if (!isDragging[0]) return;
                    isDragging[0] = false;
                    java.util.List<PricePoint> pts = pointsHolder[0];
                    if (pts == null || pts.size() < 2) { dragStart[0]=-1; dragEnd[0]=-1; priceCanvas.repaint(); return; }
                    int x1 = Math.min(dragStart[0], dragEnd[0]);
                    int x2 = Math.max(dragStart[0], dragEnd[0]);
                    if (x2 - x1 < 8) { dragStart[0]=-1; dragEnd[0]=-1; priceCanvas.repaint(); return; }
                    int w = priceCanvas.getWidth();
                    int totalN = pts.size();
                    int curStart = zoomStart[0];
                    int curEnd = zoomEnd[0] < 0 ? totalN - 1 : zoomEnd[0];
                    int visN = curEnd - curStart + 1;
                    int newStart = curStart + (int)(x1 * visN / (double)w);
                    int newEnd = curStart + (int)(x2 * visN / (double)w);
                    newStart = Math.max(0, newStart);
                    newEnd = Math.min(totalN - 1, newEnd);
                    if (newEnd - newStart < 2) { dragStart[0]=-1; dragEnd[0]=-1; priceCanvas.repaint(); return; }
                    zoomStart[0] = newStart;
                    zoomEnd[0] = newEnd;
                    // box zoom: if drag was sufficiently diagonal, lock Y axis too
                    int dy = Math.abs(dragEndY[0] - dragStartY[0]);
                    int dx = Math.abs(dragEnd[0] - dragStart[0]);
                    if (dragStartY[0] >= 0 && dy > dx * 0.25) {
                        // map Y pixels to price values using current fMin/fMax
                        // get current auto-scale bounds from visible points
                        int h = priceCanvas.getHeight();
                        long minP = Long.MAX_VALUE, maxP = Long.MIN_VALUE;
                        java.util.List<PricePoint> visPts = pts.subList(newStart, newEnd + 1);
                        for (PricePoint p : visPts) {
                            if (p.buyPrice  > 0) { minP = Math.min(minP, p.buyPrice);  maxP = Math.max(maxP, p.buyPrice); }
                            if (p.sellPrice > 0) { minP = Math.min(minP, p.sellPrice); maxP = Math.max(maxP, p.sellPrice); }
                        }
                        if (minP != Long.MAX_VALUE) {
                            long pad = Math.max((maxP - minP) / 3, 1);
                            minP -= pad; maxP += pad;
                            int y1 = Math.min(dragStartY[0], dragEndY[0]);
                            int y2 = Math.max(dragStartY[0], dragEndY[0]);
                            // invert: y=0 is top (high price), y=h is bottom (low price)
                            long boxMax = minP + (long)((1.0 - (double)y1 / h) * (maxP - minP));
                            long boxMin = minP + (long)((1.0 - (double)y2 / h) * (maxP - minP));
                            zoomBoxMinY[0] = boxMin;
                            zoomBoxMaxY[0] = boxMax;
                        }
                    } else {
                        // range zoom — clear any box zoom Y lock
                        zoomBoxMinY[0] = -1;
                        zoomBoxMaxY[0] = -1;
                    }
                    dragStart[0] = -1; dragEnd[0] = -1;
                    dragStartY[0] = -1; dragEndY[0] = -1;
                    priceCanvas.repaint(); volCanvas.repaint();
                    if (updateCanvasHolder[0] != null) updateCanvasHolder[0].repaint();
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        zoomStart[0] = 0; zoomEnd[0] = -1;
                        zoomBoxMinY[0] = -1; zoomBoxMaxY[0] = -1;
                        priceCanvas.repaint(); volCanvas.repaint();
                    }
                }
            });
            priceCanvas.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    java.util.List<PricePoint> pts = pointsHolder[0];
                    if (animating[0] || pts == null || pts.size() < 2) return;
                    int curStart = zoomStart[0];
                    int curEnd = zoomEnd[0] < 0 ? pts.size()-1 : zoomEnd[0];
                    int visN = curEnd - curStart + 1;
                    int w = priceCanvas.getWidth();
                    int nearest = 0; double minDist = Double.MAX_VALUE;
                    for (int i = 0; i < visN; i++) {
                        int px = (int)(i * (w - 1.0) / (visN - 1));
                        double dist = Math.abs(e.getX() - px);
                        if (dist < minDist) { minDist = dist; nearest = i; }
                    }
                    crosshairIdx[0] = nearest;
                    PricePoint cp = pts.get(curStart + nearest);
                    java.time.Instant inst = java.time.Instant.ofEpochSecond(cp.timestamp);
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(inst, java.time.ZoneId.of("UTC"));
                    String fmt = (activeFrame[0].equals("1D") || activeFrame[0].equals("7D"))
                            ? String.format("%s %d %s %02d:%02d", ldt.getDayOfWeek().toString().substring(0,3), ldt.getDayOfMonth(), ldt.getMonth().toString().substring(0,3), ldt.getHour(), ldt.getMinute())
                            : String.format("%d %s %d", ldt.getDayOfMonth(), ldt.getMonth().toString().substring(0,3), ldt.getYear());
                    dateCanvas.putClientProperty("dateText", fmt);
                    dateCanvas.putClientProperty("dateX", (int)(nearest * (priceCanvas.getWidth()-1.0) / Math.max(visN-1,1)));
                    dateCanvas.repaint();
                    priceCanvas.repaint(); volCanvas.repaint();
                    if (updateCanvasHolder[0] != null) updateCanvasHolder[0].repaint();
                }
                @Override
                public void mouseDragged(MouseEvent e) {
                    // pan with middle mouse button
                    if (isPanning[0]) {
                        java.util.List<PricePoint> allPts = pointsHolder[0];
                        if (allPts == null || allPts.size() < 2) return;
                        int totalN = allPts.size();
                        int w = priceCanvas.getWidth();
                        int visN = (panZoomEnd[0] < 0 ? totalN - 1 : panZoomEnd[0]) - panZoomStart[0] + 1;
                        // X pan: shift zoom window by pixel delta
                        int dx = e.getX() - panStartX[0];
                        int shiftX = (int)(dx * visN / (double)w);
                        int newStart = panZoomStart[0] - shiftX;
                        int newEnd = (panZoomEnd[0] < 0 ? totalN - 1 : panZoomEnd[0]) - shiftX;
                        // clamp to data bounds
                        if (newStart < 0) { newEnd -= newStart; newStart = 0; }
                        if (newEnd >= totalN) { newStart -= (newEnd - (totalN - 1)); newEnd = totalN - 1; }
                        newStart = Math.max(0, newStart);
                        newEnd = Math.min(totalN - 1, newEnd);
                        zoomStart[0] = newStart;
                        zoomEnd[0] = newEnd;
                        // Y pan: shift price range if box zoom is active
                        if (panBoxMinY[0] >= 0 && panBoxMaxY[0] >= 0) {
                            int h = priceCanvas.getHeight();
                            long priceRange = panBoxMaxY[0] - panBoxMinY[0];
                            int dy = e.getY() - panStartY[0];
                            long shiftY = (long)(dy * priceRange / (double)h);
                            zoomBoxMinY[0] = panBoxMinY[0] + shiftY;
                            zoomBoxMaxY[0] = panBoxMaxY[0] + shiftY;
                        }
                        priceCanvas.repaint(); volCanvas.repaint();
                        if (updateCanvasHolder[0] != null) updateCanvasHolder[0].repaint();
                        // update date label to reflect new pan position
                        if (crosshairIdx[0] >= 0 && allPts != null) {
                            int curStart = zoomStart[0];
                            int curEnd = zoomEnd[0] < 0 ? allPts.size()-1 : zoomEnd[0];
                            int idx = Math.min(curStart + crosshairIdx[0], curEnd);
                            idx = Math.max(0, Math.min(idx, allPts.size()-1));
                            PricePoint cp = allPts.get(idx);
                            java.time.Instant inst = java.time.Instant.ofEpochSecond(cp.timestamp);
                            java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(inst, java.time.ZoneId.of("UTC"));
                            String fmt = (activeFrame[0].equals("1D") || activeFrame[0].equals("7D"))
                                    ? String.format("%s %d %s %02d:%02d", ldt.getDayOfWeek().toString().substring(0,3), ldt.getDayOfMonth(), ldt.getMonth().toString().substring(0,3), ldt.getHour(), ldt.getMinute())
                                    : String.format("%d %s %d", ldt.getDayOfMonth(), ldt.getMonth().toString().substring(0,3), ldt.getYear());
                            dateCanvas.putClientProperty("dateText", fmt);
                            dateCanvas.repaint();
                        }
                        return;
                    }
                    if (!isDragging[0]) return;
                    dragEnd[0] = e.getX();
                    dragEndY[0] = e.getY();
                    // update crosshair to drag end position
                    java.util.List<PricePoint> pts = pointsHolder[0];
                    if (pts != null && pts.size() >= 2) {
                        int curStart = zoomStart[0];
                        int curEnd = zoomEnd[0] < 0 ? pts.size()-1 : zoomEnd[0];
                        int visN = curEnd - curStart + 1;
                        int w = priceCanvas.getWidth();
                        int nearest = 0; double minDist = Double.MAX_VALUE;
                        for (int i = 0; i < visN; i++) {
                            int px = (int)(i * (w - 1.0) / (visN - 1));
                            double dist = Math.abs(e.getX() - px);
                            if (dist < minDist) { minDist = dist; nearest = i; }
                        }
                        crosshairIdx[0] = nearest;
                        PricePoint cp = pts.get(curStart + nearest);
                        java.time.Instant inst = java.time.Instant.ofEpochSecond(cp.timestamp);
                        java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(inst, java.time.ZoneId.of("UTC"));
                        String fmt = (activeFrame[0].equals("1D") || activeFrame[0].equals("7D"))
                                ? String.format("%s %d %s %02d:%02d", ldt.getDayOfWeek().toString().substring(0,3), ldt.getDayOfMonth(), ldt.getMonth().toString().substring(0,3), ldt.getHour(), ldt.getMinute())
                                : String.format("%d %s %d", ldt.getDayOfMonth(), ldt.getMonth().toString().substring(0,3), ldt.getYear());
                        dateCanvas.putClientProperty("dateText", fmt);
                        dateCanvas.putClientProperty("dateX", (int)(nearest * (priceCanvas.getWidth()-1.0) / Math.max(visN-1,1)));
                        dateCanvas.repaint();
                    }
                    priceCanvas.repaint();
                    volCanvas.repaint();
                    if (updateCanvasHolder[0] != null) updateCanvasHolder[0].repaint();
                }
            });

            // store zoom state so paintComponent can use it
            priceCanvas.putClientProperty("isDragging", isDragging);
            priceCanvas.putClientProperty("dragStart", dragStart);
            priceCanvas.putClientProperty("dragEnd", dragEnd);
            volCanvas.putClientProperty("isDragging", isDragging);
            volCanvas.putClientProperty("dragStart", dragStart);
            volCanvas.putClientProperty("dragEnd", dragEnd);
        } // end DRAG_SELECT mode

// ── volume canvas mouse interaction ───────────────────────────
        // Add drag-to-zoom on volCanvas (mirrors priceCanvas drag-select)
        if (config.chartZoomMode() == ChartZoomMode.DRAG_SELECT) {
            volCanvas.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == java.awt.event.MouseEvent.BUTTON2) {
                        zoomStart[0] = 0; zoomEnd[0] = -1;
                        zoomBoxMinY[0] = -1; zoomBoxMaxY[0] = -1;
                        priceCanvas.repaint(); volCanvas.repaint(); return;
                    }
                    boolean[] isDragging = (boolean[]) priceCanvas.getClientProperty("isDragging");
                    int[] dragStart = (int[]) priceCanvas.getClientProperty("dragStart");
                    int[] dragEnd = (int[]) priceCanvas.getClientProperty("dragEnd");
                    if (isDragging == null || dragStart == null || dragEnd == null) return;
                    isDragging[0] = true;
                    dragStart[0] = e.getX();
                    dragEnd[0] = e.getX();
                    priceCanvas.putClientProperty("dragSource", "vol");
                    priceCanvas.repaint(); volCanvas.repaint();
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    boolean[] isDragging = (boolean[]) priceCanvas.getClientProperty("isDragging");
                    int[] dragStart = (int[]) priceCanvas.getClientProperty("dragStart");
                    int[] dragEnd = (int[]) priceCanvas.getClientProperty("dragEnd");
                    if (isDragging == null || !isDragging[0]) return;
                    isDragging[0] = false;
                    java.util.List<PricePoint> pts = pointsHolder[0];
                    if (pts == null || pts.size() < 2) { dragStart[0]=-1; dragEnd[0]=-1; priceCanvas.repaint(); volCanvas.repaint(); return; }
                    int x1 = Math.min(dragStart[0], dragEnd[0]);
                    int x2 = Math.max(dragStart[0], dragEnd[0]);
                    if (x2 - x1 < 8) { dragStart[0]=-1; dragEnd[0]=-1; priceCanvas.repaint(); volCanvas.repaint(); return; }
                    int w = volCanvas.getWidth();
                    int totalN = pts.size();
                    int curStart = zoomStart[0];
                    int curEnd = zoomEnd[0] < 0 ? totalN - 1 : zoomEnd[0];
                    int visN = curEnd - curStart + 1;
                    int newStart = curStart + (int)(x1 * visN / (double)w);
                    int newEnd = curStart + (int)(x2 * visN / (double)w);
                    newStart = Math.max(0, newStart);
                    newEnd = Math.min(totalN - 1, newEnd);
                    if (newEnd - newStart < 2) { dragStart[0]=-1; dragEnd[0]=-1; priceCanvas.repaint(); volCanvas.repaint(); return; }
                    zoomStart[0] = newStart;
                    zoomEnd[0] = newEnd;
                    dragStart[0] = -1; dragEnd[0] = -1;
                    priceCanvas.repaint(); volCanvas.repaint();
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        zoomStart[0] = 0; zoomEnd[0] = -1;
                        zoomBoxMinY[0] = -1; zoomBoxMaxY[0] = -1;
                        priceCanvas.repaint(); volCanvas.repaint();
                    }
                }
            });
            volCanvas.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    boolean[] isDragging = (boolean[]) priceCanvas.getClientProperty("isDragging");
                    int[] dragEnd = (int[]) priceCanvas.getClientProperty("dragEnd");
                    if (isDragging == null || !isDragging[0] || dragEnd == null) return;
                    dragEnd[0] = e.getX();
                    // update crosshair to follow drag position
                    java.util.List<PricePoint> pts = pointsHolder[0];
                    if (pts != null && pts.size() >= 2) {
                        int curStart = zoomStart[0];
                        int curEnd = zoomEnd[0] < 0 ? pts.size()-1 : zoomEnd[0];
                        int visN = curEnd - curStart + 1;
                        int w = volCanvas.getWidth();
                        int nearest = 0; double minDist = Double.MAX_VALUE;
                        for (int i = 0; i < visN; i++) {
                            int px = (int)(i * (w - 1.0) / (visN - 1));
                            double dist = Math.abs(e.getX() - px);
                            if (dist < minDist) { minDist = dist; nearest = i; }
                        }
                        crosshairIdx[0] = nearest;
                    }
                    priceCanvas.repaint(); volCanvas.repaint();
                }
            });
        }
        volCanvas.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                java.util.List<PricePoint> pts = pointsHolder[0];
                if (animating[0] || pts == null || pts.size() < 2) return;
                int curStart = zoomStart[0];
                int curEnd = zoomEnd[0] >= 0 ? zoomEnd[0] : pts.size()-1;
                int visN = curEnd - curStart + 1;
                int w = volCanvas.getWidth();
                int nearest = 0;
                double minDist = Double.MAX_VALUE;
                for (int i = 0; i < visN; i++) {
                    int px = (int)(i * (w - 1.0) / (visN - 1));
                    double dist = Math.abs(e.getX() - px);
                    if (dist < minDist) { minDist = dist; nearest = i; }
                }
                crosshairIdx[0] = nearest;
                PricePoint cp = pts.get(curStart + nearest);
                java.time.Instant inst = java.time.Instant.ofEpochSecond(cp.timestamp);
                java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(inst, java.time.ZoneId.of("UTC"));
                String fmt = (activeFrame[0].equals("1D") || activeFrame[0].equals("7D"))
                        ? String.format("%s %d %s %02d:%02d", ldt.getDayOfWeek().toString().substring(0,3), ldt.getDayOfMonth(), ldt.getMonth().toString().substring(0,3), ldt.getHour(), ldt.getMinute())
                        : String.format("%d %s %d", ldt.getDayOfMonth(),
                        ldt.getMonth().toString().substring(0,3), ldt.getYear());
                dateCanvas.putClientProperty("dateText", fmt);
                dateCanvas.putClientProperty("dateX", (int)(nearest * (priceCanvas.getWidth()-1.0) / Math.max(zoomEnd[0] >= 0 ? zoomEnd[0]-zoomStart[0] : pointsHolder[0].size()-1, 1)));
                dateCanvas.repaint();
                priceCanvas.repaint();
                volCanvas.repaint();
                if (updateCanvasHolder[0] != null) updateCanvasHolder[0].repaint();
            }
        });
        volCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                crosshairIdx[0] = -1;
                dateCanvas.putClientProperty("dateText", "");
                dateCanvas.repaint();
                priceCanvas.repaint();
                volCanvas.repaint();
                if (updateCanvasHolder[0] != null) updateCanvasHolder[0].repaint();
            }
        });

// ── timeframe button wiring ────────────────────────────────────
        for (int i = 0; i < frames.length; i++) {
            final String frame = frames[i];
            final int fi = i;
            tfBtns[i].addActionListener(e -> {
                if (frame.equals(activeFrame[0])) return;
                activeFrame[0] = frame;
                graphActiveTimeframe = frame;
                zoomStart[0] = 0;
                zoomEnd[0] = -1;
                zoomBoxMinY[0] = -1;
                zoomBoxMaxY[0] = -1;
                crosshairIdx[0] = -1;
                dateCanvas.putClientProperty("dateText", "");
                dateCanvas.repaint();

                // update button styles
                for (int j = 0; j < frames.length; j++) {
                    boolean a = frames[j].equals(frame);
                    tfBtns[j].setForeground(a ? GOLD : TAB_INACTIVE);
                    tfBtns[j].setBorder(a
                            ? BorderFactory.createLineBorder(GOLD)
                            : BorderFactory.createLineBorder(new Color(58, 53, 48)));
                }

                // load data then animate in
                pointsHolder[0] = null;
                priceCanvas.repaint();
                volCanvas.repaint();

                fetchTimeseries(itemId, frame, pts -> {
                    pointsHolder[0] = pts;
                    animating[0] = true;
                    revealW[0] = 0;
                    updateStatsLabels(pts, statsLabels);
                    javax.swing.Timer t = new javax.swing.Timer(16, null);
                    t.addActionListener(ev -> {
                        revealW[0] = Math.min(revealW[0] + 8, 100);
                        priceCanvas.repaint();
                        volCanvas.repaint();
                        if (revealW[0] >= 100) {
                            t.stop();
                            animating[0] = false;
                            revealW[0] = 100;
                            priceCanvas.repaint();
                            if (liveUpdateCanvas != null) liveUpdateCanvas.repaint();
                        }
                    });
                    t.start();
                });
            });
        }

        // ── initial data load ──────────────────────────────────────────
        fetchTimeseries(itemId, initialTimeframe, pts -> {
            pointsHolder[0] = pts;
            updateStatsLabels(pts, statsLabels);
            priceCanvas.repaint();
            volCanvas.repaint();
            if (liveUpdateCanvas != null) liveUpdateCanvas.repaint();
        });

        return wrapper;
    }

    private void updateStatsLabels(java.util.List<PricePoint> pts, JLabel[] labels) {
        if (pts == null || pts.size() < 2) return;
        long ovHigh = Long.MIN_VALUE, ovLow = Long.MAX_VALUE;
        long buyHigh = Long.MIN_VALUE, buyLow = Long.MAX_VALUE;
        long sellHigh = Long.MIN_VALUE, sellLow = Long.MAX_VALUE;
        for (PricePoint p : pts) {
            if (p.buyPrice > 0) {
                buyHigh = Math.max(buyHigh, p.buyPrice);
                buyLow = Math.min(buyLow, p.buyPrice);
                ovHigh = Math.max(ovHigh, p.buyPrice);
                ovLow = Math.min(ovLow, p.buyPrice);
            }
            if (p.sellPrice > 0) {
                sellHigh = Math.max(sellHigh, p.sellPrice);
                sellLow = Math.min(sellLow, p.sellPrice);
                ovHigh = Math.max(ovHigh, p.sellPrice);
                ovLow = Math.min(ovLow, p.sellPrice);
            }
        }
        long[] vals = {ovHigh, ovLow, buyHigh, buyLow, sellHigh, sellLow};
        for (int i = 0; i < 6; i++) {
            final long v = vals[i];
            final int idx = i;
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (v == Long.MIN_VALUE || v == Long.MAX_VALUE) {
                    labels[idx].setText("—");
                    labels[idx].setToolTipText(null);
                } else {
                    labels[idx].setText(formatPrice(String.valueOf(v)));
                    labels[idx].setToolTipText(String.format("%,d gp", v));
                }
            });
        }
    }
    private JPanel buildFloatStatRow(String label, String value, Color valueColor)
    {
        JPanel row = new JPanel(new java.awt.BorderLayout());
        row.setBackground(new Color(14, 12, 13));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

        JLabel labelText = new JLabel(label);
        labelText.setForeground(new Color(138, 134, 128));
        labelText.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JLabel valueText = new JLabel(value);
        valueText.setForeground(valueColor);
        valueText.setFont(new Font("Monospaced", Font.PLAIN, 11));

        labelText.setBorder(new EmptyBorder(0, 0, 0, 12));
        row.add(labelText, java.awt.BorderLayout.WEST);
        row.add(valueText, java.awt.BorderLayout.EAST);
        return row;
    }

    private JPanel buildStatBox(String label, String value, Color valueColor, String tooltip)
    {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(new Color(14, 12, 13));
        box.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel labelText = new JLabel(label.toUpperCase(), SwingConstants.CENTER);
        labelText.setForeground(TEXT_DIM);
        labelText.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
        labelText.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

        JLabel valueText = new JLabel(value, SwingConstants.CENTER);
        valueText.setForeground(valueColor);
        valueText.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_VALUE));
        valueText.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        if (tooltip != null) valueText.setToolTipText(tooltip);

        box.add(labelText);
        box.add(valueText);
        return box;
    }

    private JButton findWatchButton(java.awt.Container container)
    {
        for (java.awt.Component c : container.getComponents()) {
            if (c instanceof JButton) {
                String txt = ((JButton)c).getText();
                if (txt.equals("+ Watch") || txt.equals("✓ Watch") || txt.equals("- Unwatch"))
                    return (JButton)c;
            }
            if (c instanceof java.awt.Container) {
                JButton found = findWatchButton((java.awt.Container)c);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JButton buildFooterBtn(String text, boolean isGold)
    {
        JButton btn = new JButton(text);
        btn.setForeground(isGold ? GOLD : TAB_INACTIVE);
        btn.setBackground(BG_DETAIL);
        btn.setBorder(BorderFactory.createLineBorder(new Color(58, 53, 48)));
        btn.setFont(new Font("Monospaced", Font.PLAIN, FONT_BUTTON));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel buildPill(String text, boolean isUp)
    {
        JPanel pill = new JPanel();
        pill.setBackground(isUp ? new Color(10, 26, 10) : new Color(26, 10, 10));
        pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isUp ? new Color(42, 90, 42) : new Color(90, 42, 42)),
                new EmptyBorder(1, 5, 1, 5)
        ));

        JLabel label = new JLabel(text);
        label.setForeground(isUp ? GREEN_UP : RED_DOWN);
        label.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
        pill.add(label);

        return pill;
    }

    private JPanel buildRecentRow(String name, String meta)
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG_DARK);
        row.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel iconPanel = new JPanel();
        iconPanel.setPreferredSize(new Dimension(42, 42));
        iconPanel.setBackground(new Color(14, 12, 13));
        iconPanel.setOpaque(true);
        iconPanel.setBorder(BorderFactory.createLineBorder(new Color(42, 37, 40)));
        row.add(iconPanel, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(BG_DARK);
        info.setBorder(new EmptyBorder(6, 7, 6, 7));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_ITEM_NAME));

        JLabel metaLabel = new JLabel(meta);
        metaLabel.setForeground(new Color(106, 98, 88));
        metaLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));

        info.add(nameLabel);
        info.add(metaLabel);
        row.add(info, BorderLayout.CENTER);

        JLabel remove = new JLabel("✕");
        remove.setForeground(TEXT_DIM);
        remove.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
        remove.setBorder(new EmptyBorder(0, 0, 0, 8));
        remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.add(remove, BorderLayout.EAST);

        row.addMouseListener(new MouseAdapter()
        {
            public void mouseEntered(MouseEvent e)
            {
                row.setBackground(BG_ROW_HOVER);
                info.setBackground(BG_ROW_HOVER);
            }
            public void mouseExited(MouseEvent e)
            {
                row.setBackground(BG_DARK);
                info.setBackground(BG_DARK);
            }
        });

        return row;
    }

    private String formatFullPrice(String price)
    {
        try
        {
            long val = Long.parseLong(price.replace(",", ""));
            return String.format("%,d", val);
        }
        catch (NumberFormatException e)
        {
            return price;
        }
    }
    private long parseGpChange(String gpChangeStr)
    {
        try
        {
            String clean = gpChangeStr.replace("+", "").replace(" gp", "").replace(",", "").trim();
            if (clean.endsWith("B")) return (long)(Double.parseDouble(clean.replace("B", "")) * 1_000_000_000);
            if (clean.endsWith("M")) return (long)(Double.parseDouble(clean.replace("M", "")) * 1_000_000);
            if (clean.endsWith("K")) return (long)(Double.parseDouble(clean.replace("K", "")) * 1_000);
            return Long.parseLong(clean);
        }
        catch (NumberFormatException e) { return 0; }
    }
    private String formatPrice(String price)
    {
        try
        {
            long val = Long.parseLong(price.replace(",", ""));
            if (val >= 1_000_000_000) return String.format("%.3fB", val / 1_000_000_000.0);
            if (val >= 1_000_000) return String.format("%.2fM", val / 1_000_000.0);
            if (val >= 1_000) return String.format("%,d", val);
            return String.valueOf(val);
        }
        catch (NumberFormatException e)
        {
            return price;
        }
    }
}