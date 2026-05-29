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

    private final GECompanionConfig config;
    private final GECompanionPlugin plugin;

    private int activeTab = 1;
    public int getActiveTab() { return activeTab; }
    private String activeTimeFrame = "24H";
    private boolean bankAllItemsCollapsed = true;
    private JScrollPane searchScrollPane;
    private JScrollPane watchlistScrollPane;
    private JScrollPane bankScrollPane;
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

    // Live price data
    private java.util.Map<Integer, PriceData> priceCache = new java.util.HashMap<>();
    private java.util.Map<String, Integer> nameToId = new java.util.HashMap<>();
    private java.util.Map<Integer, Long> avgPrice24h = new java.util.HashMap<>();
    private java.util.Map<Integer, Long> avgPrice1h = new java.util.HashMap<>();
    private java.util.Map<Integer, Long> avgPrice6h = new java.util.HashMap<>();
    private java.util.Map<Integer, Integer> itemLimits = new java.util.HashMap<>();
    private java.util.Map<Integer, Long> buyVolume1h = new java.util.HashMap<>();
    private java.util.Map<Integer, Long> sellVolume1h = new java.util.HashMap<>();

    // Pinned/watched items
    private final java.util.Map<Integer, javax.swing.ImageIcon> iconCache = new java.util.HashMap<>();
    private final java.util.List<String> pinnedItems = new java.util.ArrayList<>();
    private final java.util.List<String> bankItems = new java.util.ArrayList<>();
    private final java.util.Map<String, Integer> bankQuantities = new java.util.HashMap<>();

    // Inline detail tracking
    private JPanel currentOpenSearchDetail = null;
    private JPanel currentOpenWatchlistDetail = null;
    private JPanel currentOpenBankDetail = null;
    private JPanel currentOpenSearchRow = null;
    private JPanel currentOpenWatchlistRow = null;
    private JPanel currentOpenBankRow = null;
    private Color currentOpenSearchRowColor = BG_DARK;
    private Color currentOpenWatchlistRowColor = BG_DARK;
    private Color currentOpenBankRowColor = BG_DARK;
    private Color currentOpenBankBorderColor = null;
