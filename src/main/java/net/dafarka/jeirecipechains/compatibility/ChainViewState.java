package net.dafarka.jeirecipechains.compatibility;

public class ChainViewState {
    public static double zoom = 1.0;
    public static int offsetX = 0;
    public static int offsetY = 0;

    // Zoom limits
    public static final double MIN_ZOOM = 0.5;
    public static final double MAX_ZOOM = 2.5;

    public static void zoomIn() {
        zoom = Math.min(MAX_ZOOM, zoom + 0.1);
    }

    public static void zoomOut() {
        zoom = Math.max(MIN_ZOOM, zoom - 0.1);
    }

    public static void move(int dx, int dy) {
        offsetX += dx;
        offsetY += dy;
    }
}
