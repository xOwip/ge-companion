package gecompanion;

import javax.swing.*;
import java.awt.*;

public class ScrollablePanel extends JPanel implements Scrollable
{
    public enum ScrollableSizeHint { NONE, FIT, STRETCH }

    private ScrollableSizeHint scrollableWidth = ScrollableSizeHint.NONE;
    private ScrollableSizeHint scrollableHeight = ScrollableSizeHint.NONE;

    public void setScrollableWidth(ScrollableSizeHint hint) { this.scrollableWidth = hint; }
    public void setScrollableHeight(ScrollableSizeHint hint) { this.scrollableHeight = hint; }

    @Override
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

    @Override
    public boolean getScrollableTracksViewportWidth()
    { return scrollableWidth == ScrollableSizeHint.FIT; }

    @Override
    public boolean getScrollableTracksViewportHeight()
    { return scrollableHeight == ScrollableSizeHint.STRETCH; }

    @Override
    public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }

    @Override
    public int getScrollableBlockIncrement(Rectangle r, int o, int d)
    { return (int)(o == SwingConstants.VERTICAL ? getVisibleRect().height : getVisibleRect().width) * 9 / 10; }
}