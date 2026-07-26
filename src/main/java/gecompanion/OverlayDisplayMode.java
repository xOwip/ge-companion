package gecompanion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OverlayDisplayMode
{
    COMPACT("Compact"),
    FULL("Full");

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}