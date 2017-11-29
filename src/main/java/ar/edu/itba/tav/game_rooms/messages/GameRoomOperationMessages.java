package ar.edu.itba.tav.game_rooms.messages;

import ar.edu.itba.tav.game_rooms.core.GameRoomsManagerActor;

/**
 * Class defining messages for game room operations.
 */
public class GameRoomOperationMessages {

    // ========================================================================
    // Result messages
    // ========================================================================

    /**
     * Marking interface for enums that can be returned as responses to senders.
     */
    public interface ResultEnum {
    }

    /**
     * Enum containing the possible values to be replied to the sender when creating a new game room.
     */
    public enum GameRoomCreationResult implements ResultEnum {
        /**
         * The game room was created successfully.
         */
        CREATED,
        /**
         * There is another game room with the given name.
         */
        NAME_REPEATED,
        /**
         * There was an unexpected failure when trying to create the game room.
         */
        FAILURE,
    }

    /**
     * Enum containing the possible values to be replied to the sender when creating a new game room.
     */
    public enum GameRoomRemovalResult implements ResultEnum {
        /**
         * The game room was removed successfully.
         */
        REMOVED,
        /**
         * There is no game room with the given name.
         */
        NO_SUCH_GAME_ROOM,
        /**
         * There was an unexpected failure when trying to create the game room.
         */
        FAILURE,
    }


    // ========================================================================
    // Request messages
    // ========================================================================

    /**
     * A message that is used to request a game room creation.
     */
    public final static class GetAllGameRoomsMessage {

        /**
         * Private constructor.
         */
        private GetAllGameRoomsMessage() {
        }

        /**
         * Static method to create a {@link GetAllGameRoomsMessage}.
         *
         * @return The new {@link GetAllGameRoomsMessage}.
         */
        public static GetAllGameRoomsMessage getMessage() {
            return new GetAllGameRoomsMessage();
        }
    }

    /**
     * Abstract class representing a game room message (i.e a message that a {@link GameRoomsManagerActor}
     * can understand in order to operate over a game room.
     */
    private abstract static class GameRoomMessage {

        /**
         * The game room's name in which the operation must be done.
         */
        private final String gameRoomName;

        /**
         * Private constructor.
         *
         * @param gameRoomName The game room's name in which the operation must be done.
         */
        private GameRoomMessage(String gameRoomName) {
            this.gameRoomName = gameRoomName;
        }

        /**
         * @return The game room's name in which the operation must be done.
         */
        public String getGameRoomName() {
            return gameRoomName;
        }
    }

    /**
     * A {@link GameRoomMessage} that is used to create a new game room.
     */
    public final static class CreateGameRoomMessage extends GameRoomMessage {

        /**
         * Private constructor.
         *
         * @param gameRoomName The name for the new game room.
         */
        private CreateGameRoomMessage(String gameRoomName) {
            super(gameRoomName);
        }

        /**
         * Static method to create a {@link CreateGameRoomMessage}.
         *
         * @param gameRoomName The name for the new game room.
         * @return The new {@link CreateGameRoomMessage}.
         */
        public static CreateGameRoomMessage getMessage(String gameRoomName) {
            return new CreateGameRoomMessage(gameRoomName);
        }
    }

    /**
     * A {@link GameRoomMessage} that is used remove game rooms.
     */
    public final static class RemoveGameRoomMessage extends GameRoomMessage {

        /**
         * Private constructor.
         *
         * @param gameRoomName The name of the game room to be removed.
         */
        private RemoveGameRoomMessage(String gameRoomName) {
            super(gameRoomName);
        }

        /**
         * Static method to create a {@link RemoveGameRoomMessage}.
         *
         * @param gameRoomName The name of the game room to be removed.
         * @return The new {@link RemoveGameRoomMessage}.
         */
        public static RemoveGameRoomMessage getMessage(String gameRoomName) {
            return new RemoveGameRoomMessage(gameRoomName);
        }
    }
}
