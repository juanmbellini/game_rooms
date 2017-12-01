package ar.edu.itba.tav.game_rooms.messages;

import ar.edu.itba.tav.game_rooms.core.GameRoomsManagerActor;

import java.util.Collections;
import java.util.Set;

/**
 * Class defining messages for game room operations.
 */
public class GameRoomOperationMessages {

    // ========================================================================
    // Response messages
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
         * The specified parameters are illegal or invalid.
         */
        INVALID,
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

    /**
     * Message representing a game room (used to transfer game room data between actors).
     */
    public static class GameRoomDataMessage {

        /**
         * The game room name.
         */
        private final String name;

        /**
         * The game room capacity.
         */
        private final int capacity;

        /**
         * The players in the game room.
         */
        private final Set<String> players;

        /**
         * Constructor.
         *
         * @param name     The game room name.
         * @param capacity The game room capacity.
         * @param players  The players in the game room.
         */
        public GameRoomDataMessage(String name, int capacity, Set<String> players) {
            this.name = name;
            this.capacity = capacity;
            this.players = Collections.unmodifiableSet(players);
        }

        /**
         * @return The game room name.
         */
        public String getName() {
            return name;
        }

        /**
         * @return The game room capacity.
         */
        public int getCapacity() {
            return capacity;
        }

        /**
         * @return The players in the game room.
         */
        public Set<String> getPlayers() {
            return players;
        }
    }


    // ========================================================================
    // Request messages
    // ========================================================================

    /**
     * A message that is used to request all game rooms.
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
     * A message that is used to request a specific game room.
     */
    public final static class GetSpecificGameRoomMessage extends GameRoomMessage {

        /**
         * Private constructor.
         *
         * @param gameRoomName The name of the game room to be fetched.
         */
        private GetSpecificGameRoomMessage(String gameRoomName) {
            super(gameRoomName);
        }

        /**
         * Static method to create a {@link GetSpecificGameRoomMessage}.
         *
         * @param gameRoomName The name of the game room to be fetched.
         * @return The new {@link GetSpecificGameRoomMessage}.
         */
        public static GetSpecificGameRoomMessage getMessage(String gameRoomName) {
            return new GetSpecificGameRoomMessage(gameRoomName);
        }
    }

    /**
     * A {@link GameRoomMessage} that is used to create a new game room.
     */
    public final static class CreateGameRoomMessage extends GameRoomMessage {

        /**
         * The capacity of the game room to be created.
         */
        private final int capacity;

        /**
         * Private constructor.
         *
         * @param gameRoomName The name for the new game room.
         * @param capacity     The capacity of the game room to be created.
         */
        private CreateGameRoomMessage(String gameRoomName, int capacity) {
            super(gameRoomName);
            this.capacity = capacity;
        }

        /**
         * @return The capacity of the game room to be created.
         */
        public int getCapacity() {
            return capacity;
        }

        /**
         * Static method to create a {@link CreateGameRoomMessage}.
         *
         * @param gameRoomName The name for the new game room.
         * @param capacity     The capacity of the game room to be created.
         * @return The new {@link CreateGameRoomMessage}.
         */
        public static CreateGameRoomMessage getMessage(String gameRoomName, int capacity) {
            return new CreateGameRoomMessage(gameRoomName, capacity);
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

    /**
     * Message to be sent from the game room manager to a game room to request it's data.
     */
    public final static class GetGameRoomDataMessage {

        /**
         * Private constructor.
         */
        private GetGameRoomDataMessage() {
        }

        /**
         * Static method to create a {@link GetGameRoomDataMessage}.
         *
         * @return The new {@link GetGameRoomDataMessage}.
         */
        public static GetGameRoomDataMessage getMessage() {
            return new GetGameRoomDataMessage();
        }
    }
}
