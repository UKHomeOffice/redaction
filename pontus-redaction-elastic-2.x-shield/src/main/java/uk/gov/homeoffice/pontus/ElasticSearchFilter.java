package uk.gov.homeoffice.pontus;

//import com.floragunn.searchguard.support.ConfigConstants;
//import uk.gov.homeoffice.pontus.ElasticSearchGuardUser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.Shell;
import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.RegexpQueryBuilder;
import org.elasticsearch.index.query.WrapperQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.internal.InternalSearchHit;
import org.elasticsearch.search.internal.InternalSearchHits;
import org.elasticsearch.search.internal.InternalSearchResponse;
import org.elasticsearch.search.profile.InternalProfileShardResults;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.shield.User;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
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
  protected String userStr;
  //    protected static boolean useSearchGuard;
  protected static boolean useShield;


  protected long start;
  protected int size;

  class RedactionKey implements HeapSize {
    public int key;

    public RedactionKey(int key) {
      this.key = key;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      RedactionKey that = (RedactionKey) o;

      return key == that.key;
    }

    @Override
    public int hashCode() {
      return key;
    }

    @Override
    public long heapSize() {
      return 4;
    }
  }

  class RedactionInfo implements HeapSize{
    public JWTClaim claim;
    public FilterData filterData;
    public boolean okToContinue;

    @Override
    public long heapSize() {
      return 100;
    }
  }


  // LPPM - create a 100MB LRU hash map to cater for parallel requests coming in.  Unfortunately, there is only
  // one instance of this class, which makes it impossible to maintain class variables across threads.  So, in order
  // to get a link between the request and response, we need to key on the ActionListener's hash code, which is the
  // only stack parameter that is common between the two apply() functions.
  static LruHashMap<RedactionKey,RedactionInfo> redactionInfoMap = new LruHashMap<>(100000000L);


  static {

    ClassLoader origLoader = ElasticSearchFilterPlugin.class.getClassLoader();// Thread.currentThread().getContextClassLoader();

    CodeSource src = PolicyStore.class.getProtectionDomain().getCodeSource();
    if (src != null) {
      URL[] myJars = {src.getLocation()};
      ClassLoader newLoader = new URLClassLoader(myJars, origLoader);
      Thread.currentThread().setContextClassLoader(newLoader);

      conf = new Configuration();

      conf.setClassLoader(newLoader);

    }


    conf.set("hadoop.security.authentication", "Kerberos");

//    conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
//    conf.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");

    conf.addResource(new Path("/usr/hdp/current/hadoop-client/conf/core-site.xml"));
    conf.addResource(new Path("/usr/hdp/current/hadoop-client/conf/hdfs-site.xml"));
    conf.addResource(new Path("/usr/hdp/current/hbase-regionserver/conf/hbase-site.xml"));

    conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
    conf.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");

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
//            useSearchGuard = conf.getBoolean("elastic.use.searchguard", true);
      useShield = conf.getBoolean("elastic.use.shield", true);


//            final Configuration config = new Configuration(false);
//            config.addResource(new Path("/usr/hdp/current/hadoop-client/conf/core-site.xml"));
//            config.addResource(new Path("/usr/hdp/current/hadoop-client/conf/hdfs-site.xml"));


//            config.addResource(conf);

      try {
        if (Shell.isBashSupported) {
          // ignore; this is causing an exception to be thrown because bash is being called wihtout
          // the /bin/bash prefix.
        }
      } catch (Throwable e) {
        // ignore
      }
      UserGroupInformation.setConfiguration(conf);

      UserGroupInformation userGroupInformation =
        UserGroupInformation.loginUserFromKeytabAndReturnUGI(conf.get("redaction.loginuser.kerb.princ", "hdfs-sandbox@YOUR_REALM_GOES_HERE"),
          conf.get("redaction.loginuser.kerb.keytab", "/etc/security/keytabs/hdfs.headless.keytab"));
      UserGroupInformation.setLoginUser(userGroupInformation);
      // force the class loader to load this here...
//      DistributedFileSystem fs = new DistributedFileSystem();
//            fs.close();

      jwtStore = JWTStore.create(conf);
      policyStore = PolicyStore.createAndStart(conf);


    } catch (Throwable e) {
      Throwable[] supp = e.getSuppressed();
      if (supp != null) {
        for (int i = 0, ilen = supp.length; i < ilen; i++) {
          supp[i].printStackTrace();
        }
      }
      e.printStackTrace();
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
  public ElasticSearchFilter(final Settings settings, ThreadPool threadPool) {
    this.settings = settings;
  }


  @Override
  public int order() {
    return 0;
  }

  public static void amendQuery(MultiSearchRequest searchReq, BoolQueryBuilder newQb) throws IOException, ClassNotFoundException {

    List<SearchRequest> requests = searchReq.requests();
    for (int i = 0, ilen = requests.size(); i < ilen; i++) {
      amendQuery(requests.get(i), newQb);
    }

  }


  public static void amendQuery(SearchRequest searchReq, BoolQueryBuilder newQb) throws IOException, ClassNotFoundException {

    BytesReference source = searchReq.source();

    BytesReference rawSource = (source == null) ? null : source;
    JSONObject postFilter = null;
    try {
      JSONObject obj = source == null ? new JSONObject() : new JSONObject(rawSource.toUtf8());

      if (source != null) {
        if (obj.has("post_filter")) {
          postFilter = obj.getJSONObject("post_filter");
        }
        if (postFilter != null) {
          final String postFilterStr = postFilter.toString();

          WrapperQueryBuilder wqb = new WrapperQueryBuilder(postFilterStr);
          newQb.must(wqb);
        }

      }
      obj.put("post_filter", new JSONObject(newQb.toString()));

      String fullQuery = obj.toString();

      LOG.debug(fullQuery);
      searchReq.source(fullQuery);


    } catch (Exception e) {

      if (source != null) {
        JsonFactory f = new SmileFactory();
// can configure instance with 'SmileParser.Feature' and 'SmileGenerator.Feature'
        ObjectMapper mapper = new ObjectMapper(f);
// and then read/write data as usual
//        SomeType value = ...;
//        byte[] smileData = mapper.writeValueAsBytes(value);
        JsonNode node = mapper.readTree(source.array());
//        SomeType otherValue = mapper.readValue(smileData, SomeType.class);

        if (node.has("post_filter")) {
          String postFilterStr = node.get("post_filter").asText();

          if (postFilterStr != null) {
            WrapperQueryBuilder wqb = new WrapperQueryBuilder(postFilterStr);

            newQb.must(wqb);

          }
        }

        JSONObject obj = new JSONObject(node.toString());
        obj.put("post_filter", new JSONObject(newQb.toString()));
        searchReq.source(obj.toString());
      }


    }

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

    RedactionKey key = new RedactionKey(listener.hashCode());

    ElasticSearchFilter.redactionInfoMap.remove(key);

    RedactionInfo info = new RedactionInfo();

    ElasticSearchFilter.redactionInfoMap.put(key,info);


    info.okToContinue = false;


    try {
      if (LOG.isTraceEnabled()) {
        LOG.trace(String.format(Locale.ROOT, "Action %s from %s/%s", action, request.remoteAddress(), listener.getClass().getSimpleName()));
//                LOG.trace(String.format("Context %s", request.getContext()));
//                LOG.trace(String.format("Header %s", request.getHeaders()));

      }

      if (useShield) {
        // LPPM Elastic 2.x needed this...

        User elasticSearchGuardUser = request.getFromContext("_shield_user");
        if (elasticSearchGuardUser == null && request.remoteAddress() == null) {
//                    elasticSearchGuardUser = ;
        }
        if (elasticSearchGuardUser != null) {
          userStr = elasticSearchGuardUser.principal();
        }
        else {
          userStr = "_shield_internal";
        }
      }
      else {
        userStr = "UNKNOWN";
      }
      LOG.info("in Apply(); user = " + userStr);

      //LogHelper.logUserTrace("--> Action {} from {}/{}", action, request.remoteAddress(), listener.getClass().getSimpleName());
      //LogHelper.logUserTrace("--> Context {}", request.getContext());
      //LogHelper.logUserTrace("--> Header {}", request.getHeaders());


      if (userStr.equals("__es_system_user")) {

        //@formatter:off
        if (action.startsWith("internal:gateway")
          || action.startsWith("cluster:monitor/")
          || action.startsWith("indices:monitor/")
          || action.startsWith("cluster:admin/reroute")
          || action.startsWith("indices:admin/mapping/put")
          || action.startsWith("internal:cluster/nodes/indices/shard/store")
          || action.startsWith("indices:admin/exists")
          || action.startsWith("indices:data/read/mget")
          || action.startsWith("indices:data/read/search")
          || action.startsWith("internal:indices/admin/upgrade")
          || action.startsWith("internal:gateway/local/meta_state")
          ) {

//                    if (log.isInfoEnabled()) {
          LOG.info("No user, will allow only standard discovery and monitoring actions");
//                    }
          info.okToContinue = true;
          info.filterData = null;
          info.claim = null;
          chain.proceed(task, action, request, listener);
          return;
        }
        else {
          LOG.error(String.format(Locale.ROOT, "unauthenticated request %s for user %s", action, userStr));
          listener.onFailure(new ElasticsearchSecurityException("unauthorized request " + action + " for user " + userStr, RestStatus.FORBIDDEN));
          return;
        }
        //@formatter:on
      }
    } catch (Throwable e) {
      LOG.error("Failed to process filters for user " + userStr, e);

    }

    try {
      info.claim = jwtStore.getUserClaim(new JWTUser(userStr));
      if (info.claim == null) {
        LOG.error("Failed to retrieve claim for user " + userStr);
        listener.onFailure(new ElasticsearchSecurityException("unauthorized request " + action + " for user " + userStr, RestStatus.FORBIDDEN));
        return;
      }
      info.filterData = policyStore.getFilterData(info.claim.getBizctx());

      if (info.filterData == null) {
        LOG.error("Failed to retrieve filter data for user " + userStr + ", claim: " + info.claim.toString());
        listener.onFailure(new ElasticsearchSecurityException("unauthorized request " + action + " for user " + userStr, RestStatus.FORBIDDEN));
        return;

      }
      String redactionElasticPostFilterQueryStr = info.filterData.getRedactionElasticPostFilterQueryStr();
      BoolQueryBuilder newQb = new BoolQueryBuilder();

      boolean skipRedaction = false;
      if (redactionElasticPostFilterQueryStr == null) {
        RegexpQueryBuilder rqb = new RegexpQueryBuilder("rmdq", info.filterData.getMetadataRegexStr());
        newQb.must(rqb);
        if (FilterData.RedactionType.FILTER == info.filterData.getRedactionType()) {
          newQb.must(new RegexpQueryBuilder("_all", info.filterData.getRedactionAllowedStr()))
            .mustNot(new RegexpQueryBuilder("_all", info.filterData.getRedactionDeniedStr()))
            .mustNot(new RegexpQueryBuilder("_all", info.filterData.getRedactionDeniedAllStr()));
        }
      }
      else {
        skipRedaction = (redactionElasticPostFilterQueryStr.startsWith("{}"));
        WrapperQueryBuilder wqb = new WrapperQueryBuilder(redactionElasticPostFilterQueryStr);
        newQb.must(wqb);
      }

      if (request instanceof SearchRequest) {
        SearchRequest searchReq = (SearchRequest) request;
        if (!skipRedaction) {
          amendQuery(searchReq, newQb);
        }
      }
      else if (request instanceof MultiSearchRequest) {
        MultiSearchRequest multiSearchRequest = (MultiSearchRequest) request;
        if (!skipRedaction) {
          amendQuery(multiSearchRequest, newQb);
        }
      }


      chain.proceed(task, action, request, listener);


    } catch (Throwable e) {
      LOG.error("Failed to retrieve claim for user " + userStr + ": ", e);
      listener.onFailure(new ElasticsearchSecurityException("unauthorized request " + action + " for user " + userStr, RestStatus.FORBIDDEN));

    }


  }

  public static SearchResponse applyRedactionHitRemoval(SearchResponse sr, FilterData filterData ) {
    InternalAggregations aggs = null;
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

      aggs = new InternalAggregations(internalAggregationsList);
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
        else {
          totalInternalHits--;
        }
      }


      InternalSearchHit[] internalHitsArrayAfterRedaction = new InternalSearchHit[hitsCounter];

      System.arraycopy(internalHitsArray, 0, internalHitsArrayAfterRedaction, 0, hitsCounter);

      intHits = new InternalSearchHits(internalHitsArrayAfterRedaction, totalInternalHits, maxScore);

    }

    Suggest suggest = sr.getSuggest();
    // LPPM - Elastic 2.x
    InternalProfileShardResults profileResults = new InternalProfileShardResults(sr.getProfileResults());

