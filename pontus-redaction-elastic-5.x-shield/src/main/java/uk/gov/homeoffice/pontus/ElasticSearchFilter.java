package uk.gov.homeoffice.pontus;

//import com.floragunn.searchguard.support.ConfigConstants;
//import uk.gov.homeoffice.pontus.ElasticSearchGuardUser;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RegexpQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.internal.InternalSearchHit;
import org.elasticsearch.search.internal.InternalSearchHits;
import org.elasticsearch.search.internal.InternalSearchResponse;
import org.elasticsearch.search.profile.SearchProfileShardResults;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.security.user.User;

import java.util.*;

/*
 * Copyright 2015 floragunn UG (haftungsbeschränkt)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


public class ElasticSearchFilter implements ActionFilter {

    /*
 {
  "took" : 10,
  "timed_out" : false,
  "_shards" : {
    "total" : 5,
    "successful" : 5,
    "failed" : 0
  },
  "hits" : {
    "total" : 36,
    "max_score" : 1.0,
    "hits" : [ {
      "_index" : "pontus_pole",
      "_type" : "driver_dangerous_event",
      "_id" : "AVhEQ_8fMboC8UxO1CtS",
      "_score" : 1.0,
      "_source" : {
        "val" : "-117.379483"
      }
    }, {
      "_index" : "pontus_pole",
      "_type" : "driver_dangerous_event",
      "_id" : "AVhEcxNe9zqG_c9kOI_x",
      "_score" : 1.0,
      "_source" : {
        "val" : "-117.3794833"
      }
    }
   ]
  }
}
     */

