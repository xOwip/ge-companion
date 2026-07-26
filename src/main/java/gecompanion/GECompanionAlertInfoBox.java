package gecompanion;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GECompanionAlertInfoBox extends InfoBox
{
    private final int itemId;
    private final GECompanionPanel panel;
    private final GECompanionConfig config;
    private final boolean isAbove;
    private final long targetPrice;

    public GECompanionAlertInfoBox(BufferedImage image, Plugin plugin, int itemId,
                                   GECompanionPanel panel, GECompanionConfig config, boolean isAbove, long targetPrice)
    {
        super(image, plugin);
        this.itemId = itemId;
        this.panel = panel;
        this.config = config;
        this.isAbove = isAbove;
        this.targetPrice = targetPrice;
    }

    @Override
    public String getText()
    {
        boolean isFired = panel.getFiredAlertsForOverlay().contains(itemId);
        return isFired ? "✓" : "";
    }

    @Override
    public Color getTextColor()
    {
        boolean isFired = panel.getFiredAlertsForOverlay().contains(itemId);
        return isFired ? config.alertFiredColor() : null;
    }

    @Override
    public String getTooltip()
    {
        boolean isFired = panel.getFiredAlertsForOverlay().contains(itemId);
        long currentPrice = panel.getCurrentPrice(itemId);
        String itemName = panel.getItemName(itemId);
        String name = itemName != null ? itemName : "Unknown";

        Color orange = new Color(0xFF981F);
        Color gold = new Color(0xD4AF37);
        Color firedColor = config.alertFiredColor();

        String orangeHex = String.format("%06X", orange.getRGB() & 0xFFFFFF);
        String goldHex = String.format("%06X", gold.getRGB() & 0xFFFFFF);
        String firedHex = String.format("%06X", firedColor.getRGB() & 0xFFFFFF);
        String whiteHex = "ffffff";

        String nowPrice = String.format("%,d", currentPrice) + " gp";
        String targetPriceStr = String.format("%,d", targetPrice) + " gp";
        String direction = isAbove ? " ▲" : " ▼";

        if (isFired) {
            Long firedTime = panel.getAlertFiredTime(itemId);
            String timeAgo = "";
            if (firedTime != null) {
                long elapsed = System.currentTimeMillis() - firedTime;
                long mins = elapsed / 60000;
                long secs = (elapsed % 60000) / 1000;
                timeAgo = mins > 0 ? mins + " min ago" : secs + "s ago";
            }
            return "<col=" + orangeHex + ">" + name + "</col><br>" +
                    "<col=" + goldHex + ">Now:</col>    <col=" + firedHex + ">" + nowPrice + "</col><br>" +
                    "<col=" + firedHex + ">Target reached! ✓</col>" +
                    (timeAgo.isEmpty() ? "" : "<br><col=aaaaaa>" + timeAgo + "</col>");
        } else {
            return "<col=" + orangeHex + ">" + name + "</col><br>" +
                    "<col=" + goldHex + ">Now:</col>    <col=" + whiteHex + ">" + nowPrice + "</col><br>" +
                    "<col=" + goldHex + ">Target:</col> <col=" + whiteHex + ">" + targetPriceStr + direction + "</col>";
        }
    }

    public int getItemId()
    {
        return itemId;
    }
}