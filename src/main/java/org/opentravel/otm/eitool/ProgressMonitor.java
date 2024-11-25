/**
 * Copyright (C) 2024 SkyTech Services, LLC. All rights reserved.
 */

package org.opentravel.otm.eitool;

/**
 * Interface used to track the progress of a background job.
 */
public interface ProgressMonitor {

    /**
     * Called when the background job has been initiated.
     */
    public void jobStarted(String message);

    /**
     * Called to update the observer on the progress of the background job.
     * 
     * @param percentComplete the percent complete of the job (between 0.0 and 1.0)
     * @param message the message to be displayed for the job
     */
    public void progress(double percentComplete, String message);

    /**
     * Called when the background job has completed normally.
     */
    public void jobComplete();

    /**
     * Called when the background job terminates with an error.
     * 
     * @param message the error message to be displayed
     */
    public void jobError(String message);

}
