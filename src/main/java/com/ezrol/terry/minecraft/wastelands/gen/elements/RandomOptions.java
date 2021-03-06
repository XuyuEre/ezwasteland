/*
 * Copyright (c) 2015-2017, Terrence Ezrol (ezterry)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * + Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.ezrol.terry.minecraft.wastelands.gen.elements;

import com.ezrol.terry.minecraft.wastelands.Logger;
import com.ezrol.terry.minecraft.wastelands.api.IRegionElement;
import com.ezrol.terry.minecraft.wastelands.api.Param;
import com.ezrol.terry.minecraft.wastelands.api.RegionCore;
import com.ezrol.terry.minecraft.wastelands.village.WastelandGenVillage;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.*;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraftforge.event.terraingen.TerrainGen;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.*;

import static net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.STRONGHOLD;

/**
 * Random Options Generation module
 * This module takes care of many non-standard wasteland generations including:
 * - Global vertical offset
 * - Oceans/ponds
 * - Village generation
 * - Stronghold generation
 * - Additional inter-mod compatibility support
 * <p>
 * Created by ezterry on 9/6/16.
 */
public class RandomOptions implements IRegionElement {
    static final private Logger log = new Logger(false);
    private final WeakHashMap<World, MapGenStronghold> StrongholdGens;
    private final WeakHashMap<World, WastelandGenVillage> VillageGens;
    private static final Biome.SpawnListEntry VILLAGE_ZOMBIE =
            new Biome.SpawnListEntry(EntityZombieVillager.class, 300, 2, 2);

    public RandomOptions() {
        StrongholdGens = new WeakHashMap<>();
        VillageGens = new WeakHashMap<>();
        RegionCore.register(this);
    }

    private MapGenStronghold getStrongholdGen(World w) {
        if (StrongholdGens.containsKey(w)) {
            return (StrongholdGens.get(w));
        }
        log.info("Creating new stronghold generator");
        Map<String, String> args = new Hashtable<>();

        args.put("count", "196");
        args.put("distance", "48.0");
        args.put("spread", "5");

        MapGenStronghold strongholdGenerator = new MapGenStronghold(args);
        strongholdGenerator = (MapGenStronghold) TerrainGen.getModdedMapGen(strongholdGenerator, STRONGHOLD);

        StrongholdGens.put(w, strongholdGenerator);
        return (strongholdGenerator);
    }

    private WastelandGenVillage getVillageGen(World w, float rate) {
        if (VillageGens.containsKey(w)) {
            return (VillageGens.get(w));
        }
        log.info("Creating new wasteland village generator");
        WastelandGenVillage villageGenerator = new WastelandGenVillage(w.getSeed(), rate);
        VillageGens.put(w, villageGenerator);
        return villageGenerator;
    }

    @Override
    public int addElementHeight(int currentoffset, int x, int z, RegionCore core, List<Object> elements) {
        return currentoffset + (Integer) elements.get(0);
    }

    @Override
    public String getElementName() {
        return "randopts";
    }

