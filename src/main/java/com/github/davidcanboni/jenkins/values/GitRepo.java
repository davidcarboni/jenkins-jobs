package com.github.davidcanboni.jenkins.values;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by david on 15/07/2015.
 */
public enum GitRepo {

    babbage("https://github.com/ONSdigital/babbage.git", true),
    florence("https://github.com/ONSdigital/florence.git", true),
    zebedee("https://github.com/Carboni/zebedee.git", false),
    brian("https://github.com/thomasridd/project-brian.git", false),
    thetrain("https://github.com/Carboni/The-Train.git", false);

    public URL url;
    public boolean nodeJs;

    GitRepo(String url, boolean nodeJs) {
        try {
            this.url = new URL(url);
            this.nodeJs = nodeJs;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error in url: " + url, e);
        }
    }
}
