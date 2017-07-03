#!/bin/bash

sh run.sh --data qAll.json --mode UnitDep --train data/perturb/old.txt --test data/handcrafted/fold.txt --model_dir models3/ --print_mistakes > log/UnitDepAllHandcraft.out

sh run.sh --data qAll.json --mode UnitDep --train fAll.txt --test data/handcrafted/fold.txt --model_dir models3/ --print_mistakes > log/UnitDepAggHandcraft.out



sh run.sh --data qAll.json --mode LCA --train data/perturb/old.txt --test data/handcrafted/fold.txt --model_dir models3/ --print_mistakes > log/LCAAllHandcraft.out

sh run.sh --data qAll.json --mode LCA --train fAll.txt --test data/handcrafted/fold.txt --model_dir models3/ --print_mistakes > log/LCAAggHandcraft.out



sh run.sh --data qAll.json --mode E2ELogic --train data/perturb/old.txt --test data/handcrafted/fold.txt --model_dir models3/ --print_mistakes > log/E2ELogicAllHandcraft.out

sh run.sh --data qAll.json --mode E2ELogic --train fAll.txt --test data/handcrafted/fold.txt --model_dir models3/ --print_mistakes > log/E2ELogicAggHandcraft.out

