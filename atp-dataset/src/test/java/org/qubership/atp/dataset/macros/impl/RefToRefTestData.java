/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.dataset.macros.impl;

import java.util.function.Supplier;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;

public class RefToRefTestData {

    protected static class AbstractCase implements Supplier<VisibilityArea> {

        public final VisibilityArea va;

        public final DataSetList country;
        public final DataSet belgium;

        public final DataSetList cases;
        public final DataSet currentCase;

        public AbstractCase(CreationFacade create) {
            va = create.va("ATPII-3231");
            country = create.dsl(va, "Country");
            belgium = create.ds(country, "Belgium");
            cases = create.dsl(va, "Cases");
            currentCase = create.ds(cases, "CurrentCase");
        }

        @Override
        public VisibilityArea get() {
            return va;
        }
    }

    public static class VoiceCaseRefToRefWithOverlap implements Supplier<VisibilityArea> {

        public final VisibilityArea va;
        /**
         * Account 'B2B Account' with AccountType 'B2B'.
         */
        public final DataSetList account;
        public final DataSet b2bAcc;
        public final Parameter b2bAccType;
        /**
         * Countries are: 'Belgium - Belgium' with Zone 'Belgium' and InternationalZone 'Belgium'.
         * Lays under 'OriginCountry' in the current sms case. 'Egypt - Magreb' with Zone 'Magreb'
         * and InternationalZone 'World 2'. Lays under 'DestinationCountry' in the current sms
         * case.
         */
        public final DataSetList country;
        public final DataSet belgium;
        public final DataSet egypt;
        public final Parameter belgiumZone;
        public final Parameter egyptZone;
        public final Parameter belgiumInternationalZone;
        public final Parameter egyptInternationalZone;
        /**
         * International Voice Rate 'Pro Contact' with SMS from International Rating Zone 'Belgium'
         * and overlapped value '0.33'.
         */
        public final DataSetList internationalVoiceRates;
        public final DataSet proContactRates;
        public final Parameter voiceRatesIntoRatingZoneRef;
        public final Parameter belgiumToWorld2RatingOverlap;
        /**
         * Subscription is 'Pro Contact', with TariffName 'Pro Contact', lays under 'Subscription'
         * in the sms case.
         */
        public final DataSetList subscription;
        public final DataSet proContact;
        public final Parameter tariffName;
        /**
         * Current sms case with UsageType 'SMS', Subscription 'Pro Contact', Account 'B2B Account',
         * OriginCountry 'Belgium - Belgium' and DestinationCountry 'Egypt - Magreb'.
         */
        public final DataSetList smsCases;
        public final DataSet belToEgypt;
        public final Parameter usageType;
        public final Parameter belToEgyptIntoSubscriptionRef;
        public final Parameter belToEgyptIntoAccountRef;
        public final Parameter belToEgyptIntoOrigCountryRef;
        public final Parameter belToEgyptIntoDestCountryRef;
        public final Parameter targetParameter;
        /**
         * Roaming Zone 'Belgium' with Magreb which is not set. Lays under 'Belgium' in the Roaming
         * SMS Rate 'B2B'.
         */
        public final DataSetList roamingZones;
        public final DataSet belgiumRoamingZone;
        public final Attribute magrebRoamingZone;
        /**
         * International Rating Zone 'Belgium' with 'World 2' which is not set. Lays under 'SMS' in
         * International Voice Rate 'Pro Contact'.
         */
        public final DataSetList internationalRatingZones;
        public final DataSet belgiumRatingZone;
        public final Attribute belgiumToWorld2Rating;
        /**
         * Roaming SMS Rate 'B2B' with Roaming Zone 'Belgium' with 'Magreb' set to International
         * Voice Rate reference macro.
         */
        public final DataSetList roamingSmsRates;
        public final DataSet b2bRates;
        public final Parameter b2bRatesIntoBelgiumRoamingZoneRef;
        public final Parameter b2bBelgiumToMagrebRate;