//    class ElasticSource {
//        String row;
//        String col;
//        String val;
//
//        @Override
//        public String toString() {
//            return gson.toJson(this);
//        }
//
//    }
//
//    class ElasticHit {
//        String _index; // " : "pontus_pole",
//        String _type; //" : "driver_dangerous_event",
//        String _id;  //              "_id" : "AVhEQ_8fMboC8UxO1CtS",
//        float _score; // " : 1.0,
//        ElasticSource _source;
//
//    }
//
//    class ElasticTotalHits {
//        long total;
//        long max_score;
//        LinkedList<ElasticHit> hits = new LinkedList<>();
//    }
//
//    class ElasticShards {
//        long total;
//        long successful;
//        long failed;
//    }
//
//
//    class ElasticResult {
//        long took;
//        boolean timed_out;
//        ElasticShards _shards;
//        ElasticTotalHits hits;
//
//        transient Map<String, Set<ElasticHit>> elasticHitByRowIdMap;
//
//        @Override
//        public String toString() {
//            return gson.toJson(this);
//        }
//
//        Map<String, Set<ElasticHit>> indexByRow() {
//
//            if (elasticHitByRowIdMap == null) {
//                elasticHitByRowIdMap = new HashMap<>(hits.hits.size());
//            } else {
//                for (Set<ElasticHit> hitSet : elasticHitByRowIdMap.values()) {
//                    hitSet.clear();
//                }
//                elasticHitByRowIdMap.clear();
//            }
//
//            List<String> retVal = new ArrayList<>();
//
//            LinkedList<ElasticHit> localHits = hits.hits;
//            for (int i = 0, ilen = localHits.size(); i < ilen; i++) {
//                ElasticHit hit = localHits.get(i);
//                ElasticSource source = hit._source;
//                Set<ElasticHit> hitSet = elasticHitByRowIdMap.get(source.row);
//                if (hitSet == null) {
//                    hitSet = new HashSet<>();
//                    elasticHitByRowIdMap.put(source.row, hitSet);
//                }
//                hitSet.add(hit);
//
//            }
//            return elasticHitByRowIdMap;
//
//        }
//
//        List<String> getAndRemoveRowIdsMatchingRowIds(List<String> rowIds) {
//
//            List<String> retVal = new ArrayList<>(elasticHitByRowIdMap.size());
//
//            for (int i = 0, ilen = rowIds.size(); i < ilen; i++) {
//                String rowId = rowIds.get(i);
//                for (Map.Entry<String, Set<ElasticHit>> entry : elasticHitByRowIdMap.entrySet()) {
//                    for (ElasticHit hit : entry.getValue()) {
//
//                        if (rowId.equals(hit._source.val)) {
//                            retVal.add(entry.getKey());
//                            Set<ElasticHit> hitsRemoved = elasticHitByRowIdMap.remove(entry.getKey());
//                            if (hitsRemoved != null) {
//                                for (ElasticHit ehit : hitsRemoved) {
//                                    hits.hits.remove(ehit);
//                                }
//                                hitsRemoved.clear();
//                            }
//                            break;
//                        }
//                    }
//                }
//            }
//            rowIds.clear();
//
//
//            return retVal;
//        }
//
//
//        List<String> getRowIdsMatchingRedaction(FilterData data) {
//
//            indexByRow();
//
//            List<String> retVal = new ArrayList<>(elasticHitByRowIdMap.size());
//
//            for (Map.Entry<String, Set<ElasticHit>> entry : elasticHitByRowIdMap.entrySet()) {
//                for (ElasticHit hit : entry.getValue()) {
//                    if (data.needRedaction(hit.toString())) {
//                        retVal.add(entry.getKey());
//                        break;
//                    }
//                }
//            }
//            return retVal;
//        }
//
//        public void redact(FilterData data){
//            List<String> matchingRowIds = getRowIdsMatchingRedaction(data);
//            while (matchingRowIds.size() > 0){
//                matchingRowIds = getAndRemoveRowIdsMatchingRowIds(matchingRowIds);
//            }
//        }
//
//    }


    protected static Gson gson = new Gson();
    protected static Configuration conf;
    protected static JWTStore jwtStore = null;
    protected static PolicyStore policyStore = null;
    public static final Log LOG = LogFactory.getLog(ElasticSearchFilter.class);
    protected ThreadContext threadContext;
    protected String userStr;
    protected static boolean useSearchGuard;
    protected static boolean useShield;

    protected JWTClaim claim;
    protected FilterData filterData;

    protected long start;
    protected int size;
    protected long numFound;

    protected boolean okToContinue;


    static {
        conf = new Configuration();
        conf.addResource(new Path("/usr/hdp/current/hadoop-client/conf/core-site.xml"));
        conf.addResource(new Path("/usr/hdp/current/hadoop-client/conf/hdfs-site.xml"));
        conf.addResource(new Path("/usr/hdp/current/hbase-regionserver/conf/hbase-site.xml"));




        Properties props = System.getProperties();
        Set<Object> keys = props.keySet();

        for (Object key : keys) {
            if (key instanceof String) {
                String keyStr = (String) key;
                String valStr = props.getProperty(keyStr);
                conf.set(keyStr, valStr);
            }
        }


        try {
            useSearchGuard = conf.getBoolean("elastic.use.searchguard", true);
            useShield = conf.getBoolean("elastic.use.shield", false);
            jwtStore = JWTStore.create(conf);
            policyStore = PolicyStore.createAndStart(conf);


            final Configuration config = new Configuration(false);
            config.addResource(new Path("/usr/hdp/current/hadoop-client/conf/core-site.xml"));
            config.addResource(new Path("/usr/hdp/current/hadoop-client/conf/hdfs-site.xml"));

            config.set("hadoop.security.authentication", "Kerberos");

            config.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
            config.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");

            UserGroupInformation.setConfiguration(config);
            UserGroupInformation userGroupInformation =
            UserGroupInformation.loginUserFromKeytabAndReturnUGI("hdfs-sandbox@YOUR_REALM_GOES_HERE",
                            "/etc/security/keytabs/hdfs.headless.keytab" );
            UserGroupInformation.setLoginUser(userGroupInformation);


        } catch (Throwable e) {
            LOG.error("Failed to create jwtStore or policyStore", e);
        }

    }


// "internal:*",
// "indices:monitor/*",
// "cluster:monitor/*",
// "cluster:admin/reroute",
// "indices:admin/mapping/put"

    //    protected ElasticSearchGuardUser user;
