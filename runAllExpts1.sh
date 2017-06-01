#!/bin/bash

sh run.sh --mode UnitDep --cv data/aggregate/fold0.txt data/aggregate/fold1.txt data/aggregate/fold2.txt data/aggregate/fold3.txt data/aggregate/fold4.txt --model_dir models1/ --print_mistakes > log/UnitDepAgg.out

sh run.sh --mode UnitDep --cv data/aggregateLex/fold0.txt data/aggregateLex/fold1.txt data/aggregateLex/fold2.txt data/aggregateLex/fold3.txt data/aggregateLex/fold4.txt --model_dir models1/ --print_mistakes > log/UnitDepAggLex.out

sh run.sh --mode UnitDep --cv data/aggregateTmpl/fold0.txt data/aggregateTmpl/fold1.txt data/aggregateTmpl/fold2.txt data/aggregateTmpl/fold3.txt data/aggregateTmpl/fold4.txt --model_dir models1/ --print_mistakes > log/UnitDepAggTmpl.out 



sh run.sh --mode LCA --cv data/aggregate/fold0.txt data/aggregate/fold1.txt data/aggregate/fold2.txt data/aggregate/fold3.txt data/aggregate/fold4.txt --model_dir models1/ --print_mistakes > log/LCAAgg.out

sh run.sh --mode LCA --cv data/aggregateLex/fold0.txt data/aggregateLex/fold1.txt data/aggregateLex/fold2.txt data/aggregateLex/fold3.txt data/aggregateLex/fold4.txt --model_dir models1/ --print_mistakes > log/LCAAggLex.out

sh run.sh --mode LCA --cv data/aggregateTmpl/fold0.txt data/aggregateTmpl/fold1.txt data/aggregateTmpl/fold2.txt data/aggregateTmpl/fold3.txt data/aggregateTmpl/fold4.txt --model_dir models1/ --print_mistakes > log/LCAAggTmpl.out 



sh run.sh --mode E2ELogic --cv data/aggregate/fold0.txt data/aggregate/fold1.txt data/aggregate/fold2.txt data/aggregate/fold3.txt data/aggregate/fold4.txt --model_dir models1/ --print_mistakes > log/E2ELogicAgg.out

sh run.sh --mode E2ELogic --cv data/aggregateLex/fold0.txt data/aggregateLex/fold1.txt data/aggregateLex/fold2.txt data/aggregateLex/fold3.txt data/aggregateLex/fold4.txt --model_dir models1/ --print_mistakes > log/E2ELogicAggLex.out

sh run.sh --mode E2ELogic --cv data/aggregateTmpl/fold0.txt data/aggregateTmpl/fold1.txt data/aggregateTmpl/fold2.txt data/aggregateTmpl/fold3.txt data/aggregateTmpl/fold4.txt --model_dir models1/ --print_mistakes > log/E2ELogicAggTmpl.out 
