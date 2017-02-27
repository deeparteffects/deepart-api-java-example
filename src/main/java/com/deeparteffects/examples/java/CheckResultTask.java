package com.deeparteffects.examples.java;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.deeparteffects.sdk.java.DeepArtEffectsClient;
import com.deeparteffects.sdk.java.model.GetResultRequest;
import com.deeparteffects.sdk.java.model.GetResultResult;

public class CheckResultTask extends TimerTask {
	
	private static final Logger logger = LoggerFactory.getLogger(CheckResultTask.class);
	
	private DeepArtEffectsClient client;
    private String submissionId;
    private CheckResultListener checkResultListener;

    public CheckResultTask(DeepArtEffectsClient client, String submissionId, CheckResultListener listener) {
    	this.client= client; 
    	this.submissionId = submissionId;
    	this.checkResultListener = listener;
    }

    @Override
    public void run() {
        try {
        	GetResultRequest getResultRequest = new GetResultRequest();
        	getResultRequest.setSubmissionId(submissionId);
        	GetResultResult result = client.getResult(getResultRequest);
            String submissionStatus = result.getResult().getStatus();
            logger.debug(String.format("Submission Status is %s.", submissionStatus));
            
            if (submissionStatus.equals(SubmissionStatus.FINISHED)) {
            	this.checkResultListener.onFinish(result.getResult().getUrl());
                cancel();
            }
        } catch (Exception e) {
        	this.checkResultListener.onError();
            cancel();
        }
    }
    
    public interface CheckResultListener {
    	void onFinish(String url);
    	void onError();
    }
}
