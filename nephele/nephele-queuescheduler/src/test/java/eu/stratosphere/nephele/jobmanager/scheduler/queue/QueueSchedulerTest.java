/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/

package eu.stratosphere.nephele.jobmanager.scheduler.queue;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.lang.reflect.Method;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.execution.Environment;
import eu.stratosphere.nephele.execution.ExecutionState;
import eu.stratosphere.nephele.executiongraph.ExecutionGraph;
import eu.stratosphere.nephele.executiongraph.ExecutionGraphIterator;
import eu.stratosphere.nephele.executiongraph.ExecutionVertex;
import eu.stratosphere.nephele.instance.AllocatedResource;
import eu.stratosphere.nephele.instance.InstanceConnectionInfo;
import eu.stratosphere.nephele.instance.InstanceException;
import eu.stratosphere.nephele.instance.InstanceManager;
import eu.stratosphere.nephele.instance.InstanceType;
import eu.stratosphere.nephele.instance.local.LocalInstance;
import eu.stratosphere.nephele.jobgraph.JobID;
import eu.stratosphere.nephele.jobmanager.scheduler.SchedulingException;
import eu.stratosphere.nephele.jobmanager.scheduler.SchedulingListener;


/**
 * @author marrus
 *This class checks the functionality of the {@link QueueScheduler} class
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(QueueScheduler.class)
@SuppressStaticInitializationFor("eu.stratosphere.nephele.jobmanager.scheduler.queue.QueueScheduler")

public class QueueSchedulerTest {
	@Mock
	ExecutionGraph executionGraph;
	@Mock
	ExecutionVertex vertex1;
	@Mock
	ExecutionVertex vertex2;
	@Mock
	ExecutionGraphIterator graphIterator;
	@Mock
	ExecutionGraphIterator graphIterator2;
	@Mock
	SchedulingListener schedulingListener;
	@Mock
	InstanceManager instanceManager;
	@Mock 
	Environment environment;
	@Mock
	Log loggerMock;
	@Mock
	Deque<ExecutionGraph> queue;

	/**
	 * Setting up the mocks and necessary internal states
	 */
	@Before
	public void before() {
	    MockitoAnnotations.initMocks(this);
	    Whitebox.setInternalState(QueueScheduler.class, this.loggerMock);
	}
	
	/**
	 * Checks the behavior of the scheduleJob() method
	 */
	@Test
	public void testSchedulJob(){
		

		try {
			whenNew(ExecutionGraphIterator.class).withArguments(Matchers.any(ExecutionGraph.class),Matchers.anyBoolean()).thenReturn(this.graphIterator);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		when(this.graphIterator.next()).thenReturn(this.vertex1);
		when(this.graphIterator.hasNext()).thenReturn(true, true, true, true, false);
		when(this.vertex1.getExecutionState()).thenReturn(ExecutionState.CREATED);
		
		when(this.vertex1.getEnvironment()).thenReturn(this.environment);
		
		
		QueueScheduler toTest = new QueueScheduler(this.schedulingListener, this.instanceManager);
		try {
			toTest.schedulJob(this.executionGraph);
			Deque<ExecutionGraph> jobQueue = Whitebox.getInternalState(toTest, "jobQueue");
			assertEquals("Job should be in list", true, jobQueue.contains(this.executionGraph));
			verify(this.vertex1, times(4)).setExecutionState(ExecutionState.SCHEDULED);
			jobQueue.remove(this.executionGraph);
			
		} catch (SchedulingException e) {
			e.printStackTrace();
		}
		//toTest = new QueueScheduler(schedulingListener, instanceManager);
		
		when(this.graphIterator.next()).thenReturn(this.vertex2);
		when(this.graphIterator.hasNext()).thenReturn(true, true, true, true, false);
		when(this.vertex2.getEnvironment()).thenReturn(this.environment);
		when(this.vertex2.getExecutionState()).thenReturn(ExecutionState.CREATED)
											.thenReturn(ExecutionState.CREATED)
											.thenReturn(ExecutionState.CANCELLED)
											.thenReturn(ExecutionState.CREATED);
		try {
			toTest.schedulJob(this.executionGraph);
			verify(this.loggerMock).error(Matchers.anyString());
			Deque<ExecutionGraph> jobQueue = Whitebox.getInternalState(toTest, "jobQueue");
			assertEquals("Job should be in list", true, jobQueue.contains(this.executionGraph));
			verify(this.vertex2, times(4)).setExecutionState(ExecutionState.SCHEDULED);
			
		} catch (SchedulingException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks the behavior of the getVerticesReadyToBeExecuted() method
	 */
	@Test
	public void testGetVerticesReadyToBeExecuted(){
		QueueScheduler toTest = new QueueScheduler(this.schedulingListener, this.instanceManager);
		//mock iterator
		HashSet<ExecutionVertex> set = mock(HashSet.class);
		try {
			whenNew(ExecutionGraphIterator.class)
				.withArguments(Matchers.any(ExecutionGraph.class),Matchers.anyInt(),Matchers.anyBoolean(), Matchers.anyBoolean()).thenReturn(this.graphIterator);
			whenNew(HashSet.class).withNoArguments().thenReturn(set);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		when(this.graphIterator.next()).thenReturn(this.vertex1);
		when(this.graphIterator.hasNext()).thenReturn(true, true, true, true, false);
		when(this.vertex1.getExecutionState()).thenReturn(ExecutionState.ASSIGNED);
		
		//no graphs in List, empty list return
		
		toTest.getVerticesReadyToBeExecuted();
		verify(set, times(0)).add(Matchers.any(ExecutionVertex.class));
		this.queue = Whitebox.getInternalState(toTest, "jobQueue");
		
		//one graph in list, but no instances: an error should be reported
		this.queue.add(this.executionGraph);
		//when(this.executionGraph.getInstanceTypesRequiredForCurrentStage()).thenReturn(null);
		 toTest.getVerticesReadyToBeExecuted();
		verify(this.loggerMock).error(Matchers.anyString());
		
		//put some instances in list, the method should work correct 
		InstanceType instanceType = new InstanceType();
		HashMap<InstanceType, Integer> map = new HashMap<InstanceType,Integer>();
		map.put(instanceType, 3);
		//when(this.executionGraph.getInstanceTypesRequiredForCurrentStage()).thenReturn(map);
		this.queue.add(this.executionGraph);
		toTest.getVerticesReadyToBeExecuted();
		verify(set, times(4)).add(Matchers.any(ExecutionVertex.class));
		verify(this.vertex1, times(4)).setExecutionState(ExecutionState.READY);
	}
	
	
	/**
	 * Checks the behavior of the resourceAllocated() method
	 * @throws Exception
	 */
	@Test
	public void testResourceAllocated() throws Exception{
		
		QueueScheduler toTest = spy(new QueueScheduler(this.schedulingListener, this.instanceManager));
		JobID jobid = mock(JobID.class);
		AllocatedResource resource = mock(AllocatedResource.class);
		InstanceType instanceType = new InstanceType();
		InstanceConnectionInfo instanceConnectionInfo = mock(InstanceConnectionInfo.class);
		when(instanceConnectionInfo.toString()).thenReturn("");
		LocalInstance instance = spy(new LocalInstance(instanceType, instanceConnectionInfo, null, null, null));
		
		//given resource is null
		toTest.resourceAllocated(null,null);
		verify(this.loggerMock).error(Matchers.anyString());
		 
		//jobs have have been canceled
		final Method methodToMock = MemberMatcher.method(QueueScheduler.class, JobID.class);
		 PowerMockito.when(toTest, methodToMock).withArguments(Matchers.any(JobID.class)).thenReturn(null);
		 when(resource.getInstance()).thenReturn(instance);
		
		 toTest.resourceAllocated(jobid,resource);
		try {
			verify(this.instanceManager).releaseAllocatedResource(Matchers.any(JobID.class), Matchers.any(Configuration.class), Matchers.any(AllocatedResource.class));
		} catch (InstanceException e1) {
			e1.printStackTrace();
		}
		
		
		//vertex resource is null
		PowerMockito.when(toTest, methodToMock).withArguments(Matchers.any(JobID.class)).thenReturn(this.executionGraph);
		when(this.graphIterator.next()).thenReturn(this.vertex1);
		when(this.graphIterator.hasNext()).thenReturn(true, true, true, true, false);
		when(this.graphIterator2.next()).thenReturn(this.vertex1);
		when(this.graphIterator2.hasNext()).thenReturn(true, true, true, true, false);
		when(this.vertex1.getExecutionState()).thenReturn(ExecutionState.ASSIGNING);
		try {
			whenNew(ExecutionGraphIterator.class).withArguments(Matchers.any(ExecutionGraph.class),Matchers.anyBoolean()).thenReturn(this.graphIterator);
			whenNew(ExecutionGraphIterator.class).withArguments(Matchers.any(ExecutionGraph.class),Matchers.anyInt(),Matchers.anyBoolean(),Matchers.anyBoolean()).thenReturn(this.graphIterator2);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
		when(this.executionGraph.getJobID()).thenReturn(jobid);
		Deque<ExecutionGraph> jobQueue = Whitebox.getInternalState(toTest, "jobQueue");
		jobQueue.add(this.executionGraph);
		Whitebox.setInternalState(toTest, "jobQueue", jobQueue);
		when(this.vertex1.getAllocatedResource()).thenReturn(null);
		when(resource.getInstance()).thenReturn(instance);
		
		toTest.resourceAllocated(jobid,resource);
		verify(this.loggerMock).warn(Matchers.anyString());
		
		//correct walk through method
		when(this.graphIterator2.hasNext()).thenReturn(true, true, true, true, false);
		when(this.graphIterator.hasNext()).thenReturn(true, true, true, true, false);
		when(this.vertex1.getAllocatedResource()).thenReturn(resource);
		
		
		toTest.resourceAllocated(jobid,resource);
		verify(this.vertex1, times(4)).setExecutionState(ExecutionState.ASSIGNED);
		
		
		
	}
	
	
}
