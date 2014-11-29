package org.shanbo.feluca.classification.lr;

import java.io.FileReader;
import java.util.Properties;

import org.shanbo.feluca.classification.common.Evaluator;
import org.shanbo.feluca.classification.fmc.SGDFactorizeMachine;
import org.shanbo.feluca.data2.DataEntry;

public class TestSGDFM {

	public static void trainTest(String predict) throws Exception{
		AbstractSGDLogisticRegression lr = new SGDFactorizeMachine();
		Properties p = new Properties();
		p.setProperty("alpha", "0.2");
		p.setProperty("lambda", "0.05");
		p.setProperty("loops", "30");
		p.setProperty("dim", "5");
		lr.setProperties(p);
		lr.loadData(DataEntry.createDataEntry("/home/lgn/data/avazutrain", false));
		
//		lr.crossValidation(5, new Evaluator.BinaryAccuracy());
		System.out.println(lr.toString());
		lr.train();

		System.out.println(lr.toString());
		DataEntry testSet = DataEntry.createDataEntry("/home/lgn/data/avazutest", false);
		lr.predict(testSet, predict, new Evaluator.BinaryAccuracy());
	}
	
	public static void testTest(String model,String predict) throws Exception{
		SGDL2LR lr = new SGDL2LR();
		Properties p = new Properties();
		p.load(new FileReader("/home/lgn/data/avazutrain/avazutrain.sta"));
		lr.loadModel(model, p);
		DataEntry testSet = DataEntry.createDataEntry("/home/lgn/data/avazutest", false);
		lr.predict(testSet, predict, new Evaluator.BinaryAccuracy());
	}
	
	public static void main(String[] args) throws  Exception {
		String predict = "/home/lgn/kaggle/avazu.predict";
		trainTest(predict);
		System.out.println("===============================");
	}
}
