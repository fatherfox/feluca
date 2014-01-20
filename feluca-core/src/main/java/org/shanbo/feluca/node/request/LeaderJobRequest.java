package org.shanbo.feluca.node.request;

import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.shanbo.feluca.node.JobManager;
import org.shanbo.feluca.node.RoleModule;
import org.shanbo.feluca.node.http.HttpResponseUtil;
import org.shanbo.feluca.node.http.NettyHttpRequest;
import org.shanbo.feluca.node.job.FelucaJob;
import org.shanbo.feluca.node.leader.LeaderModule;
import org.shanbo.feluca.util.DateUtil;
import org.shanbo.feluca.util.Strings;

import com.alibaba.fastjson.JSONObject;

public class LeaderJobRequest extends BasicRequest{

	public LeaderJobRequest(RoleModule module) {
		super(module);
	}

	public String getPath() {
		return "/job";
	}

	
	private void handleInfoRequest(NettyHttpRequest req, DefaultHttpResponse resp){
		String numJobs = req.param("last", "5"); //default 5
		String jobType = req.param(RoleModule.JOB_TYPE, RoleModule.JOB_LOCAL);
		String jobName = req.param("name");
		LeaderModule m = ((LeaderModule)module);
		if (!jobType.equalsIgnoreCase(RoleModule.JOB_LOCAL) || !jobType.equalsIgnoreCase(RoleModule.JOB_DISTRIB)){
			HttpResponseUtil.setResponse(resp, "kill job action", "require 'jobType' == 'local' OR 'distrib'");
			resp.setStatus(HttpResponseStatus.BAD_REQUEST);
		}else{
			boolean isLocal = jobType.equalsIgnoreCase("local")?true:false;
			if (jobName != null){
				JSONObject searchJobInfo = m.searchJobInfo(jobName, isLocal);
				if (searchJobInfo == null){
					HttpResponseUtil.setResponse(resp, " query job :" + jobName, JobManager.JOB_NOT_FOUND);
				}else{
					HttpResponseUtil.setResponse(resp, " query job :" + jobName, searchJobInfo);
				}
			}else if (numJobs != null){
				int num = new Integer(numJobs);
				HttpResponseUtil.setResponse(resp, "latest jobs", m.getLatestJobStates(num,isLocal));
			}else{
				String jobString = m.getLatestJobStates(1,isLocal).toJSONString();
				HttpResponseUtil.setResponse(resp, "feluca job status", jobString);
			}
		}
	}
	
	private  void handleJobSubmit(NettyHttpRequest req, DefaultHttpResponse resp){
		String jobClass = req.param("jobClass");
		String jobType = req.param(RoleModule.JOB_TYPE, RoleModule.JOB_LOCAL);
		LeaderModule m = (LeaderModule)this.module;

		if (!jobType.equalsIgnoreCase(RoleModule.JOB_LOCAL) || !jobType.equalsIgnoreCase(RoleModule.JOB_DISTRIB)){
			HttpResponseUtil.setResponse(resp, "kill job action", "require 'jobType' == 'local' OR 'distrib'");
			resp.setStatus(HttpResponseStatus.BAD_REQUEST);
		}else{
			boolean isLocal = jobType.equalsIgnoreCase("local")?true:false;
			JSONObject parameters = new JSONObject();
			parameters.put("jobClass", jobClass);
			String jobName = jobClass + "_" + DateUtil.getMsDateTimeFormat();
			parameters.put("jobName", jobName);
			try {
				String submitJob = m.submitJob(FelucaJob.class, parameters, isLocal);
				HttpResponseUtil.setResponse(resp, 
						Strings.keyValuesToJson("action", "submitJob", "isLocal", isLocal), 
						Strings.keyValuesToJson("jobName", submitJob, "isLocal", isLocal));
			} catch (Exception e) {
				HttpResponseUtil.setExceptionResponse(resp, Strings.keyValuesToJson("action", "submitJob", "isLocal", isLocal), 
						"submit error", e);
			}
			
			
		}
	}
	
	
	private void handleJobKill(NettyHttpRequest req, DefaultHttpResponse resp) {
		String jobName = req.param("jobName"); //default 5
		String jobType = req.param(RoleModule.JOB_TYPE, RoleModule.JOB_LOCAL);
		LeaderModule m = ((LeaderModule)module);
		if (jobName == null ){
			HttpResponseUtil.setResponse(resp, "kill job action", "require 'jobName'");
			resp.setStatus(HttpResponseStatus.BAD_REQUEST);
		
		}else if (!jobType.equalsIgnoreCase(RoleModule.JOB_LOCAL) || !jobType.equalsIgnoreCase(RoleModule.JOB_DISTRIB)){
			HttpResponseUtil.setResponse(resp, "kill job action", "require 'jobType' == 'local' OR 'distrib'");
			resp.setStatus(HttpResponseStatus.BAD_REQUEST);
		}else{
			if (jobType.equalsIgnoreCase("local"))
				HttpResponseUtil.setResponse(resp, "kill job [" + jobName + "]", m.killJob(jobName, true));
			else {
				HttpResponseUtil.setResponse(resp, "kill job [" + jobName + "]", m.killJob(jobName, false));
			}
		}
	
	}
	
	public void handle(NettyHttpRequest req, DefaultHttpResponse resp) {
		String action = req.param("action","status");
		if (action.equalsIgnoreCase("submit")){
			this.handleJobSubmit(req, resp);
		}else if (action.equalsIgnoreCase("info")) {
			this.handleInfoRequest(req, resp);
		}else if (action.equalsIgnoreCase("kill")) {
			this.handleJobKill(req, resp);
		}
	}

}