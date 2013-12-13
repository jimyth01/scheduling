/*
 *  
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2013 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 *  * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.scheduler.rest;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.io.IOUtils.copy;
import static org.ow2.proactive.scheduler.job.JobIdImpl.makeJobId;
import static org.ow2.proactive.scheduler.rest.ExceptionUtility.exception;
import static org.ow2.proactive.scheduler.rest.ExceptionUtility.throwJAFEOrUJEOrNCEOrPE;
import static org.ow2.proactive.scheduler.rest.ExceptionUtility.throwNCEOrPE;
import static org.ow2.proactive.scheduler.rest.ExceptionUtility.throwNCEOrPEOrSCEOrJCE;
import static org.ow2.proactive.scheduler.rest.ExceptionUtility.throwUJEOrNCEOrPE;
import static org.ow2.proactive.scheduler.rest.ExceptionUtility.throwUJEOrNCEOrPEOrUTE;
import static org.ow2.proactive.scheduler.rest.data.DataUtility.jobId;
import static org.ow2.proactive.scheduler.rest.data.DataUtility.taskState;
import static org.ow2.proactive.scheduler.rest.data.DataUtility.toJobInfos;
import static org.ow2.proactive.scheduler.rest.data.DataUtility.toJobResult;
import static org.ow2.proactive.scheduler.rest.data.DataUtility.toJobState;
import static org.ow2.proactive.scheduler.rest.data.DataUtility.toJobUsages;
import static org.ow2.proactive.scheduler.rest.data.DataUtility.toSchedulerUserInfos;
import static org.ow2.proactive.scheduler.rest.data.DataUtility.toTaskResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.ow2.proactive.db.SortParameter;
import org.ow2.proactive.scheduler.common.JobFilterCriteria;
import org.ow2.proactive.scheduler.common.JobSortParameter;
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.scheduler.common.SchedulerStatus;
import org.ow2.proactive.scheduler.common.exception.JobAlreadyFinishedException;
import org.ow2.proactive.scheduler.common.exception.JobCreationException;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.exception.SubmissionClosedException;
import org.ow2.proactive.scheduler.common.exception.UnknownJobException;
import org.ow2.proactive.scheduler.common.exception.UnknownTaskException;
import org.ow2.proactive.scheduler.common.job.Job;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobInfo;
import org.ow2.proactive.scheduler.common.job.JobPriority;
import org.ow2.proactive.scheduler.common.job.JobResult;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.job.TaskFlowJob;
import org.ow2.proactive.scheduler.common.job.factories.Job2XMLTransformer;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.TaskState;
import org.ow2.proactive.scheduler.common.usage.JobUsage;
import org.ow2.proactive.scheduler.job.SchedulerUserInfo;
import org.ow2.proactive.scheduler.rest.data.DataUtility;
import org.ow2.proactive.scheduler.rest.data.TaskResultImpl;
import org.ow2.proactive.scheduler.rest.readers.OctetStreamReader;
import org.ow2.proactive.scheduler.rest.readers.WildCardTypeReader;
import org.ow2.proactive_grid_cloud_portal.cli.utils.HttpUtility;
import org.ow2.proactive_grid_cloud_portal.common.SchedulerRestInterface;
import org.ow2.proactive_grid_cloud_portal.scheduler.client.SchedulerRestClient;
import org.ow2.proactive_grid_cloud_portal.scheduler.dto.JobIdData;
import org.ow2.proactive_grid_cloud_portal.scheduler.dto.JobResultData;
import org.ow2.proactive_grid_cloud_portal.scheduler.dto.JobStateData;
import org.ow2.proactive_grid_cloud_portal.scheduler.dto.JobUsageData;
import org.ow2.proactive_grid_cloud_portal.scheduler.dto.SchedulerStatusData;
import org.ow2.proactive_grid_cloud_portal.scheduler.dto.SchedulerUserData;
import org.ow2.proactive_grid_cloud_portal.scheduler.dto.TaskResultData;
import org.ow2.proactive_grid_cloud_portal.scheduler.dto.TaskStateData;
import org.ow2.proactive_grid_cloud_portal.scheduler.dto.UserJobData;
import org.ow2.proactive_grid_cloud_portal.scheduler.exception.NotConnectedRestException;

public class SchedulerClient extends ClientBase implements Scheduler {

    private static final long retry_interval = TimeUnit.SECONDS.toMillis(1);

    private SchedulerRestClient schedulerRestClient;
    private String sid;

    public SchedulerClient() {
    }

    public void init(String url, String login, String pwd) throws Exception {
        HttpClient client = HttpUtility.threadSafeClient();
        SchedulerRestClient restApiClient = new SchedulerRestClient(url,
                new ApacheHttpClient4Executor(client));
        
        ResteasyProviderFactory factory = ResteasyProviderFactory.getInstance();
        factory.addMessageBodyReader(new WildCardTypeReader());
        factory.addMessageBodyReader(new OctetStreamReader());

        setApiClient(restApiClient);
        try {
            String sessionId = restApi().login(login, pwd);
            setSession(sessionId);
        } catch (Exception e) {
            // unwrap
            if (!(e instanceof UndeclaredThrowableException)) {
                throw e;
            }
            Throwable undeclared = ((UndeclaredThrowableException) e)
                    .getUndeclaredThrowable();
            if (!(undeclared instanceof InvocationTargetException)) {
                throw e;
            }
            Throwable target = ((InvocationTargetException) undeclared)
                    .getTargetException();
            if (!(target instanceof Exception)) {
                throw e;
            }
            throw ((Exception) target);
        }
    }

    @Override
    public List<JobUsage> getAccountUsage(String user, Date start, Date end)
            throws NotConnectedException, PermissionException {
        List<JobUsage> jobUsages = null;
        try {
            List<JobUsageData> jobUsageDataList = restApi().getUsageOnAccount(
                    session(), user, start, end);
            jobUsages = toJobUsages(jobUsageDataList);
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return jobUsages;
    }

    @Override
    public List<JobUsage> getMyAccountUsage(Date startDate, Date endDate)
            throws NotConnectedException, PermissionException {
        List<JobUsage> jobUsages = null;
        try {
            List<JobUsageData> jobUsageDataList = restApi()
                    .getUsageOnMyAccount(session(), startDate, endDate);
            jobUsages = toJobUsages(jobUsageDataList);
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return jobUsages;
    }

    @Override
    public void changeJobPriority(JobId jobId, JobPriority priority)
            throws NotConnectedException, UnknownJobException,
            PermissionException, JobAlreadyFinishedException {
        changeJobPriority(jobId.value(), priority);
    }

    @Override
    public void changeJobPriority(String jobId, JobPriority priority)
            throws NotConnectedException, UnknownJobException,
            PermissionException, JobAlreadyFinishedException {
        try {
            restApi().schedulerChangeJobPriorityByName(session(), jobId,
                    priority.name());
        } catch (Exception e) {
            throwJAFEOrUJEOrNCEOrPE(e);
        }
    }

    @Override
    public void disconnect() throws NotConnectedException, PermissionException {
        try {
            restApi().disconnect(session());
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
    }

    @Override
    public boolean freeze() throws NotConnectedException, PermissionException {
        boolean success = false;
        try {
            success = restApi().freezeScheduler(session());
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return success;
    }

    @Override
    public JobResult getJobResult(JobId jobId) throws NotConnectedException,
            PermissionException, UnknownJobException {
        return getJobResult(jobId.value());
    }

    @Override
    public JobResult getJobResult(String jobId) throws NotConnectedException,
            PermissionException, UnknownJobException {
        JobResult jobResult = null;
        try {
            JobResultData jobResultData = restApi().jobResult(session(), jobId);
            jobResult = toJobResult(jobResultData);
        } catch (Exception e) {
            throwUJEOrNCEOrPE(e);
        }
        return jobResult;
    }

    @Override
    public JobState getJobState(String jobId) throws NotConnectedException,
            UnknownJobException, PermissionException {
        JobState jobState = null;
        try {
            JobStateData jobStateData = restApi().listJobs(session(), jobId);
            jobState = toJobState(jobStateData);
        } catch (Exception e) {
            throwUJEOrNCEOrPE(e);
        }
        return jobState;
    }

    @Override
    public JobState getJobState(JobId jobId) throws NotConnectedException,
            UnknownJobException, PermissionException {
        return getJobState(jobId.value());
    }

    @Override
    public List<JobInfo> getJobs(int index, int range,
            JobFilterCriteria criteria,
            List<SortParameter<JobSortParameter>> arg3)
            throws NotConnectedException, PermissionException {
        List<JobInfo> jobInfos = null;
        try {
            List<UserJobData> userJobDataList = restApi().jobsinfo(session(),
                    index, range);
            jobInfos = toJobInfos(userJobDataList);
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return jobInfos;
    }

    @Override
    public String getJobServerLogs(String jobId) throws UnknownJobException,
            NotConnectedException, PermissionException {
        String jobServerLog = "";
        try {
            jobServerLog = restApi().jobServerLog(session(), jobId);
        } catch (Exception e) {
            throwUJEOrNCEOrPE(e);
        }
        return jobServerLog;
    }

    @Override
    public SchedulerStatus getStatus() throws NotConnectedException,
            PermissionException {
        SchedulerStatus status = null;
        try {
            SchedulerStatusData schedulerStatus = restApi().getSchedulerStatus(
                    session());
            status = SchedulerStatus.valueOf(schedulerStatus.name());
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return status;
    }

    @Override
    public TaskResult getTaskResult(String jobId, String taskName)
            throws NotConnectedException, UnknownJobException,
            UnknownTaskException, PermissionException {
        TaskResultImpl taskResult = null;
        try {
            TaskResultData taskResultData = restApi().taskresult(session(),
                    jobId, taskName);
            taskResult = (TaskResultImpl) toTaskResult(makeJobId(jobId),
                    taskResultData);
            if (taskResult.value() == null) {
                Serializable value = restApi().valueOftaskresult(session(),
                        jobId, taskName);
                if (value != null) {
                    taskResult.setHadException(true);
                    taskResult.setValue(value);
                }
            }

            String all = restApi().tasklog(session(), jobId, taskName);
            String out = restApi().tasklogout(session(), jobId, taskName);
            String err = restApi().tasklogErr(session(), jobId, taskName);

            taskResult.setOutput(DataUtility.toTaskLogs(all, out, err));

        } catch (Throwable t) {
            throwUJEOrNCEOrPEOrUTE(exception(t));
        }
        return taskResult;
    }

    @Override
    public TaskResult getTaskResult(JobId jobId, String taskName)
            throws NotConnectedException, UnknownJobException,
            UnknownTaskException, PermissionException {
        return getTaskResult(jobId.value(), taskName);
    }

    @Override
    public String getTaskServerLogs(String arg0, String arg1)
            throws UnknownJobException, UnknownTaskException,
            NotConnectedException, PermissionException {
        String taskLogs = "";
        try {
            taskLogs = restApi().tasklog(session(), arg0, arg1);
        } catch (Exception e) {
            throwUJEOrNCEOrPEOrUTE(e);
        }
        return taskLogs;
    }

    @Override
    public List<SchedulerUserInfo> getUsers() throws NotConnectedException,
            PermissionException {
        List<SchedulerUserInfo> schedulerUserInfos = null;
        try {
            List<SchedulerUserData> users = restApi().getUsers(session());
            schedulerUserInfos = toSchedulerUserInfos(users);
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return schedulerUserInfos;
    }

    @Override
    public List<SchedulerUserInfo> getUsersWithJobs()
            throws NotConnectedException, PermissionException {
        List<SchedulerUserInfo> schedulerUserInfos = null;
        try {
            List<SchedulerUserData> usersWithJobs = restApi().getUsersWithJobs(
                    session());
            schedulerUserInfos = toSchedulerUserInfos(usersWithJobs);
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return schedulerUserInfos;
    }

    @Override
    public boolean isConnected() {
        boolean isConnected = false;
        try {
            isConnected = restApi().isConnected(session());
        } catch (NotConnectedRestException e) {
            // ignore
        }
        return isConnected;
    }

    @Override
    public boolean kill() throws NotConnectedException, PermissionException {
        boolean isKilled = false;
        try {
            isKilled = restApi().killScheduler(session());
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return isKilled;
    }

    @Override
    public boolean killJob(JobId jobId) throws NotConnectedException,
            UnknownJobException, PermissionException {
        return killJob(jobId.value());
    }

    @Override
    public boolean killJob(String jobId) throws NotConnectedException,
            UnknownJobException, PermissionException {
        boolean isJobKilled = false;
        try {
            isJobKilled = restApi().killJob(session(), jobId);
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return isJobKilled;
    }

    @Override
    public boolean linkResourceManager(String rmUrl)
            throws NotConnectedException, PermissionException {
        boolean isLinked = false;
        try {
            isLinked = restApi().linkRm(session(), rmUrl);
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return isLinked;
    }

    @Override
    public boolean pause() throws NotConnectedException, PermissionException {
        boolean isSchedulerPaused = false;
        try {
            isSchedulerPaused = restApi().pauseScheduler(session());
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return isSchedulerPaused;
    }

    @Override
    public boolean pauseJob(JobId jobId) throws NotConnectedException,
            UnknownJobException, PermissionException {
        return pauseJob(jobId.value());
    }

    @Override
    public boolean pauseJob(String jobId) throws NotConnectedException,
            UnknownJobException, PermissionException {
        boolean isJobPaused = false;
        try {
            isJobPaused = restApi().pauseJob(session(), jobId);
        } catch (Exception e) {
            throwUJEOrNCEOrPE(e);
        }
        return isJobPaused;
    }

    @Override
    public boolean preemptTask(JobId jobId, String taskName, int restartDelay)
            throws NotConnectedException, UnknownJobException,
            UnknownTaskException, PermissionException {
        return preemptTask(jobId.value(), taskName, restartDelay);
    }

    @Override
    public boolean preemptTask(String jobId, String taskName, int restartDelay)
            throws NotConnectedException, UnknownJobException,
            UnknownTaskException, PermissionException {
        boolean isTaskPreempted = false;
        try {
            isTaskPreempted = restApi().preemptTask(session(), jobId, taskName);
        } catch (Exception e) {
            throwUJEOrNCEOrPEOrUTE(e);
        }
        return isTaskPreempted;
    }

    @Override
    public boolean removeJob(JobId arg0) throws NotConnectedException,
            UnknownJobException, PermissionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeJob(String jobId) throws NotConnectedException,
            UnknownJobException, PermissionException {
        boolean isJobRemoved = false;
        try {
            isJobRemoved = restApi().removeJob(session(), jobId);
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return isJobRemoved;
    }

    @Override
    public boolean restartTask(JobId jobId, String taskName, int restartDelay)
            throws NotConnectedException, UnknownJobException,
            UnknownTaskException, PermissionException {
        return restartTask(jobId.value(), taskName, restartDelay);
    }

    @Override
    public boolean restartTask(String jobId, String taskName, int restartDelay)
            throws NotConnectedException, UnknownJobException,
            UnknownTaskException, PermissionException {
        boolean isTaskRestarted = false;
        try {
            isTaskRestarted = restApi().restartTask(session(), jobId, taskName);
        } catch (Exception e) {
            throwUJEOrNCEOrPEOrUTE(e);
        }
        return isTaskRestarted;
    }

    @Override
    public boolean resume() throws NotConnectedException, PermissionException {
        boolean isResumed = false;
        try {
            isResumed = restApi().resumeScheduler(session());
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return isResumed;
    }

    @Override
    public boolean resumeJob(JobId jobId) throws NotConnectedException,
            UnknownJobException, PermissionException {
        return resumeJob(jobId.value());
    }

    @Override
    public boolean resumeJob(String jobId) throws NotConnectedException,
            UnknownJobException, PermissionException {
        boolean isJobResumed = false;
        try {
            isJobResumed = restApi().resumeJob(session(), jobId);
        } catch (Exception e) {
            throwUJEOrNCEOrPE(e);
        }
        return isJobResumed;
    }

    @Override
    public boolean shutdown() throws NotConnectedException, PermissionException {
        boolean isShutdown = false;
        try {
            isShutdown = restApi().killScheduler(session());
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return isShutdown;
    }

    @Override
    public boolean start() throws NotConnectedException, PermissionException {
        boolean success = false;
        try {
            success = restApi().startScheduler(session());
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return success;
    }

    @Override
    public boolean stop() throws NotConnectedException, PermissionException {
        boolean isStopped = false;
        try {
            isStopped = restApi().stopScheduler(session());
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return isStopped;
    }

    @Override
    public JobId submit(Job job) throws NotConnectedException,
            PermissionException, SubmissionClosedException,
            JobCreationException {
        JobIdData jobIdData = null;
        try {
            String jobXml = (new Job2XMLTransformer())
                    .jobToxml((TaskFlowJob) job);
            jobIdData = restApiClient().submitXml(session(),
                    IOUtils.toInputStream(jobXml, Charset.defaultCharset()));
        } catch (Exception e) {
            throwNCEOrPEOrSCEOrJCE(e);
        }
        return jobId(jobIdData);
    }

    public boolean isJobFinished(JobId jobId) throws NotConnectedException,
            UnknownJobException, PermissionException {
        return isJobFinished(jobId.toString());
    }

    public boolean isJobFinished(String jobId) throws NotConnectedException,
            UnknownJobException, PermissionException {
        return getJobState(jobId).isFinished();
    }

    public JobResult waitForJob(JobId jobId, long timeout)
            throws NotConnectedException, UnknownJobException,
            PermissionException, TimeoutException {
        return waitForJob(jobId.value(), timeout);
    }

    public JobResult waitForJob(String jobId, long timeout)
            throws NotConnectedException, UnknownJobException,
            PermissionException, TimeoutException {
        timeout += currentTimeMillis();
        while (currentTimeMillis() < timeout) {
            if (isJobFinished(jobId)) {
                return getJobResult(jobId);
            }
            if (currentTimeMillis() + retry_interval < timeout) {
                sleep(retry_interval);
            } else {
                break;
            }
        }
        throw new TimeoutException(format(
                "Timeout waiting for the job: job-id=%s", jobId));
    }

    public boolean isTaskFinished(String jobId, String taskName)
            throws UnknownJobException, NotConnectedException,
            PermissionException, UnknownTaskException {
        boolean finished = false;
        try {
            TaskStateData taskStateData = restApi().jobtasks(session(), jobId,
                    taskName);
            TaskState taskState = taskState(taskStateData);
            finished = !taskState.getStatus().isTaskAlive();
        } catch (Exception e) {
            throwUJEOrNCEOrPEOrUTE(e);
        }
        return finished;
    }

    public TaskResult waitForTask(String jobId, String taskName, long timeout)
            throws UnknownJobException, NotConnectedException,
            PermissionException, UnknownTaskException, TimeoutException {
        timeout += currentTimeMillis();
        while (currentTimeMillis() < timeout) {
            if (isTaskFinished(jobId, taskName)) {
                return getTaskResult(jobId, taskName);
            }
            if (currentTimeMillis() + retry_interval < timeout) {
                sleep(retry_interval);
            } else {
                break;
            }
        }
        throw new TimeoutException(format(
                "Timeout waiting for the task: job-id=%s, task-id=%s", jobId,
                taskName));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            // ignore
        }
    }

    public List<JobResult> waitForAllJobs(List<String> jobIds, long timeout)
            throws NotConnectedException, UnknownJobException,
            PermissionException, TimeoutException {
        long timestamp = 0;
        List<JobResult> results = new ArrayList<JobResult>();
        for (String jobId : jobIds) {
            timestamp = currentTimeMillis();
            results.add(waitForJob(jobId, timeout));
            timeout = timeout - (currentTimeMillis() - timestamp);
        }
        return results;
    }

    public Map.Entry<String, JobResult> waitForAnyJob(List<String> jobIds,
            long timeout) throws NotConnectedException, UnknownJobException,
            PermissionException, TimeoutException {
        timeout += currentTimeMillis();
        while (currentTimeMillis() < timeout) {
            for (String jobId : jobIds) {
                if (isJobFinished(jobId)) {
                    return toEntry(jobId, getJobResult(jobId));
                }
            }
            if (currentTimeMillis() + retry_interval < timeout) {
                sleep(retry_interval);
            } else {
                break;
            }
        }
        throw new TimeoutException(format(
                "Timeout waiting for any job: jobIds=%s.",
                String.valueOf(jobIds)));
    }

    public Entry<String, TaskResult> waitForAnyTask(String jobId,
            List<String> taskNames, long timeout) throws UnknownJobException,
            NotConnectedException, PermissionException, UnknownTaskException,
            TimeoutException {
        timeout += currentTimeMillis();
        while (currentTimeMillis() < timeout) {
            for (String taskName : taskNames) {
                if (isTaskFinished(jobId, taskName)) {
                    return toEntry(taskName, getTaskResult(jobId, taskName));
                }
            }
            if (currentTimeMillis() + retry_interval < timeout) {
                sleep(retry_interval);
            } else {
                break;
            }
        }
        throw new TimeoutException(format(
                "Timeout waiting for any task: job-id=%s, task-ids=%s.", jobId,
                String.valueOf(taskNames)));
    }

    public List<Entry<String, TaskResult>> waitForAllTasks(String jobId,
            List<String> taskNames, long timeout) throws UnknownJobException,
            NotConnectedException, PermissionException, UnknownTaskException,
            TimeoutException {
        long timestamp = 0;
        List<Map.Entry<String, TaskResult>> taskResults = new ArrayList<Map.Entry<String, TaskResult>>();
        for (String taskName : taskNames) {
            timestamp = currentTimeMillis();
            Entry<String, TaskResult> taskResultEntry = toEntry(taskName,
                    waitForTask(jobId, taskName, timeout));
            taskResults.add(taskResultEntry);
            timeout = timeout - (currentTimeMillis() - timestamp);
        }
        return taskResults;
    }

    public boolean pushFile(String spacename, String pathname, String filename,
            String file) throws NotConnectedException, PermissionException {
        boolean uploaded = false;
        try {
            FileInputStream is = new FileInputStream(file);
            uploaded = restApiClient().pushFile(session(), spacename, pathname,
                    filename, is);
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return uploaded;
    }

    public void pullFile(String space, String pathname, String outputFile)
            throws NotConnectedException, PermissionException {
        OutputStream os = null;
        try {
            prepareToWrite(outputFile);
            os = new FileOutputStream(outputFile);
            InputStream is = restApi().pullFile(session(), space, pathname);
            copy(is, os);
        } catch (Exception e) {
            throwNCEOrPE(e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }

    public boolean deleteFile(String space, String pathname)
            throws NotConnectedException, PermissionException {
        boolean deleted = false;
        try {
            deleted = restApi().deleteFile(session(), space, pathname);
        } catch (Exception e) {
            throwNCEOrPE(e);
        }
        return deleted;
    }

    private static void prepareToWrite(String pathname) {
        File outputFile = new File(pathname);
        if (outputFile.exists()) {
            if (!outputFile.delete()) {
                throw new RuntimeException(
                        "Cannot delete the already exisiting output file: "
                                + pathname);
            }
        }
        File parentFile = outputFile.getParentFile();
        if (!parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                throw new RuntimeException(
                        "Cannot create non-existent paraent directories of the output file: "
                                + pathname);
            }
        }

    }

    private SchedulerRestInterface restApi() {
        return schedulerRestClient.getScheduler();
    }

    private void setSession(String sid) {
        this.sid = sid;
    }

    private String session() {
        return sid;
    }

    private void setApiClient(SchedulerRestClient schedulerRestClient) {
        this.schedulerRestClient = schedulerRestClient;
    }

    private SchedulerRestClient restApiClient() {
        return schedulerRestClient;
    }

    private <K, V> Map.Entry<K, V> toEntry(final K k, final V v) {
        return new AbstractMap.SimpleEntry<K, V>(k, v);

    }
}
