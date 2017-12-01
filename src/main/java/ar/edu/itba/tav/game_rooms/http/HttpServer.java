package ar.edu.itba.tav.game_rooms.http;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.*;
import akka.http.javadsl.model.headers.Location;
import akka.http.javadsl.server.*;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import ar.edu.itba.tav.game_rooms.http.dto.GameRoomDto;
import ar.edu.itba.tav.game_rooms.messages.GameRoomOperationMessages.GameRoomCreationResult;
import ar.edu.itba.tav.game_rooms.messages.GameRoomOperationMessages.GameRoomDataMessage;
import ar.edu.itba.tav.game_rooms.messages.GameRoomOperationMessages.GameRoomRemovalResult;
import ar.edu.itba.tav.game_rooms.messages.GameRoomOperationMessages.PlayerOperationResult;
import ar.edu.itba.tav.game_rooms.messages.HttpRequestMessages;
import ar.edu.itba.tav.game_rooms.messages.HttpRequestMessages.*;
import ar.edu.itba.tav.game_rooms.messages.SystemMonitorMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squbs.marshallers.MarshalUnmarshal;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
     * The {@link ActorSystem} to which this server must communicate with.
     */
    private final ActorSystem actorSystem;

    /**
     * An {@link ActorRef} to the game rooms manager.
     */
    private final ActorRef gameRoomManager;

    /**
     * An {@link ActorRef} to the system monitor.
     */
    private final ActorRef systemMonitor;


    /**
     * Private constructor.
     *
     * @param system          The {@link ActorSystem}.
     * @param gameRoomManager An {@link ActorRef} to the game rooms manager.
     * @param systemMonitor   An {@link ActorRef} to the system monitor.
     */
    private HttpServer(ActorSystem system, ActorRef gameRoomManager, ActorRef systemMonitor) {
        this.actorSystem = system;
        this.gameRoomManager = gameRoomManager;
        this.systemMonitor = systemMonitor;
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
    private static final String PLAYERS_ENDPOINT = "players";
    private static final String SYSTEM_MONITOR_ENDPOINT = "monitor";

    /**
     * Configures the routes this server will handle.
     *
     * @return The {@link Route} object with the routes this server will handle.
     */
    private Route configureRoutes() {
        final Route[] routes = {
                // Game rooms
                path(gameRoomsCollectionPathMatcher(), getAllGameRoomsRouteHandler()),
                path(specificGameRoomPathMatcher(), getGameRoomRouteHandler()),
                path(gameRoomsCollectionPathMatcher(), createGameRoomRouteHandler()),
                path(specificGameRoomPathMatcher(), removeGameRoomRouteHandler()),
                path(gameRoomAndPlayerPathMatcher(), addPlayerRouteHandler()),
                path(gameRoomAndPlayerPathMatcher(), removePlayerRouteHandler()),
                // System monitor
                path(getSystemMonitorPathMatcher(), getMonitorDataRouteHandler()),
        };

        return route(routes);
    }


    // ========================================================
    // Path matchers
    // ========================================================

    /**
     * @return A {@link PathMatcher0} to match the game rooms collection path (i.e /game-rooms).
     */
    private PathMatcher0 gameRoomsCollectionPathMatcher() {
        return PathMatchers.segment(GAME_ROOMS_ENDPOINT);
    }

    /**
     * @return A {@link PathMatcher1} of {@link String} to match the specific game room path (i.e /game-rooms/:name).
     */
    private PathMatcher1<String> specificGameRoomPathMatcher() {
        return PathMatchers.segment(GAME_ROOMS_ENDPOINT).slash(PathMatchers.segment()).concat(PathMatchers.pathEnd());
    }

    /**
     * @return A {@link PathMatcher2} of {@link String} and {@link Long} to match a game room and a player
     * (i.e /game-rooms/:name/players/:player-id).
     */
    private PathMatcher2<String, Long> gameRoomAndPlayerPathMatcher() {
        return PathMatchers.segment(GAME_ROOMS_ENDPOINT)
                .slash(PathMatchers.segment())
                .slash(PathMatchers.segment(PLAYERS_ENDPOINT))
                .slash(PathMatchers.longSegment())
                .concat(PathMatchers.pathEnd());
    }

    /**
     * @return A {@link PathMatcher0} to match the system monitor path (i.e /monitor).
     */
    private PathMatcher0 getSystemMonitorPathMatcher() {
        return PathMatchers.segment(SYSTEM_MONITOR_ENDPOINT);
    }


    // ========================================================
    // Request route handlers
    // ========================================================

    /**
     * {@link Route} {@link Supplier} for a get all game rooms request.
     * Handles the request by communicating with a {@link HttpRequestHandlerActor},
     * sending a {@link GetAllGameRoomsRequest} message to it.
     *
     * @return The {@link Route} {@link Supplier}.
     */
    private Supplier<Route> getAllGameRoomsRouteHandler() {
        return () ->
                get(() ->
                        extract(Function.identity(),
                                ctx -> {
                                    final HttpResponse response = getAllGameRoomsResponse(ctx);
                                    return complete(response);
                                }));
    }

    /**
     * {@link Route} {@link Function} for a get game room by name request.
     * Handles the request by communicating with a {@link HttpRequestHandlerActor},
     * sending a {@link GetGameRoomRequest} message to it.
     *
     * @return The {@link Function} that creates a {@link Route}.
     */
    private Function<String, Route> getGameRoomRouteHandler() {
        return gameRoomName ->
                get(() ->
                        extract(Function.identity(),
                                ctx -> {
                                    final HttpResponse response = getGameRoomResponse(gameRoomName, ctx);
                                    return complete(response);
                                }));
    }


    /**
     * {@link Route} {@link Supplier} for a create game room request.
     * Handles the request by communicating with a {@link HttpRequestHandlerActor},
     * sending a {@link CreateGameRoomRequest} message to it,
     * getting the game room name from the received json.
     *
     * @return The {@link Route} {@link Supplier}.
     */
    private Supplier<Route> createGameRoomRouteHandler() {
        return () ->
                post(() ->
                        extract(Function.identity(),
                                ctx -> entity(Jackson.unmarshaller(GameRoomDto.class),
                                        gameRoomDto -> {
                                            final HttpResponse response = createGameRoomResponse(ctx, gameRoomDto);
                                            return complete(response);
                                        })));
    }

    /**
     * {@link Route} {@link Function} for a remove game room request, taking a string as an input argument.
     * Handles the request by communicating with a {@link HttpRequestHandlerActor},
     * sending a {@link RemoveGameRoomRequest} message to it,
     * getting the game room name from the input argument.
     *
     * @return The {@link Function} that creates a {@link Route}.
     */
    private Function<String, Route> removeGameRoomRouteHandler() {
        return gameRoomName ->
                delete(() -> {
                    final HttpResponse response = removeGameRoomResponse(gameRoomName);
                    return complete(response);
                });
    }

    /**
     * {@link Route} {@link BiFunction} for an add player to game room request,
     * taking a string and a long as input argument.
     * Handles the request by communicating with a {@link HttpRequestHandlerActor},
     * sending a {@link AddPlayerToGameRoomRequest} message to it,
     * getting the game room name and player id from the input arguments.
     *
     * @return The {@link BiFunction} that creates the {@link Route}.
     */
    private BiFunction<String, Long, Route> addPlayerRouteHandler() {
        return (gameRoomName, playerId) ->
                put(() -> {
                    final HttpResponse response = addPlayerResponse(gameRoomName, playerId);
                    return complete(response);
                });
    }

    /**
     * {@link Route} {@link BiFunction} for a remove player from game room request,
     * taking a string and a long as input argument.
     * Handles the request by communicating with a {@link HttpRequestHandlerActor},
     * sending a {@link AddPlayerToGameRoomRequest} message to it,
     * getting the game room name and player id from the input arguments.
     *
     * @return The {@link BiFunction} that creates the {@link Route}.
     */
    private BiFunction<String, Long, Route> removePlayerRouteHandler() {
        return (gameRoomName, playerId) ->
                delete(() -> {
                    final HttpResponse response = removePlayerResponse(gameRoomName, playerId);
                    return complete(response);
                });
    }

    /**
     * {@link Route} {@link Supplier} for a get system monitor data request.
     * Handles the request by communicating with a {@link HttpRequestHandlerActor},
     * sending a {@link GetSystemMonitorDataRequest} message to it.
     *
     * @return The {@link Route} {@link Supplier}.
     */
    private Supplier<Route> getMonitorDataRouteHandler() {
        return () ->
                get(() ->
                        complete(getSystemMonitorDataResponse()));
    }


    // ========================================================
    // Responses methods
    // ========================================================

    /**
     * Creates an {@link HttpResponse} for a game rooms retrieval request.
     *
     * @param context The {@link RequestContext} from which request data will be taken.
     * @return The {@link HttpResponse} for this request.
     */
    private HttpResponse getAllGameRoomsResponse(RequestContext context) {
        final long timeout = 5000;
        final GetAllGameRoomsRequest request = GetAllGameRoomsRequest.createRequest(timeout);
        try {
            final List<GameRoomDataMessage> gameRoomsData = askToARequestHandlerActor(request, timeout);
            final List<GameRoomDto> gameRooms = gameRoomsData.stream()
                    .map(game -> {
                        final Uri locationUri = context.getRequest().getUri().addPathSegment(game.getName());
                        return new GameRoomDto(game.getName(), game.getCapacity(), game.getPlayers(), locationUri);
                    })
                    .collect(Collectors.toList());
            final CompletionStage<RequestEntity> marshalled =
                    new MarshalUnmarshal(actorSystem.dispatcher(), materializer)
                            .apply(Jackson.marshaller(), gameRooms);

            return HttpResponse.create()
                    .withStatus(StatusCodes.OK)
                    .withEntity(marshalled.toCompletableFuture().get());

        } catch (TimeoutException e) {
            return HttpResponse.create().withStatus(StatusCodes.REQUEST_TIMEOUT);
        } catch (Exception e) {
            return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Creates an {@link HttpResponse} for a game room retrieval by name request.
     *
     * @param gameRoomName The name of the game room to be removed.
     * @param context      The {@link RequestContext} from which request data will be taken.
     * @return The {@link HttpResponse} for this request.
     */
    private HttpResponse getGameRoomResponse(String gameRoomName, RequestContext context) {
        final long timeout = 5000;
        final HttpRequestMessages.GetGameRoomRequest request = GetGameRoomRequest.createRequest(gameRoomName, timeout);
        try {
            return this.<Optional<GameRoomDataMessage>>askToARequestHandlerActor(request, timeout)
                    .map(game -> {
                        final Uri locationUri = context.getRequest().getUri().addPathSegment(game.getName());
                        return new GameRoomDto(game.getName(), game.getCapacity(), game.getPlayers(), locationUri);
                    })
                    .map(gameDto -> new MarshalUnmarshal(actorSystem.dispatcher(), materializer)
                            .apply(Jackson.marshaller(), gameDto))
                    .map(CompletionStage::toCompletableFuture)
                    .map(cf -> {
                        try {
                            return cf.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException();
                        }
                    })
                    .map(entity -> HttpResponse.create().withStatus(StatusCodes.OK).withEntity(entity))
                    .orElse(HttpResponse.create().withStatus(StatusCodes.NOT_FOUND));

        } catch (TimeoutException e) {
            return HttpResponse.create().withStatus(StatusCodes.REQUEST_TIMEOUT);
        } catch (Exception e) {
            return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Creates an {@link HttpResponse} for a game room creation request.
     *
     * @param context     The {@link RequestContext} from which request data will be taken.
     * @param gameRoomDto The {@link GameRoomDto} holding data for the new game room.
     * @return The {@link HttpResponse} for this request.
     */
    private HttpResponse createGameRoomResponse(RequestContext context, GameRoomDto gameRoomDto) {
        final long timeout = 2000;
        final String gameRoomName = gameRoomDto.getName();
        final int capacity = gameRoomDto.getCapacity();
        final CreateGameRoomRequest request = CreateGameRoomRequest.createRequest(gameRoomName, capacity, timeout);
        try {
            switch ((GameRoomCreationResult) askToARequestHandlerActor(request, timeout)) {
                case CREATED:
                    return HttpResponse.create()
                            .withStatus(StatusCodes.CREATED)
                            .addHeader(Location.create(context.getRequest().getUri().addPathSegment(gameRoomName)));
                case NAME_REPEATED:
                    return HttpResponse.create().withStatus(StatusCodes.CONFLICT);
                case INVALID:
                    return HttpResponse.create().withStatus(StatusCodes.UNPROCESSABLE_ENTITY);
                case FAILURE:
                    return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
            }
        } catch (TimeoutException e) {
            return HttpResponse.create().withStatus(StatusCodes.REQUEST_TIMEOUT);
        } catch (Exception e) {
            return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
        }
        // Won't never reach here
        return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
    }

    /**
     * Creates an {@link HttpResponse} for a game room removal request.
     *
     * @param gameRoomName The name of the game room to be removed.
     * @return The {@link HttpResponse} for this request.
     */
    private HttpResponse removeGameRoomResponse(String gameRoomName) {
        final long timeout = 5000;
        final RemoveGameRoomRequest request = RemoveGameRoomRequest.createRequest(gameRoomName, timeout);
        try {
            switch ((GameRoomRemovalResult) askToARequestHandlerActor(request, timeout)) {
                case NO_SUCH_GAME_ROOM:
                case REMOVED:
                    return HttpResponse.create().withStatus(StatusCodes.NO_CONTENT);
                case FAILURE:
                    return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
            }
        } catch (TimeoutException e) {
            return HttpResponse.create().withStatus(StatusCodes.REQUEST_TIMEOUT);
        } catch (Exception e) {
            return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
        }
        // Won't never reach here
        return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
    }

    /**
     * Creates an {@link HttpResponse} for adding a player into a game room request.
     *
     * @param gameRoomName The name of the game room to be operated into.
     * @param playerId     The id of the player being added into the game room.
     * @return The {@link HttpResponse} for this request.
     */
    private HttpResponse addPlayerResponse(String gameRoomName, long playerId) {
        final long timeout = 2000;
        return playerOperationResponse(timeout,
                AddPlayerToGameRoomRequest.createRequest(gameRoomName, playerId, timeout));
    }

    /**
     * Creates an {@link HttpResponse} for removing a player from a game room request.
     *
     * @param gameRoomName The name of the game room to be operated into.
     * @param playerId     The id of the player being removed from the game room.
     * @return The {@link HttpResponse} for this request.
     */
    private HttpResponse removePlayerResponse(String gameRoomName, long playerId) {
        final long timeout = 2000;
        return playerOperationResponse(timeout,
                RemovePlayerFromGameRoomRequest.createRequest(gameRoomName, playerId, timeout));
    }

    /**
     * Creates an {@link HttpResponse} for getting the system monitor data.
     *
     * @return The {@link HttpResponse} for this request.
     */
    private HttpResponse getSystemMonitorDataResponse() {
        final long timeout = 2000;
        final GetSystemMonitorDataRequest request = GetSystemMonitorDataRequest.getMessage(timeout);
        try {
            final Object result = askToARequestHandlerActor(request, timeout);
            if (result instanceof SystemMonitorMessages.SystemMonitorData) {

                final SystemMonitorMessages.SystemMonitorData data = (SystemMonitorMessages.SystemMonitorData) result;
                final CompletionStage<RequestEntity> marshalled =
                        new MarshalUnmarshal(actorSystem.dispatcher(), materializer)
                                .apply(Jackson.marshaller(), data);
                return HttpResponse.create()
                        .withStatus(StatusCodes.OK)
                        .withEntity(marshalled.toCompletableFuture().get());

            } else if (result instanceof SystemMonitorMessages.SystemMonitorNotStartedMessage) {
                return HttpResponse.create()
                        .withStatus(StatusCodes.SERVICE_UNAVAILABLE);
            } else {
                return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
            }
        } catch (TimeoutException e) {
            return HttpResponse.create().withStatus(StatusCodes.REQUEST_TIMEOUT);
        } catch (Exception e) {
            return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Creates an {@link HttpResponse} for operating with a player in a game room request.
     *
     * @param timeout The timeout for the request
     * @param request The object to be sent to the {@link HttpRequestHandlerActor} as a message.
     * @return The {@link HttpResponse} for this request.
     */
    private HttpResponse playerOperationResponse(long timeout, Object request) {
        try {
            switch ((PlayerOperationResult) askToARequestHandlerActor(request, timeout)) {
                case SUCCESSFUL:
                    return HttpResponse.create().withStatus(StatusCodes.NO_CONTENT);
                case NO_SUCH_GAME_ROOM:
                    return HttpResponse.create().withStatus(StatusCodes.NOT_FOUND);
                case FULL_GAME_ROOM:
                    return HttpResponse.create().withStatus(StatusCodes.CONFLICT);
            }
        } catch (TimeoutException e) {
            return HttpResponse.create().withStatus(StatusCodes.REQUEST_TIMEOUT);
        } catch (Exception e) {
            return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
        }
        // Won't never reach here
        return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
    }


    // ========================================================
    // Helper methods
    // ========================================================

    /**
     * Passes the given {@code question} to a new {@link HttpRequestHandlerActor}.
     * If the request is longer than the given {@code timeout}, an Exception is thrown.
     *
     * @param question The request to be sent.
     * @param timeout  The duration of the request.
     * @return The Object returned as a response.
     * @throws Exception If anything goes wrong.
     */
    private <T> T askToARequestHandlerActor(Object question, long timeout) throws Exception {
        final ActorRef handlerActor = actorSystem
                .actorOf(HttpRequestHandlerActor.getProps(gameRoomManager, systemMonitor));
        final FiniteDuration duration = Duration.create(timeout, TimeUnit.MILLISECONDS);
        final Future<?> future = Patterns.ask(handlerActor, question, new Timeout(duration));

        //noinspection unchecked
        return (T) Await.result(future, duration);
    }


    // ========================================================
    // Factory methods
    // ========================================================

    /**
     * Creates a new {@link HttpServer}.
     *
     * @param actorSystem     The {@link ActorSystem}.
     * @param gameRoomManager An {@link ActorRef} to the game rooms manager.
     * @param systemMonitor   An {@link ActorRef} to the system monitor.
     * @return A new {@link HttpServer}.
     */
    public static HttpServer createServer(ActorSystem actorSystem, ActorRef gameRoomManager, ActorRef systemMonitor) {
        LOGGER.info("Creating a new HttpServer instance using {} actor system", actorSystem);
        return new HttpServer(actorSystem, gameRoomManager, systemMonitor);
    }
}
