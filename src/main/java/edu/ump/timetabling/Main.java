package edu.ump.timetabling;

import com.sun.net.httpserver.HttpServer;
import edu.ump.timetabling.controller.ApiHandler;
import edu.ump.timetabling.controller.StaticFileHandler;
import edu.ump.timetabling.dataset.UmpDataRepository;
import edu.ump.timetabling.util.I18nManager;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Main application entrypoint for the Academic Examination Timetabling System.
 */
public class Main {

    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        // Port resolution: CLI arg > PORT env var (Render/Cloud) > default 8080
        int port = DEFAULT_PORT;
        String portEnv = System.getenv("PORT");
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        } else if (portEnv != null && !portEnv.isEmpty()) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            UmpDataRepository dataRepository = new UmpDataRepository();
            I18nManager i18nManager = new I18nManager();

            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // API Endpoint routing
            ApiHandler apiHandler = new ApiHandler(dataRepository, i18nManager);
            server.createContext("/api", apiHandler);

            // Static Web Assets routing
            String webDir = "src/main/resources/web";
            if (!new File(webDir).exists()) {
                webDir = "web";
            }
            StaticFileHandler staticHandler = new StaticFileHandler("/web", webDir);
            server.createContext("/", staticHandler);

            server.setExecutor(Executors.newFixedThreadPool(8));
            server.start();

            System.out.println("================================================================================");
            System.out.println("  ACADEMIC EXAMINATION TIMETABLING RESEARCH PLATFORM (Java Web Edition)");
            System.out.println("  Universiti Malaysia Pahang (UMP) Optimization Engine");
            System.out.println("================================================================================");
            System.out.println("  * Server running at: http://localhost:" + port);
            System.out.println("  * Default Instance : Dummy UMP Dataset");
            System.out.println("  * Initial Heuristics: LD, LE, LWD, SD LD, SD LE, SD LWD");
            System.out.println("  * Metaheuristics    : HC Move, HC Swap, LAHC, SCHC");
            System.out.println("  * Timetable Views   : Xit, Yir, Zrt, Results DataGrid");
            System.out.println("  * Multi-language    : English (en), Bahasa Melayu (ms), Chinese (zh)");
            System.out.println("================================================================================");
            System.out.println("  Press Ctrl+C to stop the server.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down Timetabling server...");
                server.stop(1);
            }));

        } catch (IOException e) {
            System.err.println("Failed to start server on port " + port + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
