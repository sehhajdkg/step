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
import com.google.gson.Gson;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private final Gson gson = new Gson();

  @Override
  public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        
        // Initialise messages with three hard-coded values
        List<String> messages = new ArrayList<String>();
        messages.add("Lesser leather never weathered wetter weather better");
        messages.add("Which witch is which?");
        messages.add("Fred fed Ted bread, and Ted fed Fred bread");

        // Convert to JSON format
        String json = toJsonArray(messages);

        // Send JSON as response
        response.setContentType("application/json;");
        response.getWriter().println(json);
    }

    /**
     * Converts input to a JSON Array
     */
    private String toJsonArray(List<String> data) {
        final String json = gson.toJson(data);
        return json;

    }
}
