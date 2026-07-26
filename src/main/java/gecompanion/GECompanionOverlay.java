package gecompanion;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import javax.inject.Inject;
import java.awt.*;
import java.util.Map;
import java.util.Set;

public class GECompanionOverlay extends Overlay {
    private final GECompanionPlugin plugin;
    private final GECompanionConfig config;
    private GECompanionPanel panel;

    @Inject
    public GECompanionOverlay(GECompanionPlugin plugin, GECompanionConfig config) {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.LOW);
    }

    public void setPanel(GECompanionPanel panel) {
        this.panel = panel;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showPriceAlertOverlay()) return null;
        if (config.overlayDisplayMode() == OverlayDisplayMode.COMPACT) return null;
        if (panel == null) return null;

        Map<Integer, String> alerts = panel.getPriceAlertsForOverlay();
        Set<Integer> fired = panel.getFiredAlertsForOverlay();

        java.util.Map<Integer, String> overlayItems = new java.util.LinkedHashMap<>();
        if (alerts != null) {
            for (Map.Entry<Integer, String> entry : alerts.entrySet()) {
                if (entry.getValue().contains(":overlay")) {
                    overlayItems.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (fired != null) {
            for (Integer firedId : fired) {
                if (!overlayItems.containsKey(firedId)) {
                    overlayItems.put(firedId, "fired");
                }
            }
        }
        if (overlayItems.isEmpty()) return null;

        int iconSize = 28;
        int width = 145;
        int padding = 4;
        int gap = 3;

        int totalHeight = overlayItems.size() * (iconSize + padding * 2 + 12 + gap);

// Calculate dynamic width based on longest text
        graphics.setFont(net.runelite.client.ui.FontManager.getRunescapeSmallFont());
        java.awt.FontMetrics fm = graphics.getFontMetrics();
        int textX = padding + iconSize + 6;
        for (Map.Entry<Integer, String> e2 : overlayItems.entrySet()) {
            String n = panel.getItemName(e2.getKey());
            long cp = panel.getCurrentPrice(e2.getKey());
            String ps = cp > 0 ? String.format("%,d", cp) + " gp" : "?";
            String[] parts2 = e2.getValue().split(":");
            long tp = 0;
            if (parts2.length >= 2) try { tp = Long.parseLong(parts2[1]); } catch (NumberFormatException ex) {}
            String ts = tp > 0 ? String.format("%,d", tp) + " gp" : "";
            int nameW = fm.stringWidth(n != null ? n : "Unknown") + textX + padding;
            int nowW = fm.stringWidth("Now:    " + ps) + textX + padding;
            int targetW = fm.stringWidth("Target: " + ts + " ▲") + textX + padding;
            width = Math.max(width, Math.max(nameW, Math.max(nowW, targetW)));
        }
        int y = 0;

        for (Map.Entry<Integer, String> entry : overlayItems.entrySet()) {
            int itemId = entry.getKey();
            String alertValue = entry.getValue();
            boolean isFired = alertValue.equals("fired") || (fired != null && fired.contains(itemId));

            long currentPrice = panel.getCurrentPrice(itemId);
            long targetPrice = 0;
            boolean isAbove = false;
            if (isFired) {
                Long ft = panel.getFiredAlertTarget(itemId);
                Boolean fd = panel.getFiredAlertDirection(itemId);
                if (ft != null) targetPrice = ft;
                if (fd != null) isAbove = fd;
            } else if (alertValue.contains(":")) {
                String[] parts = alertValue.split(":");
                if (parts.length >= 2) {
                    isAbove = parts[0].equals("above");
                    try {
                        targetPrice = Long.parseLong(parts[1]);
                    } catch (NumberFormatException e) {
                    }
                }
            }

            Color textColor;
            if (isFired) {
                textColor = config.alertFiredColor();
            } else {
                textColor = config.alertBellColor();
            }

            int boxHeight = isFired ? iconSize + padding * 2 + 24 : iconSize + padding * 2 + 12;

            // Box background
            graphics.setColor(new Color(26, 26, 26, 186));
            graphics.fillRoundRect(0, y, width, boxHeight, 6, 6);

            // Box border - gold for active, fired color for triggered
            graphics.setColor(new Color(60, 55, 50));
            graphics.drawRoundRect(0, y, width - 1, boxHeight - 1, 6, 6);

            // Item icon
            java.awt.image.BufferedImage icon = panel.getItemIconForOverlay(itemId);
            if (icon != null) {
                graphics.drawImage(icon, padding, y + padding, iconSize, iconSize, null);
            } else {
                graphics.setColor(new Color(60, 55, 50));
                graphics.fillRect(padding, y + padding, iconSize, iconSize);
            }

            textX = padding + iconSize + 6;
            String name = panel.getItemName(itemId);
            String priceStr = currentPrice > 0 ? String.format("%,d", currentPrice) : "?";
            String targetPriceStr = targetPrice > 0 ? String.format("%,d", targetPrice) : "?";
            String direction = isAbove ? " ▲" : " ▼";

            graphics.setColor(textColor);
            graphics.setFont(net.runelite.client.ui.FontManager.getRunescapeSmallFont());

            graphics.setFont(net.runelite.client.ui.FontManager.getRunescapeSmallFont());

// Item name — burnt orange
            graphics.setColor(new Color(0xFF981F));
            graphics.drawString(name != null ? name : "Unknown", textX, y + padding + 11);
// "Now:" label in gold, price in white or fired color
            java.awt.FontMetrics fmLabel = graphics.getFontMetrics();
            int labelWidth = fmLabel.stringWidth("Target: ");
            graphics.setColor(new Color(0xD4AF37));
            graphics.drawString("Now:    ", textX, y + padding + 23);
            graphics.setColor(isFired ? config.alertFiredColor() : Color.WHITE);
            graphics.drawString(priceStr + " gp", textX + labelWidth, y + padding + 23);
// Target or triggered
            if (isFired) {
                graphics.setColor(config.alertFiredColor());
                String direction2 = isAbove ? " ▲" : " ▼";
                String targetStr2 = targetPrice > 0 ? targetPriceStr + " gp" + direction2 : "?";
                graphics.setColor(new Color(0xD4AF37));
                graphics.drawString("Target:", textX, y + padding + 35);
                graphics.setColor(config.alertFiredColor());
                graphics.drawString(" " + targetStr2, textX + 45, y + padding + 35);
                graphics.drawString("Target reached! ✓", textX, y + padding + 47);
            } else {
                String targetStr = targetPrice > 0 ? targetPriceStr + " gp " + direction : "?";
                graphics.setColor(new Color(0xD4AF37));
                graphics.drawString("Target: ", textX, y + padding + 35);
                graphics.setColor(Color.WHITE);
                graphics.drawString(targetStr, textX + labelWidth, y + padding + 35);
            }

            y += boxHeight + gap;
        }

        return new Dimension(width, y);
    }
}