        public VoiceCaseRefToRefWithOverlap(CreationFacade create) {
            va = create.va("ATPII-3231");
            account = create.dsl(va, "Account");
            b2bAcc = create.ds(account, "B2B Account");
            b2bAccType = create.textParam(b2bAcc, "AccountType", "B2B");
            country = create.dsl(va, "Country");
            belgium = create.ds(country, "Belgium - Belgium");
            egypt = create.ds(country, "Egypt - Magreb");
            belgiumZone = create.textParam(belgium, "Zone", "Belgium");
            egyptZone = create.textParam(egypt, belgiumZone.getAttribute(), "Magreb");
            belgiumInternationalZone = create.textParam(belgium, "IntenationalZone", "Belgium");
            egyptInternationalZone = create.textParam(egypt, belgiumInternationalZone.getAttribute(), "World 2");
            subscription = create.dsl(va, "Subscription");
            proContact = create.ds(subscription, "Pro Contact");
            tariffName = create.textParam(proContact, "TariffName", "Pro Contact");
            smsCases = create.dsl(va, "SMS cases");
            belToEgypt = create.ds(smsCases, "TLNT-SOHO-CONTACT-SMS-007 - SMS MO to Magreb number");
            usageType = create.textParam(belToEgypt, "UsageType", "SMS");
            belToEgyptIntoSubscriptionRef = create.refParam(belToEgypt, "Subscription", proContact);
            belToEgyptIntoAccountRef = create.refParam(belToEgypt, "Account", b2bAcc);
            belToEgyptIntoOrigCountryRef = create.refParam(belToEgypt, "OriginCountry", belgium);
            belToEgyptIntoDestCountryRef = create.refParam(belToEgypt, "DestinationCountry", egypt);
            roamingZones = create.dsl(va, "Roaming Zones");
            belgiumRoamingZone = create.ds(roamingZones, "Belgium");
            magrebRoamingZone = create.textAttr(roamingZones, "Magreb");
            internationalRatingZones = create.dsl(va, "International Rating Zones");
            belgiumRatingZone = create.ds(internationalRatingZones, "Belgium");
            belgiumToWorld2Rating = create.textAttr(internationalRatingZones, "World 2");
            internationalVoiceRates = create.dsl(va, "International Voice Rates");
            proContactRates = create.ds(internationalVoiceRates, "Pro Contact");
            voiceRatesIntoRatingZoneRef = create.refParam(proContactRates, "SMS", belgiumRatingZone);
            belgiumToWorld2RatingOverlap = create.overrideParam(proContactRates, belgiumToWorld2Rating,
                    "0.33", null, null, null, voiceRatesIntoRatingZoneRef.getAttribute());
            roamingSmsRates = create.dsl(va, "Roaming SMS Rates");
            b2bRates = create.ds(roamingSmsRates, "B2B");
            b2bRatesIntoBelgiumRoamingZoneRef = create.refParam(b2bRates, "Belgium", belgiumRoamingZone);
            //#REF_DSL(International Voice Rates.#REF_THIS(Subscription.TariffName).#REF_THIS(UsageType).#REF_THIS
            // (DestinationCountry.IntenationalZone)) //Bel - magreb
            String vlookupMacro = String.format("#REF_DSL(%s.#REF_THIS(%s.%s).#REF_THIS(%s).#REF_THIS(%s.%s))",
                    internationalVoiceRates.getName(),
                    belToEgyptIntoSubscriptionRef.getAttribute().getName(),
                    tariffName.getAttribute().getName(),
                    usageType.getAttribute().getName(),
                    belToEgyptIntoDestCountryRef.getAttribute().getName(),
                    egyptInternationalZone.getAttribute().getName());
            b2bBelgiumToMagrebRate = create.overrideParam(b2bRates, magrebRoamingZone, vlookupMacro, null, null,
                    null, b2bRatesIntoBelgiumRoamingZoneRef.getAttribute());
            //#REF_DSL(Roaming SMS Rates.#REF_THIS(Account.AccountType).#REF_THIS(OriginCountry.Zone).#REF_THIS
            // (DestinationCountry.Zone)) bel - egy
            String vlookupMacro2 = String.format("#REF_DSL(%s.#REF_THIS(%s.%s).#REF_THIS(%s.%s).#REF_THIS(%s.%s))",
                    roamingSmsRates.getName(),
                    belToEgyptIntoAccountRef.getAttribute().getName(),
                    b2bAccType.getAttribute().getName(),
                    belToEgyptIntoOrigCountryRef.getAttribute().getName(),
                    belgiumZone.getAttribute().getName(),
                    belToEgyptIntoDestCountryRef.getAttribute().getName(),
                    egyptZone.getAttribute().getName());
            targetParameter = create.textParam(belToEgypt, "Target", vlookupMacro2);
        }

