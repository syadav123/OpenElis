package us.mn.state.health.lims.upload.sample;

import org.apache.commons.lang3.StringUtils;
import org.bahmni.csv.EntityPersister;
import org.bahmni.csv.RowResult;
import us.mn.state.health.lims.healthcenter.dao.HealthCenterDAO;
import us.mn.state.health.lims.healthcenter.daoimpl.HealthCenterDAOImpl;
import us.mn.state.health.lims.healthcenter.valueholder.HealthCenter;
import us.mn.state.health.lims.samplesource.dao.SampleSourceDAO;
import us.mn.state.health.lims.samplesource.daoimpl.SampleSourceDAOImpl;
import us.mn.state.health.lims.samplesource.valueholder.SampleSource;
import us.mn.state.health.lims.test.dao.TestDAO;
import us.mn.state.health.lims.test.daoimpl.TestDAOImpl;
import us.mn.state.health.lims.test.valueholder.Test;
import us.mn.state.health.lims.upload.service.TestResultPersisterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestResultPersister implements EntityPersister<CSVSample> {
    private HealthCenterDAO healthCenterDAO;
    private TestResultPersisterService testResultPersisterService;
    private List<String> healthCenterCodes;

    private List<String> testNames;
    private SampleSourceDAO sampleSourceDAO;
    private ArrayList<String> sampleSourceNames;
    private TestDAO testDAO;

    public TestResultPersister() {
        this(new HealthCenterDAOImpl(), new SampleSourceDAOImpl(), new TestDAOImpl(), new TestResultPersisterService());
    }

    public TestResultPersister(HealthCenterDAO healthCenterDAO, SampleSourceDAO sampleSourceDAO, TestDAO testDAO, TestResultPersisterService testResultPersisterService) {
        this.healthCenterDAO = healthCenterDAO;
        this.sampleSourceDAO = sampleSourceDAO;
        this.testDAO = testDAO;
        this.testResultPersisterService = testResultPersisterService;
        sampleSourceNames = new ArrayList<>();
    }

    @Override
    public RowResult<CSVSample> persist(CSVSample csvSample) {
        return testResultPersisterService.persist(csvSample);
    }

    @Override
    public RowResult<CSVSample> validate(CSVSample csvSample) {
        StringBuilder errorMessage = new StringBuilder();
        if (isEmpty(csvSample.healthCenter) || !getHealthCenterCodes().contains(csvSample.healthCenter)) {
            errorMessage.append("Invalid Subcenter code.\n");
        }

        if (isEmpty(csvSample.patientRegistrationNumber))
            errorMessage.append("Registration Number is mandatory.\n");

        try {
            Integer.parseInt(csvSample.patientRegistrationNumber);
        } catch (NumberFormatException e) {
            errorMessage.append("Registration number should be a number.\n");
        }

        if (isEmpty(csvSample.sampleSource) || !getSampleSources().contains(csvSample.sampleSource)) {
            errorMessage.append("Invalid Sample source.\n");
        }

        errorMessage.append(validateTestNames(csvSample.testResults));
        errorMessage.append(validateAtLeastOneTestIsNonEmpty(csvSample.testResults));
        errorMessage.append(validateAllTestResultsAreValid(csvSample.testResults));

        if (isEmpty(csvSample.accessionNumber)) {
            errorMessage.append("AccessionNumber should not be blank.\n");
        }

        if (!csvSample.accessionNumber.matches("^[\\d-]+$")) {
            errorMessage.append("AccessionNumber format is invalid.\n");
        }

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
            simpleDateFormat.setLenient(false);
            simpleDateFormat.parse(csvSample.sampleDate);
        } catch (ParseException e) {
            errorMessage.append("Date should be in dd-mm-yyyy format and should be a valid date.\n");
        }

        if (isEmpty(errorMessage.toString()))
            return new RowResult<>(csvSample);

        return new RowResult<>(csvSample, errorMessage.toString());
    }

    private List<String> getSampleSources() {
        if (!sampleSourceNames.isEmpty()) {
            return sampleSourceNames;
        }
        List<SampleSource> sampleSources = sampleSourceDAO.getAll();
        for (SampleSource sampleSource : sampleSources) {
            sampleSourceNames.add(sampleSource.getName());
        }
        return sampleSourceNames;
    }

    private String validateAllTestResultsAreValid(List<CSVTestResult> testResults) {
        for (CSVTestResult testResult : testResults) {
            if (!testResult.isValid()) {
                return "All Tests should have a result.\n";
            }
        }
        return "";
    }

    private String validateAtLeastOneTestIsNonEmpty(List<CSVTestResult> testResults) {
        for (CSVTestResult testResult : testResults) {
            if (!testResult.isEmpty()) {
                return "";
            }
        }
        return "There should be atleast one Test with a Result.\n";
    }

    private String validateTestNames(List<CSVTestResult> testResults) {
        List<String> invalidTestNames = new ArrayList<>();
        for (CSVTestResult testResult : testResults) {
            if (!testResult.isEmpty() && !getTestNames().contains(testResult.test.toLowerCase())) {
                invalidTestNames.add(testResult.test);
            }
        }
        return invalidTestNames.isEmpty() ? "" : "Invalid test names: " + StringUtils.join(invalidTestNames.iterator(), ",") + ".\n";
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }

    private List<String> getHealthCenterCodes() {
        if (healthCenterCodes != null && !healthCenterCodes.isEmpty()) {
            return healthCenterCodes;
        }
        healthCenterCodes = new ArrayList<>();
        List<HealthCenter> healthCenters = healthCenterDAO.getAll();
        for (HealthCenter healthCenter : healthCenters) {
            healthCenterCodes.add(healthCenter.getName());
        }
        return healthCenterCodes;
    }

    public List<String> getTestNames() {
        if (testNames != null && !testNames.isEmpty()) {
            return testNames;
        }
        testNames = new ArrayList<>();
        List<Test> tests = testDAO.getAllActiveTests(false);
        for (Test test : tests) {
            testNames.add(test.getTestName().toLowerCase());
        }
        return testNames;
    }

}