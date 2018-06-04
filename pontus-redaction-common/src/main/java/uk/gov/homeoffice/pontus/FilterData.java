package uk.gov.homeoffice.pontus;


import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import kmy.regex.compiler.RCompiler;
import kmy.regex.compiler.RDebugMachine;
import kmy.regex.jvm.RJavaClassMachine;
import kmy.regex.parser.RParser;
import kmy.regex.tree.CharSet;
import kmy.regex.tree.RNode;
import kmy.regex.util.Regex;
import kmy.regex.util.RegexRefiller;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by leo on 16/10/2016.
 */

public class FilterData implements HeapSize {


  public enum RedactionType {
    FILTER,
    REDACT_BLANK,
    REDACT_REPLACE

  }


//  public class FilterDataReadColumnRule {
//    Long redactionDeltaTimeMs =  -1L;
//    String regexStr = ".*";
//    String tagRegexStr =  ".*";
//    RedactionType redactionType = RedactionType.REDACT_REPLACE;
//
//  }




  public static final String REPLACEMENT_VAL = "[REDACTED]";

  RegexRefiller refiller = new RegexRefiller() {
    public int refill(Regex regex, int boundary) {
      //System.out.println( "Refill: " + regex + " " + boundary );
      char[] buffer = regex.getCharBuffer(-1);
      if (buffer.length <= boundary) {
        regex.setRefiller(null);
        return boundary; // returning -1 here means "exit right away"
      }
      return boundary + 1;
    }
  };

