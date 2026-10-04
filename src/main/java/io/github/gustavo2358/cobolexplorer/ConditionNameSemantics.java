package io.github.gustavo2358.cobolexplorer;

import java.util.*;
import io.github.gustavo2358.cobolexplorer.semanticproduct.ConditionNames;
import static io.github.gustavo2358.cobolexplorer.ResolutionContracts.*;

/** Canonical level-88 binding and SET assignments. No storage allocation or value propagation. */
public final class ConditionNameSemantics {
    public record Declaration(SemanticEntityId condition,SemanticEntityId parent,
            Ast.DataEntry entry,Ast.DataEntry variable,List<Ast.ConditionRange> ranges,
            Optional<Ast.ConditionValue> falseValue,ConditionNames.VariableDomain domain) {
        public Declaration { ranges=List.copyOf(ranges); }
    }
    public record Use(int statement,Ast.DataReference reference,Declaration declaration,ReferenceRole role,List<ConditionNames.Index> indices) {
        public Use {indices=List.copyOf(indices);}
    }
    public record Predicate(int statement,String role,ConditionNames.Tree tree) { }
    private record Root(int statement,String role,List<Ast.Expression> alternatives,List<Boolean> negations) {
        Root(int statement,String role,Ast.Expression expression){this(statement,role,List.of(expression),List.of(false));}
    }
    public record Assignment(Use use,boolean truth,Ast.ConditionValue value) { }
    public record Metrics(long nodeVisits,long symbolVisits,long referenceVisits,long rangeVisits,long targetVisits) { }
    private final Map<ProgramUnitId,Map<Integer,Use>> uses;
    private final Map<ProgramUnitId,Map<Integer,List<Assignment>>> sets;
    private final Map<ProgramUnitId,Map<Integer,String>> failures;
    private final Metrics metrics;
    private final Map<ProgramUnitId,List<Predicate>> predicates;
    private final Map<ProgramUnitId,Map<String,Predicate>> predicatesByOwner;
    private ConditionNameSemantics(Map<ProgramUnitId,Map<Integer,Use>> uses,
            Map<ProgramUnitId,Map<Integer,List<Assignment>>> sets,
            Map<ProgramUnitId,Map<Integer,String>> failures,Map<ProgramUnitId,List<Predicate>> predicates,long[] work) {
        this.predicates=Map.copyOf(predicates);
        var indexed=new HashMap<ProgramUnitId,Map<String,Predicate>>();
        predicates.forEach((unit,list)->{var local=new HashMap<String,Predicate>();list.forEach(p->local.put(p.statement()+"/"+p.role(),p));indexed.put(unit,Map.copyOf(local));});
        this.predicatesByOwner=Map.copyOf(indexed);this.uses=freeze(uses);this.sets=freeze(sets);this.failures=freeze(failures);
        metrics=new Metrics(work[0],work[1],work[2],work[3],work[4]);
    }
    private static <T> Map<ProgramUnitId,Map<Integer,T>> freeze(Map<ProgramUnitId,Map<Integer,T>> input) {
        var result=new HashMap<ProgramUnitId,Map<Integer,T>>();input.forEach((k,v)->result.put(k,Map.copyOf(v)));return Map.copyOf(result);
    }
    public Map<Integer,Use> uses(ProgramUnitId unit){return uses.getOrDefault(unit,Map.of());}
    public Map<Integer,List<Assignment>> sets(ProgramUnitId unit){return sets.getOrDefault(unit,Map.of());}
    public Map<Integer,String> failures(ProgramUnitId unit){return failures.getOrDefault(unit,Map.of());}
    public Metrics metrics(){return metrics;}
    public List<Predicate> predicates(ProgramUnitId unit){return predicates.getOrDefault(unit,List.of());}
    public boolean complete(ProgramUnitId unit,int statement,String role){var p=predicatesByOwner.getOrDefault(unit,Map.of()).get(statement+"/"+role);return p!=null&&p.tree().complete();}
    private record Visit(Ast.Node node,Ast.DataEntry parent,int statement) { }

