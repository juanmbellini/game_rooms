package ar.edu.itba.tav.game_rooms.exceptions;

/**
 * {@link RuntimeException} to be thrown when a name is tried to be used,
 * but there is another entity with the given name.
 */
public class NameAlreadyInUseException extends RuntimeException {

    /**
     * Default constructor.
     */
    public NameAlreadyInUseException() {
        super();
    }

    /**
     * Constructor which can set a message for the exception.
     *
     * @param msg The exception message.
     */
    public NameAlreadyInUseException(String msg) {
        super(msg);
    }

    /**
     * Constructor which can set a message and a cause.
     *
     * @param msg   The exception message.
     * @param cause A {@link Throwable} that caused this exception to be thrown.
     */
    public NameAlreadyInUseException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
