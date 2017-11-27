package ar.edu.itba.tav.game_rooms.http;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Class in charge of implementing an http server to access the platform.
 */
public class HttpServer extends AllDirectives {

    /**
     * The {@link Logger} object.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(HttpServer.class);


    /**
     * The {@link ServerBinding}.
     */
    private CompletionStage<ServerBinding> binding;

    /**
     * The {@link Http} instance.
     */
    private final Http http;

    /**
     * The {@link ActorMaterializer} instance.
     */
    private final ActorMaterializer materializer;

    /**
     * The {@link Flow} instance.
     */
    private final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow;


    /**
     * Private constructor.
     *
     * @param system The {@link ActorSystem}.
     */
    private HttpServer(ActorSystem system) {
        this.binding = null;
        this.http = Http.get(system);
        this.materializer = ActorMaterializer.create(system);
        this.routeFlow = configureRoutes().flow(system, materializer);
    }


    /**
     * Starts this server in the given {@code host}, and the given {@code port}.
     *
     * @param host The hostname where the server will listen.
     * @param port The port where the server will listen.
     */
    public synchronized void start(String host, int port) {
        Optional.ofNullable(this.binding)
                .ifPresent(b -> {
                    throw new IllegalStateException("Server already started.");
                });
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("The host must not be null or empty");
        }
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("Port is out of range");
        }
        LOGGER.info("Starting server in host {} with port {}", host, port);
        this.binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost(host, port), materializer);
        LOGGER.info("Server started");
    }

    /**
     * Starts this server in the given {@code port}, using "localhost" as hostname.
     *
     * @param port The port where the server will listen.
     */
    public void start(int port) {
        start("localhost", port);
    }

    /**
     * Stops this server.
     */
    public synchronized void stop() {
        final CompletionStage<ServerBinding> serverBinding = Optional.ofNullable(this.binding)
                .orElseThrow(() -> new IllegalStateException("Server is not started."));
        LOGGER.info("Stopping server");
        serverBinding.thenCompose(ServerBinding::unbind);
        this.binding = null;
        LOGGER.info("Server stopped");

    }

    /**
     * Configures the routes this server will handle.
     *
     * @return The {@link Route} object with the routes this server will handle.
     */
    private Route configureRoutes() {
        return route(
                path("", () -> get(() -> complete("Root"))),
                path("hello-world", () -> get(() -> complete("Hello World!")))
        );
    }

    /**
     * Creates a new {@link HttpServer}.
     *
     * @param actorSystem The {@link ActorSystem}.
     * @return A new {@link HttpServer}.
     */
    public static HttpServer createServer(ActorSystem actorSystem) {
        LOGGER.info("Creating a new HttpServer instance using {} actor system", actorSystem);
        return new HttpServer(actorSystem);
    }
}
