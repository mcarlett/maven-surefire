package org.apache.maven.surefire.junitplatform;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.surefire.booter.ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP;
import static org.apache.maven.surefire.booter.ProviderParameterNames.TESTNG_GROUPS_PROP;
import static org.junit.platform.commons.util.StringUtils.isBlank;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.maven.surefire.dryrun.printers.DryRunPrinter;
import org.apache.maven.surefire.dryrun.printers.OutputType;
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ConsoleOutputCapture;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.TestsToRun;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.engine.Filter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

/**
 * JUnit 5 Platform Provider.
 *
 * @since 2.22.0
 */
public class JUnitPlatformProvider
    extends AbstractProvider
{
    static final String CONFIGURATION_PARAMETERS = "configurationParameters";

    private final ProviderParameters parameters;

    private final Launcher launcher;

    private final Filter<?>[] filters;

    private final Map<String, String> configurationParameters;

    public JUnitPlatformProvider( ProviderParameters parameters )
    {
        this( parameters, LauncherFactory.create() );
    }

    JUnitPlatformProvider( ProviderParameters parameters, Launcher launcher )
    {
        this.parameters = parameters;
        this.launcher = launcher;
        filters = newFilters();
        configurationParameters = newConfigurationParameters();
        Logger.getLogger( "org.junit" ).setLevel( WARNING );
    }

    @Override
    public Iterable<Class<?>> getSuites()
    {
        return scanClasspath();
    }

    @Override
    public RunResult invoke( Object forkTestSet )
                    throws TestSetFailedException, ReporterException
    {
    	TestsToRun testsToRun = null;
        if ( forkTestSet instanceof TestsToRun )
        {
        	testsToRun = (TestsToRun) forkTestSet;
        }
        else if ( forkTestSet instanceof Class )
        {
        	testsToRun = TestsToRun.fromClass( (Class<?>) forkTestSet );
        }
        else if ( forkTestSet == null )
        {
        	testsToRun = scanClasspath();
        }
        else
        {
            throw new IllegalArgumentException( "Unexpected value of forkTestSet: " + forkTestSet );
        }
        boolean isDryRun = Boolean.parseBoolean(parameters.getProviderProperties().get("dryRun"));
        if ( isDryRun ) {
        	print(testsToRun);
        	testsToRun.markTestSetFinished();
        	return RunResult.noTestsRun();
        }
        return invokeAllTests( testsToRun );
    }

	private void print(TestsToRun testsToRun) {
		String outType = parameters.getProviderProperties().get("dryRun.outType");
		boolean isPrintDebugInFile = Boolean
				.parseBoolean(parameters.getProviderProperties().get("dryRun.printDebugInFile"));
		String printFilePath = parameters.getProviderProperties().get("dryRun.printFilePath");
		OutputType out = OutputType.valueOf(outType != null ? outType.toUpperCase() : OutputType.LOG.name());
		
		//list of classes to print in the format class#method
		List<String> printTestsClasses = Arrays.stream(parameters.getProviderProperties()
				.get("dryRun.printTestsClasses").split(","))
				.map(String::trim)
				.filter(str -> !str.isEmpty())
				.collect(Collectors.toList());
		//generated extra content (list of cls#method)
		List<String> extraContent = Collections.emptyList();
		if(!printTestsClasses.isEmpty()) {
			LauncherDiscoveryRequest discoveryRequest = buildLauncherDiscoveryRequest( testsToRun );
			TestPlan plan = LauncherFactory.create().discover(discoveryRequest);
			extraContent = getTestsFromClasses(plan, printTestsClasses);
		}
		//if classes is printed in the form cls#method, the class itself is excluded from the class list
		DryRunPrinter.print(testsToRun, printTestsClasses, extraContent, parameters.getConsoleLogger(), out, isPrintDebugInFile, printFilePath);
	}
	
	private List<String> getTestsFromClasses(final TestPlan testPlan, final List<String> printTestsClasses) {	
		List<String> tests = new ArrayList<>();
		testPlan.getRoots().stream().map(testPlan::getChildren)
				.flatMap(set -> set.stream())
				.filter(cls -> printTestsClasses.contains(cls.getLegacyReportingName()))
				.forEach(cls -> {
					testPlan.getChildren(cls).stream().forEach(
							test -> {
								StringBuilder sb = new StringBuilder(cls.getLegacyReportingName());
								sb.append("#");
								sb.append(test.getLegacyReportingName());
								sb.setLength(sb.length() - 2); //remove last 2 chars '()'
								tests.add(sb.toString());
							}
					);
										
				});
		return tests;
		
	}

	private TestsToRun scanClasspath()
    {
        TestPlanScannerFilter filter = new TestPlanScannerFilter( launcher, filters );
        ScanResult scanResult = parameters.getScanResult();
        TestsToRun scannedClasses = scanResult.applyFilter( filter, parameters.getTestClassLoader() );
        return parameters.getRunOrderCalculator().orderTestClasses( scannedClasses );
    }

    private RunResult invokeAllTests( TestsToRun testsToRun )
    {
        RunResult runResult;
        ReporterFactory reporterFactory = parameters.getReporterFactory();
        try
        {
            RunListener runListener = reporterFactory.createReporter();
            ConsoleOutputCapture.startCapture( (ConsoleOutputReceiver) runListener );
            LauncherDiscoveryRequest discoveryRequest = buildLauncherDiscoveryRequest( testsToRun );
            launcher.execute( discoveryRequest, new RunListenerAdapter( runListener ) );
        }
        finally
        {
            runResult = reporterFactory.close();
        }
        return runResult;
    }

    private LauncherDiscoveryRequest buildLauncherDiscoveryRequest( TestsToRun testsToRun )
    {
        LauncherDiscoveryRequestBuilder builder =
                        request().filters( filters ).configurationParameters( configurationParameters );
        for ( Class<?> testClass : testsToRun )
        {
            builder.selectors( selectClass( testClass ) );
        }
        return builder.build();
    }

    private Filter<?>[] newFilters()
    {
        List<Filter<?>> filters = new ArrayList<>();

        getPropertiesList( TESTNG_GROUPS_PROP )
                .map( TagFilter::includeTags )
                .ifPresent( filters::add );

        getPropertiesList( TESTNG_EXCLUDEDGROUPS_PROP )
                .map( TagFilter::excludeTags )
                .ifPresent( filters::add );

        TestListResolver testListResolver = parameters.getTestRequest().getTestListResolver();
        if ( !testListResolver.isEmpty() )
        {
            filters.add( new TestMethodFilter( testListResolver ) );
        }

        return filters.toArray( new Filter<?>[ filters.size() ] );
    }

    Filter<?>[] getFilters()
    {
        return filters;
    }

    private Map<String, String> newConfigurationParameters()
    {
        String content = parameters.getProviderProperties().get( CONFIGURATION_PARAMETERS );
        if ( content == null )
        {
            return emptyMap();
        }
        try ( StringReader reader = new StringReader( content ) )
        {
            Map<String, String> result = new HashMap<>();
            Properties props = new Properties();
            props.load( reader );
            props.stringPropertyNames()
                    .forEach( key -> result.put( key, props.getProperty( key ) ) );
            return result;
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( "Error reading " + CONFIGURATION_PARAMETERS, e );
        }
    }

    Map<String, String> getConfigurationParameters()
    {
        return configurationParameters;
    }

    private Optional<List<String>> getPropertiesList( String key )
    {
        String property = parameters.getProviderProperties().get( key );
        return isBlank( property ) ? empty()
                        : of( stream( property.split( "[,]+" ) )
                                              .filter( StringUtils::isNotBlank )
                                              .map( String::trim )
                                              .collect( toList() ) );
    }
}
