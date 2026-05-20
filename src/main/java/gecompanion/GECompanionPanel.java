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
    private static final int FONT_STAT_LABEL = 9;
    private static final int FONT_STAT_VALUE = 13;
    private static final int FONT_BUTTON = 11;
    private static final int FONT_TIMEFRAME = 12;
    private static final int FONT_REFRESH = 17;
    private static final int FONT_META = 12;
    private static final int FONT_LIMIT = 11;

    // Colors
    private static final Color BG_DARK = new Color(35, 31, 32);
    private static final Color BG_HEADER = new Color(26, 23, 24);
    private static final Color BG_DETAIL = new Color(20, 18, 18);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color PRICE_GOLD = new Color(255, 210, 50);
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
    private String activeTimeFrame = "24H";
    private boolean bankAllItemsCollapsed = true;
    private JPanel tabContentPanel;
    private JLabel[] tabLabels = new JLabel[3];
    private JLabel timerLabel;
    private int secondsSinceRefresh = 0;
    private javax.swing.Timer liveTimer;

    // Search state
    private JPanel searchResultsPanel;
    private JPanel recentSearchesPanel;
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
    // Pinned/watched items
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
        loadPinnedItems();
        startLiveTimer();
        setLayout(new BorderLayout());
        setBackground(BG_DARK);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        build();
    }

    private long totalBankValue = 0;

    public void updateBankItems(java.util.List<String> items, java.util.Map<String, Integer> quantities, long bankValue)
    {
        this.bankItems.clear();
        this.bankItems.addAll(items);
        this.bankQuantities.clear();
        this.bankQuantities.putAll(quantities);
        this.totalBankValue = bankValue;
        if (activeTab == 2) showTab(2);
    }

    public void onPricesUpdated(java.util.Map<Integer, PriceData> priceCache, java.util.Map<String, Integer> nameToId, java.util.Map<Integer, Long> avgPrice24h, java.util.Map<Integer, Long> avgPrice1h, java.util.Map<Integer, Long> avgPrice6h, java.util.Map<Integer, Integer> itemLimits)
    {
        this.priceCache = priceCache;
        this.nameToId = nameToId;
        this.avgPrice24h = avgPrice24h;
        this.avgPrice1h = avgPrice1h;
        this.avgPrice6h = avgPrice6h;
        this.itemLimits = itemLimits;
        secondsSinceRefresh = 0;
        showTab(activeTab);
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

        add(wrapper, BorderLayout.CENTER);
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

    private void showTab(int idx)
    {
        tabContentPanel.removeAll();
        currentOpenSearchDetail = null;
        currentOpenWatchlistDetail = null;
        currentOpenBankDetail = null;
        currentOpenSearchRow = null;
        currentOpenWatchlistRow = null;
        currentOpenBankRow = null;
        selectedItemName = null;
        selectedWatchlistItemName = null;
        selectedBankItemName = null;
        switch (idx)
        {
            case 0: tabContentPanel.add(buildWatchlistTab(), BorderLayout.CENTER); break;
            case 1: tabContentPanel.add(buildSearchTab(), BorderLayout.CENTER); break;
            case 2: tabContentPanel.add(buildBankTab(), BorderLayout.CENTER); break;
        }
        tabContentPanel.revalidate();
        tabContentPanel.repaint();
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

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(BG_DETAIL);
        inner.setBorder(new EmptyBorder(6, 7, 6, 7));

        inner.add(buildDetailHeader(name, price));
        inner.add(Box.createVerticalStrut(6));

        JPanel grid = new JPanel(new GridLayout(2, 2, 2, 2));
        grid.setBackground(BG_DETAIL);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(220, 80));

        grid.add(buildStatBox("Current Price", formatPrice(price), GOLD, formatFullPrice(price) + " gp"));
        grid.add(buildStatBox("Profit (w/ Tax)", profit, isUp ? GREEN_UP : RED_DOWN, null));
        grid.add(buildStatBox("Buy Qty / hr", buyQty, TEXT_PRIMARY, null));
        grid.add(buildStatBox("Sell Qty / hr", sellQty, TEXT_PRIMARY, null));

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
        footer.add(buildFooterBtn("Tracker ↗", false));
        footer.add(buildFooterBtn("Wiki ↗", false));

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

        JTextField searchField = new JTextField();
        searchField.setBackground(new Color(14, 12, 13));
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setCaretColor(TEXT_PRIMARY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(58, 53, 48)),
                new EmptyBorder(3, 5, 3, 5)
        ));
        searchField.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
        searchWrap.add(searchField, BorderLayout.CENTER);
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_DARK);
        topBar.add(buildTimeFrameBar(), BorderLayout.NORTH);
        topBar.add(searchWrap, BorderLayout.SOUTH);
        panel.add(topBar, BorderLayout.NORTH);

        recentSearchesPanel = new JPanel();
        recentSearchesPanel.setLayout(new BoxLayout(recentSearchesPanel, BoxLayout.Y_AXIS));
        recentSearchesPanel.setBackground(BG_DARK);

        JLabel recentLabel = new JLabel("Recent Searches");
        recentLabel.setForeground(TEXT_DIM);
        recentLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
        recentLabel.setBorder(new EmptyBorder(4, 7, 2, 7));
        recentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        recentSearchesPanel.add(recentLabel);

        String[] recentItems = {"Twisted bow", "Runite bar", "Saradomin brew(4)"};
        String[] recentPrices = {"1.585B · 2m ago", "12,387 · 14m ago", "8,189 · 1h ago"};
        for (int i = 0; i < recentItems.length; i++)
        {
            recentSearchesPanel.add(buildRecentRow(recentItems[i], recentPrices[i]));
        }

        searchResultsPanel = new JPanel();
        searchResultsPanel.setLayout(new BoxLayout(searchResultsPanel, BoxLayout.Y_AXIS));
        searchResultsPanel.setBackground(BG_DARK);
        searchResultsPanel.setVisible(false);

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

        JScrollPane scrollPane = new JScrollPane(listWrapper);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG_DARK);
        scrollPane.getViewport().setBackground(BG_DARK);
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

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
        {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { onSearchChanged(searchField.getText()); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { onSearchChanged(searchField.getText()); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { onSearchChanged(searchField.getText()); }
        });

        return panel;
    }

    private void onSearchChanged(String query)
    {
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

    private void updateSearchResults(String query)
    {
        searchResultsPanel.removeAll();

        List<String[]> results = new ArrayList<>();

        if (!nameToId.isEmpty() && !priceCache.isEmpty())
        {
            // Use live data
            for (Map.Entry<String, Integer> entry : nameToId.entrySet())
            {
                if (entry.getKey().contains(query))
                {
                    int id = entry.getValue();
                    PriceData pd = priceCache.get(id);
                    if (pd != null && pd.getMid() > 0)
                    {
                        String name = entry.getKey();
                        // Capitalize first letter of each word
                        String[] words = name.split(" ");
                        StringBuilder sb = new StringBuilder();
                        for (String word : words)
                        {
                            if (word.length() > 0)
                                sb.append(Character.toUpperCase(word.charAt(0)))
                                  .append(word.substring(1)).append(" ");
                        }
                        String displayName = sb.toString().trim();
                        String price = String.valueOf(pd.getMid());
                        String delta = "0.00";
                        java.util.Map<Integer, Long> avgCache = avgPrice24h;
                        if (activeTimeFrame.equals("1H")) avgCache = avgPrice1h;
                        else if (activeTimeFrame.equals("6H")) avgCache = avgPrice6h;
                        Long avg = avgCache.get(id);
                        if (avg != null && avg > 0 && pd.getMid() > 0)
                        {
                            double pct = ((double)(pd.getMid() - avg) / avg) * 100.0;
                            delta = String.format("%+.2f", pct);
                        }
                        Integer limit = itemLimits.get(id);
                        String limitStr = (limit != null && limit > 0) ? String.format("%,d", limit) : "?";
                        results.add(new String[]{displayName, price, "0", "0", "0", delta, limitStr});
                    }
                }
            }
            // Sort alphabetically
            results.sort((a, b) -> a[0].compareTo(b[0]));
            // Limit to 50 results
            if (results.size() > 50) results = results.subList(0, 50);
        }
        else
        {
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

        if (results.isEmpty())
        {
            JLabel noResults = new JLabel("No results found");
            noResults.setForeground(TEXT_DIM);
            noResults.setFont(new Font("Monospaced", Font.PLAIN, FONT_META));
            noResults.setBorder(new EmptyBorder(8, 7, 8, 7));
            noResults.setAlignmentX(Component.LEFT_ALIGNMENT);
            searchResultsPanel.add(noResults);
        }
        else
        {
            int searchIndex = 0;
            for (String[] item : results)
            {
                searchResultsPanel.add(buildSearchItemBlock(item, searchIndex++));
            }
        }

        searchResultsPanel.revalidate();
        searchResultsPanel.repaint();
    }

    private JPanel buildSearchItemBlock(String[] item, int index)
    {
        String name = item[0];
        String price = item[1];
        String buyQty = item[3];
        String sellQty = item[4];
        String delta = item[5];
        String limit = item.length > 6 ? item[6] : "?";

        boolean isUp = delta.startsWith("+");
        boolean isDown = delta.startsWith("-");
        Color rowBg = (index % 2 == 0) ? BG_DARK : new Color(28, 25, 26);

        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBackground(rowBg);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(rowBg);
        row.setBorder(new MatteBorder(0, 0, 1, 0, new Color(80, 75, 70)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel iconPanel = new JPanel();
        iconPanel.setPreferredSize(new Dimension(42, 42));
        iconPanel.setBackground(new Color(14, 12, 13));
        iconPanel.setBorder(BorderFactory.createLineBorder(new Color(42, 37, 40)));
        row.add(iconPanel, BorderLayout.WEST);

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

        JLabel deltaLabel = new JLabel(delta + "%");
        deltaLabel.setForeground(isUp ? GREEN_UP : isDown ? RED_DOWN : TAB_INACTIVE);
        deltaLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_DELTA));
        deltaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel deltaLimitRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        deltaLimitRow.setBackground(rowBg);
        deltaLimitRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        deltaLimitRow.add(deltaLabel);
        deltaLimitRow.add(Box.createHorizontalStrut(8));
        JLabel limitLabel = new JLabel("Lmt: " + limit);
        limitLabel.setForeground(TEXT_DIM);
        limitLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
        deltaLimitRow.add(limitLabel);

        info.add(nameLabel);
        info.add(priceLabel);
        info.add(deltaLimitRow);
        row.add(info, BorderLayout.CENTER);

        // Inline detail panel for this row
        JPanel detailSlot = new JPanel(new BorderLayout());
        detailSlot.setBackground(BG_DARK);
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
                        currentOpenSearchRow.setBackground(BG_DARK);
                        currentOpenSearchRow.setBorder(new MatteBorder(0, 3, 0, 1, new Color(26, 24, 24)));
                        for (Component c : currentOpenSearchRow.getComponents())
                        {
                            if (c instanceof JPanel)
                            {
                                c.setBackground(BG_DARK);
                                for (Component cc : ((JPanel)c).getComponents())
                                    if (cc instanceof JPanel) cc.setBackground(BG_DARK);
                            }
                        }
                        currentOpenSearchRow = null;
                    }
                    selectedItemName = null;
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
                currentOpenSearchRow = row;
                currentOpenSearchDetail = detailSlot;

                detailSlot.removeAll();
                detailSlot.add(buildInlineDetail(item, false), BorderLayout.CENTER);
                detailSlot.setVisible(true);

                row.setBackground(BG_ROW_SELECTED);
                info.setBackground(BG_ROW_SELECTED);
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
                    deltaLimitRow.setBackground(BG_ROW_HOVER);
                }
            }
            public void mouseExited(MouseEvent e)
            {
                if (!name.equals(selectedItemName))
                {
                    row.setBackground(rowBg);
                    info.setBackground(rowBg);
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
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(new Color(26, 23, 24));

        JLabel pinnedLabel = new JLabel("Pinned Items");
        pinnedLabel.setForeground(TEXT_DIM);
        pinnedLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
        pinnedLabel.setBorder(new EmptyBorder(4, 7, 2, 7));
        pinnedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        listPanel.add(pinnedLabel);

        int watchlistIndex = 0;
        for (String pinnedName : pinnedItems)
        {
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

        JLabel bankLabel = new JLabel("Bank Items");
        bankLabel.setForeground(TEXT_DIM);
        bankLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
        bankLabel.setBorder(new EmptyBorder(6, 7, 2, 7));
        bankLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        listPanel.add(bankLabel);

        JLabel noBankItems = new JLabel("Scan your bank to see items here");
        noBankItems.setForeground(TEXT_DIM);
        noBankItems.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));
        noBankItems.setBorder(new EmptyBorder(4, 7, 4, 7));
        noBankItems.setAlignmentX(Component.LEFT_ALIGNMENT);
        listPanel.add(noBankItems);

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG_DARK);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollPane.addMouseWheelListener(e -> scrollPane.getVerticalScrollBar().setValue(
            scrollPane.getVerticalScrollBar().getValue() + (int)(e.getUnitsToScroll() * 8)));
        scrollPane.getViewport().addMouseWheelListener(e -> scrollPane.getVerticalScrollBar().setValue(
            scrollPane.getVerticalScrollBar().getValue() + (int)(e.getUnitsToScroll() * 8)));

        panel.add(buildTimeFrameBar(), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildWatchlistItemBlock(String[] item, int index)
    {
        String name = item[0];
        String price = item[1];
        String delta = item[5];
        String limit = item[6];

        boolean isUp = delta.startsWith("+");
        boolean isDown = delta.startsWith("-");

        Color rowBg = (index % 2 == 0) ? BG_DARK : new Color(28, 25, 26);

        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBackground(rowBg);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(rowBg);
        row.setBorder(new MatteBorder(0, 0, 1, 0, new Color(80, 75, 70)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        row.setBackground(rowBg);
        row.setBorder(new MatteBorder(0, 0, 1, 0, new Color(80, 75, 70)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
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
        info.setBackground(rowBg);
        info.setBorder(new EmptyBorder(5, 7, 8, 0));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_ITEM_NAME));
        nameLabel.setMaximumSize(new Dimension(190, 20));

        JLabel priceLabel = new JLabel(formatPrice(price) + " gp");
        priceLabel.setForeground(PRICE_GOLD);
        priceLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_PRICE));

        JLabel deltaLabel = new JLabel(delta + "%");
        deltaLabel.setForeground(isUp ? GREEN_UP : isDown ? RED_DOWN : TEXT_PRIMARY);
        deltaLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_DELTA));
        deltaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel limitLabel = new JLabel("Lmt: " + limit);
        limitLabel.setForeground(TEXT_DIM);
        limitLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_LIMIT));

        JPanel deltaLimitRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        deltaLimitRow.setBackground(rowBg);
        deltaLimitRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        deltaLimitRow.add(deltaLabel);
        deltaLimitRow.add(Box.createHorizontalStrut(8));
        deltaLimitRow.add(limitLabel);

        info.add(nameLabel);
        info.add(priceLabel);
        info.add(deltaLimitRow);
        row.add(info, BorderLayout.CENTER);
        // Inline detail slot
        JPanel detailSlot = new JPanel(new BorderLayout());
        detailSlot.setBackground(BG_DARK);
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
                        currentOpenWatchlistRow.setBorder(new MatteBorder(0, 0, 1, 0, new Color(80, 75, 70)));
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
                    return;
                }

                // Close previous
                if (currentOpenWatchlistDetail != null)
                    currentOpenWatchlistDetail.setVisible(false);
                if (currentOpenWatchlistRow != null)
                {
                    currentOpenWatchlistRow.setBackground(BG_DARK);
                    currentOpenWatchlistRow.setBorder(new MatteBorder(0, 0, 1, 0, new Color(80, 75, 70)));
                    for (Component c : currentOpenWatchlistRow.getComponents())
                        if (c instanceof JPanel) c.setBackground(BG_DARK);
                }

                // Open this one
                selectedWatchlistItemName = name;
                currentOpenWatchlistRow = row;
                currentOpenWatchlistDetail = detailSlot;

                detailSlot.removeAll();
                detailSlot.add(buildInlineDetail(item, true), BorderLayout.CENTER);
                detailSlot.setVisible(true);

                row.setBackground(BG_ROW_SELECTED);
                info.setBackground(BG_ROW_SELECTED);
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
                    deltaLimitRow.setBackground(BG_ROW_HOVER);
                }
            }
            public void mouseExited(MouseEvent e)
            {
                if (!name.equals(selectedWatchlistItemName))
                {
                    row.setBackground(rowBg);
                    info.setBackground(rowBg);
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

        String bankValueStr = bankItems.isEmpty() ? "─── No bank data ───" : formatFullPrice(String.valueOf(totalBankValue)) + " gp";
        JLabel heroValue = new JLabel(bankValueStr);
        heroValue.setForeground(PRICE_GOLD);
        heroValue.setFont(new Font("Monospaced", Font.BOLD, FONT_TITLE));
        heroValue.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel pillPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        pillPanel.setBackground(new Color(26, 23, 24));

        hero.add(heroLabel);
        hero.add(Box.createVerticalStrut(2));
        hero.add(heroValue);
        hero.add(Box.createVerticalStrut(4));
        hero.add(pillPanel);

        hero.add(Box.createVerticalStrut(4));
        hero.add(buildTimeFrameBar());
        panel.add(hero, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(BG_DARK);

        if (!bankItems.isEmpty())
        {
            JLabel gainersLabel = new JLabel("▲ Top Gainers");
            gainersLabel.setForeground(GREEN_UP);
            gainersLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
            gainersLabel.setBorder(new EmptyBorder(6, 7, 2, 7));
            gainersLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(gainersLabel);
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

            // Sort by delta descending
            allBankItems.sort((a, b) -> Double.compare(
                    Double.parseDouble(b[5].replace("+", "")),
                    Double.parseDouble(a[5].replace("+", ""))
            ));

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

            // Top Gainers
            int gainersCount = Math.min(config.gainersCount(), allBankItems.size());
            for (int i = 0; i < gainersCount; i++)
            {
                if (Double.parseDouble(allBankItems.get(i)[5].replace("+", "")) > 0)
                    listPanel.add(buildBankItemBlock(allBankItems.get(i), true));
            }

            // Top Losers
            listPanel.add(new JSeparator());
            JLabel losersLabel = new JLabel("▼ Top Losers");
            losersLabel.setForeground(RED_DOWN);
            losersLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_SECTION));
            losersLabel.setBorder(new EmptyBorder(6, 7, 2, 7));
            losersLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(losersLabel);

            int losersCount = Math.min(config.losersCount(), allBankItems.size());
            for (int i = allBankItems.size() - 1; i >= allBankItems.size() - losersCount; i--)
            {
                if (Double.parseDouble(allBankItems.get(i)[5].replace("+", "")) < 0)
                    listPanel.add(buildBankItemBlock(allBankItems.get(i), true));
            }

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

            if (!bankAllItemsCollapsed)
            {
                for (String[] item : allBankItems)
                    listPanel.add(buildBankItemBlock(item, false));
            }
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG_DARK);
        scrollPane.getViewport().setBackground(BG_DARK);
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
        searchBar.add(bankSearch, BorderLayout.CENTER);

        panel.add(scrollPane, BorderLayout.CENTER);
        if (!bankItems.isEmpty()) panel.add(searchBar, BorderLayout.SOUTH);

        return panel;
    }


    private JPanel buildBankItemBlock(String[] item, boolean colorCode)
    {
        String name = item[0];
        String price = item[1];
        String delta = item[5];

        boolean isUp = delta.startsWith("+");
        boolean isDown = delta.startsWith("-");

        Color borderColor = colorCode && isUp ? new Color(0, 100, 0) : colorCode && isDown ? new Color(100, 0, 0) : new Color(80, 75, 70);
        Color bgColor = colorCode && isUp ? new Color(10, 20, 10) : colorCode && isDown ? new Color(20, 10, 10) : BG_DARK;

        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBackground(bgColor);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(bgColor);
        row.setBorder(new MatteBorder(0, 3, 1, 0, borderColor));
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
        info.setBackground(bgColor);
        info.setBorder(new EmptyBorder(5, 7, 8, 0));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_ITEM_NAME));
        nameLabel.setMaximumSize(new Dimension(175, 20));

        JLabel priceLabel = new JLabel(formatPrice(price) + " gp");
        priceLabel.setForeground(PRICE_GOLD);
        priceLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_PRICE));

        JLabel deltaLabel = new JLabel(delta + "%");
        deltaLabel.setForeground(isUp ? GREEN_UP : isDown ? RED_DOWN : TAB_INACTIVE);
        deltaLabel.setFont(new Font("Monospaced", Font.PLAIN, FONT_DELTA));
        deltaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(nameLabel);
        info.add(priceLabel);
        info.add(deltaLabel);
        row.add(info, BorderLayout.CENTER);


        // Inline detail slot
        JPanel detailSlot = new JPanel(new BorderLayout());
        detailSlot.setBackground(BG_DARK);
        detailSlot.setVisible(false);
        detailSlot.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                // Toggle
                if (name.equals(selectedBankItemName) && currentOpenBankDetail != null)
                {
                    currentOpenBankDetail.setVisible(false);
                    currentOpenBankDetail = null;
                    if (currentOpenBankRow != null)
                    {
                        currentOpenBankRow.setBackground(BG_DARK);
                        for (Component c : currentOpenBankRow.getComponents())
                            if (c instanceof JPanel) c.setBackground(BG_DARK);
                        currentOpenBankRow = null;
                    }
                    selectedBankItemName = null;
                    tabContentPanel.revalidate();
                    tabContentPanel.repaint();
                    return;
                }

                // Close previous
                if (currentOpenBankDetail != null)
                    currentOpenBankDetail.setVisible(false);
                if (currentOpenBankRow != null)
                {
                    currentOpenBankRow.setBackground(BG_DARK);
                    for (Component c : currentOpenBankRow.getComponents())
                        if (c instanceof JPanel) c.setBackground(BG_DARK);
                }

                // Open this one
                selectedBankItemName = name;
                currentOpenBankRow = row;
                currentOpenBankDetail = detailSlot;

                detailSlot.removeAll();
                detailSlot.add(buildInlineDetail(item, false), BorderLayout.CENTER);
                detailSlot.setVisible(true);

                row.setBackground(BG_ROW_SELECTED);
                info.setBackground(BG_ROW_SELECTED);

                tabContentPanel.revalidate();
                tabContentPanel.repaint();
            }
            public void mouseEntered(MouseEvent e)
            {
                if (!name.equals(selectedBankItemName))
                {
                    row.setBackground(BG_ROW_HOVER);
                    info.setBackground(BG_ROW_HOVER);
                }
            }
            public void mouseExited(MouseEvent e)
            {
                if (!name.equals(selectedBankItemName))
                    if (!name.equals(selectedBankItemName))
                    {
                        row.setBackground(bgColor);
                        info.setBackground(bgColor);
                    }
            }
        });

        block.add(row);
        block.add(detailSlot);
        return block;
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
        Integer limit = itemLimits.get(id);
        String limitStr = (limit != null && limit > 0) ? String.format("%,d", limit) : "?";
        return new String[]{name, price, "0", "0", "0", delta, limitStr};
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
                    showTab(activeTab);
                }
            });
            bar.add(btn);
        }
        return bar;
    }

    private JPanel buildDetailHeader(String name, String price)
    {
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(BG_DETAIL);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JPanel iconBox = new JPanel();
        iconBox.setPreferredSize(new Dimension(42, 42));
        iconBox.setBackground(new Color(14, 12, 13));
        iconBox.setBorder(BorderFactory.createLineBorder(new Color(42, 37, 40)));
        headerRow.add(iconBox, BorderLayout.WEST);

        JPanel namePrice = new JPanel();
        namePrice.setLayout(new BoxLayout(namePrice, BoxLayout.Y_AXIS));
        namePrice.setBackground(BG_DETAIL);
        namePrice.setBorder(new EmptyBorder(5, 7, 5, 0));
        namePrice.setMaximumSize(new Dimension(140, 60));

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

        namePrice.add(nameLabel);
        namePrice.add(priceLabel);
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
        labelText.setForeground(TEXT_DIM);
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
        row.setBorder(new MatteBorder(0, 0, 1, 0, new Color(80, 75, 70)));
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

    private String formatPrice(String price)
    {
        try
        {
            long val = Long.parseLong(price.replace(",", ""));
            if (val >= 1_000_000_000) return String.format("%.3fB", val / 1_000_000_000.0);
            if (val >= 1_000_000) return String.format("%.1fM", val / 1_000_000.0);
            if (val >= 1_000) return String.format("%,d", val);
            return String.valueOf(val);
        }
        catch (NumberFormatException e)
        {
            return price;
        }
    }
}