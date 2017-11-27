package ar.edu.itba.tav.game_rooms.http.dto;

/**
 * Data transfer object for a game room.
 */
public class GameRoomDto {

    /**
     * The game room name.
     */
    private String name;

    /**
     * Default constructor used by Jackson.
     */
    public GameRoomDto() {
        // For Jackson
    }

    /**
     * Constructor.
     *
     * @param name The game room name.
     */
    public GameRoomDto(String name) {
        this.name = name;
    }

    /**
     * @return The game room name.
     */
    public String getName() {
        return name;
    }
}

