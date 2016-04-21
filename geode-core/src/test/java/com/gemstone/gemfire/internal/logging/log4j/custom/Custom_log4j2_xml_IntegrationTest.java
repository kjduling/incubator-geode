/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gemstone.gemfire.internal.logging.log4j.custom;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

import com.gemstone.gemfire.internal.logging.LogService;
import com.gemstone.gemfire.internal.logging.log4j.Configurator;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;

/**
 * Integration tests with custom log4j2 configuration.
 */
@Category(IntegrationTest.class)
public class Custom_log4j2_xml_IntegrationTest {

  private static final String CUSTOM_CONFIG_FILE_NAME = "log4j2.xml";

  @Rule
  public SystemErrRule systemErrRule = new SystemErrRule().enableLog();

  @Rule
  public SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private String beforeConfigFileProp;
  private Level beforeLevel;

  private File customConfigFile;

  @Before
  public void setUp() throws Exception {
    Configurator.shutdown();
    BasicAppender.clearInstance();

    this.beforeConfigFileProp = System.getProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
    this.beforeLevel = StatusLogger.getLogger().getLevel();

    URL customConfigResource = getClass().getResource(CUSTOM_CONFIG_FILE_NAME);
    File temporaryFile = this.temporaryFolder.newFile(CUSTOM_CONFIG_FILE_NAME);

    IOUtils.copy(customConfigResource.openStream(), new FileOutputStream(temporaryFile));
    assertThat(temporaryFile).hasSameContentAs(new File(customConfigResource.toURI()));

    System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, temporaryFile.getAbsolutePath());
    LogService.reconfigure();
    assertThat(LogService.isUsingGemFireDefaultConfig()).as(LogService.getConfigInformation()).isFalse();

    this.customConfigFile = temporaryFile;
  }

  @After
  public void tearDown() throws Exception {
    Configurator.shutdown();

    System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
    if (this.beforeConfigFileProp != null) {
      System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, this.beforeConfigFileProp);
    }
    StatusLogger.getLogger().setLevel(this.beforeLevel);

    LogService.reconfigure();
    assertThat(LogService.isUsingGemFireDefaultConfig()).as(LogService.getConfigInformation()).isTrue();

    BasicAppender.clearInstance();

    //assertThat(this.systemErrRule.getLog()).isEmpty();
    //assertThat(this.systemOutRule.getLog()).isEmpty();
  }

  @Test
  public void foo() throws Exception {
    String logLogger = getClass().getName();
    Level logLevel = Level.DEBUG;
    String logMessage = "this is a log statement";

    Logger logger = LogService.getLogger();
    logger.debug(logMessage);

    String systemOut = systemOutRule.getLog();
    String systemErr = systemErrRule.getLog();

    System.out.println("this.customConfigFile=" + this.customConfigFile);
    System.out.println("BasicAppender=" + BasicAppender.getInstance());
    System.out.println("CONFIGURATION_FILE_PROPERTY=" + System.getProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY));
    System.out.println("out=" + systemOut.trim());

    BasicAppender appender = BasicAppender.getInstance();
    assertThat(appender).isNotNull();
    assertThat(appender.events()).hasSize(1);

    LogEvent event = appender.events().get(0);
    System.out.println("event=" + event);

    assertThat(event.getLoggerName()).isEqualTo(logLogger);
    assertThat(event.getLevel()).isEqualTo(logLevel);
    assertThat(event.getMessage().getFormattedMessage()).isEqualTo(logMessage);

    assertThat(systemOut).contains(logLevel.name());
    assertThat(systemOut).contains(logMessage);
  }
}
