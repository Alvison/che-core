/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.workspace.server.stack.image;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import com.google.common.base.Objects;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.commons.lang.NameGenerator;

import static com.google.common.collect.ImmutableSet.of;


public class StackIcon {

    private static final Set<String> validMediaTypes = of("image/jpeg", "image/png", "image/gif", "image/svg+xml");
    private static final int         LIMIT_SIZE      = 1024 * 1024;

    private String id;
    private String mediaType;
    private byte[] data;

    public StackIcon(String mediaType, byte[] data) throws IOException, ConflictException {
        if (data == null || data.length == 0) {
            throw new IllegalStateException("Image content must not be empty");
        }

        if (data.length > LIMIT_SIZE) {
            throw new ConflictException("Maximum upload size exceeded 1 Mb limit");
        }
        this.data = data;

        if (!validMediaTypes.stream().anyMatch(elem -> elem.equals(mediaType))) {
            throw new IOException("Image media type '" + mediaType + "' is unsupported. Supported mediatypes: " + validMediaTypes);
        }
        this.mediaType = mediaType;
        this.id = NameGenerator.generate("stackIcon", 16);
    }

    public String getId() {
        return id;
    }

    public String getMediaType() {
        return mediaType;
    }

    public byte[] getData() {
        return data == null ? new byte[0] : data;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StackIcon)) {
            return false;
        }
        StackIcon another = (StackIcon)obj;
        return Objects.equal(mediaType, another.mediaType)
               && Arrays.equals(getData(), another.getData());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(mediaType);
        hash = 31 * hash + Arrays.hashCode(getData());
        return hash;
    }
}