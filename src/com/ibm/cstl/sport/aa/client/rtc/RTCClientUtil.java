package com.ibm.cstl.sport.aa.client.rtc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.cstl.sport.aa.common.retain.QueryData;
import com.ibm.cstl.sport.aa.common.rtc.Component;
import com.ibm.cstl.sport.aa.common.rtc.Project;
import com.ibm.cstl.sport.aa.common.rtc.RTCUtil;
import com.ibm.retain.sdi.dataflags.AparData;
import com.ibm.team.apt.common.IIterationPlanRecord;
import com.ibm.team.apt.internal.client.IterationPlanClient;
import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IDevelopmentLine;
import com.ibm.team.process.common.IIteration;
import com.ibm.team.process.common.IIterationHandle;
import com.ibm.team.process.common.IProcessItem;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IContributorManager;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.UUID;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IDetailedStatus;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.model.ISubscriptions;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.model.ItemProfile;

@SuppressWarnings("restriction")
public abstract class RTCClientUtil extends RTCUtil {
	
	private RTCClientData rtcData;
	
	public RTCClientUtil(RTCClientData rtcData) {
		this.rtcData = rtcData;
	}

	@Override
	public RTCClientData getRTCData() {
		return this.rtcData;
	}
	
	@Override
	public void deleteWorkItem(IWorkItem workItem, IProgressMonitor monitor) throws Exception {
		IWorkItemClient workItemClient = rtcData.getWorkItemCommon();
		workItemClient.getWorkItemWorkingCopyManager().connect(workItem, IWorkItem.SMALL_PROFILE, monitor);
		WorkItemWorkingCopy wc = workItemClient.getWorkItemWorkingCopyManager().getWorkingCopy(workItem);
		wc.delete(monitor);
	}
	
	@Override
	public void updateWorkItemSubscribers(Component component, IWorkItem workItem, IProgressMonitor monitor) throws Exception {
		IWorkItemClient workItemClient = rtcData.getWorkItemCommon();
		workItemClient.getWorkItemWorkingCopyManager().connect(workItem, IWorkItem.DEFAULT_PROFILE, monitor);
		WorkItemWorkingCopy wc = workItemClient.getWorkItemWorkingCopyManager().getWorkingCopy(workItem);
		if(resolveSubscribers(component, wc.getWorkItem())){
			wc.save(null);
		}
	}
	
	@Override
	public IWorkItem createAPARWorkItem(AparData aparData, Component component) throws Exception {
		IProjectArea projectArea=rtcData.getProjectArea();
		IWorkItemClient workItemClient = rtcData.getWorkItemCommon();
		IWorkItemType workItemType = workItemClient.findWorkItemType(projectArea,APAR_WORKITEM_TYPE_ID, null);
		IWorkItemHandle handle = workItemClient.getWorkItemWorkingCopyManager().connectNew(workItemType, null);
		WorkItemWorkingCopy wc = workItemClient.getWorkItemWorkingCopyManager().getWorkingCopy(handle);
		IWorkItem workItem = wc.getWorkItem();
		setValueToAttributes(workItem, aparData, component);
		IDetailedStatus s = wc.save(null);
		return s.isOK() ? workItem : null;
	}

	@Override
	public IWorkItem updateAPARWorkItem(IWorkItem workItem, AparData aparData, Component component) throws Exception {
		IWorkItemClient workItemClient = rtcData.getWorkItemCommon();
		WorkItemWorkingCopy wc = workItemClient.getWorkItemWorkingCopyManager().getWorkingCopy(workItem);
		setValueToAttributes(wc.getWorkItem(), aparData, component);
		wc.save(null);
		IDetailedStatus s = wc.save(null);
		return s.isOK() ? workItem : null;
	}
	
