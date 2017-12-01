package ar.edu.itba.tav.game_rooms.http.dto;

import akka.http.javadsl.model.Uri;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Data transfer object for a game room.
 */
public class GameRoomDto {

    /**
     * The game room name.
     */
    @JsonProperty
    private String name;

    /**
     * The game room capacity.
     */
    @JsonProperty
    private int capacity;

    /**
     * The {@link Set} of players in the game room.
     */
    @SuppressWarnings({"FieldCanBeLocal", "unused", "MismatchedQueryAndUpdateOfCollection"})
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final Set<Long> players;

    /**
     * The url of the location of the game room represented by this dto.
     */
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonSerialize(using = UriSerializer.class)
    private Uri locationUrl;

    /**
     * Default constructor used by Jackson.
     */
    public GameRoomDto() {
        // For Jackson
        this.players = new HashSet<>();
    }

    /**
     * Constructor.
     *
     * @param name        The game room name.
     * @param capacity    The game room capacity.
     * @param players     The players in the game room.
     * @param locationUrl The url of the location of the game room represented by this dto.
     */
    public GameRoomDto(String name, int capacity, Set<Long> players, Uri locationUrl) {
        this.name = name;
        this.capacity = capacity;
        this.players = players;
        this.locationUrl = locationUrl;
    }

    /**
     * @return The game room name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The game room capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * A {@link com.fasterxml.jackson.databind.JsonSerializer} to serialize {@link Uri}s.
     */
    private static final class UriSerializer extends StdSerializer<Uri> {

        /**
         * Private constructor.
         */
        private UriSerializer() {
            super(Uri.class);
        }

        @Override
        public void serialize(Uri value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
        }
    }

}

