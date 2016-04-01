package com.ibm.cstl.sport.aa.client.rtc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.cstl.sport.aa.common.rtc.RTCData;
import com.ibm.team.apt.internal.client.IIterationPlanClient;
import com.ibm.team.apt.internal.client.IterationPlanClient;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.IRole;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IQueryClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.IAuditableCommonProcess;
import com.ibm.team.workitem.common.IQueryCommon;

@SuppressWarnings("restriction")
public class RTCClientData extends RTCData {
	
	public static final String PLUGIN_ID = "com.ibm.cstl.sport.aa.client";
	
	private IProjectArea projectArea;
	private IAuditableClient auditableClient;
	private IWorkItemClient workItemClient;
	private IProcessItemService processItemService;
	private IQueryClient queryClient;
    private IterationPlanClient iterationPlanClient;
    private IContributor contributor;
    private Map<String, IContributor> userCache = new HashMap<String, IContributor>();
	
	public RTCClientData(IProjectArea projectArea) throws Exception{
		this.projectArea = projectArea;
		ITeamRepository teamRepository = (ITeamRepository) projectArea.getOrigin();
		this.contributor = teamRepository.loggedInContributor();
		userCache.put(this.contributor.getUserId(), this.contributor);
		this.auditableClient = (IAuditableClient) teamRepository.getClientLibrary(IAuditableClient.class);
		IAuditableCommonProcess auditableCommonProcess = auditableClient.getProcess(this.projectArea, null );
		List<IRole> roles = auditableCommonProcess.getContributorRoles(this.contributor, this.projectArea, null);
		boolean hasRole = false;
		for(IRole role : roles){
			if(AA_ROLE_ID.equals(role.getId())){
				hasRole = true;
				break;
			}
		}
		if(!hasRole){
			throw new Exception(NO_AUTHENTICATED_USER_ERROR);
		}
		this.iterationPlanClient = (IterationPlanClient) teamRepository.getClientLibrary(IIterationPlanClient.class);
		this.workItemClient = (IWorkItemClient) teamRepository.getClientLibrary(IWorkItemClient.class);
		this.queryClient = (IQueryClient) teamRepository.getClientLibrary(IQueryClient.class);
		this.processItemService = (IProcessItemService) teamRepository.getClientLibrary(IProcessItemService.class);
		resolveProjectConfig();
	}

	@Override
	public IProjectArea getProjectArea() {
		
		return this.projectArea;
	}

	@Override
	public IAuditableCommon getAuditableCommon() {
		return this.auditableClient;
	}

	@Override
	public IWorkItemClient getWorkItemCommon() {
		return this.workItemClient;
	}

	@Override
	public IQueryCommon getQueryCommon() {
		return this.queryClient;
	}
	
	public IProcessItemService getProcessItemService(){
		return this.processItemService;
	}
	
	public IterationPlanClient getIterationPlanClient(){
		return this.iterationPlanClient;
	}

	@Override
	public IContributor getContributor() {
		return this.contributor;
	}
	
	public IContributor getUserById(String id) {
		return userCache.get(id);
	}
	
	public IContributor addUser(IContributor contributor) {
		userCache.put(contributor.getUserId(), contributor);
		return contributor;
	}

}
