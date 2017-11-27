package ar.edu.itba.tav.game_rooms.http;

import akka.NotUsed;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.*;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import ar.edu.itba.tav.game_rooms.http.dto.GameRoomDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

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

    private final ActorSystem actorSystem;
    private final ActorPath gameRoomsManagerPath;


    /**
     * Private constructor.
     *
     * @param system               The {@link ActorSystem}.
     * @param gameRoomsManagerPath The path of the game room manager.
     */
    private HttpServer(ActorSystem system, ActorPath gameRoomsManagerPath) {
        this.actorSystem = system;
        this.gameRoomsManagerPath = gameRoomsManagerPath;
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

    // ========================================================
    // Route configuration
    // ========================================================

    private static final String GAME_ROOMS_ENDPOINT = "game-rooms";

    /**
     * Configures the routes this server will handle.
     *
     * @return The {@link Route} object with the routes this server will handle.
     */
    private Route configureRoutes() {
        final Route[] routes = {
                path(createGameRoomPathMatcher(), createGameRoomRouteHandler()),
                path(deleteGameRoomPathMatcher(), removeGameRoomRouteHandler()),
        };

        return route(routes);
    }


    // ========================================================
    // Path matchers
    // ========================================================

    /**
     * @return A {@link PathMatcher0} to match the create game room request (i.e /game-rooms).
     */
    private PathMatcher0 createGameRoomPathMatcher() {
        return PathMatchers.segment(GAME_ROOMS_ENDPOINT);
    }

    /**
     * @return A {@link PathMatcher1} of {@link String} to match the delete game room request (i.e /game-rooms/:name).
     */
    private PathMatcher1<String> deleteGameRoomPathMatcher() {
        return PathMatchers.segment(GAME_ROOMS_ENDPOINT).slash(PathMatchers.segment()).concat(PathMatchers.pathEnd());
    }


    // ========================================================
    // Request route handlers
    // ========================================================

    /**
     * {@link Route} {@link Supplier} for a create game room request.
     * Handles the request by communicating with a {@link RequestHandlerActor},
     * sending a {@link RequestHandlerActor.CreateGameRoomRequest} message to it,
     * getting the game room name from the received json.
     *
     * @return The {@link Route} {@link Supplier}.
     */
    private Supplier<Route> createGameRoomRouteHandler() {
        return () ->
                post(() ->
                        entity(Jackson.unmarshaller(GameRoomDto.class),
                                gameRoomDto -> {
                                    tellToARequestHandlerActor(RequestHandlerActor.CreateGameRoomRequest
                                            .createRequest(gameRoomDto.getName()));
                                    return complete("OK!");
                                }));
    }

    /**
     * {@link Route} {@link Function} for a remove game room request, taking a string as an input argument.
     * Handles the request by communicating with a {@link RequestHandlerActor},
     * sending a {@link RequestHandlerActor.RemoveGameRoomRequest} message to it,
     * getting the game room name from the input argument.
     *
     * @return The {@link Function} that creates a {@link Route}.
     */
    private Function<String, Route> removeGameRoomRouteHandler() {
        return gameRoomName ->
                delete(() -> {
                    tellToARequestHandlerActor(RequestHandlerActor.RemoveGameRoomRequest
                            .createRequest(gameRoomName));
                    return complete("OK!");
                });
    }


    // ========================================================
    // Helper methods
    // ========================================================

    /**
     * Method that wraps logic to create a new {@link RequestHandlerActor}, and communicate with it,
     * sending the given {@code msg}, using {@link ActorRef#noSender()} as the message sender.
     *
     * @param msg The message to be sent.
     */
    private void tellToARequestHandlerActor(Object msg) {
        actorSystem.actorOf(RequestHandlerActor.getProps(gameRoomsManagerPath)).tell(msg, ActorRef.noSender());
    }


    // ========================================================
    // Factory methods
    // ========================================================

    /**
     * Creates a new {@link HttpServer}.
     *
     * @param actorSystem The {@link ActorSystem}.
     * @return A new {@link HttpServer}.
     */
    public static HttpServer createServer(ActorSystem actorSystem, ActorPath gameRoomsManagerPath) {
        LOGGER.info("Creating a new HttpServer instance using {} actor system", actorSystem);
        return new HttpServer(actorSystem, gameRoomsManagerPath);
    }
}
