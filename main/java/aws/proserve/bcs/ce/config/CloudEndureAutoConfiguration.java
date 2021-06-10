// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.config;

import aws.proserve.bcs.ce.service.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty(prefix = "cloudendure", value = "enabled", havingValue = "true")
@EnableScheduling
@EnableConfigurationProperties(CloudEndureProperties.class)
@Import({CloudEndureConfig.class, CloudEndureCommonConfig.class})
class CloudEndureAutoConfiguration {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Session session;
    private final CloudEndureProperties properties;

    CloudEndureAutoConfiguration(
            Session session,
            CloudEndureProperties properties) {
        this.session = session;
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.rootUri(properties.getApi().getUrl())
                .interceptors((request, body, execution) -> {
                    if (session.getSecret() != null) {
                        request.getHeaders().set("X-XSRF-TOKEN", session.getSecret());
                    } else {
                        log.warn("Authentication information is missing.");
                    }

                    return execution.execute(request, body);
                }).build();
    }
}
