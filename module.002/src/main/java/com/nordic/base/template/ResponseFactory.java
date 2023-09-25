package com.nordic.base.template;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

@RequiredArgsConstructor
public class ResponseFactory {

    private final ResponseEntity<?> response;
    private final Gson parser = new Gson();

    public <T> ResponseEntity<T> responseEntity(Class<T> type) {
        return ResponseEntity.status(response.getStatusCode()).body(type.cast(response.getBody()));
    }

    public <T> T body(Class<T> type) {
        return parser.fromJson((String) response.getBody(), type);
    }
}
