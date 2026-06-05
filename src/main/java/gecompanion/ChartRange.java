package gecompanion;

public enum ChartRange
{
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    QUARTER("Quarter"),
    YEAR("Year");

    private final String name;

    ChartRange(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}