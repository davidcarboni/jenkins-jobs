package com.github.davidcanboni.jenkins.values;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by david on 15/07/2015.
 */
public enum GitRepo {

    babbage("https://github.com/ONSdigital/babbage.git", true),
    florence("https://github.com/ONSdigital/florence.git", true),
    zebedee("https://github.com/Carboni/zebedee.git"),
    zebedeeReader("https://github.com/Carboni/zebedee.git", false, true),
    brian("https://github.com/ONSdigital/project-brian.git"),
    thetrain("https://github.com/Carboni/The-Train.git");

    public URL url;
    public boolean nodeJs;
    public boolean submodule;

    GitRepo(String url, boolean... parameters) {
        try {
            this.url = new URL(url);
            if (parameters.length > 0) this.nodeJs = parameters[0];
            if (parameters.length > 1) this.submodule = parameters[1];
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error in url: " + url, e);
        }
    }


    @Override
    public String toString() {
        // Transform zebedeeReader into zebedee-reader:
        return name().replace("R", "-r");
    }
}
