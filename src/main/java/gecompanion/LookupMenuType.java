package gecompanion;

public enum LookupMenuType
{
    SHIFT("Shift right-click"),
    ALWAYS("Right-click");

    private final String name;

    LookupMenuType(String name) { this.name = name; }

    @Override
    public String toString() { return name; }
}