#!/usr/bin/bash

tag=""
scriptDir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
javaAnalysisDir=$scriptDir/../

for BM in antlr bloat chart eclipse fop hsqldb luindex lusearch pmd xalan
do

	cd $DOOP_HOME
	ID=${BM}${tag}

	./doop -id $ID -a 2-object-sensitive+heap --platform java_7 -i ../benchmarks/dacapo-2006/$BM.jar --dacapo --ssa --cfg --cache | tee $ID.trace
	bloxbatch -db last-analysis -addBlock -file logic/analyses/must-point-to/may-pre-analysis.logic
	bloxbatch -db last-analysis -addBlock 'RootMethodForMustAnalysis(?meth) <- Method:DeclaringType[?meth] = ?class, ApplicationClass(?class), Reachable(?meth).'

	cpp -DMUST_AFTER_MAY -P logic/analyses/must-point-to/analysis-simple.logic > must.logic
	echo "Must-Datalog START"
	{ time bloxbatch -db last-analysis -addBlock -file must.logic ; } 2>&1 | tee $ID.must.trace
	echo "Must-Datalog END"
	(
		set -x
		bloxbatch -db last-analysis -query '_(v) <- VarPointsTo(_,_,_,v), Var:DeclaringMethod(v,m), ApplicationClass(Method:DeclaringType[m]).' | wc -l
		bloxbatch -db last-analysis -query '_(v) <- MustPointTo[v,_] = _, Var:DeclaringMethod(v,m), ApplicationClass(Method:DeclaringType[m]).' | wc -l
		bloxbatch -db last-analysis -query '_(v) <- MustPointTo[v,_] = _.' | wc -l
		bloxbatch -db last-analysis -query '_(v) <- MustPointTo[v,SingleAllContext[]] = _, Var:DeclaringMethod(v,m), ApplicationClass(Method:DeclaringType[m]).' | wc -l
		bloxbatch -db last-analysis -popCount | egrep MustPointTo\|MustContext\|MustAlias
		set +x
	) 2>&1 | tee $ID.must.stats
	rm must.logic

	cpp -DMUST_AFTER_MAY -P logic/analyses/must-point-to/points-alias-simple.logic > mustOPT.logic
	echo "Must-Datalog-OPT START"
	{ time bloxbatch -db last-analysis -addBlock -file mustOPT.logic ; } 2>&1 | tee $ID.mustOPT.trace
	echo "Must-Datalog-OPT END"
	(
		set -x
		bloxbatch -db last-analysis -query '_(v) <- PTMustPointToHeap(_, v, _).' | wc -l
		set +x
	) 2>&1 | tee $ID.mustOPT.stats
	rm mustOPT.logic


	outDir=$javaAnalysisDir/resources/$ID
	rm -rf $outDir
	mkdir $outDir

	time bloxbatch -db last-analysis -addBlock -file $javaAnalysisDir/resources/fact-queries.logic

	for rel in PhiNodeHead MaySuccessorModuloThrow Instruction:Method RootMethod ActualToFormalArg FirstInstructionInMethod ReturnTo AssignHeapAllocationVar UnresolvedCall ResolvedCall
	do
		relFname=$(echo $rel | tr ':' '_')

		rm -f $outDir/$relFname.csv
		bloxbatch -db last-analysis -keepDerivedPreds -exportCsv $rel -exportDataDir $outDir -exportDelimiter '\t'
		if [ -e $outDir/$relFname.csv ]
		then
			tail -n +2 $outDir/$relFname.csv > $outDir/$relFname.facts
		else
			echo $rel not exported!
		fi
	done

	mv $outDir/MaySuccessorModuloThrow.facts $outDir/Successor.facts

	doopFactsDir="$(dirname $(readlink -f last-analysis))/facts"
	cp $doopFactsDir/AssignCast.facts $outDir
	cp $doopFactsDir/AssignLocal.facts $outDir
	cp $doopFactsDir/LoadInstanceField.facts $outDir
	cp $doopFactsDir/StoreInstanceField.facts $outDir
	rm $outDir/*.csv

	cd $javaAnalysisDir
	#make run
	mkdir bin
	javac -Xlint:unchecked -cp lib/junit-4.12.jar -d bin src/*.java src/*/*.java
	java -cp lib/junit-4.12.jar:lib/hamcrest-core-1.3.jar:bin -Xmx20G Main $ID | $DOOP_HOME/$ID.java.trace

done
