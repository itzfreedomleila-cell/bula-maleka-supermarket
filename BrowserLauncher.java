package net.codejava;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Opens the home page in the default browser, but only after the
 * application has started successfully (ApplicationReadyEvent).
 */
@Component
public class BrowserLauncher {

    @Value("${server.port:8082}")
    private int port;

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        String url = "http://localhost:" + port + "/";
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                // "start" opens the default browser; the empty "" is the window title
                new ProcessBuilder("cmd", "/c", "start", "", url).inheritIO().start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
            System.out.println("Opening " + url);
        } catch (Exception e) {
            System.out.println("Could not open the browser automatically. Open " + url);
        }
    }
}