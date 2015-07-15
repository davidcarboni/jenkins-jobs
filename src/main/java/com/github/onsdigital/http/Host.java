package com.github.onsdigital.http;

import org.apache.http.client.utils.URIBuilder;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by david on 25/03/2015.
 */
public class Host {

    URL url;

    public Host(String baseUrl) {
        try {
            URIBuilder uriBuilder = new URIBuilder(baseUrl);
            url = uriBuilder.build().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException("Er, reealy? baseUrl: " + baseUrl);
        }
    }

    public Host(URL baseUrl) {
        url = baseUrl;
    }
}
