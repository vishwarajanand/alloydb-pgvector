package gcfv2;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import java.io.BufferedWriter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

public class HelloHttpFunction implements HttpFunction {
 @Override
  public void service(HttpRequest request, HttpResponse response) throws Exception {

// Get the request body as a JSON object.
 JsonObject requestJson = new Gson().fromJson(request.getReader(), JsonObject.class);
 String searchText = requestJson.get("search").getAsString();
//Sample searchText: "A new Natural Language Processing related Machine Learning Model";
    BufferedWriter writer = response.getWriter();
    String result = "";
    HikariDataSource dataSource = AlloyDbJdbcConnector();
    JsonArray jsonArray = new JsonArray(); // Create a JSON array
     try (Connection connection = dataSource.getConnection()) {
       //Retrieve Vector Search by text (converted to embeddings) using "Cosine Similarity" method
      try (PreparedStatement statement = connection.prepareStatement("SELECT id || ' - ' || title as literature FROM patents_data ORDER BY abstract_embeddings <=> embedding('textembedding-gecko@001', '" + searchText + "' )::vector LIMIT 10")) {
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        String lit = resultSet.getString("literature");
        result = result + lit + "\n";
        System.out.println("Matching Literature: " + lit);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("result", result);
        jsonArray.add(jsonObject);
      }
      response.setContentType("application/json");
      writer.write(jsonArray.toString());
      //writer.write("Here is the closest match: " + result);
     }
  }
  
public  HikariDataSource AlloyDbJdbcConnector() {
   HikariDataSource dataSource;

   String ALLOYDB_DB = "postgres";
   String ALLOYDB_USER = "postgres";
   String ALLOYDB_PASS = "alloydb";
   String ALLOYDB_INSTANCE_NAME = "projects/YOUR_PROJECT_ID/locations/us-central1/clusters/vector-cluster/instances/vector-instance"; 
  //Replace YOUR_PROJECT_ID, YOUR_CLUSTER, YOUR_INSTANCE with your actual values
  
   HikariConfig config = new HikariConfig();

    config.setJdbcUrl(String.format("jdbc:postgresql:///%s", ALLOYDB_DB));
    config.setUsername(ALLOYDB_USER); // e.g., "postgres"
    config.setPassword(ALLOYDB_PASS); // e.g., "secret-password"
    config.addDataSourceProperty("socketFactory", "com.google.cloud.alloydb.SocketFactory");
    // e.g., "projects/my-project/locations/us-central1/clusters/my-cluster/instances/my-instance"
    config.addDataSourceProperty("alloydbInstanceName", ALLOYDB_INSTANCE_NAME);
    //config.addDataSourceProperty("alloydbEnableIAMAuth", "true");

    dataSource = new HikariDataSource(config);
    return dataSource;
  
}
}


