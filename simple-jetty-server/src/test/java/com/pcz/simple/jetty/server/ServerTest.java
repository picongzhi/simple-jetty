package com.pcz.simple.jetty.server;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServerTest {
    @Test
    public void should_instantiate() {
        Server server = new Server(8080);
        Assertions.assertThat(server).isNotNull();
    }

    @Test
    public void should_start() throws Exception {
        Server server = new Server(8080);
        Assertions.assertThat(server).isNotNull();
        server.start();
    }
}
