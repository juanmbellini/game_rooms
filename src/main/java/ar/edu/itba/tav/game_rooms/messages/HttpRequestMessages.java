package ar.edu.itba.tav.game_rooms.messages;

/**
 * Class defining messages for http requests handling.
 */
public class HttpRequestMessages {

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
        public long getTimeout() {
            return timeout;
        }
    }

    /**
     * A request to get all game rooms.
     */
    public static class GetAllGameRoomsRequest extends TimeoutRequest {

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
        public static GetAllGameRoomsRequest createRequest(long timeout) {
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
        public String getGameRoomName() {
            return gameRoomName;
        }
    }

    /**
     * A request for to get a specific game room.
     */
    public static final class GetGameRoomRequest extends GameRoomOperationRequest {

        /**
         * Private constructor.
         *
         * @param gameRoomName The name of the game room to be retrieved.
         * @param timeout      The timeout for the request.
         */
        private GetGameRoomRequest(String gameRoomName, long timeout) {
            super(gameRoomName, timeout);
        }

        /**
         * Creates a new {@link GetGameRoomRequest}.
         *
         * @param gameRoomName The name of the game room to be retrieved.
         * @param timeout      The timeout for the request.
         * @return The created request.
         */
        public static GetGameRoomRequest createRequest(String gameRoomName, long timeout) {
            return new GetGameRoomRequest(gameRoomName, timeout);
        }
    }

    /**
     * A request for creating a new game room.
     */
    public static final class CreateGameRoomRequest extends GameRoomOperationRequest {

        /**
         * The capacity of the game room to be created.
         */
        private final int capacity;

        /**
         * Private constructor.
         *
         * @param gameRoomName The name of the game room to be created.
         * @param capacity     The capacity of the game room to be created.
         * @param timeout      The timeout for the request.
         */
        private CreateGameRoomRequest(String gameRoomName, int capacity, long timeout) {
            super(gameRoomName, timeout);
            this.capacity = capacity;
        }

        /**
         * @return The capacity of the game room to be created.
         */
        public int getCapacity() {
            return capacity;
        }

        /**
         * Creates a new {@link CreateGameRoomRequest}.
         *
         * @param gameRoomName The name of the game room to be created.
         * @param capacity     The capacity of the game room to be created.
         * @param timeout      The timeout for the request.
         * @return The created request.
         */
        public static CreateGameRoomRequest createRequest(String gameRoomName, int capacity, long timeout) {
            return new CreateGameRoomRequest(gameRoomName, capacity, timeout);
        }
    }

    /**
     * A request for removing an existing game room.
     */
    public static final class RemoveGameRoomRequest extends GameRoomOperationRequest {

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
        public static RemoveGameRoomRequest createRequest(String gameRoomName, long timeout) {
            return new RemoveGameRoomRequest(gameRoomName, timeout);
        }
    }
}
