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

import static org.n52.shetland.util.CollectionHelper.union;
import static org.n52.svalbard.util.CodingHelper.encoderKeysForElements;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import net.opengis.gml.x32.BaseUnitType;
import net.opengis.gml.x32.CodeType;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.isotc211.x2005.gco.CharacterStringPropertyType;
import org.isotc211.x2005.gco.CodeListValueType;
import org.isotc211.x2005.gco.UnitOfMeasurePropertyType;
import org.isotc211.x2005.gmd.CIAddressType;
import org.isotc211.x2005.gmd.CICitationType;
import org.isotc211.x2005.gmd.CIContactType;
import org.isotc211.x2005.gmd.CIDateType;
import org.isotc211.x2005.gmd.CIResponsiblePartyDocument;
import org.isotc211.x2005.gmd.CIResponsiblePartyPropertyType;
import org.isotc211.x2005.gmd.CIResponsiblePartyType;
import org.isotc211.x2005.gmd.CIRoleCodePropertyType;
import org.isotc211.x2005.gmd.CITelephoneType;
import org.isotc211.x2005.gmd.DQConformanceResultType;
import org.isotc211.x2005.gmd.DQDomainConsistencyDocument;
import org.isotc211.x2005.gmd.DQDomainConsistencyPropertyType;
import org.isotc211.x2005.gmd.DQDomainConsistencyType;
import org.isotc211.x2005.gmd.DQQuantitativeResultType;
import org.isotc211.x2005.gmd.DQResultPropertyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.shetland.iso.GcoConstants;
import org.n52.shetland.iso.gmd.GmdCitationDate;
import org.n52.shetland.iso.gmd.GmdConformanceResult;
import org.n52.shetland.iso.gmd.GmdConstants;
import org.n52.shetland.iso.gmd.GmdDateType;
import org.n52.shetland.iso.gmd.GmdDomainConsistency;
import org.n52.shetland.iso.gmd.GmdQuantitativeResult;
import org.n52.shetland.iso.gmd.GmlBaseUnit;
import org.n52.shetland.ogc.SupportedType;
import org.n52.shetland.ogc.gml.GmlConstants;
import org.n52.shetland.ogc.sensorML.Role;
import org.n52.shetland.ogc.sensorML.SmlResponsibleParty;
import org.n52.shetland.w3c.SchemaLocation;
import org.n52.svalbard.XmlBeansEncodingFlags;
import org.n52.svalbard.encode.exception.EncodingException;
import org.n52.svalbard.encode.exception.UnsupportedEncoderInputException;
import org.n52.svalbard.util.XmlHelper;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * {@link AbstractXmlEncoder} class to decode ISO TC211 Geographic MetaData
 * (GMD) extensible markup language.
 *
 * @author <a href="mailto:c.hollmann@52north.org">Carsten Hollmann</a>
 * @since 4.2.0
 *
 */
