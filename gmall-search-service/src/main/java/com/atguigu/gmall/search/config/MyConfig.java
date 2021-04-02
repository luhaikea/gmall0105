package com.atguigu.gmall.search.config;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyConfig {

    @Bean
    public JestClient jestClient(){
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder("http://114.55.140.81:9200")
                .multiThreaded(true)
                .connTimeout(60000)
                .readTimeout(60000)
                .defaultMaxTotalConnectionPerRoute(10)
                .maxTotalConnection(100)
                .build()
        );
        return  factory.getObject();
    }
}
