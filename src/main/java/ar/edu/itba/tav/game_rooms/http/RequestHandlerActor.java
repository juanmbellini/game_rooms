package ar.edu.itba.tav.game_rooms.http;

import akka.actor.AbstractActor;
import akka.actor.ActorPath;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import ar.edu.itba.tav.game_rooms.core.GameRoomsManagerActor;

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
                .match(CreateGameRoomRequest.class, this::handleCreateGameRoomRequest)
                .match(RemoveGameRoomRequest.class, this::handleRemoveGameRoomRequest)
                .match(GameRoomsManagerActor.RemoveGameRoomMessage.class, System.out::println)
                .build();
    }

    /**
     * Handles a {@link CreateGameRoomRequest}.
     *
     * @param request The request to be handled.
     */
    private void handleCreateGameRoomRequest(CreateGameRoomRequest request) {
        tellTheGameRoomManager(GameRoomsManagerActor.CreateGameRoomMessage.getMessage(request.getGameRoomName()));
    }

    /**
     * Handles a {@link RemoveGameRoomRequest}.
     *
     * @param request The request to be handled.
     */
    private void handleRemoveGameRoomRequest(RemoveGameRoomRequest request) {
        tellTheGameRoomManager(GameRoomsManagerActor.RemoveGameRoomMessage.getMessage(request.getGameRoomName()));
    }

    /**
     * Method that wraps logic to send a message to the game rooms manager by this actor.
     *
     * @param msg The message to be sent.
     */
    private void tellTheGameRoomManager(Object msg) {
        this.getContext().getSystem().actorSelection(gameRoomManagerPath).tell(msg, getSelf());
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
     * An abstract request representing an operation over a game room.
     */
    private abstract static class GameRoomOperationRequest {

        /**
         * The name of the game room to which an operation will be performed.
         */
        private final String gameRoomName;


        /**
         * Private constructor.
         *
         * @param gameRoomName The name of the game room to which an operation will be performed.
         */
        private GameRoomOperationRequest(String gameRoomName) {
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
         */
        private CreateGameRoomRequest(String gameRoomName) {
            super(gameRoomName);
        }

        /**
         * Creates a new {@link CreateGameRoomRequest}.
         *
         * @param gameRoomName The name of the game room to be created.
         * @return The created request.
         */
        /* package */
        static CreateGameRoomRequest createRequest(String gameRoomName) {
            return new CreateGameRoomRequest(gameRoomName);
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
         */
        private RemoveGameRoomRequest(String gameRoomName) {
            super(gameRoomName);
        }

        /**
         * Creates a new {@link RemoveGameRoomRequest}.
         *
         * @param gameRoomName The name of the game room to be removed.
         * @return The created request.
         */
        /* package */
        static RemoveGameRoomRequest createRequest(String gameRoomName) {
            return new RemoveGameRoomRequest(gameRoomName);
        }
    }
}
