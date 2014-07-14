package org.shanbo.feluca.distribute.launch;

import gnu.trove.set.hash.TIntHashSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.shanbo.feluca.common.Constants;
import org.shanbo.feluca.common.FelucaException;
import org.shanbo.feluca.data.DataReader;
import org.shanbo.feluca.data.Vector;
import org.shanbo.feluca.data.util.CollectionUtil;
import org.shanbo.feluca.distribute.newmodel.VectorClient;
import org.shanbo.feluca.distribute.newmodel.VectorServer;
import org.shanbo.feluca.util.AlgoDeployConf;
import org.shanbo.feluca.util.ZKClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO more dataType support !
 * 
 * 	<p> A batch of vectors are loaded into memory,
 * <p><b> !Remember! Vector is just a ref of byte[], DO NOT store each one by yourself; </b></p>
 *  <p><b> to shuffle or partition this batch, just copy the [offsetArray] and do that to it;</b></p>
 *  <p>basic usage in  {@link #compute()}:
 * <p> {
 *  <p>      long[] offsetArray = dataReader.getOffsetArray();
 *  <p>      
 *  <p>      for(int o = 0 ; o < offsetArray.length; o++){
 *  <p>	        Vector v = dataReader.getVectorByOffset(offsetArray[o]);
 * @author lgn
 *
 */
public abstract class LoopingBase{
	
	Logger log ;
	
	protected GlobalConfig conf;
	protected int loops;

	//data & computation
	LoopMonitor loopMonitor; //with all worker, no matter model or data
	protected DataReader dataReader; //auto close;
	protected VectorClient vectorClient;
	boolean isDataManager;

	//server & startingGun(with one of the server)
	private VectorServer vectorServer;
	StartingGun startingGun; //one and only one with a job

	public static void distinct(TIntHashSet idSet, Vector v){
		for(int i = 0; i < v.getSize(); i ++){
			idSet.add(v.getFId(i));
		}
	}

	public static List<long[]> splitLongs(long[] offsetArray, int numPerBlock, boolean shuffled){
		List<long[]> result = new ArrayList<long[]>(offsetArray.length / numPerBlock + 1);
		long[] tmp = Arrays.copyOf(offsetArray, offsetArray.length);
		if (shuffled){
			CollectionUtil.shuffle(tmp, 0, tmp.length);
		}
		int i = 0;
		for( ; i < offsetArray.length / numPerBlock; i++){
			result.add(Arrays.copyOfRange(tmp, i * numPerBlock, (i+1) * numPerBlock));
		}
		if ( (i) * numPerBlock <= tmp.length){
			result.add(Arrays.copyOfRange(tmp, i * numPerBlock, tmp.length));
		}
		return result;
	}

	public LoopingBase(GlobalConfig conf) throws Exception{
		log = LoggerFactory.getLogger(this.getClass());
		init(conf);
	}

	/**
	 * 
	 * @throws Exception
	 */
	private void init(GlobalConfig conf) throws Exception{
		this.conf = conf;
		loops = conf.getAlgorithmConf().getInteger(Constants.Algorithm.LOOPS);
		AlgoDeployConf deployConf = conf.getDeployConf();
		//data server and client can be separated from a worker-node.
		//
		if (deployConf.isModelServer()){
			vectorServer = new VectorServer(conf);
		}
		if (deployConf.isStartingGun()){
			startingGun = new StartingGun(conf.getAlgorithmName(), conf.getModelServers().size(), conf.getWorkers().size());
		}
		if (deployConf.isModelClient()){
			vectorClient = new VectorClient(conf);
		}
		isDataManager = deployConf.isModelManager();
		loopMonitor = new LoopMonitor(conf.getAlgorithmName(), conf.getWorkerName());
	}

	private void openDataInput() throws IOException{
		dataReader = DataReader.createDataReader(false, 
				Constants.Base.getWorkerRepository()+ Constants.Base.DATA_DIR +
				"/" + conf.getDataName());
	}

	protected void startup() throws Exception{}

	protected void cleanup() throws Exception{}

	protected abstract void modelStart() throws Exception;
	
	protected abstract void modelClose() throws Exception;
	
	public final void run() throws Exception{
		
		if (vectorServer!= null){
			vectorServer.start();
		}
		if (vectorClient != null){
			startup();
			vectorClient.open();
			if (startingGun!= null){//only one will be started
				startingGun.start();//start watch
				startingGun.waitForModelServersStarted(); //wait for model servers started
				startingGun.submitAndWait(new Runnable() { //wait for startup() finished
					public void run() {
						try {
							ZKClient.get().setData(Constants.Algorithm.ZK_ALGO_CHROOT + "/" + conf.getAlgorithmName() , new byte[]{});
							modelStart();
						} catch (Exception e) {
							throw new FelucaException("createVectorDB error ",e);
						}
					}
				}, 10000);
			}
			loopMonitor.watchLoopSignal(); //start watching
			loopMonitor.confirmLoopFinish(); //tell startingGun I'm ok
			
			for(int i = 0 ; i < loops && earlyStop() == false;i++){
				System.out.println("loop--:----" + i);
				loopMonitor.waitForLoopStart();   //wait for other workers; according to startingGun's action 
				openDataInput();
				while(dataReader.hasNext()){
					compute();
					dataReader.releaseHolding();
				}
				loopMonitor.confirmLoopFinish();
			}
			if (isDataManager){ //the only one
				if (startingGun != null){  //do cleanup() first 
					startingGun.submitAndWait(new Runnable() {
						public void run() {
							try {
								modelClose();
							} catch (Exception e) {
								throw new FelucaException("dumpVectorDB error ",e);
							}
						}
					});
					startingGun.setFinish(); //tell all workers to finish job
				}
			}
			cleanup();
		}
		loopMonitor.waitForSignalEquals("finish", 10000); //wait for finish signal
		closeAll();
	}


	/**
	 * todo 
	 * @throws InterruptedException 
	 * @throws KeeperException 
	 */
	private void closeAll() throws InterruptedException, KeeperException{
		loopMonitor.close();
		if (vectorClient != null){
			vectorClient.close();
			
		}
		if (vectorServer!= null){
			vectorServer.stop();
		}
		if (isDataManager){
			startingGun.close();
		}
	}

	protected boolean earlyStop(){return false;}

	/**
	 * A batch of vectors are loaded into memory,
	 * <p><b> !Remember! Vector is just a ref of byte[], DO NOT store each one by yourself; </b></p>
	 *  <p><b> to shuffle or partition this batch, just copy the [offsetArray] and do that to it;</b></p>
	 *  <p>basic usage here:
	 * <p> {
	 *  <p>      long[] offsetArray = dataReader.getOffsetArray();
	 *  <p>      
	 *  <p>      for(int o = 0 ; o < offsetArray.length; o++){
	 *  <p>	        Vector v = dataReader.getVectorByOffset(offsetArray[o]);
	 *  <p>}
	 */
	protected abstract void compute() throws Exception;
}
