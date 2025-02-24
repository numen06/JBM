/*
 * Copyright (c) 2024-present HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expres or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package jbm.framework.boot.autoconfigure.mqtt.hivemq.factories;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.KeyUtil;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.config.MqttProperties;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.ssl.KeyManagerFactoryCreationException;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.ssl.TrustManagerFactoryCreationException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * Common interface for MQTT client factories.
 *
 * @author Sven Kobow
 * @since 1.0.0
 */
public interface IMqttClientFactory {

    default KeyManagerFactory getKeyManagerFactory(final MqttProperties.SslProperties certProperties) throws KeyManagerFactoryCreationException {
        try {
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            final Certificate certificate = KeyUtil.readCertificate("X.509", FileUtil.getInputStream(certProperties.getCertificate()));
//            final PrivateKey key = PrivateKeyReader.getPrivateKey(certProperties.getPrivateKey(), certProperties.getPassword());
            final byte[] passwordBytes = StrUtil.bytes(new String(certProperties.getPassword()));
            final PrivateKey key = KeyUtil.generatePrivateKey(FileUtil.readUtf8String(certProperties.getPrivateKey()), passwordBytes);

            keyStore.load(null, null);
            keyStore.setCertificateEntry("certificate", certificate);
            keyStore.setKeyEntry("private-key", key, certProperties.getPassword(), new Certificate[]{certificate});

            kmf.init(keyStore, certProperties.getPassword());

            return kmf;
        } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException |
                 UnrecoverableKeyException e) {
            throw new KeyManagerFactoryCreationException(e.getMessage(), e);
        }
    }

    default TrustManagerFactory getTrustManagerFactory(final MqttProperties.SslProperties certProperties) throws TrustManagerFactoryCreationException {
        try {
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

            final Certificate certificate = KeyUtil.readCertificate("X.509", FileUtil.getInputStream(certProperties.getCertificate()));

            keyStore.load(null);
            keyStore.setCertificateEntry("ca-certificate", certificate);

            tmf.init(keyStore);

            return tmf;
        } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException e) {
            throw new TrustManagerFactoryCreationException(e.getMessage(), e);
        }
    }
}