//    private final Provider<PrivilegesEvaluator> evalp;
    private final Settings settings;

    @Inject
    public ElasticSearchFilter(final Settings settings,  ThreadPool threadPool) {
        this.settings = settings;
        this.threadContext = threadPool.getThreadContext();
    }



    @Override
    public int order() {
        return 0;
    }

    @Override
    public void apply(Task task, final String action, final ActionRequest request, final ActionListener listener, final ActionFilterChain chain) {

        // - types testen
        // - remote address testn
//        chain.proceed(task, action, request, listener);
//        if (true) {
//            okToContinue = true;
//            return;
//        }
//        chain.proceed(task, action, request, listener);
//        if (true)
//          return;
        okToContinue = false;


        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace(String.format("Action %s from %s/%s", action, request.remoteAddress(), listener.getClass().getSimpleName()));
//                LOG.trace(String.format("Context %s", request.getContext()));
//                LOG.trace(String.format("Header %s", request.getHeaders()));

            }


            if (useShield) {
                // LPPM Elastic 2.x needed this...

                User elasticSearchGuardUser = (User) threadContext.getTransient("_shield_user");
                if (elasticSearchGuardUser == null && request.remoteAddress() == null) {
//                    elasticSearchGuardUser = ;
                }
                if (elasticSearchGuardUser != null) {
                    userStr = elasticSearchGuardUser.principal();
                } else {
                    userStr = "_shield_internal";
                }
            } else {
                userStr = "UNKNOWN";
            }

            LOG.info("in Apply(); user = " + userStr);

            //LogHelper.logUserTrace("--> Action {} from {}/{}", action, request.remoteAddress(), listener.getClass().getSimpleName());
            //LogHelper.logUserTrace("--> Context {}", request.getContext());
            //LogHelper.logUserTrace("--> Header {}", request.getHeaders());