        @Override
        public VisibilityArea get() {
            return va;
        }
    }

    protected static class ChildRefAttrIsUnreachable extends AbstractCase {

        public final Attribute capital;
        public final Parameter brussels;
        public final Parameter childRef;

        public ChildRefAttrIsUnreachable(CreationFacade create) {
            super(create);
            capital = create.textAttr(cases, "Capital");
            brussels = create.textParam(currentCase, capital, "Brussels");
            childRef = create.textParam(belgium, "ChildRef", "#REF_THIS(Capital)");
        }
    }

    public static class RefThisChildRefTargetAttrIsUnreachable extends ChildRefAttrIsUnreachable {

        public final Parameter currentCaseIntoBelgium;
        public final Parameter parentRef;

        public RefThisChildRefTargetAttrIsUnreachable(CreationFacade create) {
            super(create);
            currentCaseIntoBelgium = create.refParam(currentCase, belgium);
            parentRef = create.textParam(currentCase, "ParentRef", "#REF_THIS(Country.ChildRef)");
        }
    }

    public static class RefDslChildRefTargetAttrIsUnreachable extends ChildRefAttrIsUnreachable {

        public final Parameter parentRef;

        public RefDslChildRefTargetAttrIsUnreachable(CreationFacade create) {
            super(create);
            parentRef = create.textParam(currentCase, "ParentRef", "#REF_DSL(Country.Belgium.ChildRef)");
        }
    }

    protected static class ChildRefParamIsUninitialized extends AbstractCase {

        public final Attribute capital;
        public final Parameter childRef;

        public ChildRefParamIsUninitialized(CreationFacade create) {
            super(create);
            capital = create.textAttr(country, "Capital");
            create.textParam(currentCase, "Capital", "test");
            childRef = create.textParam(belgium, "ChildRef", "#REF_THIS(Capital)");
        }
    }

    protected static class ChildRefParamIsInitialized extends AbstractCase {

        public final Attribute capital;
        public final Parameter brussels;
        public final Parameter childRef;

        public ChildRefParamIsInitialized(CreationFacade create) {
            super(create);
            capital = create.textAttr(country, "Capital");
            brussels = create.textParam(belgium, capital, "Brussels");
            childRef = create.textParam(belgium, "ChildRef", "#REF_THIS(Capital)");
        }
    }

    public static class RefDslChildRefTargetParamHasBrackets implements Supplier<VisibilityArea> {

        public final VisibilityArea va;

        public final DataSetList country;
        public final DataSet belgium;

        public final DataSetList cases;
        public final DataSet currentCase;
        public final Attribute capital;
        public final Parameter brussels;
        public final Parameter childRef;
        public final Parameter parentRef;

        public RefDslChildRefTargetParamHasBrackets(CreationFacade create) {
            va = create.va("ATPII-3813");
            country = create.dsl(va, "Country()");
            belgium = create.ds(country, "Belgium(())");
            cases = create.dsl(va, "Cases");
            currentCase = create.ds(cases, "CurrentCase");
            capital = create.textAttr(country, "Capital()");
            brussels = create.textParam(belgium, capital, "Brussels");
            childRef = create.textParam(belgium, "ChildRef(brackets)", "#REF_THIS(Capital())");
            create.textParam(currentCase, "Capital()", "test");
            parentRef = create.textParam(currentCase, "ParentRef", "#REF_DSL(Country().Belgium(()).ChildRef(brackets)"
                    + ")");
        }

        @Override
        public VisibilityArea get() {
            return va;
        }
    }

    public static class RefDslChildRefTargetParamIsUninitialized extends ChildRefParamIsUninitialized {

        public final Parameter parentRef;

        public RefDslChildRefTargetParamIsUninitialized(CreationFacade create) {
            super(create);
            parentRef = create.textParam(currentCase, "ParentRef", "#REF_DSL(Country.Belgium.ChildRef)");
        }
    }

    public static class RefThisChildRefTargetParamIsUninitialized extends ChildRefParamIsUninitialized {

