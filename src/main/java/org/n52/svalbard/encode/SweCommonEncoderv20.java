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

import static java.util.stream.Collectors.joining;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.opengis.swe.x20.AbstractDataComponentType;
import net.opengis.swe.x20.AbstractEncodingDocument;
import net.opengis.swe.x20.AbstractEncodingType;
import net.opengis.swe.x20.BooleanType;
import net.opengis.swe.x20.CategoryType;
import net.opengis.swe.x20.CountType;
import net.opengis.swe.x20.DataArrayDocument;
import net.opengis.swe.x20.DataArrayPropertyType;
import net.opengis.swe.x20.DataArrayType;
import net.opengis.swe.x20.DataArrayType.Encoding;
import net.opengis.swe.x20.DataRecordDocument;
import net.opengis.swe.x20.DataRecordPropertyType;
import net.opengis.swe.x20.DataRecordType;
import net.opengis.swe.x20.DataRecordType.Field;
import net.opengis.swe.x20.QuantityRangeType;
import net.opengis.swe.x20.QuantityType;
import net.opengis.swe.x20.Reference;
import net.opengis.swe.x20.TextEncodingDocument;
import net.opengis.swe.x20.TextEncodingType;
import net.opengis.swe.x20.TextType;
import net.opengis.swe.x20.TimeRangeType;
import net.opengis.swe.x20.TimeType;
import net.opengis.swe.x20.UnitReference;
import net.opengis.swe.x20.VectorType;
import net.opengis.swe.x20.VectorType.Coordinate;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.janmayen.NcName;
import org.n52.shetland.ogc.OGCConstants;
import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.swe.SweAbstractDataComponent;
import org.n52.shetland.ogc.swe.SweConstants;
import org.n52.shetland.ogc.swe.SweCoordinate;
import org.n52.shetland.ogc.swe.SweDataArray;
import org.n52.shetland.ogc.swe.SweDataRecord;
import org.n52.shetland.ogc.swe.SweField;
import org.n52.shetland.ogc.swe.SweVector;
import org.n52.shetland.ogc.swe.encoding.SweAbstractEncoding;
import org.n52.shetland.ogc.swe.encoding.SweTextEncoding;
import org.n52.shetland.ogc.swe.simpleType.SweAbstractSimpleType;
import org.n52.shetland.ogc.swe.simpleType.SweBoolean;
import org.n52.shetland.ogc.swe.simpleType.SweCategory;
import org.n52.shetland.ogc.swe.simpleType.SweCount;
import org.n52.shetland.ogc.swe.simpleType.SweObservableProperty;
import org.n52.shetland.ogc.swe.simpleType.SweQuantity;
import org.n52.shetland.ogc.swe.simpleType.SweQuantityRange;
import org.n52.shetland.ogc.swe.simpleType.SweText;
import org.n52.shetland.ogc.swe.simpleType.SweTime;
import org.n52.shetland.ogc.swe.simpleType.SweTimeRange;
import org.n52.shetland.ogc.swes.SwesConstants;
import org.n52.shetland.w3c.SchemaLocation;
import org.n52.svalbard.ConformanceClass;
import org.n52.svalbard.ConformanceClasses;
import org.n52.svalbard.SosHelperValues;
import org.n52.svalbard.XmlBeansEncodingFlags;
import org.n52.svalbard.encode.exception.EncodingException;
import org.n52.svalbard.encode.exception.NotYetSupportedEncodingException;
import org.n52.svalbard.encode.exception.UnsupportedEncoderInputException;
import org.n52.svalbard.util.CodingHelper;
import org.n52.svalbard.util.XmlHelper;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class SweCommonEncoderv20 extends AbstractXmlEncoder<XmlObject, Object> implements ConformanceClass {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweCommonEncoderv20.class);

    private static final Set<EncoderKey> ENCODER_KEYS = CodingHelper.encoderKeysForElements(SweConstants.NS_SWE_20,
            SweCoordinate.class, SweAbstractSimpleType.class, SweAbstractEncoding.class,
            SweAbstractDataComponent.class, SweDataArray.class);

    private static final Set<String> CONFORMANCE_CLASSES = Sets.newHashSet(ConformanceClasses.SWE_V2_CORE,
            ConformanceClasses.SWE_V2_UML_SIMPLE_COMPONENTS, ConformanceClasses.SWE_V2_UML_RECORD_COMPONENTS,
            ConformanceClasses.SWE_V2_UML_BLOCK_ENCODINGS, ConformanceClasses.SWE_V2_UML_SIMPLE_ENCODINGS,
            ConformanceClasses.SWE_V2_XSD_SIMPLE_COMPONENTS, ConformanceClasses.SWE_V2_XSD_RECORD_COMPONENTS,
            ConformanceClasses.SWE_V2_XSD_BLOCK_COMPONENTS, ConformanceClasses.SWE_V2_XSD_SIMPLE_ENCODINGS,
            ConformanceClasses.SWE_V2_GENERAL_ENCODING_RULES, ConformanceClasses.SWE_V2_TEXT_ENCODING_RULES);

    public SweCommonEncoderv20() {
        LOGGER.debug("Encoder for the following keys initialized successfully: {}!",
                Joiner.on(", ").join(ENCODER_KEYS));
    }

    @Override
    public Set<EncoderKey> getKeys() {
        return Collections.unmodifiableSet(ENCODER_KEYS);
    }

    @Override
    public Set<String> getConformanceClasses(String service, String version) {
        if (SosConstants.SOS.equals(service) && Sos2Constants.SERVICEVERSION.equals(version)) {
            return Collections.unmodifiableSet(CONFORMANCE_CLASSES);
        }
        return Collections.emptySet();
    }

    @Override
    public void addNamespacePrefixToMap(final Map<String, String> nameSpacePrefixMap) {
        nameSpacePrefixMap.put(SweConstants.NS_SWE_20, SweConstants.NS_SWE_PREFIX);
    }

    @Override
    public Set<SchemaLocation> getSchemaLocations() {
        return Sets.newHashSet(SwesConstants.SWES_20_SCHEMA_LOCATION);
    }

    @Override
    public XmlObject encode(final Object sosSweType, EncodingContext additionalValues) throws EncodingException {
        XmlObject encodedObject = null;
        if (sosSweType instanceof SweCoordinate) {
            encodedObject = createCoordinate((SweCoordinate) sosSweType);
        } else if (sosSweType instanceof SweAbstractEncoding) {
            encodedObject = createAbstractEncoding((SweAbstractEncoding) sosSweType);
            if (additionalValues.has(XmlBeansEncodingFlags.DOCUMENT)) {
                if (encodedObject instanceof TextEncodingType) {
                    final TextEncodingDocument textEncodingDoc =
                            TextEncodingDocument.Factory.newInstance(getXmlOptions());
                    textEncodingDoc.setTextEncoding((TextEncodingType) encodedObject);
                    encodedObject = textEncodingDoc;
                } else {
                    final AbstractEncodingDocument abstractEncodingDoc =
                            AbstractEncodingDocument.Factory.newInstance(getXmlOptions());
                    abstractEncodingDoc.setAbstractEncoding((AbstractEncodingType) encodedObject);
                    return abstractEncodingDoc;
                }
            }
        } else if (sosSweType instanceof SweAbstractDataComponent) {
            encodedObject = createAbstractDataComponent((SweAbstractDataComponent) sosSweType, additionalValues);
        } else if (sosSweType instanceof SweDataArray) {
            final DataArrayType dataArrayType = createDataArray((SweDataArray) sosSweType);
            if (additionalValues.has(SosHelperValues.FOR_OBSERVATION)) {
                final DataArrayPropertyType dataArrayProperty =
                        DataArrayPropertyType.Factory.newInstance(getXmlOptions());
                dataArrayProperty.setDataArray1(dataArrayType);
            }
            encodedObject = dataArrayType;
        } else {
            throw new UnsupportedEncoderInputException(this, sosSweType);
        }
        return XmlHelper.validateDocument(encodedObject, EncodingException::new);
    }

    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    private XmlObject createAbstractDataComponent(SweAbstractDataComponent sosSweAbstractDataComponent,
            EncodingContext additionalValues) throws EncodingException {
        if (sosSweAbstractDataComponent == null) {
            throw new UnsupportedEncoderInputException(this, sosSweAbstractDataComponent);
        }
        AbstractDataComponentType abstractDataComponentType = null;
        if (sosSweAbstractDataComponent instanceof SweAbstractSimpleType) {
            abstractDataComponentType = createSimpleType((SweAbstractSimpleType) sosSweAbstractDataComponent);
        } else if (sosSweAbstractDataComponent instanceof SweVector) {
            abstractDataComponentType = createVector((SweVector) sosSweAbstractDataComponent);
        } else if (sosSweAbstractDataComponent instanceof SweDataRecord) {
            abstractDataComponentType = createDataRecord((SweDataRecord) sosSweAbstractDataComponent);
        } else if (sosSweAbstractDataComponent instanceof SweDataArray) {
            abstractDataComponentType = createDataArray((SweDataArray) sosSweAbstractDataComponent);
        } else if ((sosSweAbstractDataComponent.getXml() != null) && !sosSweAbstractDataComponent.getXml().isEmpty()) {
            try {
                return XmlObject.Factory.parse(sosSweAbstractDataComponent.getXml());
            } catch (final XmlException ex) {
                throw new EncodingException(ex, "Error while decoding %s:\n%s", new Object[] {
                        SweAbstractDataComponent.class.getName(), sosSweAbstractDataComponent.getXml() });
            }
        } else {
            throw new NotYetSupportedEncodingException(SweAbstractDataComponent.class.getName(),
                    sosSweAbstractDataComponent);
        }
        // add AbstractDataComponentType information
        if (abstractDataComponentType != null) {
            if (sosSweAbstractDataComponent.isSetDefinition()) {
                abstractDataComponentType.setDefinition(sosSweAbstractDataComponent.getDefinition());
            }
            if (sosSweAbstractDataComponent.isSetDescription()) {
                abstractDataComponentType.setDescription(sosSweAbstractDataComponent.getDescription());
            }
            if (sosSweAbstractDataComponent.isSetIdentifier()) {
                abstractDataComponentType.setIdentifier(sosSweAbstractDataComponent.getIdentifier());
            }
            if (sosSweAbstractDataComponent.isSetLabel()) {
                abstractDataComponentType.setLabel(sosSweAbstractDataComponent.getLabel());
            }
        }
        if ((abstractDataComponentType instanceof DataArrayType)
                && additionalValues.has(SosHelperValues.FOR_OBSERVATION)) {
            final DataArrayPropertyType dataArrayProperty = DataArrayPropertyType.Factory.newInstance(getXmlOptions());
            dataArrayProperty.setDataArray1((DataArrayType) abstractDataComponentType);
            return dataArrayProperty;
        }
        if ((abstractDataComponentType instanceof DataRecordType)) {
            if (additionalValues.has(SosHelperValues.FOR_OBSERVATION)) {
                final DataRecordPropertyType dataRecordProperty =
                        DataRecordPropertyType.Factory.newInstance(getXmlOptions());
                dataRecordProperty.setDataRecord((DataRecordType) abstractDataComponentType);
                return dataRecordProperty;
            }
            if (additionalValues.has(XmlBeansEncodingFlags.DOCUMENT)) {
                final DataRecordDocument dataRecordDoc = DataRecordDocument.Factory.newInstance(getXmlOptions());
                dataRecordDoc.setDataRecord((DataRecordType) abstractDataComponentType);
                return dataRecordDoc;
            }

        }
        return abstractDataComponentType;
    }

    private DataRecordType createDataRecord(final SweDataRecord sosDataRecord) throws EncodingException {
        final List<SweField> sosFields = sosDataRecord.getFields();
        final DataRecordType xbDataRecord = DataRecordType.Factory.newInstance(getXmlOptions());
        if (sosFields != null) {
            final ArrayList<Field> xbFields = new ArrayList<>(sosFields.size());
            for (final SweField sosSweField : sosFields) {
                if (sosSweField != null) {
                    final Field xbField = createField(sosSweField);
                    xbFields.add(xbField);
                } else {
                    LOGGER.error("sosSweField is null is sosDataRecord");
                }
            }
            xbDataRecord.setFieldArray(xbFields.toArray(new Field[xbFields.size()]));
        } else {
            LOGGER.error("sosDataRecord contained no fields");
        }
        return xbDataRecord;
    }

    private DataArrayType createDataArray(final SweDataArray sosDataArray) throws EncodingException {
        if (sosDataArray != null) {
            if (sosDataArray.isSetXml()) {
                try {
                    XmlObject parse = XmlObject.Factory.parse(sosDataArray.getXml());
                    if (parse instanceof DataArrayType) {
                        return (DataArrayType) parse;
                    } else if (parse instanceof DataArrayDocument) {
                        return ((DataArrayDocument) parse).getDataArray1();
                    }
                } catch (XmlException e) {
                    LOGGER.warn("Error while parsing XML representation of DataArray^", e);
                }
            }

            final DataArrayType xbDataArray = DataArrayType.Factory.newInstance(getXmlOptions());
            if (sosDataArray.isSetElementCount()) {
                xbDataArray.addNewElementCount().setCount(createCount(sosDataArray.getElementCount()));
            } else {
                xbDataArray.addNewElementCount().addNewCount();
            }
            if (sosDataArray.isSetElementTyp()) {
                final net.opengis.swe.x20.DataArrayType.ElementType elementType = xbDataArray.addNewElementType();
                if (sosDataArray.getElementType().isSetDefinition()) {
                    elementType.setName(sosDataArray.getElementType().getDefinition());
                } else {
                    elementType.setName("Components");
                }

                elementType.addNewAbstractDataComponent()
                        .set(createDataRecord((SweDataRecord) sosDataArray.getElementType()));
                elementType.getAbstractDataComponent().substitute(SweConstants.QN_DATA_RECORD_SWE_200,
                        DataRecordType.type);
            }
            if (sosDataArray.isSetEncoding()) {
                Encoding xbEncoding = xbDataArray.addNewEncoding();
                xbEncoding.setAbstractEncoding(createAbstractEncoding(sosDataArray.getEncoding()));
                xbEncoding.getAbstractEncoding().substitute(SweConstants.QN_TEXT_ENCODING_SWE_200,
                        TextEncodingType.type);
            }
            if (sosDataArray.isSetValues()) {
                xbDataArray.addNewValues().set(createValues(sosDataArray.getValues(), sosDataArray.getEncoding()));
            }
            return xbDataArray;
        }
        return null;
    }

    private XmlString createValues(final List<List<String>> values, final SweAbstractEncoding encoding) {
        // TODO How to deal with the decimal separator - is it an issue here?
        final SweTextEncoding textEncoding = (SweTextEncoding) encoding;

        String valueString = values.stream().map(block -> String.join(textEncoding.getTokenSeparator(), block))
                .collect(joining(textEncoding.getBlockSeparator()));
        // create XB result object
        final XmlString xbValueString = XmlString.Factory.newInstance(getXmlOptions());
        xbValueString.setStringValue(valueString);
        return xbValueString;
    }

    private DataRecordType.Field createField(final SweField sweField) throws EncodingException {
        final SweAbstractDataComponent sosElement = sweField.getElement();
        LOGGER.trace("sweField: {}, sosElement: {}", sweField, sosElement);
        final DataRecordType.Field xbField = DataRecordType.Field.Factory.newInstance(getXmlOptions());
        if (sweField.isSetName()) {
            xbField.setName(NcName.makeValid(sweField.getName().getValue()));
        }

        final XmlObject encodeObjectToXml = createAbstractDataComponent(sosElement, EncodingContext.empty());
        XmlObject substituteElement =
                XmlHelper.substituteElement(xbField.addNewAbstractDataComponent(), encodeObjectToXml);
        substituteElement.set(encodeObjectToXml);
        return xbField;
    }

    /*
     *
     * SIMPLE TYPES
     */
    private AbstractDataComponentType createSimpleType(final SweAbstractSimpleType<?> sosSimpleType)
            throws EncodingException {
        if (sosSimpleType instanceof SweBoolean) {
            return createBoolean((SweBoolean) sosSimpleType);
        } else if (sosSimpleType instanceof SweCategory) {
            return createCategory((SweCategory) sosSimpleType);
        } else if (sosSimpleType instanceof SweCount) {
            return createCount((SweCount) sosSimpleType);
        } else if (sosSimpleType instanceof SweObservableProperty) {
            return createObservableProperty((SweObservableProperty) sosSimpleType);
        } else if (sosSimpleType instanceof SweQuantity) {
            return createQuantity((SweQuantity) sosSimpleType);
        } else if (sosSimpleType instanceof SweQuantityRange) {
            return createQuantityRange((SweQuantityRange) sosSimpleType);
        } else if (sosSimpleType instanceof SweText) {
            return createText((SweText) sosSimpleType);
        } else if (sosSimpleType instanceof SweTimeRange) {
            return createTimeRange((SweTimeRange) sosSimpleType);
        } else if (sosSimpleType instanceof SweTime) {
            return createTime((SweTime) sosSimpleType);
        }
        throw new NotYetSupportedEncodingException(SweAbstractSimpleType.class.getSimpleName(), sosSimpleType);
    }

    private BooleanType createBoolean(final SweBoolean sosElement) {
        final BooleanType xbBoolean = BooleanType.Factory.newInstance(getXmlOptions());
        if (sosElement.isSetValue()) {
            xbBoolean.setValue(sosElement.getValue());
        }
        return xbBoolean;
    }

    private CategoryType createCategory(final SweCategory sosCategory) {
        final CategoryType xbCategory = CategoryType.Factory.newInstance(getXmlOptions());
        if (sosCategory.getCodeSpace() != null) {
            final Reference xbCodespace = xbCategory.addNewCodeSpace();
            xbCodespace.setHref(sosCategory.getCodeSpace());
        }
        if (sosCategory.isSetValue()) {
            xbCategory.setValue(sosCategory.getValue());
        }
        return xbCategory;
    }

    private CountType createCount(final SweCount sosCount) {
        final CountType xbCount = CountType.Factory.newInstance(getXmlOptions());
        if (sosCount.isSetValue()) {
            final BigInteger bigInt = new BigInteger(Integer.toString(sosCount.getValue()));
            xbCount.setValue(bigInt);
        }
        return xbCount;
    }

    private AbstractDataComponentType createObservableProperty(
            final SweObservableProperty sosSweAbstractDataComponent) {
        throw new RuntimeException("NOT YET IMPLEMENTED: encoding of swe:ObservableProperty");
    }

    protected QuantityType createQuantity(final SweQuantity quantity) {
        final QuantityType xbQuantity = QuantityType.Factory.newInstance(getXmlOptions());
        if (quantity.isSetAxisID()) {
            xbQuantity.setAxisID(quantity.getAxisID());
        }
        if (quantity.isSetValue()) {
            xbQuantity.setValue(quantity.getValue());
        }
        if (quantity.isSetUom()) {
            xbQuantity.setUom(createUnitReference(quantity.getUom()));
        } else {
            xbQuantity.setUom(createUnknownUnitReference());
        }
        if (quantity.getQuality() != null) {
            // TODO implement
            logWarnQualityNotSupported(xbQuantity.schemaType());
        }
        return xbQuantity;
    }

    protected QuantityRangeType createQuantityRange(final SweQuantityRange quantityRange) {
        final QuantityRangeType xbQuantityRange = QuantityRangeType.Factory.newInstance(getXmlOptions());
        if (quantityRange.isSetAxisID()) {
            xbQuantityRange.setAxisID(quantityRange.getAxisID());
        }
        if (quantityRange.isSetValue()) {
            xbQuantityRange.setValue(quantityRange.getValue().getRangeAsList());
        }
        if (quantityRange.isSetUom()) {
            xbQuantityRange.setUom(createUnitReference(quantityRange.getUom()));
        } else {
            xbQuantityRange.setUom(createUnknownUnitReference());
        }
        if (quantityRange.isSetQuality()) {
            // TODO implement
            logWarnQualityNotSupported(xbQuantityRange.schemaType());
        }
        return xbQuantityRange;
    }

    private TextType createText(final SweText text) {
        final TextType xbText = TextType.Factory.newInstance(getXmlOptions());
        if (text.isSetValue()) {
            xbText.setValue(text.getValue());
        }
        return xbText;
    }

    private TimeType createTime(final SweTime sosTime) {
        final TimeType xbTime = TimeType.Factory.newInstance(getXmlOptions());
        if (sosTime.isSetValue()) {
            xbTime.setValue(sosTime.getValue());
        }
        if (sosTime.isSetUom()) {
            xbTime.setUom(createUnitReference(sosTime.getUom()));
        }
        if (sosTime.getQuality() != null) {
            // TODO implement
            logWarnQualityNotSupported(xbTime.schemaType());
        }
        return xbTime;
    }

    private TimeRangeType createTimeRange(final SweTimeRange sosTimeRange) {
        final TimeRangeType xbTimeRange = TimeRangeType.Factory.newInstance(getXmlOptions());
        if (sosTimeRange.isSetUom()) {
            xbTimeRange.addNewUom().setHref(sosTimeRange.getUom());
        }
        if (sosTimeRange.isSetValue()) {
            xbTimeRange.setValue(sosTimeRange.getValue().getRangeAsStringList());
        }
        if (sosTimeRange.isSetQuality()) {
            // TODO implement
            logWarnQualityNotSupported(xbTimeRange.schemaType());
        }
        return xbTimeRange;
    }

    private VectorType createVector(SweVector sweVector) throws EncodingException {
        final VectorType xbVector = VectorType.Factory.newInstance(getXmlOptions());
        if (sweVector.isSetReferenceFrame()) {
            xbVector.setReferenceFrame(sweVector.getReferenceFrame());
        }
        if (sweVector.isSetLocalFrame()) {
            xbVector.setLocalFrame(sweVector.getLocalFrame());
        }
        if (sweVector.isSetCoordinates()) {
            for (SweCoordinate<?> coordinate : sweVector.getCoordinates()) {
                xbVector.addNewCoordinate().set(createCoordinate(coordinate));
            }
        }
        return xbVector;
    }

    private Coordinate createCoordinate(final SweCoordinate<?> coordinate) throws EncodingException {
        final Coordinate xbCoordinate = Coordinate.Factory.newInstance(getXmlOptions());
        xbCoordinate.setName(coordinate.getName());
        xbCoordinate
                .setQuantity((QuantityType) createAbstractDataComponent((SweQuantity) coordinate.getValue(), null));
        return xbCoordinate;
    }

    private AbstractEncodingType createAbstractEncoding(SweAbstractEncoding sosSweAbstractEncoding)
            throws EncodingException {
        if (sosSweAbstractEncoding instanceof SweTextEncoding) {
            return createTextEncoding((SweTextEncoding) sosSweAbstractEncoding);
        }

        try {
            if ((sosSweAbstractEncoding.getXml() != null) && !sosSweAbstractEncoding.getXml().isEmpty()) {
                XmlObject xmlObject = XmlObject.Factory.parse(sosSweAbstractEncoding.getXml());
                if (xmlObject instanceof AbstractEncodingType) {
                    return (AbstractEncodingType) xmlObject;
                }
            }
            throw new EncodingException("AbstractEncoding can not be encoded!");
        } catch (XmlException e) {
            throw new EncodingException("Error while encoding AbstractEncoding!", e);
        }
    }

    private TextEncodingType createTextEncoding(final SweTextEncoding sosTextEncoding) {
        final TextEncodingType xbTextEncoding = TextEncodingType.Factory.newInstance(getXmlOptions());
        if (sosTextEncoding.getBlockSeparator() != null) {
            xbTextEncoding.setBlockSeparator(sosTextEncoding.getBlockSeparator());
        }
        if (sosTextEncoding.isSetCollapseWhiteSpaces()) {
            xbTextEncoding.setCollapseWhiteSpaces(sosTextEncoding.isCollapseWhiteSpaces());
        }
        if (sosTextEncoding.getDecimalSeparator() != null) {
            xbTextEncoding.setDecimalSeparator(sosTextEncoding.getDecimalSeparator());
        }
        if (sosTextEncoding.getTokenSeparator() != null) {
            xbTextEncoding.setTokenSeparator(sosTextEncoding.getTokenSeparator());
        }
        return xbTextEncoding;
    }

    private UnitReference createUnitReference(final String uom) {
        final UnitReference unitReference = UnitReference.Factory.newInstance(getXmlOptions());
        if (uom.startsWith("urn:") || uom.startsWith("http://")) {
            unitReference.setHref(uom);
        } else {
            unitReference.setCode(uom);
        }
        return unitReference;
    }

    private UnitReference createUnknownUnitReference() {
        final UnitReference unitReference = UnitReference.Factory.newInstance(getXmlOptions());
        unitReference.setHref(OGCConstants.UNKNOWN);
        return unitReference;
    }

    private void logWarnQualityNotSupported(SchemaType schemaType) {
        LOGGER.warn("Quality encoding is not supported for {}", schemaType);
    }
}