//        SearchProfileShardResults profileResults = new SearchProfileShardResults(sr.getProfileResults());

    boolean timedOut = sr.isTimedOut();
    Boolean terminatedEarly = sr.isTerminatedEarly();


    InternalSearchResponse internalResponse = new InternalSearchResponse(intHits, aggs, suggest, profileResults, timedOut, terminatedEarly);
    String scrollId = sr.getScrollId();
    int totalShards = sr.getTotalShards();
    int successfulShards = sr.getSuccessfulShards();
    long tookInMillis = sr.getTookInMillis();
    ShardSearchFailure[] shardFailures = sr.getShardFailures();

    SearchResponse resp = new SearchResponse(internalResponse, scrollId, totalShards, successfulShards, tookInMillis, shardFailures);

    return resp;
  }

  public static SearchResponse applyRedactionTextReplacement(SearchResponse sr, FilterData filterData) {
    Aggregations aggrs = sr.getAggregations();
    if (aggrs != null) {
      List<Aggregation> aggrsList = aggrs.asList();

      for (int i = 0, ilen = aggrsList.size(); i < ilen; i++) {
        Aggregation aggr = aggrsList.get(i);
        String aggrStr = aggr.toString();
        if (filterData.needRedactionJre(aggrStr)) {
          Set<Map.Entry<String, Object>> entries = aggr.getMetaData().entrySet();
          for (Map.Entry<String, Object> entry : entries) {
            if (entry.getValue() instanceof String) {
              String str = (String) entry.getValue();
              entry.setValue(filterData.redactWithTextReplacement(str, FilterData.REPLACEMENT_VAL));
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
        else {
          Set<Map.Entry<String, Object>> entries = hit.getSource().entrySet();
          for (Map.Entry<String, Object> entry : entries) {
            if (entry.getValue() instanceof String) {
              String str = (String) entry.getValue();
              entry.setValue(filterData.redactWithTextReplacement(str, FilterData.REPLACEMENT_VAL));
            }
          }
          String id = hit.getId();// new String(FilterData.REPLACEMENT_VAL); // hit.getId();
          Text type = new Text(hit.getType()); // new Text(FilterData.REPLACEMENT_VAL); // (hit.getType());
          Map<String, SearchHitField> fields = Collections.EMPTY_MAP; // hit.fields())
          int docId = ((InternalSearchHit) hit).docId();
          if (hit.getNestedIdentity() == null) {
            //     public InternalSearchHit(int docId, String id, Text type, Map<String, SearchHitField> fields) {

            hitsArray[i] = new InternalSearchHit(docId, id, type, fields);
          }
          else {
            //   public InternalSearchHit(int nestedTopDocId, String id, Text type, InternalNestedIdentity nestedIdentity, Map<String, SearchHitField> fields) {
            hitsArray[i] = new InternalSearchHit(docId, id, type,
              (InternalSearchHit.InternalNestedIdentity) hit.getNestedIdentity(),
              fields);

          }

          ((InternalSearchHit) hitsArray[i]).shard(((InternalSearchHit) hit).getShard());
          ((InternalSearchHit) hitsArray[i]).score(hit.score());
          ((InternalSearchHit) hitsArray[i]).sourceRef(new BytesArray("{\"val\":\"\"}"));
//                    ((InternalSearchHit)hitsArray[i]).sourceRef(new BytesArray("{}"));
        }

      }
    }
    return sr;
  }

  public static GetResponse applyRedaction(GetResponse resp, FilterData filterData, String replacement){

    BytesReference bf = resp.getSourceAsBytesRef();

    String data = bf.toUtf8();
    if (filterData.needRedactionJre(data)) {
      data = filterData.redactWithTextReplacement(data, replacement);
      bf = new BytesArray(data);
    }
    GetResult getResult = new GetResult(resp.getIndex(), resp.getType(),resp.getId(),resp.getVersion(), resp.isExists(),bf, resp.getFields());

    GetResponse newResp = new GetResponse(getResult);

    return newResp;

  }


  @Override
  public void apply(final String action, final ActionResponse response, final ActionListener listener, final ActionFilterChain chain) {


//        chain.proceed(action, response, listener);
//
//        if (true)
//            return;
    RedactionKey key = new RedactionKey(listener.hashCode());
    RedactionInfo info = redactionInfoMap.remove(key);

    if (info == null){
      LOG.error(String.format(Locale.ROOT, "Could not find redaction info for key " + key.key));
      listener.onFailure(new ElasticsearchSecurityException("Could not find redaction info for key "+ key.key, RestStatus.FORBIDDEN));
      return;
    }


    String reply = response.toString();

    if (info.filterData != null) {
      FilterData.RedactionType redactionType = info.filterData.getRedactionType();

      if (redactionType == FilterData.RedactionType.REDACT_BLANK
        || redactionType == FilterData.RedactionType.REDACT_REPLACE) {
        if (info.filterData.needRedactionJre(reply)) {

          if (response instanceof SearchResponse) {
            SearchResponse sr = (SearchResponse) response;
            SearchResponse resp = redactionType == FilterData.RedactionType.REDACT_REPLACE ?
              applyRedactionTextReplacement(sr, info.filterData):
              applyRedactionHitRemoval(sr, info.filterData);
            chain.proceed(action, resp, listener);
          }
          else if (response instanceof MultiSearchResponse) {
            MultiSearchResponse msr = (MultiSearchResponse) response;
            MultiSearchResponse.Item[] responses = msr.getResponses();
            for (int i = 0, ilen = responses.length; i < ilen; i++) {
              SearchResponse sr = responses[i].getResponse();
              SearchResponse resp = redactionType == FilterData.RedactionType.REDACT_REPLACE ?
                applyRedactionTextReplacement(sr,info.filterData):
                applyRedactionHitRemoval(sr,info.filterData);
              responses[i] = new MultiSearchResponse.Item(resp, null);
            }

            chain.proceed(action, msr, listener);

          }
          else if (response instanceof MultiGetResponse) {
            MultiGetResponse resp = (MultiGetResponse) response;
            MultiGetItemResponse items[] = resp.getResponses();
            String replacement = (redactionType == FilterData.RedactionType.REDACT_REPLACE)?
              FilterData.REPLACEMENT_VAL:
              "";

            for (int i = 0, ilen = items.length; i < ilen; i++) {
              MultiGetItemResponse item = items[i];
              GetResponse newResp = applyRedaction(item.getResponse(),info.filterData,replacement);
              items[i] = new MultiGetItemResponse(newResp,null);
            }
            chain.proceed(action, resp, listener);

          }
          else if (response instanceof GetResponse) {
            GetResponse resp = (GetResponse) response;
            String replacement = (redactionType == FilterData.RedactionType.REDACT_REPLACE)?
              FilterData.REPLACEMENT_VAL:
              "";
            GetResponse newResp = applyRedaction(resp,info.filterData,replacement);
            chain.proceed(action, newResp, listener);

          }


        }
      }
      else {
        chain.proceed(action, response, listener);
      }
    }
    else if (info.okToContinue) {
      chain.proceed(action, response, listener);

    }

  }


//    public static ElasticResult fromJson(String json) {
//        return gson.fromJson(json, ElasticResult.class);
//    }


}