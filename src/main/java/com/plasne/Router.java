package com.plasne;

import java.net.UnknownHostException;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.time.Duration;
import akka.actor.ActorSystem;
import akka.actor.ActorRef;
import akka.actor.AbstractActorWithTimers;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;
import com.plasne.Aircraft.Telemetry;

public class Router extends AbstractActorWithTimers {

    // variables
    private String id;
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private HashMap<Integer, Integer> seqs = new HashMap<Integer, Integer>();
    private Random rand = new Random();

    // constructor
    public Router() {
        getTimers().startPeriodicTimer("GenerateTelemetry", new GenerateTelemetry(), Duration.ofMillis(1000));
        if (System.getenv("ID") != null) {
            this.id = System.getenv("ID");
        } else {
            try {
                this.id = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                this.id = "unknown";
            }
        }
    }

    // used to create a new router
    public static Props props() {
        return Props.create(Router.class, () -> new Router());
    }

    @Override
    public void preStart() {
        String path = akka.serialization.Serialization.serializedActorPath(getSelf());
        this.log.info("router started up ({})", path);

        // define a message extractor for routing to the right aircraft
        ShardRegion.MessageExtractor messageExtractor = new ShardRegion.MessageExtractor() {

            @Override
            public String entityId(Object message) {
                if (message instanceof Aircraft.Telemetry)
                    return String.valueOf(((Aircraft.Telemetry) message).icao);
                else
                    return null;
            }

            @Override
            public Object entityMessage(Object message) {
                return message;
            }

            @Override
            public String shardId(Object message) {
                int numberOfShards = 100; // recommended 10x nodes
                if (message instanceof Aircraft.Telemetry) {
                    int id = ((Aircraft.Telemetry) message).icao;
                    return String.valueOf(id % numberOfShards);
                } else {
                    return null;
                }
            }

        };

        // shard the aircraft
        ActorSystem system = getContext().getSystem();
        ClusterShardingSettings settings = ClusterShardingSettings.create(system);
        ClusterSharding.get(system).start("Aircraft", Props.create(Aircraft.class), settings, messageExtractor);

    }

    // define the GenerateMessage class
    public static final class GenerateTelemetry {
    }

    // define receiver
    @Override
    public Receive createReceive() {
        return receiveBuilder().match(GenerateTelemetry.class, x -> {

            // generate the telemetry message
            int icao = rand.nextInt(2) + 1000;
            int seq = 0;
            if (this.seqs.containsKey(icao))
                seq = this.seqs.get(icao);
            seq++;
            this.seqs.put(icao, seq);
            int heading = rand.nextInt(360);
            Telemetry telemetry = new Telemetry(this.id, new Date(), icao, seq, heading);

            // route the message to the appropriate shard
            ActorSystem system = getContext().getSystem();
            ActorRef region = ClusterSharding.get(system).shardRegion("Aircraft");
            region.tell(telemetry, getSelf());

        }).build();
    }

}