//            if (LOG.isTraceEnabled()) {
//                LOG.trace("remote address: {}", threadContext.getTransient(ConfigConstants.SG_REMOTE_ADDRESS));
//            }


            if (userStr.equals("_sg_internal")) {

                //@formatter:off
                if (action.startsWith("internal:gateway")
                        || action.startsWith("cluster:monitor/")
                        || action.startsWith("indices:monitor/")
                        || action.startsWith("cluster:admin/reroute")
                        || action.startsWith("indices:admin/mapping/put")
                        || action.startsWith("internal:cluster/nodes/indices/shard/store")
                        || action.startsWith("indices:admin/exists")
                        || action.startsWith("indices:data/read/mget")
                        || action.startsWith("internal:indices/admin/upgrade")
                        || action.startsWith("internal:gateway/local/meta_state")
                        ) {

//                    if (log.isInfoEnabled()) {
                    LOG.info("No user, will allow only standard discovery and monitoring actions");
//                    }
                    okToContinue = true;

                    chain.proceed(task, action, request, listener);
                    return;
                } else {
                    LOG.info(String.format("unauthenticated request %s for user %s", action, userStr));
                    listener.onFailure(new ElasticsearchSecurityException("unauthorized request " + action + " for user " + userStr, RestStatus.FORBIDDEN));
                    return;
                }
                //@formatter:on
            }
        } catch (Throwable e) {
            LOG.error("Failed to process filters for user " + userStr, e);

        }

        try {
            claim = jwtStore.getUserClaim(new JWTUser(userStr));
            filterData = policyStore.getFilterData(claim.getBizctx());

            if (request instanceof SearchRequest){
                SearchRequest searchReq = (SearchRequest)request;
                SearchSourceBuilder ssb = searchReq.source();

                RegexpQueryBuilder rqb = new RegexpQueryBuilder("rmdq",filterData.getMetadataRegexStr());

                QueryBuilder origPost = ssb.postFilter();

                BoolQueryBuilder newQb = new BoolQueryBuilder();
                if (origPost != null) {
                    newQb.must(origPost);
                }

                // LPPM - TODO: RE-enable the next two lines when our records are stored properly, and when we have the
                // getRedactionType() working:

//                newQb.must(rqb);
                //if (FilterData.RedactionType.FILTER == filterData.getRedactionType())
                {
                    newQb.must(new RegexpQueryBuilder("val",filterData.getRedactionAllowedStr()))
                         .mustNot(new RegexpQueryBuilder("val",filterData.getRedactionDeniedStr()))
                         .mustNot(new RegexpQueryBuilder("val",filterData.getRedactionDeniedAllStr()));
                }

                ssb.postFilter(newQb);



            }

            chain.proceed(task, action, request, listener);

        } catch (Throwable e) {
            LOG.error("Failed to retrieve claim for user " + userStr, e);
            listener.onFailure(new ElasticsearchSecurityException("unauthorized request " + action + " for user " + userStr, RestStatus.FORBIDDEN));

        }


    }

    public SearchResponse applyRedactionHitRemoval(SearchResponse sr){
        InternalAggregations aggregations = null;
        Aggregations aggrs = sr.getAggregations();
        if (aggrs != null) {
            List<Aggregation> aggrsList = aggrs.asList();

            List<InternalAggregation> internalAggregationsList = new ArrayList<>(aggrsList.size());

            for (int i = 0, ilen = aggrsList.size(); i < ilen; i++) {
                Aggregation aggr = aggrsList.get(i);
                String aggrStr = aggr.toString();
                if (!filterData.needRedactionJre(aggrStr)) {
                    internalAggregationsList.add((InternalAggregation) aggr);
                }
            }

            aggregations = new InternalAggregations(internalAggregationsList);
        }
        SearchHits hits = sr.getHits();
        InternalSearchHits intHits = null;
        if (hits != null) {

            SearchHit[] hitsArray = hits.getHits();
            InternalSearchHit[] internalHitsArray = new InternalSearchHit[hitsArray.length];
            long totalInternalHits = hits.getTotalHits();
            int hitsCounter = 0;
            float maxScore = 0.0f;
            for (int i = 0, ilen = hitsArray.length; i < ilen; i++) {
                SearchHit hit = hitsArray[i];
                String hitStr = hit.getSource().toString();
                if (!filterData.needRedactionJre(hitStr)) {
                    maxScore = Math.max(hit.getScore(), maxScore);
                    internalHitsArray[hitsCounter] = (InternalSearchHit) hit;
                    hitsCounter++;
                }
                else{
                    totalInternalHits--;
                }
            }


            InternalSearchHit[] internalHitsArrayAfterRedaction = new InternalSearchHit[hitsCounter];

            System.arraycopy(internalHitsArray,0,internalHitsArrayAfterRedaction,0,hitsCounter);

            intHits = new InternalSearchHits(internalHitsArrayAfterRedaction, totalInternalHits, maxScore);

        }

        Suggest suggest = sr.getSuggest();
        // LPPM - Elastic 2.x
        // InternalProfileShardResults profileResults = new InternalProfileShardResults(sr.getProfileResults());

        SearchProfileShardResults profileResults = new SearchProfileShardResults(sr.getProfileResults());

        boolean timedOut = sr.isTimedOut();
        Boolean terminatedEarly = sr.isTerminatedEarly();


        InternalSearchResponse internalResponse = new InternalSearchResponse(intHits, aggregations,suggest,profileResults,timedOut, terminatedEarly);
        String scrollId = sr.getScrollId();
        int totalShards = sr.getTotalShards();
        int successfulShards = sr.getSuccessfulShards();
        long tookInMillis = sr.getTookInMillis();
        ShardSearchFailure[] shardFailures = sr.getShardFailures();

        SearchResponse resp = new SearchResponse(internalResponse, scrollId,totalShards,successfulShards,tookInMillis,shardFailures);

        return resp;
    }

    public SearchResponse applyRedactionTextReplacement(SearchResponse sr){
        Aggregations aggrs = sr.getAggregations();
        if (aggrs != null) {
            List<Aggregation> aggrsList = aggrs.asList();

            for (int i = 0, ilen = aggrsList.size(); i < ilen; i++) {
                Aggregation aggr = aggrsList.get(i);
                String aggrStr = aggr.toString();
                if (filterData.needRedactionJre(aggrStr)) {
                    Set<Map.Entry<String,Object>> entries =aggr.getMetaData().entrySet();
                    for (Map.Entry<String,Object> entry: entries){
                        if (entry.getValue() instanceof String){
                            String str = (String) entry.getValue();
                            entry.setValue(filterData.redactWithTextReplacement(str,FilterData.REPLACEMENT_VAL));
                        }
                    }
                }
            }
        }
        SearchHits hits = sr.getHits();
        if (hits != null) {

            SearchHit[] hitsArray = hits.getHits();
            float maxScore = 0.0f;
            for (int i = 0, ilen = hitsArray.length; i < ilen; i++) {
                SearchHit hit = hitsArray[i];
                String hitStr = hit.getSource().toString();
                if (!filterData.needRedactionJre(hitStr)) {
                    maxScore = Math.max(hit.getScore(), maxScore);
                }
                else{
                    Set<Map.Entry<String,Object>> entries =hit.getSource().entrySet();
                    for (Map.Entry<String,Object> entry: entries){
                        if (entry.getValue() instanceof String){
                            String str = (String) entry.getValue();
                            entry.setValue(filterData.redactWithTextReplacement(str,FilterData.REPLACEMENT_VAL));
                        }
                    }
                    String id = hit.getId();// new String(FilterData.REPLACEMENT_VAL); // hit.getId();
                    Text type = new Text (hit.getType()); // new Text(FilterData.REPLACEMENT_VAL); // (hit.getType());
                    Map<String, SearchHitField> fields = Collections.EMPTY_MAP; // hit.fields())
                    int docId = ((InternalSearchHit)hit).docId();
                    if (hit.getNestedIdentity() == null) {
                        //     public InternalSearchHit(int docId, String id, Text type, Map<String, SearchHitField> fields) {

                        hitsArray[i] = new InternalSearchHit(docId,id,type,fields);
                    }
                    else{
                        //   public InternalSearchHit(int nestedTopDocId, String id, Text type, InternalNestedIdentity nestedIdentity, Map<String, SearchHitField> fields) {
                        hitsArray[i] = new InternalSearchHit(docId,id,type,
                                (InternalSearchHit.InternalNestedIdentity) hit.getNestedIdentity(),
                                fields);

                    }

                    ((InternalSearchHit)hitsArray[i]).shard(((InternalSearchHit)hit).getShard());
                    ((InternalSearchHit)hitsArray[i]).score(hit.score());
                    ((InternalSearchHit)hitsArray[i]).sourceRef(new BytesArray("{\"val\":\"\"}"));
//                    ((InternalSearchHit)hitsArray[i]).sourceRef(new BytesArray("{}"));
                }

            }
        }
        return sr;
    }




    @Override
    public void apply(final String action, final ActionResponse response, final ActionListener listener, final ActionFilterChain chain) {


//        chain.proceed(action, response, listener);
//
//        if (true)
//            return;
        String reply = response.toString();

        if (filterData != null) {
            if (filterData.needRedactionJre(reply)) {

//                listener.onFailure(new ElasticsearchSecurityException("unauthorized request " + action + " for user " + userStr, RestStatus.FORBIDDEN));
// LPPM - TODO: need to recreate a new response to handle redaction properly...

                if (response instanceof SearchResponse){

                    SearchResponse sr = (SearchResponse) response;
//                    SearchResponse resp = applyRedactionHitRemoval(sr);
                    SearchResponse resp = applyRedactionTextReplacement(sr);

                    chain.proceed(action, resp, listener);

//                    while (hitsIt.hasNext()){
//                        hitsIt.
//                    }
                }
                if (response instanceof MultiGetResponse){
                    MultiGetResponse resp = (MultiGetResponse)response;
                    MultiGetItemResponse items [] = resp.getResponses();
                    for (int i = 0, ilen = items.length; i < ilen; i++){
                        MultiGetItemResponse item = items[i];

                        Map<String, Object> src = item.getResponse().getSource();

                    }
                }


            }
            else {
                chain.proceed(action, response, listener);
            }
        } else if (okToContinue) {
            chain.proceed(action, response, listener);

        }

    }


//    public static ElasticResult fromJson(String json) {
//        return gson.fromJson(json, ElasticResult.class);
//    }


}