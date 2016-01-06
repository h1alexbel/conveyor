package com.aegisql.conveyor.utils.scalar;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;

public class ScalarConvertingConveyor <K,IN,OUT> extends AssemblingConveyor<K, SmartLabel<ScalarConvertingBuilder<IN,?>>, OUT> {

	public ScalarConvertingConveyor() {
		super();
		this.setName("ScalarConvertingConveyor");
		this.setReadinessEvaluator((state,builder) -> true ); //ready right after evaluation
	}

}
