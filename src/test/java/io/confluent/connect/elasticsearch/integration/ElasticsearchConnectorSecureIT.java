/*
 * Copyright 2020 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.connect.elasticsearch.integration;

import io.confluent.common.utils.IntegrationTest;
import io.confluent.connect.elasticsearch.ElasticsearchClient;
import io.confluent.connect.elasticsearch.ElasticsearchSinkConnectorConfig;
import io.confluent.connect.elasticsearch.ElasticsearchSinkConnectorConfig.SecurityProtocol;
import io.confluent.connect.elasticsearch.helper.ElasticsearchContainer;
import io.confluent.connect.elasticsearch.helper.ElasticsearchHelperClient;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.confluent.connect.elasticsearch.ElasticsearchSinkConnectorConfig.CONNECTION_PASSWORD_CONFIG;
import static io.confluent.connect.elasticsearch.ElasticsearchSinkConnectorConfig.CONNECTION_URL_CONFIG;
import static io.confluent.connect.elasticsearch.ElasticsearchSinkConnectorConfig.CONNECTION_USERNAME_CONFIG;
import static io.confluent.connect.elasticsearch.ElasticsearchSinkConnectorConfig.SECURITY_PROTOCOL_CONFIG;
import static io.confluent.connect.elasticsearch.ElasticsearchSinkConnectorConfig.SSL_CONFIG_PREFIX;
import static io.confluent.connect.elasticsearch.helper.ElasticsearchContainer.ELASTIC_PASSWORD;

@Category(IntegrationTest.class)
public class ElasticsearchConnectorSecureIT extends ElasticsearchConnectorBaseIT {

  private static final Logger log = LoggerFactory.getLogger(ElasticsearchConnectorSecureIT.class);

  @BeforeClass
  public static void setupBeforeAll() {
    container = ElasticsearchContainer.fromSystemProperties().withSslEnabled(true);
    container.start();
  }

  /**
   * Run test against docker image running Elasticsearch.
   * Certificates are generated in src/test/resources/ssl/start-elasticsearch.sh
   */
  @Test
  public void testSecureConnectionVerifiedHostname() throws Throwable {
    // Use IP address here because that's what the certificates allow
    String address = container.getConnectionUrl();
    address = address.replace(container.getContainerIpAddress(), container.hostMachineIpAddress());
    log.info("Creating connector for {}.", address);

    props.put(CONNECTION_URL_CONFIG, address);
    addSslProps();

    helperClient = new ElasticsearchHelperClient(new ElasticsearchClient(new ElasticsearchSinkConnectorConfig(props), null).client());

    // Start connector
    runSimpleTest(props);
  }

  @Test
  public void testSecureConnectionHostnameVerificationDisabled() throws Throwable {
    // Use 'localhost' here that is not in self-signed cert
    String address = container.getConnectionUrl();
    address = address.replace(container.getContainerIpAddress(), "localhost");
    log.info("Creating connector for {}", address);

    props.put(CONNECTION_URL_CONFIG, address);
    addSslProps();

    // disable hostname verification
    props.put(SSL_CONFIG_PREFIX + SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

    helperClient = new ElasticsearchHelperClient(new ElasticsearchClient(new ElasticsearchSinkConnectorConfig(props), null).client());

    // Start connector
    runSimpleTest(props);
  }

  private void addSslProps() {
    props.put(SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name());
    props.put(SSL_CONFIG_PREFIX + SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, container.getKeystorePath());
    props.put(SSL_CONFIG_PREFIX + SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, container.getKeystorePassword());
    props.put(SSL_CONFIG_PREFIX + SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, container.getTruststorePath());
    props.put(SSL_CONFIG_PREFIX + SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, container.getTruststorePassword());
    props.put(SSL_CONFIG_PREFIX + SslConfigs.SSL_KEY_PASSWORD_CONFIG, container.getKeyPassword());
    props.put(CONNECTION_USERNAME_CONFIG, "elastic");
    props.put(CONNECTION_PASSWORD_CONFIG, ELASTIC_PASSWORD);
  }
}
