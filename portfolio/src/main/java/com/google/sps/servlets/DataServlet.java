// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.lang.Integer;
import com.google.gson.Gson;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.sps.data.Comment;

/** Servlet to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private final Gson gson = new Gson();

  @Override
  public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

    Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    // Limit on the number of comments to get from datastore.
    String userLimit = request.getParameter("limit");

    // Initialise limit with max (all comments in datastore)
    FetchOptions fetchOptionsWithLimit = FetchOptions.Builder.withLimit(results.countEntities(FetchOptions.Builder.withDefaults()));

    if(userLimit != null && userLimit.isEmpty() == false) {       
      try {
        fetchOptionsWithLimit = FetchOptions.Builder.withLimit(Integer.parseInt(userLimit));
      } 
      catch(Exception e) {
        System.out.println(e + "Warning! The number of comments to display MUST be an integer. You have inputted " + userLimit);
      }    
    }
    
    List<Comment> comments = new ArrayList<>();
    for (Entity entity : results.asIterable(fetchOptionsWithLimit)) {
      long id = entity.getKey().getId();
      String name = (String) entity.getProperty("name");
      String message = (String) entity.getProperty("message");
      long timestamp = (long) entity.getProperty("timestamp");
      
      Comment comment = new Comment(id, name, message, timestamp);
      comments.add(comment);
    }

    // Send JSON as response.
    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(comments));
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    // Get the input from the form.
    String name = request.getParameter("full-name");
    String message = request.getParameter("message");
    long timestamp = System.currentTimeMillis();

    // Only add comments where a name and a message is given.
    if(!name.isEmpty() && !message.isEmpty()) {
      Entity commentEntity = new Entity("Comment");
      commentEntity.setProperty("name", name);
      commentEntity.setProperty("message", message);
      commentEntity.setProperty("timestamp", timestamp);

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      datastore.put(commentEntity);

    }  
    response.sendRedirect("/index.html");  

  } 
}
