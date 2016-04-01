package com.ibm.cstl.sport.aa.client;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import org.eclipse.core.runtime.Assert;

/**
 * 
 *   parser the arguments
 *
 */
public class CommandLineParser {

	public static final String REPOSITORY_URL_ARGUMENT_NAME = "-repositoryURL"; //$NON-NLS-1$
	public static final String PROJECT_AREA_ARGUMENT_NAME = "-projectArea"; //$NON-NLS-1$
	public static final String RTC_LOGIN_FILE_ARGUMENT_NAME = "-rtcLoginFile"; //$NON-NLS-1$
	public static final String RETAIN_LOGIN_FILE_ARGUMENT_NAME = "-retainLoginFile"; //$NON-NLS-1$
	public static final String CREATE_RTC_LOGIN_FILE_ARGUMENT_NAME = "-createRTCLoginFile"; //$NON-NLS-1$
	public static final String CREATE_RETAIN_LOGIN_FILE_ARGUMENT_NAME = "-createRETAINLoginFile"; //$NON-NLS-1$

	private String repositoryURL = null;
	private String projeactArea = null;
	private String rtcUserId = null;
	private String rtcPassword = null;
	private File rtcLoginFile = null;
	private String retainId = null;
	private String retainPassword = null;
	private File retainLoginFile = null;
	private boolean createRTCLoginFile = false;
	private boolean createRETAINLoginFile = false;
	
	public CommandLineParser(String[] args) throws Exception {
		parseArguments(args);
		validateArguments();
	}

	private void parseArguments(String[] args) {
		Assert.isNotNull(args);
		Assert.isTrue(args.length > 0, "No Arguments found!");
		for (int i = 0; i < args.length; i++) {
			String argumentName = args[i];
			if (argumentName.equalsIgnoreCase(REPOSITORY_URL_ARGUMENT_NAME)) {
                if (args.length > i + 1) {
                	repositoryURL = args[i + 1];
                    i++;
                }
            } else if (argumentName.equalsIgnoreCase(PROJECT_AREA_ARGUMENT_NAME)) {
                if (args.length > i + 1) {
                	projeactArea = args[i + 1].replaceAll("%20", " ");
                    i++;
                }
            } else if (argumentName.equalsIgnoreCase(RTC_LOGIN_FILE_ARGUMENT_NAME)) {
                if (args.length > i + 1) {
                	rtcLoginFile = new File(args[i + 1]);
                    i++;
                }
            } else if (argumentName.equalsIgnoreCase(RETAIN_LOGIN_FILE_ARGUMENT_NAME)) {
                if (args.length > i + 1) {
                	retainLoginFile = new File(args[i + 1]);
                    i++;
                }
            } else if (argumentName.equalsIgnoreCase(CREATE_RTC_LOGIN_FILE_ARGUMENT_NAME)) {
                createRTCLoginFile =true;
            } else if (argumentName.equalsIgnoreCase(CREATE_RETAIN_LOGIN_FILE_ARGUMENT_NAME)) {
                createRETAINLoginFile = true;
            }
		}
	}
	
	private void validateArguments() throws IllegalArgumentException, GeneralSecurityException, IOException {
		if(!(createRTCLoginFile||createRETAINLoginFile)){
			if(repositoryURL==null){
				throw new IllegalArgumentException("Missing argument '-repositoryURL' or it has no value specified!");
			}
			if(projeactArea==null){
				throw new IllegalArgumentException("Missing argument '-projectArea' or it has no value specified!");
			}
			if(rtcLoginFile==null){
				rtcLoginFile = new File("rtc");
			}
			String[] login = PasswordHelper.getPassword("RTC", rtcLoginFile);
			this.rtcUserId = login[0];
			this.rtcPassword = login[1];
			if(retainLoginFile==null){
				retainLoginFile = new File("retain");
			}
			login = PasswordHelper.getPassword("RETAIN", retainLoginFile);
			this.retainId= login[0];
			this.retainPassword = login[1];
		}
	}

	public String getRepositoryURL() {
		return repositoryURL;
	}

	public String getProjeactArea() {
		return projeactArea;
	}

	public String getRtcUserId() {
		return rtcUserId;
	}

	public String getRtcPassword() {
		return rtcPassword;
	}

	public String getRetainId() {
		return retainId;
	}

	public String getRetainPassword() {
		return retainPassword;
	}

	public boolean isCreateRTCLoginFile() {
		return createRTCLoginFile;
	}

	public boolean isCreateRETAINLoginFile() {
		return createRETAINLoginFile;
	}
	
}
