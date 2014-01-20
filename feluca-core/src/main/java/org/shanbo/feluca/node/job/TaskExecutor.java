package org.shanbo.feluca.node.job;

import org.shanbo.feluca.node.job.FelucaJob.JobState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * execute task through running another process
 * @author lgn
 *
 */
public abstract class TaskExecutor {
	
	protected JobState state;
	protected Logger log ;
	public TaskExecutor(JSONObject conf) {
		log = LoggerFactory.getLogger(this.getClass());
	}
	
	/**
	 * <li>invoke by FelucaJob</li>
	 * <li>create a list interpret the subjob's steps & concurrent-level</li>
	 * <li>format: [[{type:local, <b>task:xxx</b>, param:{xxx}},{},{concurrent-level}],[]... [steps]]</li>
	 * @return
	 */
	public abstract JSONArray parseConfForJob(JSONObject param);
	
	public abstract String getTaskName();
	
	public abstract void execute();
	
	public abstract void kill();
	
	public JobState currentState(){
		//get state through Process
		return  state;
	}
	
	public static JSONObject baseConfTemplate(boolean isLocal){
		JSONObject conf = new JSONObject();
		conf.put("type", isLocal?"local":"distrib");
		conf.put("param", new JSONObject());
		return conf;
	}
	
}