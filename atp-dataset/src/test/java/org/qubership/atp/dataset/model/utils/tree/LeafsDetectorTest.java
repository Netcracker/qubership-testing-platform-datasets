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

package org.qubership.atp.dataset.model.utils.tree;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeafsDetectorTest extends TestData {
    private static final Logger LOG = LoggerFactory.getLogger(LeafsDetectorTest.class);

    @Test
    public void IterateOverTree_OneRoot_TraverseHandlerMethodsInvocationOrderIsCorrect() {
        animals.printTree(LOG::info);
        Position.ofLeafsDetector(item -> item.children.iterator(), animals)
                //initialize
                .toTheNextItem(animals, h -> {
                    //just takes first root
                }, animals.getPath())
                //went from animals to fish by going under animals
                .toTheNextItem(fish, h -> h.checkChildren(animals).wentUnder(animals), animals.getPathInclusive())
                //went from fish to reptile: found leaf fish, still under animals
                .toTheNextItem(reptile, h -> h.checkChildren(fish).leafFound(fish), animals.getPathInclusive())
                //went from reptile to lizard by going under reptile
                .toTheNextItem(lizard, h -> h.checkChildren(reptile).wentUnder(reptile), lizard.getPath())
                //went from lizard to mammal: found leaf lizard, went under animals then
                .toTheNextItem(mammal, h -> h.checkChildren(lizard).leafFound(lizard).wentUpper(1), mammal.getPath())
                //went from mammal to equine by going under mammal
                .toTheNextItem(equine, h -> h.checkChildren(mammal).wentUnder(mammal), equine.getPath())
                //went from equine to bovine: found leaf equine, still under mammal
                .toTheNextItem(bovine, h -> h.checkChildren(equine).leafFound(equine), bovine.getPath())
                //went from bovine to the finish: found leaf bovine, went under start (nothing before animals) then
                .toTheFinish(h -> h.checkChildren(bovine).leafFound(bovine).wentUpper(2));
    }
}
