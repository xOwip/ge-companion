package gecompanion;

public enum DefaultTab
{
    SEARCH("Search"),
    WATCHLIST("Watchlist"),
    BANK("Bank");

    private final String name;

    DefaultTab(String name) { this.name = name; }

    @Override
    public String toString() { return name; }
}