package org.shanbo.feluca.node.job;

import java.util.HashMap;
import java.util.Map;

import org.shanbo.feluca.node.job.subjob.LeaderSleepJob;
import org.shanbo.feluca.node.job.subjob.SubJobAllocator;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * basic 
 * @author lgn
 *
 */
public class JobUtil {

	private static Map<String, SubJobAllocator> SUBJOBS = new HashMap<String, SubJobAllocator>();
	

	private static void addJob(SubJobAllocator job){
		SUBJOBS.put(job.getName(), job);
	}
	
	static{
		addJob(new LeaderSleepJob());
	}
		
	
	
	public static JSONArray allocateSubJobs(JSONObject udConf){
		return SUBJOBS.get(udConf.get("task")).allocateSubJobs(udConf);
	}
}