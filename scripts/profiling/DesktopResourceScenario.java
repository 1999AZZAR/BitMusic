import java.awt.EventQueue;
import java.awt.Window;
import java.awt.KeyboardFocusManager;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.SocketAddress;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public final class DesktopResourceScenario {
    private static Object singleton(String name) throws Exception {
        return Class.forName("com.music.bitchord.desktop." + name).getField("INSTANCE").get(null);
    }

    private static void action(String phase, Runnable action) {
        EventQueue.invokeLater(() -> {
            try {
                action.run();
                System.out.println("PROFILE_PHASE=" + phase);
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.exit(2);
            }
        });
    }

    private static void shortcut(String name) {
        try {
            Object owner = singleton("DesktopKeyboardShortcuts");
            Object callback = owner.getClass().getMethod(name).invoke(owner);
            if (callback == null) throw new IllegalStateException("Player controls not mounted");
            Class.forName("kotlin.jvm.functions.Function0").getMethod("invoke").invoke(callback);
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static void window(boolean show) {
        try {
            Object visibility = singleton("DesktopWindowVisibility");
            visibility.getClass().getMethod("setKeepRunningWhenClosed", boolean.class).invoke(visibility, true);
            visibility.getClass().getMethod(show ? "show" : "onCloseRequest").invoke(visibility);
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        double timeScale = args.length > 1 ? Double.parseDouble(args[1]) : 1.0;
        if (!Double.isFinite(timeScale) || timeScale < 0.5 || timeScale > 2) {
            throw new IllegalArgumentException("Time scale must be between 0.5 and 2");
        }
        Path audio = Path.of(args[0]).toAbsolutePath();
        Preferences preferences = Preferences.userRoot().node("com.music.bitchord.desktop");
        String[] fields = {"local:resource-probe", "Resource probe", "BitMusic", "", "10:00", "", "", "",
            audio.toUri().toString(), audio.toString(), "false", "false"};
        String song = String.join("|", java.util.Arrays.stream(fields)
            .map(value -> Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)))
            .toList());
        fields[0] += "2";
        String second = String.join("|", java.util.Arrays.stream(fields)
            .map(value -> Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)))
            .toList());
        preferences.put("queue", song + "\n" + second);
        preferences.put("volume", "0");
        preferences.putBoolean("autoplay", false);
        preferences.putBoolean("animated_canvas", false);
        preferences.put("repeat_mode", "ALL");
        preferences.flush();
        ProxySelector.setDefault(new ProxySelector() {
            public java.util.List<Proxy> select(URI uri) {
                return java.util.List.of(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 9)));
            }
            public void connectFailed(URI uri, SocketAddress address, java.io.IOException failure) {}
        });
        Object persistence = Class.forName("com.music.bitchord.desktop.DesktopPersistence").getConstructor().newInstance();
        var queue = (java.util.List<?>) persistence.getClass().getMethod("queue").invoke(persistence);
        if (queue.size() != 2) throw new IllegalStateException("Isolated queue fixture was not loaded");
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> true);

        var scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> EventQueue.invokeLater(() -> {
            for (Window window : Window.getWindows()) {
                window.setEnabled(false);
                window.setFocusableWindowState(false);
                if (window instanceof JFrame frame && !(frame.getGlassPane() instanceof InputBlocker)) {
                    var blocker = new InputBlocker();
                    blocker.setOpaque(false);
                    blocker.addMouseListener(new java.awt.event.MouseAdapter() {});
                    blocker.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {});
                    frame.setGlassPane(blocker);
                    blocker.setVisible(true);
                }
            }
        }), 1, 1, TimeUnit.SECONDS);
        scheduler.schedule(() -> action("idle", () -> {}), Math.round(20 * timeScale), TimeUnit.SECONDS);
        scheduler.schedule(() -> action("playback", () -> shortcut("getOnNext")), Math.round(35 * timeScale), TimeUnit.SECONDS);
        scheduler.schedule(() -> action("hidden-playback", () -> window(false)), Math.round(65 * timeScale), TimeUnit.SECONDS);
        scheduler.schedule(() -> action("hidden-paused", () -> shortcut("getOnPlayPause")), Math.round(95 * timeScale), TimeUnit.SECONDS);
        scheduler.schedule(() -> action("visible-paused", () -> window(true)), Math.round(120 * timeScale), TimeUnit.SECONDS);
        scheduler.schedule(() -> System.exit(0), Math.round(145 * timeScale), TimeUnit.SECONDS);
        Class.forName("com.music.bitchord.desktop.MainKt").getMethod("main").invoke(null);
    }

    private static final class InputBlocker extends JPanel {}
}
