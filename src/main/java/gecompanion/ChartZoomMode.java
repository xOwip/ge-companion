package gecompanion;

public enum ChartZoomMode
{
    DRAG_SELECT("Drag Select & Pan"),
    MAGNIFIER("Magnifier");

    private final String name;

    ChartZoomMode(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}