/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.itempipes.blocks;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.segmentedpaths.SegmentMeta;
import org.terasology.segmentedpaths.blocks.PathFamily;
import org.terasology.segmentedpaths.components.BlockMappingComponent;
import org.terasology.segmentedpaths.components.PathDescriptorComponent;
import org.terasology.segmentedpaths.controllers.PathFollowerSystem;
import org.terasology.segmentedpaths.controllers.SegmentCacheSystem;
import org.terasology.segmentedpaths.controllers.SegmentMapping;
import org.terasology.segmentedpaths.controllers.SegmentSystem;
import org.terasology.segmentedpaths.segments.Segment;
import org.terasology.itempipes.event.PipeMappingEvent;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.family.BlockFamily;

import java.util.List;

public class PipeBlockSegmentMapper implements SegmentMapping {
    private PathFollowerSystem pathFollowerSystem;
    private SegmentSystem segmentSystem;
    private SegmentCacheSystem segmentCacheSystem;
    private BlockEntityRegistry blockEntityRegistry;

    public PipeBlockSegmentMapper(BlockEntityRegistry blockEntityRegistry, PathFollowerSystem pathFollowerSystem,SegmentSystem segmentSystem, SegmentCacheSystem segmentCacheSystem) {
        this.blockEntityRegistry = blockEntityRegistry;
        this.pathFollowerSystem = pathFollowerSystem;
        this.segmentCacheSystem = segmentCacheSystem;
        this.segmentSystem = segmentSystem;
    }


    @Override
    public MappingResult nextSegment(SegmentMeta meta, SegmentEnd ends) {
        if (meta.association.hasComponent(BlockComponent.class)) {
            BlockComponent blockComponent = meta.association.getComponent(BlockComponent.class);

            BlockFamily blockFamily = blockComponent.getBlock().getBlockFamily();

            Vector3f v1 = segmentSystem.segmentPosition(meta.association);
            Quat4f q1 = segmentSystem.segmentRotation(meta.association);

            Segment currentSegment = segmentCacheSystem.getSegment(meta.prefab);


            BlockMappingComponent blockMappingComponent = meta.prefab.getComponent(BlockMappingComponent.class);
            if (blockFamily instanceof PathFamily) {

                Rotation rotation = ((PathFamily) blockFamily).getRotationFor(blockComponent.getBlock().getURI());
                switch (ends) {
                    case START: {
                        Vector3i segment = new Vector3i(blockComponent.getPosition()).add(rotation.rotate(blockMappingComponent.s1).getVector3i());
                        EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(segment);
                        PathDescriptorComponent pathDescriptor = blockEntity.getComponent(PathDescriptorComponent.class);
                        if (pathDescriptor == null)
                            return null;

                        Vector3f v2 = segmentSystem.segmentPosition(blockEntity);
                        Quat4f q2 = segmentSystem.segmentRotation(blockEntity);

                        List<Prefab> paths = Lists.newArrayList();
                        for (Prefab d : pathDescriptor.descriptors) {
                            Segment nextSegment = segmentCacheSystem.getSegment(d);
                            if (segmentSystem.segmentMatch(currentSegment, v1, q1, nextSegment, v2, q2) != SegmentSystem.JointMatch.None) {
                                paths.add(d);
                            }
                        }
                        PipeMappingEvent pipeMappingEvent = blockEntity.send(new PipeMappingEvent(paths));
                        return new MappingResult(pipeMappingEvent.getSelectedPath(), blockEntity);

                    }
                    case END: {
                        Vector3i segment = new Vector3i(blockComponent.getPosition()).add(rotation.rotate(blockMappingComponent.s2).getVector3i());
                        EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(segment);
                        PathDescriptorComponent pathDescriptor = blockEntity.getComponent(PathDescriptorComponent.class);
                        if (pathDescriptor == null)
                            return null;

                        Vector3f v2 = segmentSystem.segmentPosition(blockEntity);
                        Quat4f q2 = segmentSystem.segmentRotation(blockEntity);

                        List<Prefab> paths = Lists.newArrayList();
                        for (Prefab d : pathDescriptor.descriptors) {
                            Segment nextSegment = segmentCacheSystem.getSegment(d);
                            if (segmentSystem.segmentMatch(currentSegment, v1, q1, nextSegment, v2, q2) != SegmentSystem.JointMatch.None) {
                                paths.add(d);
                            }
                        }
                        PipeMappingEvent pipeMappingEvent = blockEntity.send(new PipeMappingEvent(paths));
                        return new MappingResult(pipeMappingEvent.getSelectedPath(), blockEntity);
                    }
                }
            }
        }

        return null;
    }

    private Vector3i findOffset(Vector3i loc, Side main, Side influence, Rotation r) {
        Vector3i current = new Vector3i(loc).add(r.rotate(main).getVector3i());
        return current;
    }
}
