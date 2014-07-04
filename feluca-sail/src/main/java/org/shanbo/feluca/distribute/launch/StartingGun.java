package org.shanbo.feluca.distribute.launch;

import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.shanbo.feluca.common.Constants;
import org.shanbo.feluca.util.ZKClient;
import org.shanbo.feluca.util.ZKClient.ChildrenWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * not seen by algorithms
 * TODO 
 * @author lgn
 *
 */
public class StartingGun {

	Logger log = LoggerFactory.getLogger(StartingGun.class);

	
	private ChildrenWatcher workerWatcher;
	
	private int totalWorkers; 
	private int waitingWorkers;
	private  String path;
	private int loop;
	private int maxLoop;
	
	public StartingGun(String taskName, int totalWorkers, int maxLoop) {
		this.path = Constants.Algorithm.ZK_ALGO_CHROOT + "/" + taskName;
		
		this.totalWorkers = totalWorkers;
		this.maxLoop = maxLoop;
	}
	
	private void createZKPath() throws KeeperException, InterruptedException{
		ZKClient.get().createIfNotExist(this.path);
		ZKClient.get().createIfNotExist(path + Constants.Algorithm.ZK_LOOP_PATH); 
		
		ZKClient.get().createIfNotExist(this.path + Constants.Algorithm.ZK_WAITING_PATH);
		
		
	}
	
	private void startWatch() {
		workerWatcher = new ChildrenWatcher() {
			
			@Override
			public void nodeRemoved(String node) {
			}
			
			@Override
			public void nodeAdded(String node) {
				waitingWorkers += 1;
				if (waitingWorkers  == totalWorkers){
					waitingWorkers = 0;
					String workerPath = path + Constants.Algorithm.ZK_WAITING_PATH;
					try {
						List<String> waitingList = ZKClient.get().getChildren(workerPath);
						for(String workerNode : waitingList){
							ZKClient.get().forceDelete(workerPath + "/" + workerNode);
						}
						ZKClient.get().setData(path + Constants.Algorithm.ZK_LOOP_PATH, ("" + loop).getBytes());
					} catch (Exception e) {
						log.error("all workers are ready. but error here :" + loop, e);
					}
					loop += 1;
				}
				if (loop >= maxLoop){
					close();
				}
			}
		};
		ZKClient.get().watchChildren(path + "/workers", workerWatcher);
	}
	
	
	public void start() throws KeeperException, InterruptedException{
		this.createZKPath();
		this.startWatch();
	}
	
	private void close(){
		ZKClient.get().destoryWatch(workerWatcher);
		
	}
	public String toString() {
		return path + " of (" + totalWorkers+ ")" ;
	}
	
}