    /** Indexed passes O(AST + symbols + references + declared ranges + SET destinations).
     * A range of a billion values costs exactly one range visit. No boolean expansion. */
    public static ConditionNameSemantics analyze(CompilationUnitBuildResult frontend,
            CompilationUnitSymbolTables tables,ReferenceResolution resolution) {
        long[] work=new long[5];
        var declarations=new HashMap<SemanticEntityId,Declaration>();
        var dataNodes=new HashMap<SemanticEntityId,Integer>();
        var owners=new HashMap<ScalarMoveSemantics.NodeKey,Integer>();
        var roots=new HashMap<ProgramUnitId,List<Root>>();
        var references=new HashMap<ScalarMoveSemantics.NodeKey,Ast.DataReference>();
        var setSurfaces=new HashMap<ProgramUnitId,Map<Integer,Ast.ConditionSetSurface>>();
        for(var unit:frontend.compilationUnit().programUnits()) {
            var identities=new HashMap<Integer,SemanticEntityId>();
            for(var symbol:tables.forProgramUnit(unit.id()).orElseThrow().symbolTable().symbols()) {
                work[1]++;
                if(symbol.kind()==SymbolTable.SymbolKind.DATA_ITEM||symbol.kind()==SymbolTable.SymbolKind.CONDITION_NAME)
                    {var entity=new SemanticEntityId(unit.id(),SemanticEntityDomain.DATA_SYMBOL,symbol.id());
                        identities.put(symbol.declarationAstNodeId(),entity);
                        if(symbol.kind()==SymbolTable.SymbolKind.DATA_ITEM)dataNodes.put(entity,symbol.declarationAstNodeId());}
            }
            var todo=new ArrayDeque<Visit>();todo.push(new Visit(unit.program(),null,-1));
            while(!todo.isEmpty()) {
                var visit=todo.pop();var n=visit.node();work[0]++;
                if(n instanceof Ast.Program&&n!=unit.program())continue;
                int statement=n instanceof Ast.Statement?n.meta().id():visit.statement();
                if(n instanceof Ast.DataReference r){var key=new ScalarMoveSemantics.NodeKey(unit.id(),r.meta().id());references.put(key,r);owners.put(key,statement);}
                var localRoots=roots.computeIfAbsent(unit.id(),k->new ArrayList<>());
                if(n instanceof Ast.IfStatement branch)localRoots.add(new Root(statement,"IF",branch.condition()));
                if(n instanceof Ast.EvaluateStatement evaluate){
                    for(int i=0;i<evaluate.subjects().size();i++)localRoots.add(new Root(statement,"EVALUATE_SUBJECT/"+i,evaluate.subjects().get(i)));
                    var booleanSubject=evaluate.subjects().size()==1&&evaluate.subjects().get(0) instanceof Ast.LiteralExpression l?l.booleanValue():Optional.<Boolean>empty();
                    for(int i=0;i<evaluate.branches().size();i++){
                        var selectors=evaluate.branches().get(i).selectors();
                        if(booleanSubject.isPresent()&&!selectors.isEmpty())localRoots.add(new Root(statement,"EVALUATE_WHEN/"+i,selectors.stream().map(Ast.EvaluateSelector::expression).toList(),selectors.stream().map(s->s.negated()^!booleanSubject.get()).toList()));
                        else for(int j=0;j<selectors.size();j++)localRoots.add(new Root(statement,"EVALUATE_SELECTOR/"+i+"/"+j,selectors.get(j).expression()));
                    }
                }
                if(n instanceof Ast.PerformStatement perform)for(var control:perform.controls())if(control.context()==Ast.PerformControlContext.CONDITION)
                    localRoots.add(new Root(statement,"PERFORM_UNTIL/"+control.varyingLevel(),control.expression()));
                if(n instanceof Ast.ModeledStatement s)s.conditionSet().ifPresent(v->setSurfaces.computeIfAbsent(unit.id(),k->new LinkedHashMap<>()).put(s.meta().id(),v));
                if(n instanceof Ast.DataEntry d&&d.levelKind()==Ast.DataLevelKind.CONDITION_88&&visit.parent()!=null) {
                    var values=d.clauses().stream().filter(Ast.ValueClause.class::isInstance).map(Ast.ValueClause.class::cast).toList();
                    var identity=identities.get(d.meta().id());var parent=identities.get(visit.parent().meta().id());
                    if(values.size()==1&&identity!=null&&(parent!=null||visit.parent().filler())) {
                        var value=values.get(0);boolean known=!value.ranges().isEmpty();
                        for(var range:value.ranges()){work[3]++;known&=available(range.first())&&range.last().map(ConditionNameSemantics::available).orElse(true);}
                        if(known)declarations.put(identity,new Declaration(identity,parent,d,visit.parent(),value.ranges(),value.falseValue(),domain(visit.parent())));
                    }
                }
                var parent=n instanceof Ast.DataEntry d?d:visit.parent();
                for(var child:Ast.children(n))todo.push(new Visit(child,parent,statement));
            }
        }
        var boundNodes=new HashMap<ScalarMoveSemantics.NodeKey,Integer>();
        for(var binding:resolution.entries())if(binding.status()==ResolutionStatus.RESOLVED&&binding.candidates().size()==1) {
            var node=dataNodes.get(binding.selectedCandidate().orElseThrow().entityId());
            if(node!=null)boundNodes.put(new ScalarMoveSemantics.NodeKey(binding.occurrence().programUnitId(),binding.occurrence().referenceAstNodeId()),node);
        }
        var uses=new HashMap<ProgramUnitId,Map<Integer,Use>>();
        for(var binding:resolution.entries()) {
            work[2]++;
            if(binding.status()!=ResolutionStatus.RESOLVED||binding.candidates().size()!=1)continue;
            var definition=declarations.get(binding.selectedCandidate().orElseThrow().entityId());
            var occurrence=binding.occurrence();var ref=references.get(new ScalarMoveSemantics.NodeKey(occurrence.programUnitId(),occurrence.referenceAstNodeId()));
            if(definition!=null&&ref!=null&&ref.understanding()==Ast.ReferenceUnderstanding.STRUCTURED&&ref.referenceModification()==null)
                uses.computeIfAbsent(occurrence.programUnitId(),k->new HashMap<>()).put(ref.meta().id(),new Use(owners.get(new ScalarMoveSemantics.NodeKey(occurrence.programUnitId(),ref.meta().id())),ref,definition,occurrence.role(),ref.subscriptGroups().stream().flatMap(g->g.subscripts().stream()).map(e->index(e,occurrence.programUnitId(),boundNodes)).toList()));
        }
        var sets=new HashMap<ProgramUnitId,Map<Integer,List<Assignment>>>();var failures=new HashMap<ProgramUnitId,Map<Integer,String>>();
        setSurfaces.forEach((unit,surfaces)->surfaces.forEach((statement,surface)->{
            var assignments=new ArrayList<Assignment>();String failure=null;
            for(var target:surface.assignments()) {
                work[4]++;var use=uses.getOrDefault(unit,Map.of()).get(target.target().meta().id());
                if(use==null){failure="SET_CONDITION_BINDING_NOT_PROVEN";continue;}
                var value=target.truth()?Optional.of(use.declaration().ranges().get(0).first()):use.declaration().falseValue();
                if(value.isEmpty()){failure="SET_FALSE_VALUE_NOT_DECLARED";continue;}
                if(!available(value.get())){failure="SET_CONDITION_VALUE_NOT_AVAILABLE";continue;}
                assignments.add(new Assignment(use,target.truth(),value.get()));
            }
            if(failure==null)sets.computeIfAbsent(unit,k->new HashMap<>()).put(statement,List.copyOf(assignments));
            else failures.computeIfAbsent(unit,k->new HashMap<>()).put(statement,failure);
        }));
        var predicates=new HashMap<ProgramUnitId,List<Predicate>>();
        roots.forEach((unit,list)->{
            var published=new ArrayList<Predicate>();
            for(var root:list){
                var alternatives=new ArrayList<ConditionNames.Tree>();
                for(int i=0;i<root.alternatives().size();i++) {var t=tree(root.alternatives().get(i),uses.getOrDefault(unit,Map.of()));alternatives.add(root.negations().get(i)?new ConditionNames.Tree("NOT","",List.of(t)):t);}
                var tree=alternatives.size()==1?alternatives.get(0):new ConditionNames.Tree("OR","",alternatives);
                if(hasTest(tree))published.add(new Predicate(root.statement(),root.role(),tree));
            }
            predicates.put(unit,List.copyOf(published));
        });
        return new ConditionNameSemantics(uses,sets,failures,predicates,work);
    }
    private static ConditionNames.Index index(Ast.Expression e,ProgramUnitId unit,Map<ScalarMoveSemantics.NodeKey,Integer> nodes) {
        if(e instanceof Ast.LiteralExpression l&&l.integerValue().isPresent())return new ConditionNames.Index("INTEGER",l.integerValue().get().toString(),List.of());
        var node=nodes.get(new ScalarMoveSemantics.NodeKey(unit,e.meta().id()));
        if(node!=null)return new ConditionNames.Index("READ","storage-node:"+node,List.of());
        if(e instanceof Ast.OperationExpression op) {
            String kind=switch(op.operator()){case "+"->"ADD";case "-"->"SUBTRACT";case "*"->"MULTIPLY";case "/"->"DIVIDE";default->null;};
            if(kind!=null&&op.operands().size()>=2)return new ConditionNames.Index(kind,"",op.operands().stream().map(x->index(x,unit,nodes)).toList());
        }
        return new ConditionNames.Index("UNKNOWN","",List.of());
    }
    private static boolean hasTest(ConditionNames.Tree tree){var todo=new ArrayDeque<ConditionNames.Tree>();todo.add(tree);while(!todo.isEmpty()){var t=todo.removeFirst();if(t.kind().equals("TEST"))return true;todo.addAll(t.children());}return false;}
    private static ConditionNames.Tree tree(Ast.Expression root,Map<Integer,Use> uses) {
        // Explicit postorder stack: nesting consumes heap proportional to syntax, not Java stack.
        record Pending(Ast.Expression expression,boolean finish) { }
        var todo=new ArrayDeque<Pending>();var trees=new IdentityHashMap<Ast.Expression,ConditionNames.Tree>();todo.push(new Pending(root,false));
        while(!todo.isEmpty()) {
            var pending=todo.pop();var e=pending.expression();List<Ast.Expression> children=e instanceof Ast.LogicalCondition l?l.operands():e instanceof Ast.NegatedCondition n?List.of(n.operand()):e instanceof Ast.GroupedCondition g?List.of(g.inner()):List.of();
            if(!pending.finish()&&!children.isEmpty()){todo.push(new Pending(e,true));for(int i=children.size()-1;i>=0;i--)todo.push(new Pending(children.get(i),false));continue;}
            ConditionNames.Tree value;
            if(e instanceof Ast.DataReference r&&uses.containsKey(r.meta().id()))value=new ConditionNames.Tree("TEST","condition-use:"+r.meta().id(),List.of());
            else if(e instanceof Ast.ContextualConditionTail t&&uses.containsKey(t.nominalReference().meta().id()))
                value=new ConditionNames.Tree("TEST","condition-use:"+t.nominalReference().meta().id(),List.of());
            else if(e instanceof Ast.GroupedCondition g)value=trees.get(g.inner());
            else if(e instanceof Ast.LogicalCondition l&&children.size()>=2)value=new ConditionNames.Tree(l.connector().name(),"",children.stream().map(trees::get).toList());
            else if(e instanceof Ast.NegatedCondition n)value=new ConditionNames.Tree("NOT","",List.of(trees.get(n.operand())));
            else value=new ConditionNames.Tree("UNKNOWN","",List.of());
            trees.put(e,value);
        }
        return trees.get(root);
    }
    private static ConditionNames.VariableDomain domain(Ast.DataEntry variable) {
        var pictures=variable.clauses().stream().filter(Ast.PictureClause.class::isInstance).map(Ast.PictureClause.class::cast).toList();
        if(pictures.isEmpty()&&variable.children().stream().anyMatch(d->d.levelKind()!=Ast.DataLevelKind.CONDITION_88))return ConditionNames.VariableDomain.TEXT;
        if(pictures.size()==1){if(pictures.get(0).textExtent().isPresent())return ConditionNames.VariableDomain.TEXT;if(pictures.get(0).integerDigits().isPresent())return ConditionNames.VariableDomain.INTEGER;}
        return ConditionNames.VariableDomain.UNKNOWN;
    }
    private static boolean available(Ast.ConditionValue value){return value.kind()!=Ast.ConditionValueKind.UNAVAILABLE;}
}
