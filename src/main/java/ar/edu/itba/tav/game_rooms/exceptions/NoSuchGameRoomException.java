package ar.edu.itba.tav.game_rooms.exceptions;

/**
 * {@link RuntimeException} to be thrown when a game room was tried to be retrieved,
 * but there is no game room with the given characteristics.
 */
public class NoSuchGameRoomException extends RuntimeException {

    /**
     * Default constructor.
     */
    public NoSuchGameRoomException() {
        super();
    }

    /**
     * Constructor which can set a message for the exception.
     *
     * @param msg The exception message.
     */
    public NoSuchGameRoomException(String msg) {
        super(msg);
    }

    /**
     * Constructor which can set a message and a cause.
     *
     * @param msg   The exception message.
     * @param cause A {@link Throwable} that caused this exception to be thrown.
     */
    public NoSuchGameRoomException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
