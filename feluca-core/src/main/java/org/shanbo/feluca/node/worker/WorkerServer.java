package org.shanbo.feluca.node.worker;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.shanbo.feluca.common.Constants;
import org.shanbo.feluca.common.Server;
import org.shanbo.feluca.node.http.BaseChannelHandler;
import org.shanbo.feluca.node.http.Handler;
import org.shanbo.feluca.node.http.Handlers;
import org.shanbo.feluca.node.job.FelucaJob;
import org.shanbo.feluca.node.request.WorkerJobRequest;
import org.shanbo.feluca.node.request.WorkerStatusRequest;
import org.shanbo.feluca.util.ZKClient;

public class WorkerServer extends Server{
	WorkerModule module;
	
	final Handlers handlers = new Handlers();
	
	BaseChannelHandler channel = new BaseChannelHandler(handlers);
	
	@Override
	public String serverName() {
		return "feluca.worker";
	}

	@Override
	public int defaultPort() {
		return 12030;
	}

	@Override
	public String zkRegisterPath() {
		return Constants.Base.ZK_WORKER_PATH;
	}

	public void addHandler(Handler... hander){
		this.handlers.addHandler(hander);
	}


	protected ChannelPipelineFactory getChannelPipelineFactory(){
		return new ChannelPipelineFactory(){

			public ChannelPipeline getPipeline()
					throws Exception{
				// Create a default pipeline implementation.
				ChannelPipeline pipeline = Channels.pipeline();

				pipeline.addLast("decoder", new HttpRequestDecoder());
				pipeline.addLast("encoder", new HttpResponseEncoder());
				pipeline.addLast("channel", channel);

				return pipeline;
			}
		};
	}
	
	@Override
	public void preStart() throws Exception {
		
		ZKClient.get().createIfNotExist(Constants.Base.ZK_CHROOT);
		ZKClient.get().createIfNotExist(zkRegisterPath() );
				
		module = new WorkerModule();
		
		this.addHandler(new WorkerJobRequest(module));
		this.addHandler(new WorkerStatusRequest(module));
		
		module.init(zkRegisterPath(), getServerAddress());
		


	}

	@Override
	public void postStop() throws Exception {
		module.shutdown();
		
	}
	public static void main(String[] args) {
		WorkerServer server = new WorkerServer();
		server.start();
	}
}
