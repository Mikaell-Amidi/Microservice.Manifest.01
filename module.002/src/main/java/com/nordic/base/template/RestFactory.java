package com.nordic.base.template;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class RestFactory {


    private final RestTemplate restTemplate;

    public <R> ResponseFactory withBody(String url, R body, HttpMethod method) {
        return new ResponseFactory(restTemplate.exchange(url, method, new HttpEntity<>(body), String.class));
    }

    public <T> ResponseFactory post(String url, T t) {
        return new ResponseFactory(restTemplate.postForEntity(url, t, String.class));
    }

    public ResponseFactory get(String url, Object... uriVariables) {
        return new ResponseFactory(restTemplate.getForEntity(url, String.class, uriVariables));
    }

    public void addInterceptor(BasicAuthenticationInterceptor interceptor) {
        this.restTemplate.getInterceptors().add(interceptor);
    }
}
