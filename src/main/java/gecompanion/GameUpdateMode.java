package gecompanion;

public enum GameUpdateMode
{
    OFF("Off"),
    MAJOR_ONLY("Major Only"),
    ALL("All");

    private final String name;

    GameUpdateMode(String name) { this.name = name; }

    @Override
    public String toString() { return name; }
}
