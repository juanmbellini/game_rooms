package ar.edu.itba.tav.game_rooms.messages;

/**
 * Class defining messages for system monitor communication.
 */
public class SystemMonitorMessages {


    // ================================
    // Request messages
    // ================================

    /**
     * Message to be sent to the system monitor to request data from it.
     */
    public final static class GetDataMessage {

        /**
         * Single instance (no state for objects of this type).
         */
        private final static GetDataMessage SINGLETON = new GetDataMessage();

        /**
         * Private constructor.
         */
        private GetDataMessage() {

        }

        /**
         * @return The singleton for this class.
         */
        public static GetDataMessage getMessage() {
            return SINGLETON;
        }
    }


    // ================================
    // Response messages
    // ================================

    /**
     * Message to be sent by the system monitor in case the process of monitoring did not started.
     */
    public final static class SystemMonitorNotStartedMessage {

        /**
         * Single instance (no state for objects of this type).
         */
        private final static SystemMonitorNotStartedMessage SINGLETON = new SystemMonitorNotStartedMessage();

        /**
         * Private constructor.
         */
        private SystemMonitorNotStartedMessage() {

        }

        /**
         * @return The singleton for this class.
         */
        public static SystemMonitorNotStartedMessage getMessage() {
            return SINGLETON;
        }
    }

    /**
     * Message to be sent by the system monitor, which includes all the data that it got.
     */
    public final static class SystemMonitorData {

        /**
         * The percentage of cpu usage.
         */
        private final Double cpuUsage;

        /**
         * The amount of available memory.
         */
        private final long freeMemory;

        /**
         * The max. amount of memory available for the process.
         */
        private final long maxMemory;

        /**
         * The actual amount of memory the jvm has.
         */
        private final long jvmMemory;


        /**
         * Private constructor.
         *
         * @param cpuUsage   The percentage of cpu usage.
         * @param freeMemory The amount of available memory.
         * @param maxMemory  The max. amount of memory available for the process.
         * @param jvmMemory  The actual amount of memory the jvm has.
         */
        private SystemMonitorData(Double cpuUsage, long freeMemory, long maxMemory, long jvmMemory) {
            this.cpuUsage = cpuUsage;
            this.freeMemory = freeMemory;
            this.maxMemory = maxMemory;
            this.jvmMemory = jvmMemory;
        }

        /**
         * @return The percentage of cpu usage.
         */
        public Double getCpuUsage() {
            return cpuUsage;
        }

        /**
         * @return The amount of available memory.
         */
        public long getFreeMemory() {
            return freeMemory;
        }

        /**
         * @return The max. amount of memory available for the process.
         */
        public long getMaxMemory() {
            return maxMemory;
        }

        /**
         * @return The actual amount of memory the jvm has.
         */
        public long getJvmMemory() {
            return jvmMemory;
        }


        /**
         * Builds a {@link SystemMonitorData} message.
         *
         * @param cpuUsage   The percentage of cpu usage.
         * @param freeMemory The amount of available memory.
         * @param maxMemory  The max. amount of memory available for the process.
         * @param jvmMemory  The actual amount of memory the jvm has.
         * @return The build message.
         */
        public static SystemMonitorData getMessage(Double cpuUsage, long freeMemory, long maxMemory, long jvmMemory) {
            return new SystemMonitorData(cpuUsage, freeMemory, maxMemory, jvmMemory);
        }
    }

}
