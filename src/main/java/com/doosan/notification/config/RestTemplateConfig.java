package com.doosan.notification.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.config.RequestConfig;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class RestTemplateConfig {

    public static RestTemplate restTemplate() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000) // 연결 타임아웃 (ms)
                .setSocketTimeout(10000) // 읽기 타임아웃 (ms)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        return new RestTemplate(factory);
    }
}

