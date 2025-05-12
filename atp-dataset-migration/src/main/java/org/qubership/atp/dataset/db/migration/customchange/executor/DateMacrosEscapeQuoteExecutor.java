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

package org.qubership.atp.dataset.db.migration.customchange.executor;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

import org.qubership.atp.dataset.db.migration.customchange.constant.JoinerSplitterConstants;
import org.qubership.atp.dataset.db.migration.customchange.constant.RegExpConstants;
import org.qubership.atp.dataset.db.migration.customchange.model.DateMacros;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class DateMacrosEscapeQuoteExecutor implements Executor<String, String> {

    @Override
    public String execute(String inputParameter) {
        return escapeQuoteDateMacros(inputParameter);
    }

    /**
     * Escape quotes for date macros.
     */
    public String escapeQuoteDateMacros(String updatedValue) {
        log.debug("escapeQuoteDateMacros (updatedValue: {})", updatedValue);
        List<DateMacros> dateMacros = findDateMacros(updatedValue);
        escapeTorZSymbolDateMacros(dateMacros);
        return replaceMacrosDateInInputString(updatedValue, dateMacros);
    }

    private String replaceMacrosDateInInputString(String updatedValue, List<DateMacros> dateMacros) {
        AtomicReference<String> updatedMacrosValue = new AtomicReference<>(updatedValue);
        AtomicInteger indexAdded = new AtomicInteger(0);

        dateMacros.forEach(macrosDate -> {
            log.debug("replaceMacrosDateInInputString (macrosDate: {})", macrosDate);
            int startIndex = macrosDate.getStartIndex() + indexAdded.get();
            int lastIndex = macrosDate.getLastIndex() + indexAdded.get();
            log.debug("replaceMacrosDateInInputString (startIndex: {}, lastIndex: {})", startIndex, lastIndex);
            updatedMacrosValue.set(JoinerSplitterConstants.EMPTY_STRING_JOINER.join(
                    getStringBeforeMacros(startIndex, updatedMacrosValue.get()),
                    macrosDate.getMacros(),
                    getStringAfterMacros(lastIndex, updatedMacrosValue.get())));

            indexAdded.set(calculateIndexForReplace(indexAdded.get(),
                    macrosDate.getLastIndex() - macrosDate.getStartIndex(),
                    macrosDate.getMacros().length()));
        });
        log.info("Update macros syntax (escape quote for date macros) from: {}, to: {}",
                updatedValue, updatedMacrosValue.get());
        return updatedMacrosValue.get();
    }

    private String getStringBeforeMacros(int startIndex, String updateMacrosValue) {
        log.debug("getStringBeforeMacros (startIndex: {}, updateMacrosValue: {})", startIndex, updateMacrosValue);
        return updateMacrosValue.substring(0, startIndex);
    }

    private String getStringAfterMacros(int lastIndex, String updateMacrosValue) {
        log.debug("getStringAfterMacros (lastIndex: {}, updateMacrosValue: {})", lastIndex, updateMacrosValue);
        return updateMacrosValue.substring(lastIndex);
    }

    private void escapeTorZSymbolDateMacros(List<DateMacros> dateMacros) {
        dateMacros
                .stream()
                .filter(macrosDate -> isDateContainsTorZSymbols(macrosDate.getMacros()))
                .forEachOrdered(macrosDate -> macrosDate.setMacros(escapeTorZSymbols(macrosDate.getMacros())));
    }

    private List<DateMacros> findDateMacros(String updatedValue) {
        List<DateMacros> dateMacros = new LinkedList<>();
        Matcher matcher = RegExpConstants.DATE_MACROS_PATTERN.matcher(updatedValue);
        while (matcher.find()) {
            DateMacros dateMacrosValue = DateMacros.builder()
                    .startIndex(matcher.start())
                    .lastIndex(matcher.end())
                    .macros(getDateMacros(updatedValue, matcher.start(), matcher.end()))
                    .build();
            log.info("Date macros was find: {}", dateMacrosValue);
            dateMacros.add(dateMacrosValue);
        }
        return dateMacros;
    }

    private int calculateIndexForReplace(int indexAdded, int macrosDateLength, int macrosDateTorZEscapedLength) {
        log.debug("calculateIndexForReplace (indexAdded: {}, macrosDateLength: {}, "
                + "macrosDateTorZEscapedLength: {})", indexAdded, macrosDateLength, macrosDateTorZEscapedLength);
        return indexAdded + macrosDateTorZEscapedLength - macrosDateLength;
    }

    private boolean isDateContainsTorZSymbols(String macrosDateWithParameters) {
        log.debug("isDateContainsTorZSymbols (macrosDateWithParameters: {})", macrosDateWithParameters);
        Matcher dateTorZMatcher = RegExpConstants.TZ_QUOTE_PATTERN.matcher(macrosDateWithParameters);
        return dateTorZMatcher.find();
    }

    private String getDateMacros(String updatedValue, int startIndex, int lastIndex) {
        log.debug("getDateMacros (updatedValue: {}, startIndex: {}, "
                + "lastIndex: {})", updatedValue, startIndex, lastIndex);
        return updatedValue.substring(startIndex, lastIndex);
    }

    private String escapeTorZSymbols(String macrosDateWithParameters) {
        log.debug("escapeTorZSymbols (macrosDateWithParameters: {})", macrosDateWithParameters);
        return macrosDateWithParameters
                .replaceAll("'T\\\\'|\\\\'T'|\\\\'T\\\\'", "'T'")
                .replaceAll("\\\\'Z\\\\'|'Z\\\\'|\\\\'Z'", "'Z'")
                .replaceAll("'T'", "\\\\'T\\\\'")
                .replaceAll("'Z'", "\\\\'Z\\\\'");
    }

}
