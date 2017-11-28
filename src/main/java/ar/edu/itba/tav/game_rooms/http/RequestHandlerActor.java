package ar.edu.itba.tav.game_rooms.http;

import akka.actor.AbstractActor;
import akka.actor.ActorPath;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import akka.util.Timeout;
import ar.edu.itba.tav.game_rooms.core.GameRoomsManagerActor;
import ar.edu.itba.tav.game_rooms.core.GameRoomsManagerActor.GetAllGameRoomsMessage;
import ar.edu.itba.tav.game_rooms.core.GameRoomsManagerActor.CreateGameRoomMessage;
import ar.edu.itba.tav.game_rooms.core.GameRoomsManagerActor.GameRoomCreationResult;
import ar.edu.itba.tav.game_rooms.core.GameRoomsManagerActor.GameRoomRemovalResult;
import ar.edu.itba.tav.game_rooms.core.GameRoomsManagerActor.RemoveGameRoomMessage;
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
/* package */ class RequestHandlerActor extends AbstractActor {

    /**
     * The path of the game room manager.
     */
    private final ActorPath gameRoomManagerPath;

    /**
     * Private constructor.
     *
     * @param gameRoomManagerPath The path of the game room manager.
     */
    private RequestHandlerActor(ActorPath gameRoomManagerPath) {
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
        return Props.create(RequestHandlerActor.class, () -> new RequestHandlerActor(gameRoomManagerPath));
    }

    /**
     * An abstract request that contains a timeout value.
     */
    private abstract static class TimeoutRequest {

        /**
         * The timeout for the request.
         */
        private final long timeout;


        /**
         * @param timeout The timeout for the request.
         */
        private TimeoutRequest(long timeout) {
            this.timeout = timeout;
        }

        /**
         * @return The timeout for the request.
         */
        /* package */ long getTimeout() {
            return timeout;
        }
    }

    /* package */ static class GetAllGameRoomsRequest extends TimeoutRequest {

        /**
         * @param timeout The timeout for the request.
         */
        private GetAllGameRoomsRequest(long timeout) {
            super(timeout);
        }

        /**
         * Creates a new {@link GetAllGameRoomsRequest}.
         *
         * @param timeout The timeout for the request.
         * @return The created request.
         */
        /* package */
        static GetAllGameRoomsRequest createRequest(long timeout) {
            return new GetAllGameRoomsRequest(timeout);
        }
    }

    /**
     * An abstract request representing an operation over a game room.
     */
    private abstract static class GameRoomOperationRequest extends TimeoutRequest {

        /**
         * The name of the game room to which an operation will be performed.
         */
        private final String gameRoomName;


        /**
         * Private constructor.
         *
         * @param gameRoomName The name of the game room to which an operation will be performed.
         * @param timeout      The timeout for the request.
         */
        private GameRoomOperationRequest(String gameRoomName, long timeout) {
            super(timeout);
            this.gameRoomName = gameRoomName;
        }

        /**
         * @return The name of the game room to which an operation will be performed.
         */
        /* package */ String getGameRoomName() {
            return gameRoomName;
        }
    }

    /**
     * A request for creating a new game room.
     */
    /* package */ static final class CreateGameRoomRequest extends GameRoomOperationRequest {

        /**
         * Private constructor.
         *
         * @param gameRoomName The name of the game room to be created.
         * @param timeout      The timeout for the request.
         */
        private CreateGameRoomRequest(String gameRoomName, long timeout) {
            super(gameRoomName, timeout);
        }

        /**
         * Creates a new {@link CreateGameRoomRequest}.
         *
         * @param gameRoomName The name of the game room to be created.
         * @param timeout      The timeout for the request.
         * @return The created request.
         */
        /* package */
        static CreateGameRoomRequest createRequest(String gameRoomName, long timeout) {
            return new CreateGameRoomRequest(gameRoomName, timeout);
        }
    }

    /**
     * A request for removing an existing game room.
     */
    /* package */ static final class RemoveGameRoomRequest extends GameRoomOperationRequest {

        /**
         * Private constructor.
         *
         * @param gameRoomName The name of the game room to be removed.
         * @param timeout      The timeout for the request.
         */
        private RemoveGameRoomRequest(String gameRoomName, long timeout) {
            super(gameRoomName, timeout);
        }

        /**
         * Creates a new {@link RemoveGameRoomRequest}.
         *
         * @param gameRoomName The name of the game room to be removed.
         * @param timeout      The timeout for the request.
         * @return The created request.
         */
        /* package */
        static RemoveGameRoomRequest createRequest(String gameRoomName, long timeout) {
            return new RemoveGameRoomRequest(gameRoomName, timeout);
        }
    }
}
