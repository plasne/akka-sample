package com.plasne;

import java.io.IOException;
import java.util.ArrayList;
import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Startup {
    public static void main(String[] args) {
        ArrayList<ActorSystem> systems = new ArrayList<ActorSystem>();
        try {

            // startup on the specified port
            String port = System.getenv("PORT") != null ? System.getenv("PORT") : "0";
            Config config = ConfigFactory.parseString("akka.remote.artery.canonical.port=" + port)
                    .withFallback(ConfigFactory.load());
            final ActorSystem system = ActorSystem.create("app", config);
            systems.add(system);
            system.actorOf(Router.props(), "router");

            // wait for exit
            System.out.println(">>> Press ENTER to exit <<<");
            System.in.read();

        } catch (IOException ioe) {
            System.out.println(ioe);
        } finally {
            for (ActorSystem system : systems) {
                system.terminate();
            }
        }
    }
}
