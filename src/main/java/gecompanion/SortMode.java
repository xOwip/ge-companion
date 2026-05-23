package gecompanion;

public enum SortMode
{
    GP_CHANGE("GP"),
    PERCENT_CHANGE("%");

    private final String label;

    SortMode(String label) { this.label = label; }

    public String getLabel() { return label; }

    @Override
    public String toString() { return label + " Change"; }
}