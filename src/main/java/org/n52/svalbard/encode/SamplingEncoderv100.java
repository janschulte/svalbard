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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.XmlObject;
import org.joda.time.DateTime;
import org.n52.shetland.ogc.OGCConstants;
import org.n52.shetland.ogc.SupportedType;
import org.n52.shetland.ogc.gml.AbstractFeature;
import org.n52.shetland.ogc.gml.CodeType;
import org.n52.shetland.ogc.gml.GmlConstants;
import org.n52.shetland.ogc.om.features.FeatureCollection;
import org.n52.shetland.ogc.om.features.SfConstants;
import org.n52.shetland.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.shetland.ogc.sos.FeatureType;
import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.util.CollectionHelper;
import org.n52.shetland.w3c.SchemaLocation;
import org.n52.svalbard.ConformanceClass;
import org.n52.svalbard.ConformanceClasses;
import org.n52.svalbard.SosHelperValues;
import org.n52.svalbard.encode.exception.EncodingException;
import org.n52.svalbard.encode.exception.UnsupportedEncoderInputException;
import org.n52.svalbard.util.CodingHelper;
import org.n52.svalbard.util.XmlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import net.opengis.gml.FeaturePropertyType;
import net.opengis.sampling.x10.SamplingCurveDocument;
import net.opengis.sampling.x10.SamplingCurveType;
import net.opengis.sampling.x10.SamplingFeatureCollectionDocument;
import net.opengis.sampling.x10.SamplingFeatureCollectionType;
import net.opengis.sampling.x10.SamplingFeaturePropertyType;
import net.opengis.sampling.x10.SamplingFeatureType;
import net.opengis.sampling.x10.SamplingPointDocument;
import net.opengis.sampling.x10.SamplingPointType;
import net.opengis.sampling.x10.SamplingSurfaceDocument;
import net.opengis.sampling.x10.SamplingSurfaceType;

/**
 * @since 4.0.0
 *
 */
