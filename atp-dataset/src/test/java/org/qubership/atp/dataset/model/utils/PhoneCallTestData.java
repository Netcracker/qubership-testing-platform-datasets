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

package org.qubership.atp.dataset.model.utils;

import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;

public class PhoneCallTestData implements Supplier<VisibilityArea> {
    public final VisibilityArea va;
    public final DataSetList voiceCall;
    public final DataSet defaultVoiceCall;
    public final Parameter voiceCallIntoOriginCountryRef;
    public final Parameter voiceCallIntoDestinationCountryRef;
    public final DataSetList country;
    public final DataSet originCountry;
    public final DataSet destinationCountry;
    public final Attribute countryZone;
    public final Parameter originCountryZone;
    public final Parameter destinationCountryZone;
    public final DataSetList internationalRateCost;
    public final DataSet originCountryRate;
    public final Attribute targetCountryRate;
    public final Parameter rateCost;

    public PhoneCallTestData(@Nonnull CreationFacade create) {
        va = create.va("ATPII-3164");
        voiceCall = create.dsl(va, "Voice call");
        defaultVoiceCall = create.ds(voiceCall, "Default voice call");
        country = create.dsl(va, "Country");
        originCountry = create.ds(country, "France - Zone EU");
        originCountryZone = create.textParam(originCountry, "Zone", "Zone EU");
        countryZone = originCountryZone.getAttribute();
        destinationCountry = create.ds(country, "Belgium - Belgium");
        destinationCountryZone = create.textParam(destinationCountry, countryZone, "Belgium");
        voiceCallIntoOriginCountryRef = create.refParam(defaultVoiceCall, "OriginCountry", originCountry);
        voiceCallIntoDestinationCountryRef = create.refParam(defaultVoiceCall, "DestinationCountry", destinationCountry);
        internationalRateCost = create.dsl(va, "InternationalRateCost");
        originCountryRate = create.ds(internationalRateCost, "Zone EU");
        rateCost = create.textParam(originCountryRate, "Belgium", "-/-/0.08");
        targetCountryRate = rateCost.getAttribute();
    }

    @Override
    public VisibilityArea get() {
        return va;
    }
}
