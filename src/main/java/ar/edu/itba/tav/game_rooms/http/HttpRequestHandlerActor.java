package ar.edu.itba.tav.game_rooms.http;

import akka.actor.AbstractActor;
import akka.actor.ActorPath;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import akka.util.Timeout;
import ar.edu.itba.tav.game_rooms.messages.GameRoomOperationMessages.*;
import ar.edu.itba.tav.game_rooms.messages.HttpRequestMessages.CreateGameRoomRequest;
import ar.edu.itba.tav.game_rooms.messages.HttpRequestMessages.GetAllGameRoomsRequest;
import ar.edu.itba.tav.game_rooms.messages.HttpRequestMessages.RemoveGameRoomRequest;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * {@link akka.actor.Actor} in charge of handling
 */
/* package */ class HttpRequestHandlerActor extends AbstractActor {

    /**
     * The path of the game room manager.
     */
    private final ActorPath gameRoomManagerPath;

    /**
     * Private constructor.
     *
     * @param gameRoomManagerPath The path of the game room manager.
     */
    private HttpRequestHandlerActor(ActorPath gameRoomManagerPath) {
        this.gameRoomManagerPath = gameRoomManagerPath;
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(GetAllGameRoomsRequest.class, this::handlerGetAllGameRoomsRequest)
                .match(CreateGameRoomRequest.class, this::handleCreateGameRoomRequest)
                .match(RemoveGameRoomRequest.class, this::handleRemoveGameRoomRequest)
                .build();
    }

    /**
     * Handles a {@link GetAllGameRoomsRequest}.
     *
     * @param request The request to be handled.
     */
    private void handlerGetAllGameRoomsRequest(GetAllGameRoomsRequest request) {
        final GetAllGameRoomsMessage msg = GetAllGameRoomsMessage.getMessage();
        final List<String> gameRooms = askTheGameRoomManager(msg, request.getTimeout(), new LinkedList<>());
        reportSender(gameRooms);
    }

    /**
     * Handles a {@link CreateGameRoomRequest}.
     *
     * @param request The request to be handled.
     */
    private void handleCreateGameRoomRequest(CreateGameRoomRequest request) {
        final CreateGameRoomMessage msg = CreateGameRoomMessage.getMessage(request.getGameRoomName());
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
        final ActorSelection gameRoomManager = getContext().getSystem().actorSelection(gameRoomManagerPath);
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
     * @param gameRoomManagerPath The path of the game room manager.
     * @return The created {@link Props}.
     */
    /* package */
    static Props getProps(ActorPath gameRoomManagerPath) {
        return Props.create(HttpRequestHandlerActor.class, () -> new HttpRequestHandlerActor(gameRoomManagerPath));
    }

}