public class Iso19139GmdEncoder extends AbstractXmlEncoder<XmlObject, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iso19139GmdEncoder.class);

    private static final QName QN_GCO_DATE = new QName(GcoConstants.NS_GCO, "Date", GcoConstants.NS_GCO_PREFIX);

    private static final QName QN_GMD_CONFORMANCE_RESULT =
            new QName(GmdConstants.NS_GMD, "DQ_ConformanceResult", GmdConstants.NS_GMD_PREFIX);

    private static final QName QN_GMD_QUANTITATIVE_RESULT =
            new QName(GmdConstants.NS_GMD, "DQ_QuantitativeResult", GmdConstants.NS_GMD_PREFIX);

    private static final QName QN_GML_BASE_UNIT =
            new QName(GmlConstants.NS_GML_32, "BaseUnit", GmlConstants.NS_GML_PREFIX);

    private static final Set<EncoderKey> ENCODER_KEYS = union(
            encoderKeysForElements(GmdConstants.NS_GMD, SmlResponsibleParty.class, GmdQuantitativeResult.class,
                    GmdConformanceResult.class),
            encoderKeysForElements(null, GmdQuantitativeResult.class, GmdConformanceResult.class));

    public Iso19139GmdEncoder() {
        LOGGER.debug("Encoder for the following keys initialized successfully: {}!",
                Joiner.on(", ").join(ENCODER_KEYS));
    }

    @Override
    public Set<EncoderKey> getKeys() {
        return Collections.unmodifiableSet(ENCODER_KEYS);
    }

    @Override
    public Set<SupportedType> getSupportedTypes() {
        return Collections.emptySet();
    }

    @Override
    public void addNamespacePrefixToMap(final Map<String, String> nameSpacePrefixMap) {
        nameSpacePrefixMap.put(GmdConstants.NS_GMD, GmdConstants.NS_GMD_PREFIX);
    }

    @Override
    public Set<SchemaLocation> getSchemaLocations() {
        return Sets.newHashSet(GmdConstants.GMD_SCHEMA_LOCATION);
    }

    @Override
    public XmlObject encode(Object element, EncodingContext additionalValues)
            throws EncodingException, UnsupportedEncoderInputException {
        XmlObject encodedObject = null;
        // try {
        if (element instanceof SmlResponsibleParty) {
            encodedObject = encodeResponsibleParty((SmlResponsibleParty) element, additionalValues);
        } else {
            if (element instanceof GmdDomainConsistency) {
                encodedObject = encodeGmdDomainConsistency((GmdDomainConsistency) element, additionalValues);
            } else {
                throw new UnsupportedEncoderInputException(this, element);
            }
        }
        // } catch (final XmlException xmle) {
        // throw new NoApplicableCodeException().causedBy(xmle);
        // }
        if (encodedObject != null) {
            XmlHelper.validateDocument(encodedObject, EncodingException::new);
        }
        return encodedObject;
    }

    private XmlObject encodeResponsibleParty(SmlResponsibleParty responsibleParty, EncodingContext additionalValues)
            throws EncodingException {
        if (responsibleParty.isSetHref()) {
            CIResponsiblePartyPropertyType cirppt =
                    CIResponsiblePartyPropertyType.Factory.newInstance(getXmlOptions());
            cirppt.setHref(responsibleParty.getHref());
            if (responsibleParty.isSetTitle()) {
                cirppt.setTitle(responsibleParty.getTitle());
            }
            if (responsibleParty.isSetRole()) {
                cirppt.setRole(responsibleParty.getRole());
            }
            return cirppt;
        }
        CIResponsiblePartyType cirpt = CIResponsiblePartyType.Factory.newInstance(getXmlOptions());
        if (responsibleParty.isSetIndividualName()) {
            cirpt.addNewIndividualName().setCharacterString(responsibleParty.getIndividualName());
        }
        if (responsibleParty.isSetOrganizationName()) {
            cirpt.addNewOrganisationName().setCharacterString(responsibleParty.getOrganizationName());
        }
        if (responsibleParty.isSetPositionName()) {
            cirpt.addNewPositionName().setCharacterString(responsibleParty.getPositionName());
        }
        // set contact
        encodeContact(cirpt.addNewContactInfo().addNewCIContact(), responsibleParty);
        // set role
        encodeRole(cirpt.addNewRole(), responsibleParty.getRoleObject());
        if (additionalValues.has(XmlBeansEncodingFlags.PROPERTY_TYPE)) {
            CIResponsiblePartyPropertyType cirppt =
                    CIResponsiblePartyPropertyType.Factory.newInstance(getXmlOptions());
            cirppt.setCIResponsibleParty(cirpt);
            return cirppt;
        } else if (additionalValues.has(XmlBeansEncodingFlags.DOCUMENT)) {
            CIResponsiblePartyDocument cirpd = CIResponsiblePartyDocument.Factory.newInstance(getXmlOptions());
            cirpd.setCIResponsibleParty(cirpt);
        }
        return cirpt;
    }

    private void encodeContact(CIContactType cic, SmlResponsibleParty responsibleParty) {
        if (responsibleParty.isSetAddress()) {
            encodeCiAddress(cic.addNewAddress().addNewCIAddress(), responsibleParty);
        }
        if (responsibleParty.isSetContactInstructions()) {
            cic.addNewContactInstructions().setCharacterString(responsibleParty.getContactInstructions());
        }
        if (responsibleParty.isSetHoursOfService()) {
            cic.addNewHoursOfService().setCharacterString(responsibleParty.getHoursOfService());
        }
        if (responsibleParty.isSetOnlineResources()) {
            cic.addNewOnlineResource().setHref(responsibleParty.getOnlineResources().get(0));
        }
        if (responsibleParty.isSetPhone()) {
            encodePhone(cic.addNewPhone().addNewCITelephone(), responsibleParty);
        }

    }

    private void encodeCiAddress(CIAddressType ciat, SmlResponsibleParty responsibleParty) {
        if (responsibleParty.isSetAdministrativeArea()) {
            ciat.addNewAdministrativeArea().setCharacterString(responsibleParty.getAdministrativeArea());
        }
        if (responsibleParty.isSetCity()) {
            ciat.addNewCity().setCharacterString(responsibleParty.getCity());
        }
        if (responsibleParty.isSetCountry()) {
            ciat.addNewCountry().setCharacterString(responsibleParty.getCountry());
        }
        if (responsibleParty.isSetPostalCode()) {
            ciat.addNewPostalCode().setCharacterString(responsibleParty.getPostalCode());
        }
        if (responsibleParty.isSetDeliveryPoint()) {
            ciat.setDeliveryPointArray(listToCharacterStringPropertyTypeArray(responsibleParty.getDeliveryPoint()));

        }
        if (responsibleParty.isSetEmail()) {
            ciat.setElectronicMailAddressArray(
                    listToCharacterStringPropertyTypeArray(Lists.newArrayList(responsibleParty.getEmail())));
        }
    }

    private void encodePhone(CITelephoneType citt, SmlResponsibleParty responsibleParty) {
        if (responsibleParty.isSetPhoneVoice()) {
            citt.setVoiceArray(listToCharacterStringPropertyTypeArray(responsibleParty.getPhoneVoice()));
        }
        if (responsibleParty.isSetPhoneFax()) {
            citt.setFacsimileArray(listToCharacterStringPropertyTypeArray(responsibleParty.getPhoneFax()));
        }
    }

    private void encodeRole(CIRoleCodePropertyType circpt, Role role) throws EncodingException {
        XmlObject encodeObjectToXml = encodeObjectToXml(GcoConstants.NS_GCO, role);
        if (encodeObjectToXml != null) {
            circpt.addNewCIRoleCode().set(encodeObjectToXml);
        }
    }

    private XmlObject encodeGmdDomainConsistency(GmdDomainConsistency element, EncodingContext additionalValues)
            throws EncodingException {
        if (additionalValues.has(XmlBeansEncodingFlags.DOCUMENT)) {
            DQDomainConsistencyDocument document = DQDomainConsistencyDocument.Factory.newInstance(getXmlOptions());
            DQResultPropertyType addNewResult = document.addNewDQDomainConsistency().addNewResult();
            encodeGmdDomainConsistency(addNewResult, element);
            return document;
        } else if (additionalValues.has(XmlBeansEncodingFlags.PROPERTY_TYPE)) {
            DQDomainConsistencyPropertyType propertyType =
                    DQDomainConsistencyPropertyType.Factory.newInstance(getXmlOptions());
            DQResultPropertyType addNewResult = propertyType.addNewDQDomainConsistency().addNewResult();
            encodeGmdDomainConsistency(addNewResult, element);
            return propertyType;
        } else {
            DQDomainConsistencyType type = DQDomainConsistencyType.Factory.newInstance(getXmlOptions());
            DQResultPropertyType addNewResult = type.addNewResult();
            encodeGmdDomainConsistency(addNewResult, element);
            return type;
        }
    }

    private void encodeGmdDomainConsistency(DQResultPropertyType xbResult, GmdDomainConsistency domainConsistency)
            throws EncodingException {
        if (domainConsistency instanceof GmdConformanceResult) {
            encodeGmdConformanceResult(xbResult, (GmdConformanceResult) domainConsistency);
        } else if (domainConsistency instanceof GmdQuantitativeResult) {
            encodeGmdQuantitativeResult(xbResult, (GmdQuantitativeResult) domainConsistency);
        } else {
            throw new UnsupportedEncoderInputException(this, domainConsistency);
        }
    }

    private void encodeGmdConformanceResult(DQResultPropertyType xbResult, GmdConformanceResult gmdConformanceResult) {
        DQConformanceResultType dqConformanceResultType = (DQConformanceResultType) xbResult.addNewAbstractDQResult()
                .substitute(QN_GMD_CONFORMANCE_RESULT, DQConformanceResultType.type);
        if (gmdConformanceResult.isSetPassNilReason()) {
            dqConformanceResultType.addNewPass().setNilReason(gmdConformanceResult.getPassNilReason().name());
        } else {
            dqConformanceResultType.addNewPass().setBoolean(gmdConformanceResult.isPass());
        }
        dqConformanceResultType.addNewExplanation()
                .setCharacterString(gmdConformanceResult.getSpecification().getExplanation());
        CICitationType xbCitation = dqConformanceResultType.addNewSpecification().addNewCICitation();
        xbCitation.addNewTitle().setCharacterString(gmdConformanceResult.getSpecification().getCitation().getTitle());
        CIDateType xbCiDate = xbCitation.addNewDate().addNewCIDate();
        CodeListValueType xbCIDateTypeCode = xbCiDate.addNewDateType().addNewCIDateTypeCode();
        GmdCitationDate gmdCitationDate = gmdConformanceResult.getSpecification().getCitation().getDate();
        GmdDateType gmdDateType = gmdCitationDate.getDateType();
        xbCIDateTypeCode.setCodeList(gmdDateType.getCodeList());
        xbCIDateTypeCode.setCodeListValue(gmdDateType.getCodeListValue());
        if (gmdDateType.getCodeSpace() != null && !gmdDateType.getCodeSpace().isEmpty()) {
            xbCIDateTypeCode.setCodeSpace(gmdDateType.getCodeSpace());
        }
        xbCIDateTypeCode.setStringValue(gmdDateType.getValue());
        XmlCursor newCursor = xbCiDate.addNewDate().newCursor();
        newCursor.toNextToken();
        newCursor.beginElement(QN_GCO_DATE);
        newCursor.insertChars(gmdCitationDate.getDate());
        newCursor.dispose();
    }

    private void encodeGmdQuantitativeResult(DQResultPropertyType xbResult,
            GmdQuantitativeResult gmdQuantitativeResult) {
        DQQuantitativeResultType dqQuantitativeResultType = (DQQuantitativeResultType) xbResult
                .addNewAbstractDQResult().substitute(QN_GMD_QUANTITATIVE_RESULT, DQQuantitativeResultType.type);
        GmlBaseUnit unit = gmdQuantitativeResult.getUnit();
        UnitOfMeasurePropertyType valueUnit = dqQuantitativeResultType.addNewValueUnit();
        BaseUnitType xbBaseUnit =
                (BaseUnitType) valueUnit.addNewUnitDefinition().substitute(QN_GML_BASE_UNIT, BaseUnitType.type);
        CodeType xbCatalogSymbol = xbBaseUnit.addNewCatalogSymbol();
        xbCatalogSymbol.setCodeSpace(unit.getCatalogSymbol().getCodeSpace().toString());
        xbCatalogSymbol.setStringValue(unit.getCatalogSymbol().getValue());
        xbBaseUnit.setId(unit.getId());
        xbBaseUnit.addNewUnitsSystem().setHref(unit.getUnitSystem());
        xbBaseUnit.addNewIdentifier().setCodeSpace(unit.getIdentifier());
        if (gmdQuantitativeResult.isSetValueNilReason()) {
            dqQuantitativeResultType.addNewValue().setNilReason(gmdQuantitativeResult.getValueNilReason().name());
        } else {
            XmlCursor cursor = dqQuantitativeResultType.addNewValue().addNewRecord().newCursor();
            cursor.toNextToken();
            cursor.insertChars(gmdQuantitativeResult.getValue());
            cursor.dispose();
        }
    }

    private CharacterStringPropertyType[] listToCharacterStringPropertyTypeArray(List<String> list) {
        return list.stream().map(string -> {
            CharacterStringPropertyType cspt = CharacterStringPropertyType.Factory.newInstance();
            cspt.setCharacterString(string);
            return string;
        }).toArray(CharacterStringPropertyType[]::new);
    }
}
