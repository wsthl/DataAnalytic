package com.ibm.cstl.sport.aa.client.rcp;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import com.ibm.cstl.sport.aa.client.PollerCMD;


public class PollApplication implements IApplication {
	

	@Override
	public Object start(IApplicationContext context) throws Exception {
		String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		Date date = new Date();
		String timestamp = new SimpleDateFormat("yyyyMMdd_hhmmss").format(date);
		File log = new File("poll_"+timestamp+".log");
		File errLog = new File("err_" + timestamp + ".log");
		return PollerCMD.runCMD(args, errLog, log);
	}

	@Override
	public void stop() {
	}
	
}
