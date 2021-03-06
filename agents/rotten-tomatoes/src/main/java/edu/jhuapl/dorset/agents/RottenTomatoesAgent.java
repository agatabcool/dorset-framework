/**
 * Copyright 2016 The Johns Hopkins University Applied Physics Laboratory LLC
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.jhuapl.dorset.agents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.jhuapl.dorset.agent.AgentBase;
import edu.jhuapl.dorset.agent.AgentRequest;
import edu.jhuapl.dorset.agent.AgentResponse;
import edu.jhuapl.dorset.agent.Description;
import edu.jhuapl.dorset.http.HttpClient;
import edu.jhuapl.dorset.nlp.BasicTokenizer;

public class RottenTomatoesAgent extends AgentBase {
    private final Logger logger = LoggerFactory.getLogger(RottenTomatoesAgent.class);
    
    private static final String SUMMARY = "Get answers to questions about movies. Ask about actors, actors, runtime, or release date.";
    private static final String EXAMPLE = "Who are the actors in the movie The Shining?";

    private String apikey;
    private HttpClient client;

    public RottenTomatoesAgent(HttpClient client, String apikey) {
        this.apikey = apikey;
        this.client = client;
        this.setName("movies");
        this.setDescription(new Description(name, SUMMARY, EXAMPLE));
    }

    @Override
    public AgentResponse process(AgentRequest request) {

        String agentRequest = request.text;
        String movieTitle = findMovieTitle(agentRequest);

        if (movieTitle.equals("")) {
            return new AgentResponse(
                    "I'm sorry, I don't understand your question.");
        }

        String keyword = findKeyWord(agentRequest);

        String agentResponse = null;
        String response = null;
        response = requestData(movieTitle);

        if (response == null) {
            agentResponse = "I'm sorry, something went wrong with the Rotten Tomatoes API request. "
                    + "Please make sure you have a proper API key.";
        } else {
            agentResponse = formatResponse(keyword, response);
            if (agentResponse == null) {
                agentResponse = "I'm sorry, I don't understand your question regarding movies.";
            }
        }

        return new AgentResponse(agentResponse);
    }

    public String requestData(String movieTitle) {
        movieTitle = movieTitle.replace(" ", "%20");
        String url = "http://api.rottentomatoes.com/api/public/v1.0/movies.json?apikey="
                + this.apikey + "&q=" + movieTitle;
        return client.get(url);
    }

    public String findMovieTitle(String agentRequest) {

        BasicTokenizer bt = new BasicTokenizer();
        String movieTitle = "";
        boolean filmFlag = false;

        String[] tokens = bt.tokenize(agentRequest);
        for (String token : tokens) {
            token = token.toLowerCase();
            if (filmFlag) {
                movieTitle = movieTitle + " " + token;
            }
            if (token.equals("movie") || token.equals("film") && !filmFlag) {
                filmFlag = true;
            }
        }
        movieTitle = movieTitle.replace("?", "").replace(".", "").trim();

        return movieTitle;
    }

    public String findKeyWord(String agentRequest) {
        String keyword;
        // Year film was made
        if (agentRequest.toLowerCase().contains("year")) {
            keyword = "year";
        }
        // Runtime for the film
        else if (agentRequest.toLowerCase().contains("runtime")) {
            keyword = "runtime";
        }
        // MPAA rating for the film
        else if (agentRequest.toLowerCase().contains("mpaa rating")) {
            keyword = "mpaa_rating";
        }
        // Main actor for the film
        else if (agentRequest.toLowerCase().contains("actor")) {
            keyword = "name";
        }
        // None of the keywords above is mentioned within the request
        else {
            keyword = "unsure";
        }
        return keyword;
    }

    public String formatResponse(String keyword, String response) {
        Gson gson = new Gson();

        JsonObject jsonObj = gson.fromJson(response.toString(),
                JsonObject.class);
        JsonArray jsonMoviesArray = jsonObj.get("movies").getAsJsonArray();

        if (jsonMoviesArray.size() == 0) {
            return "I am sorry, I don't know that movie.";
        }

        JsonObject jsonObjMovie = gson.fromJson(jsonMoviesArray.get(0),
                JsonObject.class);

        JsonArray jsonCastArray = jsonObjMovie.get("abridged_cast")
                .getAsJsonArray();

        String movieTitle = jsonObjMovie.get("title").toString();

        String agentResponse = null;

        switch (keyword) {
        case "runtime": {
            agentResponse = "The runtime for the film, "
                    + movieTitle.replace("\"", " ").trim() + ", is "
                    + jsonObjMovie.get(keyword) + " minutes long.";
            break;
        }

        case "year": {
            agentResponse = "The year the film, "
                    + movieTitle.replace("\"", " ").trim()
                    + ", was created is " + jsonObjMovie.get(keyword) + ".";

            break;
        }
        case "mpaa_rating": {
            agentResponse = "The MPAA rating for the film, "
                    + movieTitle.replace("\"", " ").trim() + ", is "
                    + jsonObjMovie.get(keyword) + ".";

            break;
        }

        case "name": {
            String nameList = "";
            String name = "";
            for (int i = 0; i < jsonCastArray.size(); i++) {
                JsonObject jsonObjNames = gson.fromJson(jsonCastArray.get(i),
                        JsonObject.class);
                name = jsonObjNames.get("name").toString();

                if (i != jsonCastArray.size() - 1) {
                    nameList = nameList + name + ", ";
                } else {
                    nameList = nameList + name + ".";
                }
            }
            nameList = nameList.replace("\"", "").trim();

            agentResponse = "The film, " + movieTitle.replace("\"", " ").trim()
                    + ", stars actors " + nameList;
        }
        default:
            break;
        }
        return agentResponse;
    }

}
