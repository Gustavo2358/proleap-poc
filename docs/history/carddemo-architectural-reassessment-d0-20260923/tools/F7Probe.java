package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.lower.adapters.sp.*;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.air.validation.*;
import java.nio.file.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
public class F7Probe {
 public static void main(String[] args)throws Exception{
  var read=new SpFileInput(CobolLower.INPUT_LIMITS).read(Path.of(args[0]));
  var input=((SpJsonDecoder.Decoded)read).input();
  var plan=new PartialProgramAdmission().plan(input,new AdmitInput.Limits(100));
  if(plan.admission().status()!=Admission.Status.ADMITTED)throw new IllegalStateException(plan.admission().toString());
  var pub=new PublicationId(CanonicalRevision.partial(input,1_000_000).orElseThrow());var unit=new UnitId(pub,"unit");var ids=new LocalIds();
  var fragment=PartialProgramLowerer.fragment(input,plan,pub,unit,ids,null);
  var validation=AirValidator.validate(fragment.publication(),new ValidationOptions(Integer.MAX_VALUE,Integer.MAX_VALUE,30000));
  var field=LocalIds.class.getDeclaredField("registered");field.setAccessible(true);var registry=(Map<?,?>)field.get(ids);
  var out=new LinkedHashMap<String,Object>();out.put("validation",validation.status().toString());out.put("counts",validation.diagnostics().counts());out.put("traversalCompleted",validation.diagnostics().traversalCompleted());
  var labels=new LinkedHashMap<String,Object>();for(var u:fragment.publication().units())for(var s:u.sequences())labels.put(s.label().localId(),registry.get(s.label().localId()).toString());
  out.put("materializedLabels",labels);var rows=new ArrayList<Object>();
  for(var issue:validation.issues())if(issue.kind().toString().equals("INVALID_IR")){
   var row=new LinkedHashMap<String,Object>();var op=(OperationId)issue.subject().orElseThrow();var matcher=java.util.regex.Pattern.compile("localId=([^] ]+)\\]$").matcher(issue.detail());if(!matcher.find())throw new IllegalStateException(issue.detail());var target=matcher.group(1);
   row.put("rule",issue.rule());row.put("operation",op.localId());row.put("operationIdentity",registry.get(op.localId()).toString());row.put("targetLabel",target);row.put("targetIdentity",registry.get(target).toString());row.put("targetMaterialized",labels.containsKey(target));
   row.put("sourceStatements",fragment.statements().stream().filter(s->s.target().equals(op)).map(s->s.source().handle()).toList());rows.add(row);
  }out.put("issues",rows);
  var plans=new LinkedHashMap<String,Object>();plan.compositions().forEach((k,v)->plans.put(k.handle(),v.stream().map(s->s.header().id().handle()).toList()));out.put("compositions",plans);
  out.put("ordinaryInventory",plan.statements().stream().map(s->s.header().id().handle()).toList());
  System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(out));
 }
}