    @Override
    public List<Param> getParamTemplate() {
        List<Param> lst = new ArrayList<>();
        lst.add(new Param.BooleanParam(
                "oceans",
                "config.ezwastelands.randopts.oceans.help",
                false));
        lst.add(new Param.StringParam("oceanblock",
                "config.ezwastelands.randopts.oceanblock.help",
                "minecraft:water"));
        lst.add(new Param.IntegerParam("globaloffset",
                "config.ezwastelands.randopts.globaloffset.help",
                0, -25, 25));
        lst.add(new Param.BooleanParam("integration",
                "config.ezwastelands.randopts.integration.help",
                true));
        lst.add(new Param.BooleanParam("villages",
                "config.ezwastelands.randopts.villages.help",
                false));
        lst.add(new Param.FloatParam("villagechance",
                "config.ezwastelands.randopts.villagechance.help",
                10.0f, 0.0f, 100.0f));
        lst.add(new Param.BooleanParam("strongholds",
                "config.ezwastelands.randopts.strongholds.help",
                false));
        return lst;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public List<Object> calcElements(Random r, int x, int z, RegionCore core) {
        List<Object> placeholder = new ArrayList<>();
        placeholder.add(((Param.IntegerParam) core.lookupParam(this,"globaloffset")).get());
        return placeholder;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void postFill(ChunkPrimer chunkprimer, int height, int x, int z, RegionCore core) {

        if (((Param.BooleanParam) core.lookupParam(this, "oceans")).get()) {
            IBlockState water;
            ResourceLocation oceanBlockName = new ResourceLocation(
                    ((Param.StringParam) core.lookupParam(this, "oceanblock")).get());
            if(ForgeRegistries.BLOCKS.containsKey(oceanBlockName)){
                water=ForgeRegistries.BLOCKS.getValue(oceanBlockName).getDefaultState();
            }
            else{
                water = Blocks.WATER.getDefaultState();
            }

            if (height < 0) {
                height = 0;
            }
            for (int i = height + 1; i <= RegionCore.WASTELAND_HEIGHT; i++) {
                chunkprimer.setBlockState(x & 0x0F, i, z & 0x0F, water);
            }
        }
    }

    private Random chunkBasedRNG(ChunkPos p, long seed) {
        Random r;
        long localSeed;

        long x = p.x;
        long z = p.z;

        localSeed = (x << 32) + (z * 31);
        localSeed = localSeed ^ seed;
        localSeed += 5147;

        r = new Random(localSeed);
        r.nextInt();
        r.nextInt();
        return (r);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void additionalTriggers(String event, IChunkGenerator gen, ChunkPos cords, ChunkPrimer chunkprimer, RegionCore core) {
        boolean genStrongholds = ((Param.BooleanParam) core.lookupParam(this, "strongholds")).get();
        boolean genVillages = ((Param.BooleanParam) core.lookupParam(this, "villages")).get();
        float villageRate = ((Param.FloatParam) core.lookupParam(this, "villagechance")).get();
        World w = core.getWorld();

        if (event.equals("chunkcleanup") || event.equals("recreateStructures")) {
            if (core.isStructuresEnabled()) {
                if (genStrongholds) {
                    getStrongholdGen(w).generate(w, cords.x, cords.z, chunkprimer);
                }
                if (genVillages) {
                    if (core.addElementHeight( cords.x << 4, cords.z << 4) >= RegionCore.WASTELAND_HEIGHT) {
                        getVillageGen(w, villageRate).generate(w, cords.x,
                                cords.z, chunkprimer);
                    }
                }
            }
        } else if (event.equals("populate")) {
            boolean flag = false;
            boolean triggers = ((Param.BooleanParam) core.lookupParam(this,"integration")).get();
            Random rng = chunkBasedRNG(cords, w.getSeed());
            WastelandGenVillage villageGenerator=null;

            if (triggers) {
                //noinspection ConstantConditions
                net.minecraftforge.event.ForgeEventFactory.onChunkPopulate(true, gen, w, rng,
                        cords.x, cords.z, flag);
            }
            if (core.isStructuresEnabled()) {
                if (genStrongholds) {
                    getStrongholdGen(w).generateStructure(w, rng, cords);
                }
                if (genVillages) {
                    villageGenerator = getVillageGen(w, villageRate);
                    flag = villageGenerator.generateStructure(w, rng, cords);
                }
            }
            if (triggers) {
                net.minecraftforge.event.ForgeEventFactory.onChunkPopulate(false, gen, w, rng,
                        cords.x, cords.z, flag);
            }
            if (flag){
                cleanupVillage(villageGenerator, core, cords);
            }
        }
    }

    private void cleanupVillage(WastelandGenVillage villageGenerator, RegionCore core, ChunkPos cords) {
        //first "regenerate" paths
        villageGenerator.fixPathBlock(core.getWorld(),cords,core);
        for(int x=-1;x<=1;x++){
            for(int z=-1;z<=1;z++){
                ChunkPos check = new ChunkPos(cords.x + x,cords.z + z);
                if(core.getWorld().isBlockLoaded(check.getBlock(8,8,8),false)){
                    villageGenerator.reduceDirt(core.getWorld(),check,core);
                }
            }
        }
    }

    @Override
    public BlockPos getNearestStructure(String name, BlockPos curPos, boolean findUnexplored, RegionCore core) {
        World w = core.getWorld();
        switch(name){
            case "Stronghold":
                if (((Param.BooleanParam) core.lookupParam(this, "strongholds")).get()) {
                    return (getStrongholdGen(w).getNearestStructurePos(w,curPos,findUnexplored));
                }
                break;
            case "Village":
                if (((Param.BooleanParam) core.lookupParam(this, "villages")).get()) {
                    float rate = ((Param.FloatParam) core.lookupParam(this, "villagechance")).get();
                    return (getVillageGen(w,rate).getNearestStructurePos(w,curPos,findUnexplored));
                }
                break;
        }
        return null;
    }

    @Override
    public List<Biome.SpawnListEntry> getSpawnable(List<Biome.SpawnListEntry> lst, EnumCreatureType creatureType, BlockPos pos, RegionCore core) {


        if(creatureType == EnumCreatureType.MONSTER &&
                ((Param.BooleanParam) core.lookupParam(this, "villages")).get()){

            float rate = ((Param.FloatParam) core.lookupParam(this, "villagechance")).get();
            WastelandGenVillage vg = getVillageGen(core.getWorld(),rate);
            if(vg != null && vg.isPositionInVillageRegion(pos)) {
                //we are near a village, make zombie villages more common
                lst.add(VILLAGE_ZOMBIE);
            }
        }

        return lst;
    }

    @Override
    public boolean isInsideStructure(String name, BlockPos pos, RegionCore core) {
        World w = core.getWorld();
        switch(name){
            case "Stronghold":
                if (((Param.BooleanParam) core.lookupParam(this, "strongholds")).get()) {
                    return (getStrongholdGen(w).isInsideStructure(pos));
                }
                break;
            case "Village":
                if (((Param.BooleanParam) core.lookupParam(this, "villages")).get()) {
                    float rate = ((Param.FloatParam) core.lookupParam(this, "villagechance")).get();
                    return (getVillageGen(w,rate).isInsideStructure(pos));
                }
                break;
        }
        return false;
    }
}