public class SamplingEncoderv100 extends AbstractXmlEncoder<XmlObject, AbstractFeature> implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(SamplingEncoderv100.class);

    @SuppressWarnings("unchecked")
    private static final Set<EncoderKey> ENCODER_KEYS =
            CollectionHelper.union(CodingHelper.encoderKeysForElements(SfConstants.NS_SA, AbstractFeature.class));

    // TODO here also the question, sa:samplingPoint sampling/1.0 vs 2.0 mapping
    // or not and where and how to handle
    private static final Set<SupportedType> SUPPORTED_TYPES = ImmutableSet.<SupportedType> builder()
            .add(new FeatureType(OGCConstants.UNKNOWN)).add(new FeatureType(SfConstants.EN_SAMPLINGPOINT))
            .add(new FeatureType(SfConstants.EN_SAMPLINGSURFACE)).add(new FeatureType(SfConstants.EN_SAMPLINGCURVE))
            .build();

    private static final Set<String> CONFORMANCE_CLASSES =
            ImmutableSet.of(ConformanceClasses.OM_V2_SPATIAL_SAMPLING, ConformanceClasses.OM_V2_SAMPLING_POINT,
                    ConformanceClasses.OM_V2_SAMPLING_CURVE, ConformanceClasses.OM_V2_SAMPLING_SURFACE);

    public SamplingEncoderv100() {
        LOGGER.debug("Encoder for the following keys initialized successfully: {}!",
                Joiner.on(", ").join(ENCODER_KEYS));
    }

    @Override
    public Set<EncoderKey> getKeys() {
        return Collections.unmodifiableSet(ENCODER_KEYS);
    }

    @Override
    public Set<SupportedType> getSupportedTypes() {
        return Collections.unmodifiableSet(SUPPORTED_TYPES);
    }

    @Override
    public Set<String> getConformanceClasses(String service, String version) {
        if (SosConstants.SOS.equals(service) && Sos2Constants.SERVICEVERSION.equals(version)) {
            return Collections.unmodifiableSet(CONFORMANCE_CLASSES);
        }
        return Collections.emptySet();
    }

    @Override
    public void addNamespacePrefixToMap(Map<String, String> nameSpacePrefixMap) {
        nameSpacePrefixMap.put(SfConstants.NS_SA, SfConstants.NS_SA_PREFIX);
    }

    @Override
    public Set<SchemaLocation> getSchemaLocations() {
        return Sets.newHashSet(SfConstants.SA_SCHEMA_LOCATION);
    }

    @Override
    public XmlObject encode(AbstractFeature abstractFeature, EncodingContext additionalValues)
            throws EncodingException {
        return XmlHelper.validateDocument(createFeature(abstractFeature), EncodingException::new);
    }

    private XmlObject createFeature(AbstractFeature absFeature) throws EncodingException {
        if (absFeature instanceof SamplingFeature) {
            SamplingFeature sampFeat = (SamplingFeature) absFeature;
            if (sampFeat.getFeatureType().equals(SfConstants.FT_SAMPLINGPOINT)
                    || sampFeat.getFeatureType().equals(SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_POINT)
                    || sampFeat.getGeometry() instanceof Point) {
                SamplingPointDocument xbSamplingPointDoc = SamplingPointDocument.Factory.newInstance(getXmlOptions());
                SamplingPointType xbSamplingPoint = xbSamplingPointDoc.addNewSamplingPoint();
                addValuesToFeature(xbSamplingPoint, sampFeat);
                XmlObject xbGeomety = getEncodedGeometry(sampFeat.getGeometry(), absFeature.getGmlId());
                xbSamplingPoint.addNewPosition().addNewPoint().set(xbGeomety);
                return xbSamplingPointDoc;
            } else if (sampFeat.getFeatureType().equals(SfConstants.FT_SAMPLINGCURVE)
                    || sampFeat.getFeatureType().equals(SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_CURVE)
                    || sampFeat.getGeometry() instanceof LineString) {
                SamplingCurveDocument xbSamplingCurveDoc = SamplingCurveDocument.Factory.newInstance(getXmlOptions());
                SamplingCurveType xbSamplingCurve = xbSamplingCurveDoc.addNewSamplingCurve();
                addValuesToFeature(xbSamplingCurve, sampFeat);
                XmlObject xbGeomety = getEncodedGeometry(sampFeat.getGeometry(), absFeature.getGmlId());
                xbSamplingCurve.addNewShape().addNewCurve().set(xbGeomety);
                return xbSamplingCurveDoc;
            } else if (sampFeat.getFeatureType().equals(SfConstants.FT_SAMPLINGSURFACE)
                    || sampFeat.getFeatureType().equals(SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_SURFACE)
                    || sampFeat.getGeometry() instanceof Polygon) {
                SamplingSurfaceDocument xbSamplingSurfaceDoc =
                        SamplingSurfaceDocument.Factory.newInstance(getXmlOptions());
                SamplingSurfaceType xbSamplingSurface = xbSamplingSurfaceDoc.addNewSamplingSurface();
                addValuesToFeature(xbSamplingSurface, sampFeat);
                XmlObject xbGeomety = getEncodedGeometry(sampFeat.getGeometry(), absFeature.getGmlId());
                xbSamplingSurface.addNewShape().addNewSurface().set(xbGeomety);
                return xbSamplingSurfaceDoc;
            }
        } else if (absFeature instanceof FeatureCollection) {
            createFeatureCollection((FeatureCollection) absFeature);
        }
        throw new UnsupportedEncoderInputException(this, absFeature);
    }

    private XmlObject getEncodedGeometry(Geometry geometry, String gmlId) throws EncodingException {
        Encoder<XmlObject, Geometry> encoder =
                getEncoderRepository().getEncoder(CodingHelper.getEncoderKey(GmlConstants.NS_GML, geometry));
        if (encoder != null) {
            return encoder.encode(geometry, EncodingContext.empty().with(SosHelperValues.GMLID, gmlId));
        } else {
            throw new EncodingException("Error while encoding geometry for feature, needed encoder is missing!");
        }
    }

    private void addValuesToFeature(SamplingFeatureType xbSamplingFeature, SamplingFeature sampFeat)
            throws EncodingException {
        xbSamplingFeature.setId(sampFeat.getGmlId());
        if (sampFeat.isSetIdentifier()) {
            xbSamplingFeature.addNewName()
                    .set(encodeObjectToXml(GmlConstants.NS_GML, sampFeat.getIdentifierCodeWithAuthority()));
        }

        if (sampFeat.isSetName()) {
            for (CodeType sosName : sampFeat.getName()) {
                xbSamplingFeature.addNewName().set(encodeObjectToXml(GmlConstants.NS_GML, sosName));
            }
        }

        // set sampledFeatures
        // TODO: CHECK
        if (sampFeat.getSampledFeatures() != null && !sampFeat.getSampledFeatures().isEmpty()) {
            sampFeat.getSampledFeatures().stream().map(sampledFeature -> {
                FeaturePropertyType sp = xbSamplingFeature.addNewSampledFeature();
                sp.setHref(sampledFeature.getIdentifier());
                return sp;
            }).filter(sp -> sampFeat.isSetName() && sampFeat.getFirstName().isSetValue())
                    .forEachOrdered(sp -> sp.setTitle(sampFeat.getFirstName().getValue()));
        } else {
            xbSamplingFeature.addNewSampledFeature().setHref(GmlConstants.NIL_UNKNOWN);
        }
    }

    private XmlObject createFeatureCollection(FeatureCollection sosFeatureCollection) throws EncodingException {
        SamplingFeatureCollectionDocument xbSampFeatCollDoc =
                SamplingFeatureCollectionDocument.Factory.newInstance(getXmlOptions());
        SamplingFeatureCollectionType xbSampFeatColl = xbSampFeatCollDoc.addNewSamplingFeatureCollection();
        xbSampFeatColl.setId("sfc_" + Long.toString(new DateTime().getMillis()));
        for (AbstractFeature sosAbstractFeature : sosFeatureCollection.getMembers().values()) {
            SamplingFeaturePropertyType xbFeatMember = xbSampFeatColl.addNewMember();
            xbFeatMember.set(createFeature(sosAbstractFeature));
        }
        return xbSampFeatCollDoc;
    }
}
