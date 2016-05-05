/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.core.util;

import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.NameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.HttpHeaders;

/**
 * DownloadPlugin that downloads single file.
 *
 * @author andrew00x
 */
public final class HttpDownloadPlugin implements DownloadPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(HttpDownloadPlugin.class);

    private static final int CONNECT_TIMEOUT = (int)TimeUnit.MINUTES.toMillis(3);
    private static final int READ_TIMEOUT    = (int)TimeUnit.MINUTES.toMillis(3);

    @Override
    public void download(String downloadUrl, java.io.File downloadTo, Callback callback) {
        HttpURLConnection conn = null;
        try {
            conn = openUrlConnection(downloadUrl);
            final String contentDisposition = conn.getHeaderField(HttpHeaders.CONTENT_DISPOSITION);
            String fileName = null;
            if (contentDisposition != null) {
                int fNameStart = contentDisposition.indexOf("filename=");
                if (fNameStart > 0) {
                    int fNameEnd = contentDisposition.indexOf(';', fNameStart + 1);
                    if (fNameEnd < 0) {
                        fNameEnd = contentDisposition.length();
                    }
                    fileName = contentDisposition.substring(fNameStart, fNameEnd).split("=")[1];
                    if (fileName.charAt(0) == '"' && fileName.charAt(fileName.length() - 1) == '"') {
                        fileName = fileName.substring(1, fileName.length() - 1);
                    }
                }
            }
            final java.io.File downloadFile =
                    new java.io.File(downloadTo, fileName == null ? NameGenerator.generate("downloaded.file", 4) : fileName);
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, downloadFile.toPath());
            }
            callback.done(downloadFile);
        } catch (IOException e) {
            LOG.debug(String.format("Failed access: %s, error: %s", downloadUrl, e.getMessage()), e);
            callback.error(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public void download(String downloadUrl, java.io.File downloadTo, String fileName, boolean replaceExisting) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = openUrlConnection(downloadUrl);
            final java.io.File downloadFile = new java.io.File(downloadTo, fileName);
            try (InputStream in = conn.getInputStream()) {
                if (replaceExisting) {
                    Files.copy(in, downloadFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.copy(in, downloadFile.toPath());
                }
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static HttpURLConnection openUrlConnection(String downloadUrl) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)new URL(downloadUrl).openConnection();
        // Set timeouts
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        // Set authorization if present
        final EnvironmentContext context = EnvironmentContext.getCurrent();
        if (context.getUser() != null && context.getUser().getToken() != null) {
            conn.setRequestProperty(HttpHeaders.AUTHORIZATION, context.getUser().getToken());
        }
        // Connect
        final int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException(String.format("Invalid response status %d from remote server. ", responseCode));
        }
        return conn;
    }
}
