package ar.edu.itba.tav.game_rooms;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point.
 */
public class Main {

    /**
     * The {@link Logger} object.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    /**
     * Entry point.
     *
     * @param args The program arguments.
     */
    public static void main(String[] args) {
        LOGGER.info("Starting application...");

        JCommander jCommander = new JCommander();
        jCommander.setProgramName("java -jar <path-to-jar>");
        ProgramArguments arguments = initializeProgramArguments(jCommander, args);
        if (arguments.isUsage()) {
            jCommander.usage();
            return;
        }

        final ActorSystem system = ActorSystem.create("game_rooms");
        MainActor.StartSystemMessage startSystemMessage = MainActor.StartSystemMessage
                .createMessage(arguments.getHttpServerHostname(), arguments.getHttpServerPort());

        system.actorOf(MainActor.getProps()).tell(startSystemMessage, ActorRef.noSender());
    }

    /**
     * Initializes the program arguments.
     *
     * @param jCommander The {@link JCommander} instance that will perform the task.
     * @param args       The program arguments in the {@code String[]} format.
     * @return The initialized {@link ProgramArguments} instance, with data taken from the given {@code args}.
     */
    private static ProgramArguments initializeProgramArguments(JCommander jCommander, String[] args) {
        LOGGER.debug("Initializing program arguments...");
        final ProgramArguments arguments = new ProgramArguments();
        jCommander.addObject(arguments);
        jCommander.parse(args);
        LOGGER.debug("Program arguments initialized.");

        return arguments;
    }


    /**
     * Container class holding the program arguments.
     */
    private final static class ProgramArguments {

        /**
         * Indicates if the execution of the system is for printing the usage message.
         */
        @Parameter(names = {"-h", "--help"}, description = "Prints the usage message", help = true)
        private boolean usage;

        /**
         * The hostname for the http server.
         */
        @Parameter(names = {"-H", "--host"}, description = "Sets the http server hostname")
        private String httpServerHostname = "localhost";

        /**
         * The port for the http server.
         */
        @Parameter(names = {"-p", "--port"}, description = "Sets the http server port")
        private int httpServerPort = 9000;

        /**
         * Private constructor.
         */
        private ProgramArguments() {

        }

        /**
         * @return {@code true} if the execution of the system is for printing the usage message,
         * or {@code false} otherwise.
         */
        private boolean isUsage() {
            return usage;
        }

        /**
         * @return The hostname for the http server.
         */
        private String getHttpServerHostname() {
            return httpServerHostname;
        }

        /**
         * @return The port for the http server.
         */
        private int getHttpServerPort() {
            return httpServerPort;
        }
    }
}
