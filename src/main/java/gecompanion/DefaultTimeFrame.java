package gecompanion;

public enum DefaultTimeFrame
{
    ONE_HOUR("1H"),
    SIX_HOUR("6H"),
    TWENTY_FOUR_HOUR("24H");

    private final String value;

    DefaultTimeFrame(String value) { this.value = value; }

    public String getValue() { return value; }

    @Override
    public String toString() { return value; }
}