        public final Parameter currentCaseIntoBelgium;
        public final Parameter parentRef;

        public RefThisChildRefTargetParamIsUninitialized(CreationFacade create) {
            super(create);
            currentCaseIntoBelgium = create.refParam(currentCase, belgium);
            parentRef = create.textParam(currentCase, "ParentRef", "#REF_THIS(Country.ChildRef)");
        }
    }

    public static class VoiceCaseRefToDslWithOverlap implements Supplier<VisibilityArea> {

        public final VisibilityArea va;

        /**
         * Subscription is 'Pro Contact', with TariffName 'Pro Contact', lays under 'Subscription'
         * in the voice case.
         */
        public final DataSetList subscription;
        public final DataSet proContact;
        public final Parameter tariffName;
        /**
         * Country is 'USA - USA/Canada', with IntenationalZone 'USA / Canada', lays under
         * 'DestinationCountry' in the voice case.
         */
        public final DataSetList country;
        public final DataSet usa;
        public final Parameter internationalZone;
        /**
         * Current voice case with Subscription 'Pro Contact' and DestinationCountry 'USA -
         * USA/Canada' and UsageType 'Voice Mobile'.
         */
        public final DataSetList voiceCases;
        public final DataSet belToUsa;
        public final Parameter usageType;
        public final Parameter belToUsaIntoSubscriptionRef;
        public final Parameter belToUsaIntoCountryRef;
        /**
         * International Rating Zone is 'Belgium' with USA / Canada rating without value. Lays under
         * 'Voice Mobile' in the International Voice Rates. USA / Canada rating is overlapped with
         * '0.58' for 'Pro Contact' data set.
         */
        public final DataSetList internationalRatingZones;
        public final DataSet belgium;
        public final Parameter belgiumToUsaRating;
        /**
         * International Voice Rate 'Pro Contact' with 'Voice Mobile' group in which for rating zone
         * 'Belgium'->'USA / Canada' set value '0.58'.
         */
        public final DataSetList internationalVoiceRates;
        public final DataSet proContactRates;
        public final Parameter voiceRatesIntoRatingZoneRef;
        public final Parameter belgiumToUsaRatingOverlap;

        public VoiceCaseRefToDslWithOverlap(CreationFacade create) {
            va = create.va("ATPII-3231");
            subscription = create.dsl(va, "Subscription");
            proContact = create.ds(subscription, "Pro Contact");
            tariffName = create.textParam(proContact, "TariffName", "Pro Contact");
            country = create.dsl(va, "Country");
            usa = create.ds(country, "USA - USA/Canada");
            internationalZone = create.textParam(usa, "IntenationalZone", "USA / Canada");
            voiceCases = create.dsl(va, "Voice cases");
            belToUsa = create.ds(voiceCases, "TLNT-SOHO-CONTACT-VOICE-007 - Voice MO to USA/Canada number");
            usageType = create.textParam(belToUsa, "UsageType", "Voice Mobile");
            belToUsaIntoSubscriptionRef = create.refParam(belToUsa, "Subscription", proContact);
            belToUsaIntoCountryRef = create.refParam(belToUsa, "DestinationCountry", usa);
            internationalRatingZones = create.dsl(va, "International Rating Zones");
            belgium = create.ds(internationalRatingZones, "Belgium");
            belgiumToUsaRating = create.textParam(belgium, "USA / Canada", "");
            internationalVoiceRates = create.dsl(va, "International Voice Rates");
            proContactRates = create.ds(internationalVoiceRates, "Pro Contact");
            voiceRatesIntoRatingZoneRef = create.refParam(proContactRates, "Voice Mobile", belgium);
            belgiumToUsaRatingOverlap = create.overrideParam(proContactRates, belgiumToUsaRating.getAttribute(),
                    "0.58", null, null, null, voiceRatesIntoRatingZoneRef.getAttribute());
        }

        @Override
        public VisibilityArea get() {
            return va;
        }
    }

    public static class ChildRefIsOverlapped extends AbstractCase {

        public final Parameter currentCaseIntoBelgium;
        public final Attribute capital;
        public final Parameter childRef;
        public final Parameter origChildRef;
        public final Parameter brussels;
        public final Parameter parentRef;

