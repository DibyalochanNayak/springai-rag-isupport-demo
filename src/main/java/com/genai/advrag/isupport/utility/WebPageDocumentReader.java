package com.genai.advrag.isupport.utility;

import com.genai.advrag.isupport.exception.WebPageNotFoundException;
import com.genai.advrag.isupport.exception.WebPageReadException;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WebPageDocumentReader {

    public List<Document> read(String url) {

        try {

            org.jsoup.nodes.Document html = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(30000)
                    .get();

            if (html.body() == null) {
                return List.of();
            }

            String content = html.body().text();

            if (!StringUtils.hasText(content)) {
                return List.of();
            }

            return List.of(new Document(content));

        }
        catch (HttpStatusException ex) {

            if (ex.getStatusCode() == 404) {
                throw new WebPageNotFoundException(url);
            }

            throw new WebPageReadException(
                    "Unable to read webpage : " + url,
                    ex.getMessage());
        }
        catch (IOException ex) {

            throw new WebPageReadException(
                    "Unable to read webpage : " + url,
                    ex.getMessage());
        }
    }
}
