package org.shanbo.feluca.distribute.model.horizon;

import org.msgpack.rpc.loop.EventLoop;
import org.shanbo.feluca.common.ClusterUtil;
import org.shanbo.feluca.common.Constants;
import org.shanbo.feluca.common.Server;
import org.shanbo.feluca.distribute.model.vertical.ReduceServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SyncModelServer extends Server{
	static Logger log = LoggerFactory.getLogger(ReduceServer.class);
	EventLoop loop;
	org.msgpack.rpc.Server server;

	MModelImpl modelImpl;
	
	String algoName;
	int port ;
	public SyncModelServer(String workerAddress, String algoName, MModelImpl model){
		this.modelImpl = model;
		this.port = new Integer(workerAddress.split(":")[1]) + MModel.PORT_AWAY;
		this.algoName = algoName;
	}
	
	@Override
	public String serverName() {
		return "modelServer";
	}

	@Override
	public int defaultPort() {
		return port;
	}

	@Override
	public String zkPathRegisterTo() {
		return Constants.Algorithm.ZK_ALGO_CHROOT + "/" + algoName + Constants.Algorithm.ZK_MODELSERVER_PATH ;

	}

	@Override
	public void preStart() throws Exception {
		ClusterUtil.getWorkerList();
		loop = EventLoop.defaultEventLoop();
		server = new org.msgpack.rpc.Server(loop);
		server.serve(modelImpl);
		server.listen("0.0.0.0", defaultPort());
		System.out.println("modelServer[" + port + "] started");

		
	}

	@Override
	public void postStop() throws Exception {
		server.close();
		loop.shutdown();
		System.out.println("modelServer[" + port + "] closed");
		
	}
	
}