	@Override
	public boolean resolveSubscribers(Component component,
			IWorkItem workItem) throws TeamRepositoryException{
		ITeamRepository teamRepository = (ITeamRepository) rtcData.getProjectArea().getOrigin();
		IContributorManager contributorManager = teamRepository.contributorManager();
		ISubscriptions subscriptions = workItem.getSubscriptions();
		List<String> subscribers = component.getProject().getSubscribers();
		List<IContributorHandle> existing = new ArrayList<IContributorHandle>();
		boolean update = false;
		if(subscriptions!=null){
			for(IContributorHandle c : subscriptions.getContents()) {
				existing.add(c);
			}
			Set<UUID> ids = new HashSet<UUID>();
			for(String userid : subscribers){
				IContributor contributor = rtcData.getUserById(userid);
				if(contributor == null){
					contributor = rtcData.addUser(contributorManager.fetchContributorByUserId(userid, null));
				}
				ids.add(contributor.getItemId());
				if(!subscriptions.contains(contributor)){
					subscriptions.add(contributor);
					update = true;
				}
			}
			for(IContributorHandle c : existing){
				if(!ids.contains(c.getItemId())){
					subscriptions.remove(c);
					update = true;
				}
			}
		}
		return update;
	}

	@Override
	public void resolveIterationPlans(QueryData queryData) throws Exception {
		IProjectArea projectArea = rtcData.getProjectArea();
		ITeamRepository repository = (ITeamRepository) projectArea.getOrigin();
		IProcessItemService processItemService= rtcData.getProcessItemService();
		IAuditableClient auditableClient = (IAuditableClient) rtcData.getAuditableCommon();
		IterationPlanClient iterationPlanClient = rtcData.getIterationPlanClient();
		IDevelopmentLine defaultDevelopmentLine = auditableClient.findDefaultDevelopmentLine(projectArea, null);
		IDevelopmentLine developmentLine = null;
		IProcessItem[] items = null;
		if(defaultDevelopmentLine==null){
			developmentLine = (IDevelopmentLine) IDevelopmentLine.ITEM_TYPE.createItem();
			IProjectArea projectAreaWC = (IProjectArea) projectArea.getWorkingCopy();
			developmentLine.setProjectArea(projectAreaWC);
			projectAreaWC.addDevelopmentLine(developmentLine);
			projectAreaWC.setProjectDevelopmentLine(developmentLine);
			developmentLine.setId(rtcData.getDevelopementLineId());
		    developmentLine.setName(rtcData.getDevelopementLineName());
			items = processItemService.save(new IProcessItem[] {developmentLine, projectAreaWC}, null);
			
			projectArea = (IProjectArea)repository.itemManager().fetchCompleteItem(items[1].getItemHandle(), IItemManager.DEFAULT, null);
		}else{
			developmentLine = (IDevelopmentLine) defaultDevelopmentLine.getWorkingCopy();
			developmentLine.setId(rtcData.getDevelopementLineId());
		    developmentLine.setName(rtcData.getDevelopementLineName());
		    items = processItemService.save(new IProcessItem[] {developmentLine}, null);
		}
	    developmentLine = (IDevelopmentLine)repository.itemManager().fetchCompleteItem(items[0].getItemHandle(), IItemManager.DEFAULT, null);
	    developmentLine = (IDevelopmentLine) developmentLine.getWorkingCopy();
	    IIterationHandle[] iterations = developmentLine.getIterations();
	    Map<String, IIteration> existingProjectIterations = new HashMap<String, IIteration>();
	    rtcData.clearIterations();
	    for(IIterationHandle iterationHandle : iterations){
	    	IIteration it = (IIteration) auditableClient.fetchCurrentAuditable(iterationHandle, ItemProfile.createFullProfile(IIteration.ITEM_TYPE), null);
	    	String id = it.getId();
			existingProjectIterations.put(id,it);
	    	rtcData.addIteration(id, it);
	    }
	    List<Project> projects = queryData.getProjects();
	    for(Project project : projects){
	    	IIteration projectIteration = null;
	    	String projectName = project.getName();
	    	if(!existingProjectIterations.keySet().contains(projectName)){
	    		projectIteration = (IIteration) IIteration.ITEM_TYPE.createItem();
	    		projectIteration.setName(projectName);
	    		projectIteration.setId(projectName);
	    		projectIteration.setHasDeliverable(true);
	    		projectIteration.setDevelopmentLine(developmentLine);
	    		developmentLine.addIteration(projectIteration);
	        	items = processItemService.save(new IProcessItem[]{developmentLine, projectIteration}, null);
	        	developmentLine = (IDevelopmentLine)repository.itemManager().fetchCompleteItem(items[0].getItemHandle(), IItemManager.DEFAULT, null);
	        	developmentLine = (IDevelopmentLine) developmentLine.getWorkingCopy();
	        	projectIteration = (IIteration)repository.itemManager().fetchCompleteItem(items[1].getItemHandle(), IItemManager.DEFAULT, null);
	        	rtcData.addIteration(projectName, projectIteration);
	    	}else{
	    		projectIteration = existingProjectIterations.get(projectName);
	    	}
	    	projectIteration = (IIteration) projectIteration.getWorkingCopy();
	    	for(IIterationHandle iterationHandle : projectIteration.getChildren()){
	    		IIteration it = (IIteration) auditableClient.fetchCurrentAuditable(iterationHandle, ItemProfile.createFullProfile(IIteration.ITEM_TYPE), null);
	    		rtcData.addIteration(it.getId(), it);
	    	}
	    	int start = queryData.getFromY()*10 + queryData.getFromQ();
	    	int end = queryData.getToY()*10 + queryData.getToQ();
	    	int pre = -1;
	    	IIteration quarterIteration = null;
	    	for(int m = start; m <= end; m++){
	    		if(m<rtcData.getCreatePlanAfter() || m%10>4 || m%10==0){
	    			continue;
	    		}
	    		if(m%10==1){
	    			pre = (m/10-1)*10 + 4;
	    		}else{
	    			pre = m - 1;
	    		}
	    		String id = projectName + String.valueOf(m);
	    		String name = m/10 + "Q" + m%10;
				if(!rtcData.containsIteration(id)){
					quarterIteration = (IIteration) IIteration.ITEM_TYPE.createItem();
	        		quarterIteration.setName(name);
	        		quarterIteration.setId(id);
	        		quarterIteration.setHasDeliverable(true);
	        		quarterIteration.setParent(projectIteration);
					String preId = projectName + String.valueOf(pre);
					if(!rtcData.containsIteration(preId)){
						projectIteration.insertChildAfter(quarterIteration, null);
					}else{
						projectIteration.insertChildAfter(quarterIteration, rtcData.getIteration(preId));
					}
					quarterIteration.setDevelopmentLine(developmentLine);
					items = processItemService.save(new IProcessItem[]{developmentLine,projectIteration,quarterIteration}, null);
					developmentLine = (IDevelopmentLine)repository.itemManager().fetchCompleteItem(items[0].getItemHandle(), IItemManager.DEFAULT, null);
		        	developmentLine = (IDevelopmentLine) developmentLine.getWorkingCopy();
		        	projectIteration = (IIteration)repository.itemManager().fetchCompleteItem(items[1].getItemHandle(), IItemManager.DEFAULT, null);
		        	projectIteration = (IIteration) projectIteration.getWorkingCopy();
		        	quarterIteration = (IIteration)repository.itemManager().fetchCompleteItem(items[2].getItemHandle(), IItemManager.DEFAULT, null);
		        	rtcData.addIteration(id, quarterIteration);
					IIterationPlanRecord plan = (IIterationPlanRecord) IIterationPlanRecord.ITEM_TYPE.createItem();
				    plan.setName(projectName +" - "+name);
				    plan.setOwner(projectArea);
				    plan.setIteration(quarterIteration);
				    iterationPlanClient.create(plan, XMLString.EMPTY, new NullProgressMonitor());
	    		}
	    	}
		}
	}

	@Override
	public boolean isClient() {
		return true;
	}

}
