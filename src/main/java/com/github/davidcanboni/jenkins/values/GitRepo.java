package com.github.davidcanboni.jenkins.values;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by david on 15/07/2015.
 */
public enum GitRepo {

    babbage("https://github.com/ONSdigital/babbage.git"),
    florence("https://github.com/ONSdigital/florence.git"),
    zebedee("https://github.com/Carboni/zebedee.git"),
    brian("https://github.com/thomasridd/project-brian.git");

    public URL url;

    GitRepo(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error in url: " + url, e);
        }
    }
}