        public ChildRefIsOverlapped(CreationFacade create) {
            super(create);
            currentCaseIntoBelgium = create.refParam(currentCase, belgium);
            capital = create.textAttr(country, "Capital");
            brussels = create.textParam(belgium, capital, "Brussels");
            origChildRef = create.textParam(belgium, "ChildRef", "#REF_THIS(Capital)");
            parentRef = create.textParam(currentCase, "ParentRef", "#REF_THIS(Country.ChildRef)");
            childRef = create.overrideParam(currentCase, origChildRef.getAttribute(),
                    "#REF_THIS(Capital)", null, null,
                    null, currentCaseIntoBelgium.getAttribute());
        }
    }

    public static class RefTargetParamIsOverlapped extends AbstractCase {

        public final Parameter currentCaseIntoBelgium;
        public final Attribute capital;
        public final Parameter testCapital;
        public final Parameter childRef;
        public final Parameter brussels;

        public RefTargetParamIsOverlapped(CreationFacade create) {
            super(create);
            currentCaseIntoBelgium = create.refParam(currentCase, belgium);
            capital = create.textAttr(country, "Capital");
            testCapital = create.textParam(belgium, capital, "test");
            childRef = create.textParam(belgium, "ChildRef", "#REF_THIS(Capital)");
            brussels = create.overrideParam(currentCase, capital, "Brussels", null, null, null,
                    currentCaseIntoBelgium.getAttribute());
        }
    }

    public static class ChildRefTargetParamIsOverlapped extends RefTargetParamIsOverlapped {

        public final Parameter parentRef;

        public ChildRefTargetParamIsOverlapped(CreationFacade create) {
            super(create);
            parentRef = create.textParam(currentCase, "ParentRef", "#REF_THIS(Country.ChildRef)");
        }
    }

    public static class RefThisChildRefTargetParamIsInitialized extends ChildRefParamIsInitialized {

        public final Parameter currentCaseIntoBelgium;
        public final Parameter parentRef;

        public RefThisChildRefTargetParamIsInitialized(CreationFacade create) {
            super(create);
            currentCaseIntoBelgium = create.refParam(currentCase, belgium);
            parentRef = create.textParam(currentCase, "ParentRef", "#REF_THIS(Country.ChildRef)");
        }
    }

    public static class RefDslChildRefTargetParamIsInitialized extends ChildRefParamIsInitialized {

        public final Parameter parentRef;

        public RefDslChildRefTargetParamIsInitialized(CreationFacade create) {
            super(create);
            create.textParam(currentCase, "Capital", "test");
            parentRef = create.textParam(currentCase, "ParentRef", "#REF_DSL(Country.Belgium.ChildRef)");
        }
    }

    public static class TwoRefsToTheSameRandom implements Supplier<VisibilityArea> {

        public final VisibilityArea va;
        public final DataSetList dsl;
        public final DataSet ds;
        public final DataSetList paramsDsl;
        public final DataSet paramsDs;
        public final String macro = "IP_AccessServiceOrderItemSpec_#REF_DSL(Params.DEFAULT_SMF_IqSP.CRM)";
        public final Parameter dsIntoParams;
        public final Parameter crm;
        public final Parameter keyB;
        public final Parameter keyA;

        public TwoRefsToTheSameRandom(CreationFacade create, AliasWrapperService wrapperService) {
            va = create.va("ATPII-4102");
            dsl = create.dsl(va, "SMF IqSP");
            ds = create.ds(dsl, "Automatic PzT WS (SDSL, 3, Montage_mit_Kundenanwesenheit)");
            paramsDsl = create.dsl(va, "Params");
            paramsDs = create.ds(paramsDsl, "DEFAULT_SMF_IqSP");
            dsIntoParams = create.refParam(ds, paramsDs);
            crm = create.textParam(paramsDs, "CRM", "#RANDOMBETWEEN(30000000, 39999999)");
            String wrappedMacro = wrapperService.wrapToAlias(macro, va, dsl);
            keyA = create.textParam(paramsDs, "keyA", wrappedMacro);
            keyB = create.textParam(paramsDs, "keyB", wrappedMacro);
        }

        @Override
        public VisibilityArea get() {
            return va;
        }
    }
}
