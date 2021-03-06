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
package org.n52.svalbard.encode;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityConstants;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityResponse;
import org.n52.shetland.w3c.SchemaLocation;
import org.n52.svalbard.encode.exception.EncodingException;
import org.n52.svalbard.util.XmlHelper;
import org.n52.svalbard.write.GetDataAvailabilityStreamWriter;

import com.google.common.collect.Sets;

/**
 * {@code Encoder} to handle {@link GetDataAvailabilityResponse}s.
 *
 * @author Christian Autermann
 *
 * @since 4.0.0
 */
public class GetDataAvailabilityXmlEncoder extends AbstractResponseEncoder<GetDataAvailabilityResponse> {
    private static final Logger LOG = LoggerFactory.getLogger(GetDataAvailabilityXmlEncoder.class);
    public GetDataAvailabilityXmlEncoder() {
        super(SosConstants.SOS, Sos2Constants.SERVICEVERSION, GetDataAvailabilityConstants.OPERATION_NAME,
                Sos2Constants.NS_SOS_20, SosConstants.NS_SOS_PREFIX, GetDataAvailabilityResponse.class, false);
    }

    @Override
    protected Set<SchemaLocation> getConcreteSchemaLocations() {
        return Sets.newHashSet(GetDataAvailabilityConstants.GET_DATA_AVAILABILITY_SCHEMA_LOCATION);
    }

    @Override
    protected XmlObject create(GetDataAvailabilityResponse response) throws EncodingException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new GetDataAvailabilityStreamWriter(response.getVersion(), response.getDataAvailabilities()).write(out);
            XmlObject encodedObject = XmlObject.Factory.parse(out.toString("UTF8"));
            XmlHelper.validateDocument(encodedObject, EncodingException::new);
            return encodedObject;
        } catch (XMLStreamException | XmlException | UnsupportedEncodingException ex) {
            throw new EncodingException("Error encoding response", ex);
        }
    }
}