private JPanel currentOpenBankInfo = null;
private JPanel currentOpenBankDeltaRow = null;
private String openBankItemName = null;

    // Mock item database
    private static final String[][] ITEMS = {
            {"Twisted bow", "1,585,016,000", "+1,482,000", "7", "7", "-0.21"},
            {"Tumeken's shadow", "867,000,000", "+320,000", "7", "5", "+0.10"},
            {"Scythe of vitur", "1,469,092,000", "-890,000", "2", "2", "-0.41"},
            {"Saradomin brew(4)", "8,189", "+26", "19,449", "18,200", "+0.33"},
            {"Runite bar", "12,387", "-240", "22,064", "20,000", "-1.94"},
            {"Zaryte crossbow", "362,653,000", "+44,000", "5", "4", "0.00"},
            {"Bandos chestplate", "18,200,000", "+44,000", "22", "18", "+0.15"},
            {"Amulet of torture", "88,200,000", "+95,000", "8", "7", "+0.11"},
            {"Abyssal whip", "2,850", "+12", "18,200", "16,000", "-0.07"},
            {"Dragon dagger", "17,800", "+90", "5,800", "5,200", "+0.03"},
            {"Armadyl crossbow", "42,500,000", "+120,000", "8", "7", "+0.28"},
            {"Berserker ring", "3,200,000", "-15,000", "70", "65", "-0.47"},
            {"Dragon warhammer", "49,000,000", "+200,000", "8", "7", "+0.41"},
            {"Ancestral robe top", "74,000,000", "+350,000", "8", "6", "+0.47"},
            {"Kodai wand", "88,000,000", "-100,000", "8", "7", "-0.11"},
    };

    public GECompanionPanel(GECompanionConfig config, GECompanionPlugin plugin)
    {
        this.config = config;
        this.plugin = plugin;
        loadBankData();
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

    public void updateBankItems(java.util.List<String> items, java.util.Map<String, Integer> quantities, long bankValue)
    {
        this.bankItems.clear();
        this.bankItems.addAll(items);
        this.bankQuantities.clear();
        this.bankQuantities.putAll(quantities);
        this.totalBankValue = bankValue;
        if (config.showBankValueChange()) saveBankValueLog();
        saveBankData();
        if (activeTab == 2)
        {
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

    public void onPricesUpdated(java.util.Map<Integer, PriceData> priceCache, java.util.Map<String, Integer> nameToId, java.util.Map<Integer, Long> avgPrice24h, java.util.Map<Integer, Long> avgPrice1h, java.util.Map<Integer, Long> avgPrice6h, java.util.Map<Integer, Integer> itemLimits, java.util.Map<Integer, Long> buyVolume1h, java.util.Map<Integer, Long> sellVolume1h)
    {
        this.priceCache = priceCache;
        this.nameToId = nameToId;
        this.avgPrice24h = avgPrice24h;
        this.avgPrice1h = avgPrice1h;
        this.avgPrice6h = avgPrice6h;
        this.itemLimits = itemLimits;
        this.buyVolume1h = buyVolume1h;
        this.sellVolume1h = sellVolume1h;
        secondsSinceRefresh = 0;

        // Preserve open detail panel state before refresh
        String savedSearchItem = selectedItemName;
        String savedWatchlistItem = selectedWatchlistItemName;
        String savedBankItem = selectedBankItemName;

        isRefreshing = true;
        showTab(activeTab);
        isRefreshing = false;

// Restore open detail panel state after refresh
        selectedItemName = savedSearchItem;
        selectedWatchlistItemName = savedWatchlistItem;
        selectedBankItemName = savedBankItem;

// Reopen detail panel for Search tab
        if (!isRefreshing && savedSearchItem != null && activeTab == 1 && searchReopenAction != null)
        {
            javax.swing.Timer reopenTimer = new javax.swing.Timer(50, e2 -> searchReopenAction.run());
            reopenTimer.setRepeats(false);
            reopenTimer.start();
        }

        // Reopen detail panel for Watchlist tab
        if (!isRefreshing && savedWatchlistItem != null && activeTab == 0 && watchlistReopenAction != null)
        {
            javax.swing.Timer reopenTimer = new javax.swing.Timer(150, e2 -> {
                watchlistReopenAction.run();
            });
            reopenTimer.setRepeats(false);
            reopenTimer.start();
        }

        // Reopen detail panel for Bank tab
        if (!isRefreshing && savedBankItem != null && activeTab == 2 && bankReopenAction != null)
        {
            javax.swing.Timer reopenTimer = new javax.swing.Timer(150, e2 -> bankReopenAction.run());
            reopenTimer.setRepeats(false);
            reopenTimer.start();
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
            String[] parts = entry.split(":");
            if (parts.length == 2)
            {
                try
                {
                    long ts = Long.parseLong(parts[0]);
                    long val = Long.parseLong(parts[1]);
                    bankValueLog.add(new long[]{ts, val});
                }
                catch (NumberFormatException e) { }
            }
        }
    }

    private void saveBankValueLog()
    {
        long nowSeconds = System.currentTimeMillis() / 1000;
        bankValueLog.add(new long[]{nowSeconds, totalBankValue});
        long cutoff = nowSeconds - (48 * 3600);
        bankValueLog.removeIf(e -> e[0] < cutoff);
        StringBuilder sb = new StringBuilder();
        for (long[] entry : bankValueLog)
        {
            if (sb.length() > 0) sb.append(",");
            sb.append(entry[0]).append(":").append(entry[1]);
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
        showTab(1);
        activeTab = 1;
        wrapper.add(tabContentPanel, BorderLayout.CENTER);


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

    // ── SHARED DETAIL PANEL BUILDER ──
    private JPanel buildInlineDetail(String[] item, boolean isWatchlist)
    {
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

        JPanel inner = new JPanel()
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, new Color(212, 175, 55, 180), getWidth(), 0, new Color(0, 0, 0, 0));
                g2.setPaint(gp);
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                g2.dispose();
            }
        };
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(BG_DETAIL);
        inner.setBorder(new EmptyBorder(6, 7, 6, 7));

        inner.add(buildDetailHeader(name, price, item.length > 6 ? item[6] : "?"));
        inner.add(Box.createVerticalStrut(6));

// Full width Last Traded stat box
        String lastTradedPrice = item.length > 10 ? item[10] : "0";
        String lastTradedTime = item.length > 11 ? item[11] : "";
        String lastTradedLabel = "LAST TRADED" + (lastTradedTime.isEmpty() || lastTradedTime.equals("unknown") ? "" : "  ·  " + lastTradedTime);
        String lastTradedDisplay = lastTradedPrice.equals("0") ? "?" : formatFullPrice(lastTradedPrice) + " gp";

        JPanel lastTradedBox = new JPanel();
        lastTradedBox.setLayout(new BoxLayout(lastTradedBox, BoxLayout.Y_AXIS));
        lastTradedBox.setBackground(new Color(14, 12, 13));
        lastTradedBox.setBorder(new EmptyBorder(6, 5, 6, 5));
        lastTradedBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        lastTradedBox.setMaximumSize(new Dimension(225, 52));
        lastTradedBox.setMinimumSize(new Dimension(225, 52));
        lastTradedBox.setPreferredSize(new Dimension(225, 52));

        JLabel lastTradedLabelComp = new JLabel(lastTradedLabel, SwingConstants.CENTER);
        lastTradedLabelComp.setForeground(TEXT_DIM);
        lastTradedLabelComp.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
        lastTradedLabelComp.setAlignmentX(Component.CENTER_ALIGNMENT);
        lastTradedLabelComp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

        JLabel lastTradedValueComp = new JLabel(lastTradedDisplay, SwingConstants.CENTER);
        lastTradedValueComp.setForeground(STAT_GOLD);
        lastTradedValueComp.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_VALUE));
        lastTradedValueComp.setAlignmentX(Component.CENTER_ALIGNMENT);
        lastTradedValueComp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        lastTradedBox.add(lastTradedLabelComp);
        lastTradedBox.add(lastTradedValueComp);
        inner.add(lastTradedBox);
        inner.add(Box.createVerticalStrut(4));

        JPanel grid = new JPanel(new GridLayout(2, 2, 2, 2));
        grid.setBackground(BG_DETAIL);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(225, 90));

        grid.add(buildStatBox("Buy Price", item[2].equals("0") ? "?" : formatPrice(item[2]), STAT_GOLD, item[2].equals("0") ? null : formatFullPrice(item[2]) + " gp"));
        grid.add(buildStatBox("Sell Price", item[3].equals("0") ? "?" : formatPrice(item[3]), STAT_GOLD, item[3].equals("0") ? null : formatFullPrice(item[3]) + " gp"));
        grid.add(buildStatBox("Buy Qty/hr", item.length > 8 ? item[8] : "?", STAT_GOLD, null));
        grid.add(buildStatBox("Sell Qty/hr", item.length > 9 ? item[9] : "?", STAT_GOLD, null));

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
        if (activeTab == 2) showTab(2);
            // Update button live
            boolean nowWatched = pinnedItems.contains(item[0]);
            watchBtn.setText(isWatchlist ? "- Unwatch" : (nowWatched ? "✓ Watch" : "+ Watch"));
            watchBtn.setBorder(BorderFactory.createLineBorder(
                (isWatchlist || nowWatched) ? GOLD : new Color(58, 53, 48)
            ));
            watchBtn.setForeground(nowWatched || isWatchlist ? GOLD : TAB_INACTIVE);
        });
        footer.add(watchBtn);

        JButton trackerBtn = buildFooterBtn("Tracker ↗", false);
        trackerBtn.addActionListener(e -> {
            try {
                String urlName = name.toLowerCase().replace("'", "-").replace(" ", "-").replace("(", "-").replace(")", "").replace("--", "-");
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.ge-tracker.com/item/" + urlName));
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
        searchWrap.setBackground(new Color(28, 25, 26));
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

        searchScrollPane = new JScrollPane(listWrapper);
        JScrollPane scrollPane = searchScrollPane;
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG_DARK);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.getViewport().addMouseWheelListener(e -> {
            javax.swing.JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getValue() + (int)(e.getUnitsToScroll() * 8));
        });
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollPane.addMouseWheelListener(e -> scrollPane.getVerticalScrollBar().setValue(
            scrollPane.getVerticalScrollBar().getValue() + (int)(e.getUnitsToScroll() * 8)));
        scrollPane.getViewport().addMouseWheelListener(e -> scrollPane.getVerticalScrollBar().setValue(
            scrollPane.getVerticalScrollBar().getValue() + (int)(e.getUnitsToScroll() * 8)));

        panel.add(scrollPane, BorderLayout.CENTER);

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
        Color rowBg = (index % 2 == 0) ? BG_DARK : new Color(28, 25, 26);

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
        JPanel detailSlot = new JPanel(new BorderLayout());
        detailSlot.setBackground(BG_DARK);
        detailSlot.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        detailSlot.setVisible(false);
        detailSlot.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                // Toggle — click same item to close
                if (name.equals(selectedItemName) && currentOpenSearchDetail != null)
                {
                    currentOpenSearchDetail.setVisible(false);
                    currentOpenSearchDetail = null;
                    if (currentOpenSearchRow != null)
                    {
                        currentOpenSearchRow.setBackground(currentOpenSearchRowColor);
                        currentOpenSearchRow.setBorder(new MatteBorder(0, 3, 0, 1, new Color(26, 24, 24)));
                        for (Component c : currentOpenSearchRow.getComponents())
                        {
                            if (c instanceof JPanel)
                            {
                                c.setBackground(currentOpenSearchRowColor);
                                for (Component cc : ((JPanel)c).getComponents())
                                    if (cc instanceof JPanel) cc.setBackground(currentOpenSearchRowColor);
                            }
                        }
                        currentOpenSearchRow = null;
                        currentOpenSearchRowColor = BG_DARK;
                        currentOpenSearchRow = null;
                    }
                    selectedItemName = null;
                    searchReopenAction = null;
                    searchResultsPanel.revalidate();
                    searchResultsPanel.repaint();
                    return;
                }

                // Close previous
                if (currentOpenSearchDetail != null)
                {
                    currentOpenSearchDetail.setVisible(false);
                }
                if (currentOpenSearchRow != null)
                {
                    currentOpenSearchRow.setBackground(BG_DARK);
                    currentOpenSearchRow.setBorder(new MatteBorder(0, 3, 0, 1, new Color(26, 24, 24)));
                    for (Component c : currentOpenSearchRow.getComponents())
                        if (c instanceof JPanel) c.setBackground(BG_DARK);
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
                suppressSearchChange = true;
                searchField.setText(name);
                searchField.selectAll();
                suppressSearchChange = false;

                detailSlot.removeAll();
                detailSlot.add(buildInlineDetail(item, false), BorderLayout.CENTER);
                detailSlot.setVisible(true);
                scheduleRepaint(detailSlot);
                final String[] itemData = item;
                searchReopenAction = () -> {
                    for (java.awt.Component c : searchResultsPanel.getComponents())
                    {
                        if (!(c instanceof JPanel)) continue;
                        JPanel block = (JPanel) c;
                        if (block.getComponentCount() < 2) continue;
                        java.awt.Component second = block.getComponent(1);
                        if (!(second instanceof JPanel)) continue;
                        JPanel newDetailSlot = (JPanel) second;
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
                                    newDetailSlot.setVisible(true);
                                    scheduleRepaint(newDetailSlot);
                                    currentOpenSearchDetail = newDetailSlot;
                                    selectedItemName = itemData[0];
                                    newRow.setBorder(BorderFactory.createCompoundBorder(
                                            new MatteBorder(0, 3, 0, 0, GOLD),
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
                    new MatteBorder(0, 3, 0, 0, GOLD),
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
        block.add(detailSlot);
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

        JLabel pinnedLabel = new JLabel("Pinned Items");
        pinnedLabel.setForeground(TEXT_DIM);
        pinnedLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
        pinnedLabel.setBorder(new EmptyBorder(4, 7, 2, 7));
        pinnedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        listPanel.add(pinnedLabel);

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

        watchlistScrollPane = new JScrollPane(listPanel);
        JScrollPane scrollPane = watchlistScrollPane;
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG_DARK);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.getViewport().addMouseWheelListener(e -> {
            javax.swing.JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getValue() + (int)(e.getUnitsToScroll() * 8));
        });
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollPane.addMouseWheelListener(e -> scrollPane.getVerticalScrollBar().setValue(
            scrollPane.getVerticalScrollBar().getValue() + (int)(e.getUnitsToScroll() * 8)));
        scrollPane.getViewport().addMouseWheelListener(e -> scrollPane.getVerticalScrollBar().setValue(
            scrollPane.getVerticalScrollBar().getValue() + (int)(e.getUnitsToScroll() * 8)));

        panel.add(buildTimeFrameBar(), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
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

        Color rowBg = (index % 2 == 0) ? BG_DARK : new Color(28, 25, 26);

        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBackground(rowBg);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(javax.swing.BorderFactory.createEmptyBorder());

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(rowBg);
        row.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel iconPanel = new JPanel(new java.awt.GridBagLayout());
        iconPanel.setPreferredSize(new Dimension(42, 42));
        iconPanel.setBackground(new Color(14, 12, 13));
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
        // Inline detail slot
        JPanel detailSlot = new JPanel(new BorderLayout());
        detailSlot.setBackground(BG_DARK);
        detailSlot.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        detailSlot.setVisible(false);
        detailSlot.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                // Toggle
                if (name.equals(selectedWatchlistItemName) && currentOpenWatchlistDetail != null)
                {
                    currentOpenWatchlistDetail.setVisible(false);
                    currentOpenWatchlistDetail = null;
                    if (currentOpenWatchlistRow != null)
                    {
                        currentOpenWatchlistRow.setBackground(BG_DARK);
                        currentOpenWatchlistRow.setBorder(javax.swing.BorderFactory.createEmptyBorder());
                        for (Component c : currentOpenWatchlistRow.getComponents())
                        {
                            if (c instanceof JPanel)
                            {
                                c.setBackground(BG_DARK);
                                for (Component cc : ((JPanel)c).getComponents())
                                    if (cc instanceof JPanel) cc.setBackground(BG_DARK);
                            }
                        }
                        currentOpenWatchlistRow = null;
                    }
                    selectedWatchlistItemName = null;
                    watchlistReopenAction = null;
                    return;
                }

                // Close previous
                if (currentOpenWatchlistDetail != null)
                    currentOpenWatchlistDetail.setVisible(false);
                if (currentOpenWatchlistRow != null)
                {
                    currentOpenWatchlistRow.setBackground(currentOpenWatchlistRowColor);
                    currentOpenWatchlistRow.setBorder(javax.swing.BorderFactory.createEmptyBorder());
                    for (Component c : currentOpenWatchlistRow.getComponents())
                        if (c instanceof JPanel) c.setBackground(currentOpenWatchlistRowColor);
                }

                // Open this one
                selectedWatchlistItemName = name;
                currentOpenWatchlistRow = row;
                currentOpenWatchlistRowColor = rowBg;
                currentOpenWatchlistDetail = detailSlot;

                detailSlot.removeAll();
                detailSlot.add(buildInlineDetail(item, true), BorderLayout.CENTER);
                detailSlot.setVisible(true);
                scheduleRepaint(detailSlot);
                final String[] itemData = item;
                watchlistReopenAction = () -> {
                    for (java.awt.Component c : watchlistListPanel.getComponents())
                    {
                        if (!(c instanceof JPanel)) continue;
                        JPanel block = (JPanel) c;
                        if (block.getComponentCount() < 2) continue;
                        java.awt.Component second = block.getComponent(1);
                        if (!(second instanceof JPanel)) continue;
                        JPanel newDetailSlot = (JPanel) second;
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
                                    newDetailSlot.setVisible(true);
                                    scheduleRepaint(newDetailSlot);
                                    currentOpenWatchlistDetail = newDetailSlot;
                                    selectedWatchlistItemName = itemData[0];
                                    newRow.setBorder(new MatteBorder(0, 3, 0, 0, GOLD));
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
                row.setBorder(new MatteBorder(0, 3, 0, 0, GOLD));
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
                }
            }
        });

        block.add(row);
        block.add(detailSlot);
        return block;
    }

    // ── BANK TAB ──
    private JPanel buildBankTab()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);

        JPanel hero = new JPanel();
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setBackground(new Color(26, 23, 24));
        hero.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel heroLabel = new JLabel("Total Bank Value (approx.)");
        heroLabel.setForeground(TEXT_DIM);
        heroLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
        heroLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

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

        String bankValueStr = totalBankValue == 0 ? "No bank data" : formatFullPrice(String.valueOf(totalBankValue)) + " gp";
        JLabel heroValue = new JLabel(bankValueStr);
        heroValue.setForeground(PRICE_GOLD);
        heroValue.setFont(new Font("Monospaced", Font.BOLD, 20));
        heroValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (!bankItems.isEmpty()) heroValue.setToolTipText(formatFullPrice(String.valueOf(totalBankValue)) + " gp");

        nowSeconds = System.currentTimeMillis() / 1000;
        long targetSeconds = nowSeconds;
        if (activeTimeFrame.equals("1H")) targetSeconds = nowSeconds - 3600;
        else if (activeTimeFrame.equals("6H")) targetSeconds = nowSeconds - 21600;
        else if (activeTimeFrame.equals("24H")) targetSeconds = nowSeconds - 86400;

        long tolerance = 1800;
        if (activeTimeFrame.equals("6H")) tolerance = 7200;
        else if (activeTimeFrame.equals("24H")) tolerance = 14400;

        long[] closestEntry = null;
        long closestDiff = Long.MAX_VALUE;
        for (long[] entry : bankValueLog)
        {
            long diff = Math.abs(entry[0] - targetSeconds);
            if (diff < closestDiff && diff <= tolerance)
            {
                closestDiff = diff;
                closestEntry = entry;
            }
        }

        String bankChangeStr;
        Color bankChangeColor;
        String noDataMessage;
        if (activeTimeFrame.equals("1H")) noDataMessage = "Open your bank again in ~1H";
        else if (activeTimeFrame.equals("6H")) noDataMessage = "Open your bank again in ~6H";
        else noDataMessage = "Open your bank again in ~24H";

        if (closestEntry == null || bankItems.isEmpty())
        {
            bankChangeStr = "─ " + noDataMessage;
            bankChangeColor = TEXT_DIM;
        }
        else
        {
            long historicalValue = closestEntry[1];
            long bankGpChange = totalBankValue - historicalValue;
            double bankPctChange = historicalValue > 0 ?
                    ((double) bankGpChange / historicalValue) * 100.0 : 0;
            String gpStr = bankGpChange >= 0 ?
                    "+" + formatPrice(String.valueOf(Math.abs(bankGpChange))) + " gp" :
                    "-" + formatPrice(String.valueOf(Math.abs(bankGpChange))) + " gp";
            String pctStr = String.format("%+.2f%%", bankPctChange);
            bankChangeStr = gpStr + "  (" + pctStr + ")";
            bankChangeColor = bankGpChange > 0 ? GREEN_UP :
                    bankGpChange < 0 ? RED_DOWN : TEXT_DIM;
        }

        if (config.showBankValueChange())
        {
            JLabel bankChangeLabel = new JLabel(bankItems.isEmpty() ? "" : bankChangeStr);
            bankChangeLabel.setForeground(bankChangeColor);
            bankChangeLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
            bankChangeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            bankChangeLabel.setToolTipText("Bank value change based on actual scans. Open your bank regularly for accurate data.");

            JLabel contextLabel = new JLabel("· " + activeTimeFrame + " · " + lastUpdatedStr);
            contextLabel.setForeground(TEXT_DIM);
            contextLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
            contextLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            hero.add(heroValue);
            hero.add(Box.createVerticalStrut(2));
            hero.add(bankChangeLabel);
            hero.add(contextLabel);
            hero.add(Box.createVerticalStrut(4));
        }
        else
        {
            hero.add(heroValue);
            hero.add(Box.createVerticalStrut(4));
        }

        JPanel pillPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        pillPanel.setBackground(new Color(26, 23, 24));

        hero.add(pillPanel);
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
            gainersHeader.setBackground(new Color(20, 18, 18));
            gainersHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            gainersHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            JLabel gainersLabel = new JLabel("▲ Top Gainers (by " + config.sortMode().getLabel() + ")");
            gainersLabel.setForeground(GREEN_UP);
            gainersLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
            gainersLabel.setBorder(new EmptyBorder(6, 7, 2, 7));
            gainersHeader.add(gainersLabel, BorderLayout.WEST);
            listPanel.add(gainersHeader);
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
            for (String name : bankItems)
            {
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
            listPanel.add(new JSeparator());
            JPanel losersHeader = new JPanel(new BorderLayout());
            losersHeader.setBackground(new Color(20, 18, 18));
            losersHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            losersHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            JLabel losersLabel = new JLabel("▼ Top Losers (by " + config.sortMode().getLabel() + ")");
            losersLabel.setForeground(RED_DOWN);
            losersLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
            losersLabel.setBorder(new EmptyBorder(6, 7, 2, 7));
            losersHeader.add(losersLabel, BorderLayout.WEST);
            listPanel.add(losersHeader);

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
            bankSearchWrap.setBackground(new Color(28, 25, 26));
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

        bankScrollPane = new JScrollPane(listPanel);
        JScrollPane scrollPane = bankScrollPane;
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG_DARK);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.getViewport().addMouseWheelListener(e -> {
            javax.swing.JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getValue() + (int)(e.getUnitsToScroll() * 8));
        });
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollPane.addMouseWheelListener(e -> scrollPane.getVerticalScrollBar().setValue(
            scrollPane.getVerticalScrollBar().getValue() + (int)(e.getUnitsToScroll() * 8)));
        scrollPane.getViewport().addMouseWheelListener(e -> scrollPane.getVerticalScrollBar().setValue(
            scrollPane.getVerticalScrollBar().getValue() + (int)(e.getUnitsToScroll() * 8)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        JPanel searchBar = new JPanel(new BorderLayout());
        searchBar.setBackground(new Color(28, 25, 26));
        searchBar.setBorder(new EmptyBorder(4, 6, 4, 6));

        JTextField bankSearch = new JTextField();
        bankSearch.setBackground(new Color(14, 12, 13));
        bankSearch.setForeground(TEXT_PRIMARY);
        bankSearch.setCaretColor(TEXT_PRIMARY);
        bankSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(58, 53, 48)),
                new EmptyBorder(3, 5, 3, 5)
        ));
        bankSearch.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
        bankSearch.addFocusListener(new FocusAdapter()
        {
            public void focusGained(FocusEvent e)
            {
                bankSearch.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(GOLD),
                        new EmptyBorder(3, 5, 3, 5)
                ));
            }
            public void focusLost(FocusEvent e)
            {
                bankSearch.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(58, 53, 48)),
                        new EmptyBorder(3, 5, 3, 5)
                ));
            }
        });

        panel.add(scrollPane, BorderLayout.CENTER);
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
        Color bgColor = (index % 2 == 0) ? BG_DARK : new Color(28, 25, 26);

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
        block.setBorder(new MatteBorder(0, 0, 1, 0, new Color(40, 36, 34)));

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(bgColor);
        row.setBorder(new MatteBorder(0, 4, 0, 0, borderColor));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel iconPanel = new JPanel();
        iconPanel.setPreferredSize(new Dimension(42, 42));
        iconPanel.setBackground(new Color(14, 12, 13));
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
        JPanel detailSlot = new JPanel(new BorderLayout());
        detailSlot.setBackground(BG_DARK);
        detailSlot.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        detailSlot.setVisible(false);
        detailSlot.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if (name.equals(selectedBankItemName) && currentOpenBankDetail != null)
                {
                    currentOpenBankDetail.setVisible(false);
                    currentOpenBankDetail = null;
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
                    tabContentPanel.revalidate();
                    tabContentPanel.repaint();
                    return;
                }
