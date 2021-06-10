// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.ce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("cloudendure")
public class CloudEndureProperties {

    /**
     * Api object which belongs to the cloud endure.
     */
    private Api api;

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public static class Api {
        /**
         * Url of the service.
         */
        private String url = "https://console.cloudendure.com/api/latest";

        String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
