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

    // Calculates progress toward the alert target as a percentage (0-100), based on movement from the
// alert's creation price to the current price, relative to the total distance to the target.
// Returns -1 if progress cannot be calculated (no creation price recorded - legacy alert).
    private int calculateProgressPercent(long creationPrice, long currentPrice, long targetPrice, boolean isAbove)
    {
        if (creationPrice <= 0) {
            return -1;
        }
        long totalDistance = isAbove ? (targetPrice - creationPrice) : (creationPrice - targetPrice);
        if (totalDistance <= 0) {
            // Alert was created at or past the target already - treat as complete.
            return 100;
        }
        long moved = isAbove ? (currentPrice - creationPrice) : (creationPrice - currentPrice);
        double percent = (moved / (double) totalDistance) * 100.0;
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        return (int) Math.round(percent);
    }

    // Builds a text-based progress bar using block characters, e.g. "\u2588\u2588\u2588\u2588\u2591\u2591\u2591\u2591".
// filledColorHex colors only the filled portion; the empty portion uses a muted gray. barLength is the
// total number of characters in the bar (filled + empty).
    private String buildProgressBar(int percent, int barLength, String filledColorHex, String emptyColorHex)
    {
        int filled = (int) Math.round((percent / 100.0) * barLength);
        if (filled < 0) filled = 0;
        if (filled > barLength) filled = barLength;
        int empty = barLength - filled;
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (filled > 0) {
            sb.append("<col=").append(filledColorHex).append(">");
            for (int i = 0; i < filled; i++) sb.append('=');
            sb.append("</col>");
        }
        if (empty > 0) {
            sb.append("<col=").append(emptyColorHex).append(">");
            for (int i = 0; i < empty; i++) sb.append('-');
            sb.append("</col>");
        }
        sb.append("]");
        return sb.toString();
    }

    // Builds a subtle horizontal separator line using box-drawing characters.
    private String buildSeparator(String colorHex, int length)
    {
        StringBuilder sb = new StringBuilder("<col=").append(colorHex).append(">");
        for (int i = 0; i < length; i++) sb.append('\u2500');
        sb.append("</col>");
        return sb.toString();
    }

    @Override
    public String getTooltip()
    {
        boolean isFired = panel.getFiredAlertsForOverlay().contains(itemId);
        long currentPrice = panel.getCurrentPrice(itemId);
        String itemName = panel.getItemName(itemId);
        String name = itemName != null ? itemName : "Unknown";
        Long creationPrice = panel.getAlertCreationPrice(itemId);

        // Locked Option B color palette
        String nameHex = "FFB000";
        String labelHex = "E6C15A";
        String nowHex = "4EC8FF";
        String targetHex = "7DD3FC";
        String arrowHex = "FFFFFF";
        String inProgressHex = "FFB347";
        String successHex = "57D27A";
        String barFillHex = "FFC33D";
        String barEmptyHex = "2A2A2A";
        String timestampHex = "A3A3A3";
        String separatorHex = "666666";

        String nowPrice = String.format("%,d", currentPrice) + " gp";
        String targetPriceStr = String.format("%,d", targetPrice) + " gp";
        StringBuilder sb = new StringBuilder();
        sb.append("<col=").append(nameHex).append(">").append(name).append("</col><br><br>");
        sb.append("<col=").append(labelHex).append(">Now:</col>    <col=").append(nowHex).append(">").append(nowPrice).append("</col><br>");
        sb.append("<col=").append(labelHex).append(">Target:</col> <col=").append(targetHex).append(">").append(targetPriceStr).append("</col> <col=").append(arrowHex).append(">").append(isAbove ? "\u25B2" : "\u25BC").append("</col><br><br>");

        if (isFired) {
            Long firedTime = panel.getAlertFiredTime(itemId);
            String timeAgo = "";
            if (firedTime != null) {
                long elapsed = System.currentTimeMillis() - firedTime;
                long mins = elapsed / 60000;
                long secs = (elapsed % 60000) / 1000;
                timeAgo = mins > 0 ? mins + " min ago" : secs + "s ago";
            }
            long diff = Math.abs(currentPrice - targetPrice);
            String diffDirection = isAbove ? "above" : "below";
            sb.append("<col=").append(successHex).append(">\u2713 Target reached! (").append(String.format("%,d", diff)).append(" gp ").append(diffDirection).append(" target)</col><br><br>");
            sb.append(buildProgressBar(100, 10, barFillHex, barEmptyHex)).append(" <col=").append(barFillHex).append(">100%</col>");
            if (!timeAgo.isEmpty()) {
                sb.append("<br><col=").append(timestampHex).append(">Updated ").append(timeAgo).append("</col>");
            }
        } else {
            long diff = Math.abs(currentPrice - targetPrice);
            double pct = targetPrice != 0 ? (diff / (double) targetPrice) * 100.0 : 0;
            sb.append("<col=").append(inProgressHex).append(">").append(String.format("%,d", diff)).append(" gp away (").append(String.format("%.2f", pct)).append("%)</col><br><br>");

            int progressPercent = creationPrice != null ? calculateProgressPercent(creationPrice, currentPrice, targetPrice, isAbove) : -1;
            if (progressPercent >= 0) {
                sb.append(buildProgressBar(progressPercent, 10, barFillHex, barEmptyHex)).append(" <col=").append(barFillHex).append(">").append(progressPercent).append("%</col>");
            }
            // else: legacy alert with no creation price recorded - omit the progress bar entirely.
        }

        return sb.toString();
    }

    public int getItemId()
    {
        return itemId;
    }
}