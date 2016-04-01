package com.ibm.cstl.sport.aa.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Calendar;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.cstl.sport.aa.client.rtc.RTCClientData;
import com.ibm.cstl.sport.aa.client.rtc.RTCClientUtil;
import com.ibm.cstl.sport.aa.common.AAUtils;
import com.ibm.cstl.sport.aa.common.retain.QueryData;
import com.ibm.cstl.sport.aa.common.rtc.AALog;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler;
import com.ibm.team.repository.client.TeamPlatform;

public class PollerCMD {
	
	private static final String NEWLINE = System.getProperty( "line.separator" );

	public static int runCMD(String[] args, File errLog, File log){
		try{
			final CommandLineParser parser = new CommandLineParser(args);
			if(parser.isCreateRETAINLoginFile() || parser.isCreateRTCLoginFile()) {
				if(parser.isCreateRTCLoginFile()){
					createLoginFile("RTC", new File("rtc"));
				}
				if(parser.isCreateRETAINLoginFile()){
					createLoginFile("RETAIN", new File("retain"));
				}
			} else{
				Calendar cal = Calendar.getInstance();
				int year = cal.get(Calendar.YEAR);
				int quarter = cal.get(Calendar.MONTH)/3+1;
				BufferedWriter bw = null;
				if(log!=null){
					bw = new BufferedWriter(new FileWriter(log));
				}
				// PlatForm  to start up
				TeamPlatform.startup();
				try {
					// begin to login into teamPlatform 
					ITeamRepository teamRepository = TeamPlatform.getTeamRepositoryService().getTeamRepository(parser.getRepositoryURL());
					teamRepository.registerLoginHandler(new ILoginHandler() {
						@Override
						public ILoginInfo challenge(ITeamRepository repository) {
							return new ILoginInfo() {
					            public String getUserId() {
					                return parser.getRtcUserId();
					            }
					            public String getPassword() {
					                return parser.getRtcPassword();                        
					            }
					        };
						}
					});
					teamRepository.login(null);
					IProcessClientService processClient= (IProcessClientService) teamRepository.getClientLibrary(IProcessClientService.class);
					URI uri= URI.create(parser.getProjeactArea().replaceAll(" ", "%20"));
					//Login finished and get the projectArea
					IProjectArea projectArea= (IProjectArea) processClient.findProcessArea(uri, null, null);
					if (projectArea == null) {
						throw new Exception("Project area \""+ parser.getProjeactArea() +"\" not found.");
					}
			
					RTCClientData rtcData = new RTCClientData(projectArea);
					final BufferedWriter _bw = bw;
					RTCClientUtil util = new RTCClientUtil(rtcData) {
						@Override
						public AALog getLog() {
							return new AALog() {
								@Override
								public void startLog() {
								}
								@Override
								public void log(String message) {
									System.out.println(message);
									try {
										if(_bw!=null){
											_bw.write(message);
											_bw.write(NEWLINE);
											_bw.flush();
										}
									} catch (IOException e) {
									}
								}
								@Override
								public void endLog() {
								}
							};
						}
					};				
					
					
					QueryData queryData = new QueryData(rtcData.getProjects(), false, parser.getRetainId(), parser.getRetainPassword(), 2014, 1, year, quarter);
					queryData.validateRETAINUser();
					AAUtils.run(util, queryData, RTCClientData.PLUGIN_ID, new IProgressMonitor() {
						@Override
						public void worked(int arg0) {
						}
						@Override
						public void subTask(String name) {
							print(name);
						}
						@Override
						public void setTaskName(String name) {
							print(name);
						}
						@Override
						public void setCanceled(boolean arg0) {
						}
						@Override
						public boolean isCanceled() {
							return false;
						}
						@Override
						public void internalWorked(double arg0) {
						}
						@Override
						public void done() {
						}
						@Override
						public void beginTask(String name, int arg1) {
							print(name);
						}
						private void print(String name) {
					        if(name != null)
					            System.out.println(name);
					    }
					});
				} catch (Exception e) {
					String stack = AAUtils.printToString(e);
					System.err.println(stack);
					if(bw!=null){
						bw.write(stack);
						bw.flush();
					}
					return 13;
				} finally {
					if(bw!=null){
						bw.close();
					}
				}
			}
		}catch(Exception e){
			String stack = AAUtils.printToString(e);
			System.err.println(stack);
			if(errLog!=null){
				try {
					BufferedWriter bw = new BufferedWriter(new FileWriter(errLog));
					bw.write(stack);
					bw.flush();
					bw.close();
				} catch (IOException e1) {
				}
			}
			return 13;
		}
		return 0;
	}
	
	private static void createLoginFile(String client, File file) throws Exception {
		System.out.println((System.getProperty("line.separator")));
		System.out.println("*** Create " + client +" login file.");
		System.out.print(client + " User Id:");
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String user = reader.readLine();
		System.out.println("*** Warning: password will be printed to the screen.");
		System.out.print(client + " Password:");
		String password = reader.readLine();
		PasswordHelper.createPasswordFile(file, user, password);
		System.out.println(client + " Login file saved in " + file.getAbsolutePath());
	}

}
