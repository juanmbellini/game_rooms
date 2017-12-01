package ar.edu.itba.tav.game_rooms.http;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import akka.util.Timeout;
import ar.edu.itba.tav.game_rooms.messages.GameRoomOperationMessages.*;
import ar.edu.itba.tav.game_rooms.messages.HttpRequestMessages.*;
import ar.edu.itba.tav.game_rooms.messages.SystemMonitorMessages.GetDataMessage;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * {@link akka.actor.Actor} in charge of handling
 */
/* package */ class HttpRequestHandlerActor extends AbstractActor {

    /**
     * The {@link ActorRef} for the game room manager.
     */
    private final ActorRef gameRoomManager;

    /**
     * The {@link ActorRef} for the system monitor.
     */
    private final ActorRef systemMonitor;

    /**
     * Private constructor.
     *
     * @param gameRoomManager The {@link ActorRef} for the game room manager.
     * @param systemMonitor   The {@link ActorRef} for the system monitor.ø
     */
    private HttpRequestHandlerActor(ActorRef gameRoomManager, ActorRef systemMonitor) {
        this.gameRoomManager = gameRoomManager;
        this.systemMonitor = systemMonitor;
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(GetAllGameRoomsRequest.class, this::handleGetAllGameRoomsRequest)
                .match(GetGameRoomRequest.class, this::handleGetGameRoomRequest)
                .match(CreateGameRoomRequest.class, this::handleCreateGameRoomRequest)
                .match(RemoveGameRoomRequest.class, this::handleRemoveGameRoomRequest)
                .match(AddPlayerToGameRoomRequest.class, this::handleAddPlayerToGameRoomRequest)
                .match(RemovePlayerFromGameRoomRequest.class, this::handleRemovePlayerToGameRoomRequest)
                .match(GetSystemMonitorDataRequest.class, msg -> this.handleSystemMonitorRequest())
                .build();
    }

    /**
     * Handles a {@link GetAllGameRoomsRequest}.
     *
     * @param request The request to be handled.
     */
    private void handleGetAllGameRoomsRequest(GetAllGameRoomsRequest request) {
        final GetAllGameRoomsMessage msg = GetAllGameRoomsMessage.getMessage();
        final List<GameRoomDataMessage> gameRooms = askTheGameRoomManager(msg, request.getTimeout(), new LinkedList<>());
        reportSender(gameRooms);
    }

    /**
     * Handles a {@link GetGameRoomRequest}.
     *
     * @param request The request to be handled.
     */
    private void handleGetGameRoomRequest(GetGameRoomRequest request) {
        final GetSpecificGameRoomMessage msg = GetSpecificGameRoomMessage.getMessage(request.getGameRoomName());
        final Optional<GameRoomDataMessage> gameRoom =
                askTheGameRoomManager(msg, request.getTimeout(), Optional.empty());
        reportSender(gameRoom);
    }

    /**
     * Handles a {@link CreateGameRoomRequest}.
     *
     * @param request The request to be handled.
     */
    private void handleCreateGameRoomRequest(CreateGameRoomRequest request) {
        final String gameRoomName = request.getGameRoomName();
        final int capacity = request.getCapacity();
        final CreateGameRoomMessage msg = CreateGameRoomMessage.getMessage(gameRoomName, capacity);
        final GameRoomCreationResult result = askTheGameRoomManagerToCreateAGameRoom(msg, request.getTimeout());
        reportSender(result);
    }

    /**
     * Handles a {@link RemoveGameRoomRequest}.
     *
     * @param request The request to be handled.
     */
    private void handleRemoveGameRoomRequest(RemoveGameRoomRequest request) {
        final RemoveGameRoomMessage msg = RemoveGameRoomMessage.getMessage(request.getGameRoomName());
        final GameRoomRemovalResult result = askTheGameRoomManagerToRemoveAGameRoom(msg, request.getTimeout());
        reportSender(result);
    }

    /**
     * Handles a {@link AddPlayerToGameRoomRequest}.
     *
     * @param request The request to be handled.
     */
    private void handleAddPlayerToGameRoomRequest(AddPlayerToGameRoomRequest request) {
        final AddPlayerMessage msg = AddPlayerMessage.getMessage(request.getGameRoomName(), request.getPlayerId());
        reportSender(askTheGameRoomManager(msg, request.getTimeout(), PlayerOperationResult.FAILURE));
    }

    /**
     * Handles a {@link RemovePlayerFromGameRoomRequest}.
     *
     * @param request The request to be handled.
     */
    private void handleRemovePlayerToGameRoomRequest(RemovePlayerFromGameRoomRequest request) {
        final RemovePlayerMessage msg = RemovePlayerMessage
                .getMessage(request.getGameRoomName(), request.getPlayerId());
        reportSender(askTheGameRoomManager(msg, request.getTimeout(), PlayerOperationResult.FAILURE));
    }

    /**
     * Handles the process of requesting the system monitor for data.
     */
    private void handleSystemMonitorRequest() {
        final Future<Object> future = Patterns.ask(systemMonitor, GetDataMessage.getMessage(), 100);
        Patterns.pipe(future, getContext().dispatcher()).to(getSender());
    }


    /**
     * Asks the game room manager to create a game room.
     *
     * @param question The {@link CreateGameRoomMessage} representing the request to the game room manager.
     * @param timeout  The timeout for the request.
     * @return The results of the request.
     */
    private GameRoomCreationResult askTheGameRoomManagerToCreateAGameRoom(CreateGameRoomMessage question, long timeout) {
        return askTheGameRoomManager(question, timeout, GameRoomCreationResult.FAILURE);
    }

    /**
     * Asks the game room manager to remove a game room.
     *
     * @param question The {@link RemoveGameRoomMessage} representing the request to the game room manager.
     * @param timeout  The timeout for the request.
     * @return The results of the request.
     */
    private GameRoomRemovalResult askTheGameRoomManagerToRemoveAGameRoom(RemoveGameRoomMessage question, long timeout) {
        return askTheGameRoomManager(question, timeout, GameRoomRemovalResult.FAILURE);
    }


    /**
     * Method that wraps logic to ask something to the game rooms manager.
     *
     * @param question     The object representing the "question" to the game room manager.
     * @param timeout      The timeout of the question.
     * @param defaultValue The default value to get when there is any issue.
     * @param <T>          The concrete type of the response.
     * @return The response of the question.
     */
    private <T> T askTheGameRoomManager(Object question, long timeout, T defaultValue) {
        final FiniteDuration duration = Duration.create(timeout, TimeUnit.MILLISECONDS);
        Future<Object> future = Patterns.ask(gameRoomManager, question, new Timeout(duration));
        try {
            //noinspection unchecked
            return (T) Await.result(future, duration);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    /**
     * Replies the sender with the given {@code result} value.
     *
     * @param result The value to send as a reply to the sender.
     */
    private void reportSender(Object result) {
        this.getSender().tell(result, this.getSelf());
    }

    /**
     * Create {@link Props} for an {@link akka.actor.Actor} of this type.
     *
     * @param gameRoomManager The {@link ActorRef} for the game room manager.
     * @param systemMonitor   The {@link ActorRef} for the system monitor.
     * @return The created {@link Props}.
     */
    /* package */
    static Props getProps(ActorRef gameRoomManager, ActorRef systemMonitor) {
        return Props.create(HttpRequestHandlerActor.class,
                () -> new HttpRequestHandlerActor(gameRoomManager, systemMonitor));
    }

}
