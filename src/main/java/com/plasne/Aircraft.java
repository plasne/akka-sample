package com.plasne;

import java.io.Serializable;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import akka.actor.Props;
import akka.actor.AbstractActor;
// import akka.persistence.AbstractPersistentActor;
// import akka.persistence.SnapshotOffer;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Aircraft extends AbstractActor {

    /*
     * static private class State implements Serializable { private static final
     * long serialVersionUID = 1L; public String lastSource;
     * 
     * public State() { this("none"); }
     * 
     * public State(String lastSource) { this.lastSource = lastSource; }
     * 
     * public State copy() { return new State(this.lastSource); }
     * 
     * public void update(Telemetry t) { this.lastSource = t.source; } }
     */

    // variables
    // private State state = new State();
    // private int snapShotInterval = 1000;
    private int icao = 0;
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    // object constructor
    public Aircraft() {
    }

    // actor constructor
    static public Props props() {
        return Props.create(Aircraft.class, () -> new Aircraft());
    }

    // event called on startup
    @Override
    public void preStart() {
        String name = getSelf().path().name();
        this.icao = Integer.parseInt(name);
        String path = akka.serialization.Serialization.serializedActorPath(getSelf());
        this.log.info("aircraft ({}) started up ({})", this.icao, path);
    }

    /*
     * @Override public String persistenceId() { return "aircraft-" + this.icao; }
     */

    // telemetry message
    static public class Telemetry implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String source;
        public final Date timestamp;
        public final int icao;
        public final int seq;
        public final int heading;

        public Telemetry(String source, Date timestamp, int icao, int seq, int heading) {
            this.source = source;
            this.timestamp = timestamp;
            this.icao = icao;
            this.seq = seq;
            this.heading = heading;
        }
    }

    /*
     * @Override public Receive createReceiveRecover() { return
     * receiveBuilder().match(Telemetry.class, state::update)
     * .match(SnapshotOffer.class, ss -> state = (State) ss.snapshot()).build(); }
     */

    // define receiver
    @Override
    public Receive createReceive() {
        return receiveBuilder().match(Telemetry.class, x -> {
            log.info(x.timestamp.toString() + " " + x.icao + " " + x.heading);

            // send messages for 1000 to a tracker
            if (x.icao == 1000) {
                URL url = new URL("http://192.168.12.178:8100");
                URLConnection con = url.openConnection();
                HttpURLConnection http = (HttpURLConnection) con;
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                byte[] out = ("{ \"source\": \"" + x.source + "\", \"icao\": \"" + x.icao + "\", \"seq\": \"" + x.seq
                        + "\", \"heading\":\"" + x.heading + "\"}").getBytes(StandardCharsets.UTF_8);
                int length = out.length;
                http.setFixedLengthStreamingMode(length);
                http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                http.connect();
                try (OutputStream os = http.getOutputStream()) {
                    os.write(out);
                }
            }

            // persist
            /*
             * persist(x, (Telemetry t) -> { this.state.update(t);
             * getContext().getSystem().getEventStream().publish(t); if (lastSequenceNr() %
             * this.snapShotInterval == 0 && lastSequenceNr() != 0)
             * saveSnapshot(this.state.copy()); });
             */

        }).build();
    }

}
