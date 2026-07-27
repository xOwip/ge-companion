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

    private final java.awt.image.BufferedImage originalImage;

    public GECompanionAlertInfoBox(java.awt.image.BufferedImage image, Plugin plugin, int itemId,
                                   GECompanionPanel panel, GECompanionConfig config, boolean isAbove, long targetPrice)
    {
        super(image, plugin);
        this.originalImage = image;
        this.itemId = itemId;
        this.panel = panel;
        this.config = config;
        this.isAbove = isAbove;
        this.targetPrice = targetPrice;
    }

    @Override
    public java.awt.image.BufferedImage getImage()
    {
        boolean isFired = panel.getFiredAlertsForOverlay().contains(itemId);
        if (!isFired) return originalImage;
        if (originalImage == null) return null;
        if (originalImage.getWidth() <= 0 || originalImage.getHeight() <= 0) return originalImage;

        // Create blue tinted version
        java.awt.image.BufferedImage tinted = new java.awt.image.BufferedImage(
                originalImage.getWidth(), originalImage.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = tinted.createGraphics();
        // Draw transparent tint first
        g2.setColor(new java.awt.Color(config.alertFiredBgColor().getRed(), config.alertFiredBgColor().getGreen(), config.alertFiredBgColor().getBlue(), config.alertFiredBgOpacity()));
        g2.fillRect(0, 0, tinted.getWidth(), tinted.getHeight());
        // Draw icon on top
        g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));
        g2.drawImage(originalImage, 0, 0, null);
        g2.dispose();
        return tinted;
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
                    "<col=" + goldHex + ">Target:</col> <col=" + firedHex + ">" + targetPriceStr + " " + (isAbove ? "▲" : "▼") + "</col><br>" +
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