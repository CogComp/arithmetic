#!/bin/bash

sh run.sh --mode UnitDep --cv data/allArith/fold0.txt data/allArith/fold1.txt data/allArith/fold2.txt data/allArith/fold3.txt data/allArith/fold4.txt --model_dir models2/ --print_mistakes > log/UnitDepAll.out

sh run.sh --mode UnitDep --cv data/allArithLex/fold0.txt data/allArithLex/fold1.txt data/allArithLex/fold2.txt data/allArithLex/fold3.txt data/allArithLex/fold4.txt --model_dir models2/ --print_mistakes > log/UnitDepAllLex.out

sh run.sh --mode UnitDep --cv data/allArithTmpl/fold0.txt data/allArithTmpl/fold1.txt data/allArithTmpl/fold2.txt data/allArithTmpl/fold3.txt data/allArithTmpl/fold4.txt --model_dir models2/ --print_mistakes > log/UnitDepAllTmpl.out 

sh run.sh --mode UnitDep --train data/perturb/old.txt --test data/perturb/new.txt --model_dir models2/ --print_mistakes > log/UnitDepPerturb.out



sh run.sh --mode LCA --cv data/allArith/fold0.txt data/allArith/fold1.txt data/allArith/fold2.txt data/allArith/fold3.txt data/allArith/fold4.txt --model_dir models2/ --print_mistakes > log/LCAAll.out

sh run.sh --mode LCA --cv data/allArithLex/fold0.txt data/allArithLex/fold1.txt data/allArithLex/fold2.txt data/allArithLex/fold3.txt data/allArithLex/fold4.txt --model_dir models2/ --print_mistakes > log/LCAAllLex.out

sh run.sh --mode LCA --cv data/allArithTmpl/fold0.txt data/allArithTmpl/fold1.txt data/allArithTmpl/fold2.txt data/allArithTmpl/fold3.txt data/allArithTmpl/fold4.txt --model_dir models2/ --print_mistakes > log/LCAAllTmpl.out 

sh run.sh --mode LCA --train data/perturb/old.txt --test data/perturb/new.txt --model_dir models2/ --print_mistakes > log/LCAPerturb.out



sh run.sh --mode E2ELogic --cv data/allArith/fold0.txt data/allArith/fold1.txt data/allArith/fold2.txt data/allArith/fold3.txt data/allArith/fold4.txt --model_dir models2/ --print_mistakes > log/E2ELogicAll.out

sh run.sh --mode E2ELogic --cv data/allArithLex/fold0.txt data/allArithLex/fold1.txt data/allArithLex/fold2.txt data/allArithLex/fold3.txt data/allArithLex/fold4.txt --model_dir models2/ --print_mistakes > log/E2ELogicAllLex.out

sh run.sh --mode E2ELogic --cv data/allArithTmpl/fold0.txt data/allArithTmpl/fold1.txt data/allArithTmpl/fold2.txt data/allArithTmpl/fold3.txt data/allArithTmpl/fold4.txt --model_dir models2/ --print_mistakes > log/E2ELogicAllTmpl.out 

sh run.sh --mode E2ELogic --train data/perturb/old.txt --test data/perturb/new.txt --model_dir models2/ --print_mistakes > log/E2ELogicPerturb.out