// Close previous
                if (currentOpenBankDetail != null)
                    currentOpenBankDetail.setVisible(false);
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
                row.setBorder(new MatteBorder(0, 4, 0, 0, GOLD));

                detailSlot.removeAll();
                detailSlot.add(buildInlineDetail(item, false), BorderLayout.CENTER);
                detailSlot.setVisible(true);
                scheduleRepaint(detailSlot);
                final String[] itemData = item;
                bankReopenAction = () -> {
                    for (java.awt.Component c : bankListPanel.getComponents())
                    {
                        if (!(c instanceof JPanel)) continue;
                        JPanel block = (JPanel) c;
                        if (block.getComponentCount() < 2) continue;
                        java.awt.Component second = block.getComponent(1);
                        if (!(second instanceof JPanel)) continue;
                        JPanel newDetailSlot = (JPanel) second;
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
                                    newDetailSlot.setVisible(true);
                                    scheduleRepaint(newDetailSlot);
                                    currentOpenBankDetail = newDetailSlot;
                                    selectedBankItemName = itemData[0];
                                    newRow.setBorder(new MatteBorder(0, 4, 0, 0, GOLD));
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
        block.add(detailSlot);
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

private String[] buildItemDataFromCache(String name)
    {
        String normalizedName = name.toLowerCase()
            .replace('\u2019', '\'')
            .replace('\u2018', '\'');
        Integer id = nameToId.get(normalizedName);
        if (id == null) id = nameToId.get(name.toLowerCase());
        if (id == null) return null;
        PriceData pd = priceCache.get(id);
        if (pd == null || pd.getMid() == 0) return null;

        String price = String.valueOf(pd.getMid());
        String delta = "0.00";
        java.util.Map<Integer, Long> avgCache = avgPrice24h;
        if (activeTimeFrame.equals("1H")) avgCache = avgPrice1h;
        else if (activeTimeFrame.equals("6H")) avgCache = avgPrice6h;
        Long avg = avgCache.get(id);
        if (avg != null && avg > 0)
        {
            double pct = ((double)(pd.getMid() - avg) / avg) * 100.0;
            delta = String.format("%+.2f", pct);
        }
        long gpChange = (avg != null && avg > 0) ? (pd.getMid() - avg) : 0;
        String gpChangeStr = gpChange > 0 ? "+" + formatPrice(String.valueOf(Math.abs(gpChange))) + " gp" :
                gpChange < 0 ? "-" + formatPrice(String.valueOf(Math.abs(gpChange))) + " gp" : "0 gp";
        Integer limit = itemLimits.get(id);
        String limitStr = (limit != null && limit > 0) ? String.format("%,d", limit) : "?";
        String buyPrice = pd.high > 0 ? String.valueOf(pd.high) : pd.getMid() > 0 ? String.valueOf(pd.getMid()) : "0";
        String sellPrice = pd.low > 0 ? String.valueOf(pd.low) : pd.getMid() > 0 ? String.valueOf(pd.getMid()) : "0";
        String buyQty = buyVolume1h.containsKey(id) ? String.valueOf(buyVolume1h.get(id)) : "?";
        String sellQty = sellVolume1h.containsKey(id) ? String.valueOf(sellVolume1h.get(id)) : "?";
        long lastTraded = (pd.highTime >= pd.lowTime) ? pd.high : pd.low;
        String lastTradedStr = lastTraded > 0 ? String.valueOf(lastTraded) : "0";
        String lastTradedTime = pd.getTimeSince();
        return new String[]{name, price, buyPrice, sellPrice, "0", delta, limitStr, gpChangeStr, buyQty, sellQty, lastTradedStr, lastTradedTime};
    }


    private JPanel buildTimeFrameBar()
    {
        JPanel bar = new JPanel(new GridLayout(1, 4, 3, 0));
        bar.setBackground(new Color(26, 23, 24));
        bar.setBorder(new EmptyBorder(5, 6, 5, 6));

        String[] frames = {"1H", "6H", "24H", "30D"};
        for (String frame : frames)
        {
            JButton btn = new JButton(frame);
            btn.setFont(new Font("Monospaced", Font.PLAIN, FONT_TIMEFRAME));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            boolean isDisabled = frame.equals("30D");
            btn.setBackground(frame.equals(activeTimeFrame) ? new Color(26, 21, 0) : new Color(14, 12, 13));
            btn.setForeground(isDisabled ? new Color(45, 40, 38) : frame.equals(activeTimeFrame) ? GOLD : TAB_INACTIVE);
            btn.setBorder(BorderFactory.createLineBorder(isDisabled ? new Color(35, 30, 28) : frame.equals(activeTimeFrame) ? GOLD : new Color(58, 53, 48)));
            btn.setEnabled(!isDisabled);
            btn.addActionListener(e -> {
                if (!isDisabled) {
                    activeTimeFrame = frame;
                    isRefreshing = true;
                    showTab(activeTab);
                    isRefreshing = false;
                    if (searchReopenAction != null && activeTab == 1)
                    {
                        javax.swing.Timer t = new javax.swing.Timer(50, e2 -> searchReopenAction.run());
                        t.setRepeats(false);
                        t.start();
                    }
                    if (watchlistReopenAction != null && activeTab == 0)
                    {
                        javax.swing.Timer t = new javax.swing.Timer(150, e2 -> watchlistReopenAction.run());
                        t.setRepeats(false);
                        t.start();
                    }
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

        JPanel iconBox = new JPanel(new java.awt.GridBagLayout());
        iconBox.setPreferredSize(new Dimension(42, 42));
        iconBox.setBackground(new Color(14, 12, 13));
        iconBox.setBorder(BorderFactory.createLineBorder(new Color(42, 37, 40)));

        Integer detailItemId = nameToId.get(name.toLowerCase()
            .replace('\u2019', '\'')
            .replace('\u2018', '\''));
        if (detailItemId == null) detailItemId = nameToId.get(name.toLowerCase());
        if (detailItemId != null)
        {
            final int finalDetailId = detailItemId;
            new Thread(() -> {
                java.awt.image.BufferedImage detailIcon = plugin.getItemManager().getImage(finalDetailId);
                if (detailIcon != null)
                {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        javax.swing.JLabel iconLabel = new javax.swing.JLabel(new javax.swing.ImageIcon(detailIcon));
                        iconBox.add(iconLabel);
                        iconBox.revalidate();
                        iconBox.repaint();
                        if (iconBox.getParent() != null) iconBox.getParent().repaint();
                    });
                }
            }).start();
        }

        JPanel detailIconWrapper = new JPanel(new java.awt.GridBagLayout());
        detailIconWrapper.setBackground(BG_DETAIL);
        detailIconWrapper.add(iconBox);
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

    private JPanel buildStatBox(String label, String value, Color valueColor, String tooltip)
    {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(new Color(14, 12, 13));
        box.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel labelText = new JLabel(label.toUpperCase());
        labelText.setForeground(TEXT_PRIMARY);
        labelText.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_LABEL));
        labelText.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueText = new JLabel(value);
        valueText.setForeground(valueColor);
        valueText.setFont(new Font("Monospaced", Font.PLAIN, FONT_STAT_VALUE));
        valueText.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (tooltip != null) valueText.setToolTipText(tooltip);

        box.add(labelText);
        box.add(valueText);
        return box;
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