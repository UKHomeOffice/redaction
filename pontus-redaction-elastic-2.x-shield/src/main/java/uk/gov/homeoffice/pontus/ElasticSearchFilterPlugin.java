package uk.gov.homeoffice.pontus;

/**
 * Created by leo on 04/11/2016.
 */
/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.ActionModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;

/**
 * Example of a plugin.
 */

public class ElasticSearchFilterPlugin extends Plugin {

    public final static String ENABLE_PLUGIN_CONF = "pontus.enablePlugin";

    private final Settings settings;
    private boolean tribeNodeClient;
    protected static final Log LOG = LogFactory.getLog(ElasticSearchFilterPlugin.class);

    public ElasticSearchFilterPlugin(Settings settings) {
        this.settings = settings;
        tribeNodeClient = this.settings.get("tribe.name", null) != null;

    }

    @Override
    public String name() {
        return "pontus_pole_redaction";
    }

    @Override
    public String description() {
        return "Common Data Platform Person, Object, Location, Event (POLE) security filter to redact unauthorized data";
    }

    @Override
    public Settings additionalSettings() {
        return Settings.EMPTY;
    }


    public void onModule(final ActionModule module) {

//        if(!tribeNodeClient) {
//            module.registerAction(ConfigUpdateAction.INSTANCE, TransportConfigUpdateAction.class);
//            if (!client) {
//            }
//        }
        if (Boolean.parseBoolean(System.getProperties().getProperty(ENABLE_PLUGIN_CONF, "false"))) {
          try {
            Class clazz = getClass().getClassLoader().loadClass("uk.gov.homeoffice.pontus.ElasticSearchFilter");
            module.registerFilter(clazz);
            LOG.info("Registered gov.uk.homeoffice.pontus.ElasticSearchFilter");

          } catch (ClassNotFoundException e) {
            LOG.info("Failed to Register gov.uk.homeoffice.pontus.ElasticSearchFilter:", e);
            System.exit(-1);
          }

        }
    }

}