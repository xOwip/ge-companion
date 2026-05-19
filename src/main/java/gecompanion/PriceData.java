package gecompanion;

public class PriceData
{
    public final int id;
    public final long high;
    public final long low;
    public final long highTime;
    public final long lowTime;

    public PriceData(int id, long high, long low, long highTime, long lowTime)
    {
        this.id = id;
        this.high = high;
        this.low = low;
        this.highTime = highTime;
        this.lowTime = lowTime;
    }

    public long getMid()
    {
        if (high > 0 && low > 0) return (high + low) / 2;
        if (high > 0) return high;
        if (low > 0) return low;
        return 0;
    }

    public String getTimeSince()
    {
        long now = System.currentTimeMillis() / 1000;
        long latest = Math.max(highTime, lowTime);
        if (latest == 0) return "unknown";
        long diff = now - latest;
        if (diff < 60) return diff + "s ago";
        if (diff < 3600) return (diff / 60) + "m ago";
        return (diff / 3600) + "h ago";
    }
}