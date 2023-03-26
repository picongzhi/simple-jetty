module simple.jetty.server {
    requires org.slf4j;
    requires javax.servlet.api;

    requires transitive simple.jetty.core;
    requires transitive simple.jetty.http;
}