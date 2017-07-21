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
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Example of a plugin.
 */

public class ElasticSearchFilterPlugin extends Plugin implements ActionPlugin, SearchPlugin {


    private static final String CLIENT_TYPE = "client.type";
    private static final String TRIBE_NAME = "tribe.name";

    private final Settings settings;
    private boolean tribeNodeClient;
    private boolean client;
    protected static final Log LOG = LogFactory.getLog(ElasticSearchFilterPlugin.class);

    public ElasticSearchFilterPlugin(Settings settings) {
        this.settings = settings;
        tribeNodeClient = this.settings.get(TRIBE_NAME, null) != null;
        client = !"node".equals(this.settings.get(CLIENT_TYPE, "node"));

    }

    // LPPM - 2.x
//    @Override
//    public String name() {
//        return "pontus_pole_redaction";
//    }
//
//    @Override
//    public String description() {
//        return "Common Data Platform Person, Object, Location, Event (POLE) security filter to redact unauthorized data";
//    }

    @Override
    public Settings additionalSettings() {
        return Settings.EMPTY;
    }

    // LPPM - 2.x
//
//    public void onModule(final ActionModule module) {
//
////        if(!tribeNodeClient) {
////            module.registerAction(ConfigUpdateAction.INSTANCE, TransportConfigUpdateAction.class);
////            if (!client) {
////            }
////        }
//        if (Boolean.parseBoolean(System.getProperties().getProperty(PontusElasticEmbeddedKafkaSubscriber.ENABLE_PLUGIN_CONF, "false"))) {
//            module.registerFilter(ElasticSearchFilter.class);
//            LOG.info("Registered gov.uk.homeoffice.pontus.ElasticSearchFilter");
//        }
//    }


//    @Override
//    public Collection<Class<? extends LifecycleComponent>> getGuiceServiceClasses() {
//        return Collections.emptyList();
//    }


// LPPM _ had to stop using this approach, as it was causing an exception similar to the one below:
//    java.lang.IllegalArgumentException: action for name [indices:data/read/search] already registered
//    at org.elasticsearch.common.NamedRegistry.register(NamedRegistry.java:48)
//    at org.elasticsearch.action.ActionModule$1ActionRegistry.register(ActionModule.java:355)
//    at java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:184)
//    at java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1374)
//    at java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:580)
//    at java.util.stream.ReferencePipeline$7$1.accept(ReferencePipeline.java:270)
//    at java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1374)
//    at java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:481)
//    at java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:471)
//    at java.util.stream.ForEachOps$ForEachOp.evaluateSequential(ForEachOps.java:151)
//    at java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(ForEachOps.java:174)
//    at java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
//    at java.util.stream.ReferencePipeline.forEach(ReferencePipeline.java:418)
//    at org.elasticsearch.action.ActionModule.setupActions(ActionModule.java:457)
//    at org.elasticsearch.action.ActionModule.<init>(ActionModule.java:335)
//    at org.elasticsearch.node.Node.<init>(Node.java:339)
//    at org.elasticsearch.bootstrap.Bootstrap$5.<init>(Bootstrap.java:217)
//    at org.elasticsearch.bootstrap.Bootstrap.setup(Bootstrap.java:217)
//    at org.elasticsearch.bootstrap.Bootstrap.init(Bootstrap.java:314)
//    at org.elasticsearch.bootstrap.PontusElasticEmbeddedKafkaSubscriber.init(PontusElasticEmbeddedKafkaSubscriber.java:443)
//    at org.elasticsearch.bootstrap.Elasticsearch.execute(Elasticsearch.java:112)
//    at org.elasticsearch.cli.SettingCommand.execute(SettingCommand.java:54)
//    at org.elasticsearch.cli.Command.mainWithoutErrorHandling(Command.java:96)
//    at org.elasticsearch.cli.Command.main(Command.java:62)
//    at org.elasticsearch.bootstrap.Elasticsearch.main(Elasticsearch.java:89)
//    at org.elasticsearch.bootstrap.PontusElasticEmbeddedKafkaSubscriber.<init>(PontusElasticEmbeddedKafkaSubscriber.java:270)
//    at org.elasticsearch.bootstrap.PontusElasticEmbeddedKafkaSubscriber.create(PontusElasticEmbeddedKafkaSubscriber.java:289)
//    at org.elasticsearch.bootstrap.PontusElasticEmbeddedKafkaSubscriber.main(PontusElasticEmbeddedKafkaSubscriber.java:407)
//    @Override
//    public List<ActionHandler<? extends ActionRequest<?>, ? extends ActionResponse>> getActions() {
//        List<ActionHandler<? extends ActionRequest<?>, ? extends ActionResponse>> retVal = new ArrayList<>(1);
//
//        ActionHandler<? extends ActionRequest<?>, ? extends ActionResponse> handler =
//                new ActionHandler<SearchRequest, SearchResponse>(SearchAction.INSTANCE,TransportSearchAction.class);
//
//        retVal.add(handler);
//
//        return retVal;
//    }


