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
        private final Set<Long> players;

        /**
         * Constructor.
         *
         * @param name     The game room name.
         * @param capacity The game room capacity.
         * @param players  The players in the game room.
         */
        public GameRoomDataMessage(String name, int capacity, Set<Long> players) {
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
        public Set<Long> getPlayers() {
            return players;
        }
    }

    /**
     * Enum containing results that can occur while performing player operations over a game room
     * (i.e adding or removing a game room).
     */
    public enum PlayerOperationResult implements ResultEnum {
        /**
         * The operation was successful.
         */
        SUCCESSFUL,
        /**
         * No game room with the given name.
         */
        NO_SUCH_GAME_ROOM,
        /**
         * Tried to add a player in a game room which reached it's capacity.
         */
        FULL_GAME_ROOM,
        /**
         * Something unexpected happened while trying to perform the operation.
         */
        FAILURE,
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
     * Message to be sent when referring a game room and a player (i.e adding/removing a player into/from a game room).
     */
    private abstract static class PlayerMessage extends GameRoomMessage {

        /**
         * The id of the player being referred.
         */
        private final long playerId;

        /**
         * Private constructor.
         *
         * @param gameRoomName The game room's name in which the operation must be done.
         * @param playerId     The id of the player being referred.
         */
        private PlayerMessage(String gameRoomName, long playerId) {
            super(gameRoomName);
            this.playerId = playerId;
        }

        /**
         * @return The id of the player being referred.
         */
        public long getPlayerId() {
            return playerId;
        }
    }

    /**
     * Message to be sent when adding a player into a game room.
     */
    public final static class AddPlayerMessage extends PlayerMessage {

        /**
         * Private constructor.
         *
         * @param gameRoomName The game room's name in which the operation must be done.
         * @param playerId     The id of the player being added.
         */
        private AddPlayerMessage(String gameRoomName, long playerId) {
            super(gameRoomName, playerId);
        }

        /**
         * Static method to create a {@link AddPlayerMessage}.
         *
         * @param gameRoomName The game room's name in which the operation must be done.
         * @param playerId     The id of the player being added.
         * @return The new {@link AddPlayerMessage}.
         */
        public static AddPlayerMessage getMessage(String gameRoomName, long playerId) {
            return new AddPlayerMessage(gameRoomName, playerId);
        }
    }

    /**
     * Message to be sent when removing a player from a game room.
     */
    public final static class RemovePlayerMessage extends PlayerMessage {

        /**
         * Private constructor.
         *
         * @param gameRoomName The game room's name in which the operation must be done.
         * @param playerId     The id of the player being removed.
         */
        private RemovePlayerMessage(String gameRoomName, long playerId) {
            super(gameRoomName, playerId);
        }

        /**
         * Static method to create a {@link RemovePlayerMessage}.
         *
         * @param gameRoomName The game room's name in which the operation must be done.
         * @param playerId     The id of the player being removed.
         * @return The new {@link RemovePlayerMessage}.
         */
        public static RemovePlayerMessage getMessage(String gameRoomName, long playerId) {
            return new RemovePlayerMessage(gameRoomName, playerId);
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