  static Regex createRegex(String patt) {
    RNode regex = (new RParser()).parse(patt);
    System.out.println(regex);
    System.out.println("\tmin = " + regex.minLeft + " max = " +
      (regex.maxLeft == Integer.MAX_VALUE ? "*" : "" + regex.maxLeft));
    System.out.println("\tprefix: " + regex.findPrefix(CharSet.FULL_CHARSET));
    System.out.println();
    RDebugMachine debug = new RDebugMachine();
    RCompiler dcomp = new RCompiler(debug);
    dcomp.compile(regex, patt);
    System.out.println("\t.stop");

    Regex re = null;

    try {
      RJavaClassMachine jmachine = new RJavaClassMachine();
      jmachine.setSaveBytecode(true);
      RCompiler jcomp = new RCompiler(jmachine);
      jcomp.compile(regex, patt);
      re = jmachine.makeRegex();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return re;
  }
  public FilterData getReadColumnRules(String qualifier)
  {
    return columnRules.get(qualifier);
  }


  public void setRedactionType(String redactionType) {
    this.redactionType = RedactionType.valueOf(redactionType);
  }



  public HashMap<String, FilterData> columnRules = new HashMap<>();

  private RedactionType redactionType = RedactionType.FILTER;
  private String metadataTagRegexStr;
  private String redactionAllowedStr;
  private String redactionDeniedStr;
  private String redactionDeniedAllStr;
  private String columnRulesStr;

  private String redactionElasticPostFilterQueryStr;

  private RegExp dkbMetadataRegex;
  private RegExp dkbRedactionAllowed;
  private RegExp dkbRedactionDenied;
  private RegExp dkbRedactionDeniedAll;
  private Automaton dkbAutoMetadataRegex;
  private Automaton dkbAutoRedactionAllowed;
  private Automaton dkbAutoRedactionDenied;
  private Automaton dkbAutoRedactionDeniedAll;

  private long deltaTime =  -1;
//
//    private Regex kmyMetadataRegex;
//    private Regex kmyRedactionAllowed;
//    private Regex kmyRedactionDenied;
//    private Regex kmyRedactionDeniedAll;

  private jregex.Pattern jreMetadataTagRegex;
  private jregex.Pattern jreRedactionAllowed;
  private jregex.Pattern jreRedactionDenied;
  private jregex.Pattern jreRedactionDeniedAll;


  private Pattern metadataTagRegex;
  private Pattern redactionAllowed;
  private Pattern redactionDenied;
  private Pattern redactionDeniedAll;

  public FilterData() {
    this.metadataTagRegexStr = null;
    this.redactionAllowedStr = null;
    this.redactionDeniedStr = null;
    this.redactionDeniedAllStr = null;
    this.redactionElasticPostFilterQueryStr = null;
    this.metadataTagRegex = null;
    this.redactionAllowed = null;
    this.redactionDenied = null;
    this.redactionDeniedAll = null;
    this.redactionType = RedactionType.FILTER;
  }

  public FilterData(String columnRulesJsonStr, String redactionAllowedStr, String redactionDeniedStr, String redactionDeniedAllStr, String redactionTypeStr, String redactionElasticPostFilterQueryStr) {
    this.setColumnRulesStr(columnRulesJsonStr);
    this.setRedactionAllowedStr(redactionAllowedStr);
    this.setRedactionDeniedStr(redactionDeniedStr);
    this.setRedactionDeniedAllStr(redactionDeniedAllStr);
    this.setRedactionType(redactionTypeStr);
    this.setRedactionElasticPostFilterQueryStr(redactionElasticPostFilterQueryStr);
  }

  public FilterData(String redactionAllowedStr, String redactionDeniedStr, String redactionDeniedAllStr, String redactionTypeStr, long deltaTime, String tagsStr) {
    this.setRedactionAllowedStr(redactionAllowedStr);
    this.setRedactionDeniedStr(redactionDeniedStr);
    this.setRedactionDeniedAllStr(redactionDeniedAllStr);
    this.setMetadataTagRegexStr(tagsStr);
    this.setRedactionType(redactionTypeStr);
    this.deltaTime = deltaTime;

  }

  @Override
  public long heapSize() {
    return ((metadataTagRegex == null) ? 0 : metadataTagRegex.pattern().length()) +
      ((redactionAllowed == null) ? 0 : redactionAllowed.pattern().length()) +
      ((redactionDenied == null) ? 0 : redactionDenied.pattern().length()) +
      ((redactionDeniedAll == null) ? 0 : redactionDeniedAll.pattern().length() +
        ((redactionElasticPostFilterQueryStr == null) ? 0 : redactionElasticPostFilterQueryStr.length()));
  }

  public boolean needRedactionDkb(String val) {
    return !(dkbAutoRedactionAllowed.run(val)
      && !dkbAutoRedactionDenied.run(val)
      && !dkbAutoRedactionDeniedAll.run(val));
  }

//    public boolean needRedactionKmy(String val) {
//        return !(kmyRedactionAllowed.matches(val)
//                && !kmyRedactionDenied.matches(val)
//                && !kmyRedactionDeniedAll.matches(val));
//    }



  public boolean needRedactionJre(String val, long timeStamp, long currTime, String tag) {
    if (currTime - timeStamp <= this.deltaTime){
      return true;
    }
    return !(
           (".*".equals(redactionAllowedStr) ||   jreRedactionAllowed.matches(val))
      &&   (tag != null && jreMetadataTagRegex.matches (tag))
      && ! (".*".equals(redactionAllowedStr) ||   jreRedactionDenied.matches(val) )
      && ! (".*".equals(redactionAllowedStr) ||   jreRedactionDeniedAll.matches(val)) );
  }

  public boolean needRedaction(String val) {
    return !(redactionAllowed.matcher(val).matches()
      && !redactionDenied.matcher(val).matches()
      && !redactionDeniedAll.matcher(val).matches());
  }

  public String redactWithTextReplacement(String val, String replacement) {
    String retVal = redactionDenied.matcher(val).replaceAll(replacement);
    retVal = redactionDeniedAll.matcher(retVal).replaceAll(replacement);
    return retVal;
  }


  public boolean metaDataMatches(String metadataStr) {
    return metadataTagRegex.matcher(metadataStr).matches();
  }

  public boolean metaDataMatchesJre(String metadataStr) {
    return jreMetadataTagRegex.matcher(metadataStr).matches();
  }
//    public boolean metaDataMatchesKmy(String metadataStr) {
//        return kmyMetadataRegex.matches(metadataStr);
//    }

  public boolean metaDataMatchesDkb(String metadataStr) {
    return dkbAutoMetadataRegex.run(metadataStr);
  }

  public String getRedactionElasticPostFilterQueryStr() {
    return redactionElasticPostFilterQueryStr;
  }

  public void setRedactionElasticPostFilterQueryStr(String redactionElasticPostFilterQueryStr) {
    this.redactionElasticPostFilterQueryStr = redactionElasticPostFilterQueryStr;
  }


  public String getMetadataTagRegexStr() {
    return metadataTagRegexStr;
  }

  public void setMetadataTagRegexStr(String metadataTagRegexStr) {
    if (metadataTagRegexStr == null) {
      this.metadataTagRegexStr = "(?!x)x"; // match nothing.
    }
    else {
      this.metadataTagRegexStr = metadataTagRegexStr;
    }
    this.metadataTagRegex = Pattern.compile(this.metadataTagRegexStr, Pattern.DOTALL);
    this.jreMetadataTagRegex = new jregex.Pattern(this.metadataTagRegexStr, jregex.Pattern.DOTALL);

    this.dkbMetadataRegex = new RegExp(this.metadataTagRegexStr);
    this.dkbAutoMetadataRegex= this.dkbMetadataRegex.toAutomaton();
    //        this.kmyRedactionAllowed = createRegex(redactionAllowedStr);
    //        this.kmyRedactionAllowed.setRefiller(refiller);


  }
  public String getColumnRulesStr (){
    return this.columnRulesStr;
  }


  public void setColumnRulesStr(String columnRulesStr) {

    this.columnRulesStr = columnRulesStr;

    ObjectMapper mapper = new ObjectMapper();
    try
    {
      JsonNode metadataRedaction  = mapper.readTree(columnRulesStr);
      JsonNode colRules = metadataRedaction.get("readRulesCols");
      Iterator<Map.Entry<String, JsonNode>> colRulesIterator =  colRules.getFields();

      while (colRulesIterator.hasNext()){
        Map.Entry<String, JsonNode> entry = colRulesIterator.next();
        JsonNode entryVal = entry.getValue();

        JsonNode val;
        val = entryVal.get("redactionAllowedRegex");
        String redactionAllowedRegex = (val == null)? null: val.asText();

        val = entryVal.get("redactionDeniedRegex");
        String redactionDeniedRegex = (val == null)? null: val.asText();

        val = entryVal.get("redactionDeniedAllRegex");
        String redactionDeniedAllRegex = (val == null)? null: val.asText();

        val = entryVal.get("redactionType");
        String redactionType = (val == null)? RedactionType.FILTER.name(): val.asText();

        val = entryVal.get("redactionDeltaTimeMs");
        Long redactionDeltaTimeMs = (val == null)? -1L: val.asLong(-1L);


        val = entryVal.get("redactionTagRegex");
        String redactionTagRegex = (val == null)? null: val.asText();

        FilterData colFilterData = new FilterData(redactionAllowedRegex,redactionDeniedRegex,redactionDeniedAllRegex,redactionType,redactionDeltaTimeMs,redactionTagRegex);


        columnRules.put(entry.getKey(), colFilterData);
      }

    }
    catch (Exception e){

    }


  }

  public String getRedactionAllowedStr() {
    return redactionAllowedStr;
  }




  public void setRedactionAllowedStr(String redactionAllowedStr) {
    if (redactionAllowedStr == null) {
      this.redactionAllowedStr = "(?!x)x"; // match nothing.
    }
    else {
      this.redactionAllowedStr = redactionAllowedStr;
    }
    this.redactionAllowed = Pattern.compile(this.redactionAllowedStr, Pattern.DOTALL);
    this.jreRedactionAllowed = new jregex.Pattern(this.redactionAllowedStr, jregex.Pattern.DOTALL);

    this.dkbRedactionAllowed = new RegExp(this.redactionAllowedStr);
    this.dkbAutoRedactionAllowed = this.dkbRedactionAllowed.toAutomaton();
//        this.kmyRedactionAllowed = createRegex(redactionAllowedStr);
//        this.kmyRedactionAllowed.setRefiller(refiller);


  }

  public String getRedactionDeniedStr() {
    return redactionDeniedStr;
  }

  public void setRedactionDeniedStr(String redactionDeniedStr) {
    if (redactionDeniedStr == null) {
      this.redactionDeniedStr = ".*";
    }
    else {
      this.redactionDeniedStr = redactionDeniedStr;
    }
    this.redactionDenied = Pattern.compile(this.redactionDeniedStr, Pattern.DOTALL);
    this.jreRedactionDenied = new jregex.Pattern(this.redactionDeniedStr, jregex.Pattern.DOTALL);

    this.dkbRedactionDenied = new RegExp(this.redactionDeniedStr);
    this.dkbAutoRedactionDenied = this.dkbRedactionDenied.toAutomaton();
//        this.kmyRedactionDenied = createRegex(redactionDeniedStr);
//        this.kmyRedactionDenied.setRefiller(refiller);

  }

  public String getRedactionDeniedAllStr() {
    return redactionDeniedAllStr;
  }

  public void setRedactionDeniedAllStr(String redactionDeniedAllStr) {
    if (redactionDeniedAllStr == null) {
      this.redactionDeniedAllStr = ".*";
    }
    else {
      this.redactionDeniedAllStr = redactionDeniedAllStr;
    }
    this.redactionDeniedAll = Pattern.compile(this.redactionDeniedAllStr, jregex.Pattern.DOTALL);
    this.jreRedactionDeniedAll = new jregex.Pattern(this.redactionDeniedAllStr, jregex.Pattern.DOTALL);

    this.dkbRedactionDeniedAll = new RegExp(this.redactionDeniedAllStr);
    this.dkbAutoRedactionDeniedAll = this.dkbRedactionDeniedAll.toAutomaton();
//        this.kmyRedactionDeniedAll = createRegex(redactionDeniedAllStr);
//        this.kmyRedactionDeniedAll.setRefiller(refiller);

  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FilterData that = (FilterData) o;

    if (metadataTagRegexStr != null ? !metadataTagRegexStr.equals(that.metadataTagRegexStr) : that.metadataTagRegexStr != null)
      return false;
    if (redactionAllowedStr != null ? !redactionAllowedStr.equals(that.redactionAllowedStr) : that.redactionAllowedStr != null)
      return false;
    if (redactionDeniedStr != null ? !redactionDeniedStr.equals(that.redactionDeniedStr) : that.redactionDeniedStr != null)
      return false;
    if (redactionDeniedAllStr != null ? !redactionDeniedAllStr.equals(that.redactionDeniedAllStr) : that.redactionDeniedAllStr != null)
      return false;
    return true;

  }

  @Override
  public int hashCode() {
    int result = metadataTagRegexStr != null ? metadataTagRegexStr.hashCode() : 0;
    result = 31 * result + (redactionAllowedStr != null ? redactionAllowedStr.hashCode() : 0);
    result = 31 * result + (redactionDeniedStr != null ? redactionDeniedStr.hashCode() : 0);
    result = 31 * result + (redactionDeniedAllStr != null ? redactionDeniedAllStr.hashCode() : 0);
    result = 31 * result + (redactionElasticPostFilterQueryStr != null ? redactionElasticPostFilterQueryStr.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "FilterData{" +
      "metadataRegexStr='" + metadataTagRegexStr + '\'' +
      ", redactionAllowedStr='" + redactionAllowedStr + '\'' +
      ", redactionDeniedStr='" + redactionDeniedStr + '\'' +
      ", redactionDeniedAllStr='" + redactionDeniedAllStr + '\'' +
      ", redactionElasticPostFilterQueryStr='" + redactionElasticPostFilterQueryStr + '\'' +

      '}';
  }

  public RedactionType getRedactionType() {
    return redactionType;
  }
}