    @Override
    public List<Class<? extends ActionFilter>> getActionFilters() {
        List<Class<? extends ActionFilter>> filters = new ArrayList<>(1);
        if (!tribeNodeClient && !client) {
            filters.add(ElasticSearchFilter.class);
        }
        return filters;
    }


//@Override
//    public void onIndexModule(IndexModule indexModule) {
//        indexModule.setSearcherWrapper(new IndexModule.IndexSearcherWrapperFactory() {
//            @Override
//            public IndexSearcherWrapper newWrapper(IndexService indexService) {
//                return new PontusIndexSearcherWrapper(indexService,settings);
//            }
//        });
//
//    }
//
//
//
//    public class PontusIndexSearcherWrapper extends IndexSearcherWrapper {
//
//        protected final ThreadContext threadContext;
//        protected final Index index;
////        protected final String searchguardIndex;
//
//        //constructor is called per index, so avoid costly operations here
//        public PontusIndexSearcherWrapper(final IndexService indexService, Settings settings) {
//            index = indexService.index();
//            threadContext = indexService.getThreadPool().getThreadContext();
////            this.searchguardIndex = settings.get(ConfigConstants.SG_CONFIG_INDEX, ConfigConstants.SG_DEFAULT_CONFIG_INDEX);
//        }
//
//        @Override
//        public final DirectoryReader wrap(final DirectoryReader reader) throws IOException {
//
////            if (!isAdminAuthenticatedOrInternalRequest()) {
////                return dlsFlsWrap(reader);
////            }
//
//
//
//            return reader;
//        }
//
//        @Override
//        public final IndexSearcher wrap(final IndexSearcher searcher) throws EngineException {
//
////            if (isSearchGuardIndexRequest() && !isAdminAuthenticatedOrInternalRequest()) {
////                return new IndexSearcher(new EmptyReader());
////            }
////
////            if (!isAdminAuthenticatedOrInternalRequest()) {
////                return dlsFlsWrap(searcher);
////            }
//
//            return searcher;
//        }
//
//        protected IndexSearcher dlsFlsWrap(final IndexSearcher searcher) throws EngineException {
//            return searcher;
//        }
//
//        protected DirectoryReader dlsFlsWrap(final DirectoryReader reader) throws IOException {
//            return reader;
//        }
//
////        protected final boolean isAdminAuthenticatedOrInternalRequest() {
////
////            final User user = (User) threadContext.getTransient(ConfigConstants.SG_USER);
////
////            if (user != null && AdminDNs.isAdmin(user.getName())) { //TODO static hack
////                return true;
////            }
////
////            if ("true".equals(HeaderHelper.getSafeFromHeader(threadContext, ConfigConstants.SG_CONF_REQUEST_HEADER))) {
////                return true;
////            }
////
////            return false;
////        }
////
////        protected final boolean isSearchGuardIndexRequest() {
////            return index.getName().equals(searchguardIndex);
////        }
//    }


}
