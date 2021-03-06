/*
 * Copyright 2016-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.svalbard.util;

import java.util.HashSet;
import java.util.Set;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.n52.janmayen.http.MediaTypes;
import org.n52.svalbard.decode.DecoderKey;
import org.n52.svalbard.decode.OperationDecoderKey;
import org.n52.svalbard.decode.XmlNamespaceDecoderKey;
import org.n52.svalbard.decode.XmlStringOperationDecoderKey;
import org.n52.svalbard.decode.exception.XmlDecodingException;
import org.n52.svalbard.encode.EncoderKey;
import org.n52.svalbard.encode.XmlEncoderKey;

/**
 * TODO implement encodeToXml(Object o) using a Map from o.getClass().getName()
 * -> namespaces
 *
 * @author <a href="mailto:c.autermann@52north.org">Christian Autermann</a>
 *
 * @since 4.0.0
 *
 */
public final class CodingHelper {

    private CodingHelper() {
    }

    public static Set<DecoderKey> decoderKeysForElements(String namespace, Class<?>... elements) {
        final HashSet<DecoderKey> keys = new HashSet<>(elements.length);
        for (final Class<?> x : elements) {
            keys.add(new XmlNamespaceDecoderKey(namespace, x));
        }
        return keys;
    }

    public static Set<DecoderKey> xmlDecoderKeysForOperation(String service, String version, Enum<?>... operations) {
        final HashSet<DecoderKey> set = new HashSet<>(operations.length);
        for (final Enum<?> o : operations) {
            set.add(new OperationDecoderKey(service, version, o.name(), MediaTypes.TEXT_XML));
            set.add(new OperationDecoderKey(service, version, o.name(), MediaTypes.APPLICATION_XML));
        }
        return set;
    }

    public static Set<DecoderKey> xmlDecoderKeysForOperation(String service, String version, String... operations) {
        HashSet<DecoderKey> set = new HashSet<>(operations.length);
        for (String o : operations) {
            set.add(new OperationDecoderKey(service, version, o, MediaTypes.TEXT_XML));
            set.add(new OperationDecoderKey(service, version, o, MediaTypes.APPLICATION_XML));
        }
        return set;
    }

    public static Set<DecoderKey> xmlStringDecoderKeysForOperationAndMediaType(String service, String version,
            Enum<?>... operations) {
        final HashSet<DecoderKey> set = new HashSet<>(operations.length);
        for (final Enum<?> o : operations) {
            set.add(new XmlStringOperationDecoderKey(service, version, o, MediaTypes.TEXT_XML));
            set.add(new XmlStringOperationDecoderKey(service, version, o, MediaTypes.APPLICATION_XML));
        }
        return set;
    }

    public static Set<DecoderKey> xmlStringDecoderKeysForOperationAndMediaType(String service, String version,
            String... operations) {
        HashSet<DecoderKey> set = new HashSet<>(operations.length);
        for (String o : operations) {
            set.add(new XmlStringOperationDecoderKey(service, version, o, MediaTypes.TEXT_XML));
            set.add(new XmlStringOperationDecoderKey(service, version, o, MediaTypes.APPLICATION_XML));
        }
        return set;
    }

    public static Set<EncoderKey> encoderKeysForElements(final String namespace, final Class<?>... elements) {
        final HashSet<EncoderKey> keys = new HashSet<>(elements.length);
        for (final Class<?> x : elements) {
            keys.add(new XmlEncoderKey(namespace, x));
        }
        return keys;
    }

    public static EncoderKey getEncoderKey(String namespace, Object o) {
        return new XmlEncoderKey(namespace, o.getClass());
    }

    public static DecoderKey getDecoderKey(final XmlObject doc) {
        return new XmlNamespaceDecoderKey(XmlHelper.getNamespace(doc), doc.getClass());
    }

    public static <T extends XmlObject> DecoderKey getDecoderKey(final T[] doc) {
        return new XmlNamespaceDecoderKey(XmlHelper.getNamespace(doc[0]), doc.getClass());
    }

    public static XmlObject readXML(String string) throws XmlDecodingException {
        try {
            return XmlObject.Factory.parse(string);
        } catch (XmlException e) {
            throw new XmlDecodingException("XML string", string, e);
        }
    }
}
