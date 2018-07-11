package org.flavin.helloWorld;

import lombok.extern.slf4j.Slf4j;
import org.flavin.config.FlowableHelloWorldTestConfig;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.FlowableRule;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = FlowableHelloWorldTestConfig.class)
@Slf4j
public class FlowableHelloWorldTest {

    @Autowired TaskService taskService;
    @Autowired ProcessEngine processEngine;
    @Autowired RuntimeService runtimeService;
    @Autowired RepositoryService repositoryService;

    @Rule public TestName testName = new TestName();

    @Autowired @Rule public FlowableRule flowableRule;

    @Before
    public void setup() throws Exception {
        log.info("Begin test " + testName.getMethodName());
    }

    @Test
    public void testSpringConfiguration() throws Exception {
        assertThat(processEngine, not(nullValue()));
    }

    @Test
    public void testDeployProcessXml() throws Exception {
        final String processXMLPath = "org/flavin/helloWorld/hello-world-with-bean.bpmn20.xml";

        log.info("Deploying process XML {}.", processXMLPath);
        final String id = repositoryService.createDeployment()
                .addClasspathResource(processXMLPath)
                .deploy()
                .getId();

        log.info("Got deployment id {}.", id);
        assertThat(id, not(isEmptyOrNullString()));
    }

    @Test
    public void testRunProcessWhichReferencesABean() throws Exception {
        final String processXMLPath = "org/flavin/helloWorld/hello-world-with-bean.bpmn20.xml";

        log.debug("Deploying process XML {}.", processXMLPath);
        final String deploymentId = repositoryService.createDeployment()
                .addClasspathResource(processXMLPath)
                .deploy()
                .getId();
        log.debug("Got deployment id {}.", deploymentId);
        assertThat(deploymentId, not(isEmptyOrNullString()));

        log.debug("Getting process definition from deployment.");
        final ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult();
        final String processId = processDefinition.getId();
        log.debug("Got process definition with id {}.", processId);


        log.debug("Before I start a process instance, let's verify that I can find the bean I will need.");
        final SpringProcessEngineConfiguration peconfig = (SpringProcessEngineConfiguration)processEngine.getProcessEngineConfiguration();

        log.debug("Does spring have the bean?");
        final Object springPrinterBean = peconfig.getApplicationContext().getBean("printer");
        assertThat(springPrinterBean, is(notNullValue()));
        log.debug("Ok, spring has the bean.");

        log.debug("Does the process engine configuration have the bean?");
        final Map<Object, Object> beans = peconfig.getBeans();
        log.debug("Checking for a bean of class Printer with name \"printer\".");
        assertThat(beans, hasKey("printer"));
        final Object printer = beans.get("printer");
        assertThat(printer.getClass().isAssignableFrom(Printer.class), is(true));
        log.debug("Verified that process engine configuration has a Printer bean named \"printer\".");

        log.info("Starting process instance from process definition.");
        final ProcessInstance processInstance = runtimeService.startProcessInstanceById(processId);

        assertThat(processInstance, not(nullValue()));
        log.debug("Started process instance. ID: " + processInstance.getId());

        verifyRunningProcessNumber(runtimeService.createProcessInstanceQuery().count(), 1);

        log.info("Getting task instance for process instance.");
        final Task task = taskService.createTaskQuery().singleResult();
        assertThat(task, not(nullValue()));
        log.debug("Task found. Name: {}", task.getName());

        log.info("Completing task.");
        taskService.complete(task.getId());

        verifyRunningProcessNumber(runtimeService.createProcessInstanceQuery().count(), 0);
    }

    @Test
    public void testRunProcess() throws Exception {
        final String processXMLPath = "org/flavin/helloWorld/single-task.bpmn20.xml";

        log.debug("Deploying process XML {}.", processXMLPath);
        final String deploymentId = repositoryService.createDeployment()
                .addClasspathResource(processXMLPath)
                .deploy()
                .getId();
        log.debug("Got deployment id {}.", deploymentId);
        assertThat(deploymentId, not(isEmptyOrNullString()));

        log.debug("Getting process definition from deployment.");
        final ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult();
        final String processId = processDefinition.getId();
        log.debug("Got process definition with id {}.", processId);

        log.info("Starting process instance from process definition.");
        final ProcessInstance processInstance = runtimeService.startProcessInstanceById(processId);

        assertThat(processInstance, not(nullValue()));
        log.debug("Started process instance. ID: " + processInstance.getId());

        verifyRunningProcessNumber(runtimeService.createProcessInstanceQuery().count(), 1);

        log.info("Getting task instance for process instance.");
        final Task task = taskService.createTaskQuery().singleResult();
        assertThat(task, not(nullValue()));
        log.debug("Task found. Name: {}", task.getName());

        log.info("Completing task.");
        taskService.complete(task.getId());

        verifyRunningProcessNumber(runtimeService.createProcessInstanceQuery().count(), 0);
    }

    private void verifyRunningProcessNumber(final long numProcesses, final long expectedNumProcesses) {
        log.debug("According to runtime service there {} {} process{} running.",
                numProcesses == 1 ? "is" : "are",
                numProcesses,
                numProcesses == 1 ? "" : "es"
        );
        assertThat(numProcesses, is(expectedNumProcesses));
    }
}
