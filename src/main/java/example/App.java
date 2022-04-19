package example;

import com.datastax.dse.driver.api.core.config.DseDriverOption;
import com.datastax.dse.driver.api.core.graph.DseGraph;
import com.datastax.dse.driver.api.core.graph.GraphNode;
import com.datastax.dse.driver.api.core.graph.GraphResultSet;
import com.datastax.dse.driver.api.core.graph.ScriptGraphStatement;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import java.time.Duration;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Run queries with DSE graph and search workloads.
 */
public class App {

  public static void runGraphScript(String graphName, String name) {
    try (CqlSession session = CqlSession.builder()
        .withLocalDatacenter("Graph")
        .withConfigLoader(DriverConfigLoader.programmaticBuilder()
            .withDuration(DseDriverOption.GRAPH_TIMEOUT, Duration.ofSeconds(5))
            .withString(DseDriverOption.GRAPH_NAME, graphName)
            .withString(DseDriverOption.GRAPH_TRAVERSAL_SOURCE, "g").build())
        .build()) {
      String script = "g.V().has('person', 'name', name)";
      ScriptGraphStatement statement = ScriptGraphStatement.builder(script)
          .setQueryParam("name", name)
          .build();

      GraphResultSet result = session.execute(statement);
      for (GraphNode node : result) {
        System.out.println(node.asVertex());
      }
    }
  }

  public static void runGraphFluent(String graphName, String name) {
    try (CqlSession session = CqlSession.builder()
        .withLocalDatacenter("Graph")
        .withConfigLoader(DriverConfigLoader.programmaticBuilder()
            .withDuration(DseDriverOption.GRAPH_TIMEOUT, Duration.ofSeconds(5))
            .withString(DseDriverOption.GRAPH_NAME, graphName)
            .withString(DseDriverOption.GRAPH_TRAVERSAL_SOURCE, "g").build())
        .build()) {

      GraphTraversalSource g = DseGraph.g
          .withRemote(DseGraph.remoteConnectionBuilder(session).build());

      List<Vertex> vertices = g.V().has("person", "name", name).toList();
      for (Vertex vertex : vertices) {
        System.out.println(vertex);
      }
    }
  }

  public static void runSearchQueries() {
    try (CqlSession session = CqlSession.builder()
        .withLocalDatacenter("Solr").build()) {
      session.execute("CREATE KEYSPACE IF NOT EXISTS search WITH REPLICATION = {     'class' : 'NetworkTopologyStrategy',     'Solr' : 3    }");
      session.execute("CREATE TABLE IF NOT EXISTS search.test (k text, v1 text, v2 text,  PRIMARY KEY (k, v1))");
      session.execute("CREATE SEARCH INDEX IF NOT EXISTS ON search.test WITH COLUMNS v2");
      session.execute("INSERT INTO \"search\".test (k, v1, v2) VALUES (?, ?, ?)",
          "a", "a", "abc");
      session.execute("INSERT INTO \"search\".test (k, v1, v2) VALUES (?, ?, ?)",
          "a", "b", "abcabc");
      session.execute("INSERT INTO \"search\".test (k, v1, v2) VALUES (?, ?, ?)",
          "a", "c", "abcdef");

      ResultSet rs;

      rs = session.execute("SELECT * FROM \"search\".test WHERE v2 LIKE '%ab%'");
      for (Row row : rs) {
        System.out.println(row.getFormattedContents());
      }


      rs = session.execute("SELECT * FROM \"search\".test WHERE solr_query = '{\"q\" : \"*:*\"}'");
      for (Row row : rs) {
        System.out.println(row.getFormattedContents());
      }
    }
  }

  public static void main(String[] args) throws InterruptedException {
    runGraphScript("classic", "Julia Child");
    runGraphFluent("classic", "Julia Child");
    runGraphScript("core", "Julia CHILD");
    runGraphFluent("core", "Julia CHILD");
    runSearchQueries();
  }
}
