package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** IMS option grammar; COBOL owns the host operand grammar. Unknown shapes stay opaque. */
final class DliCommandSyntax {
    record Host(String option,int optionStart,String operand,int operandStart,Ast.EmbeddedHostRole role) { }
    record Command(String name,List<Host> hosts) { Command { hosts=List.copyOf(hosts); } }
    static Optional<Command> parse(String raw) {
        var framed=CicsCommandSyntax.parseEmbedded(raw,"DLI");
        if(framed.isEmpty()||!framed.get().ended()||!framed.get().gaps().isEmpty())return Optional.empty();
        var c=framed.get();var hosts=new ArrayList<Host>();var options=c.options();
        if(c.name().equals("TERM"))return options.isEmpty()?Optional.of(new Command(c.name(),hosts)):Optional.empty();
        if(c.name().equals("SCHD")) {
            var seen=new HashSet<String>();boolean psb=false;
            for(var o:options) {
                if(!seen.add(o.name()))return Optional.empty();
                if(Set.of("SYSSERVE","NODHABEND").contains(o.name())){if(o.operand().isPresent())return Optional.empty();continue;}
                if(!o.name().equals("PSB")||o.operand().isEmpty())return Optional.empty();
                String v=o.operand().get().strip();psb=true;
                if(name(v))continue;
                if(v.length()<3||v.charAt(0)!='('||v.charAt(v.length()-1)!=')')return Optional.empty();
                int begin=raw.indexOf('(',o.start())+1+o.operand().get().indexOf('(')+1;
                if(!host(hosts,o,v.substring(1,v.length()-1),begin,Ast.EmbeddedHostRole.READ,raw,false))return Optional.empty();
            }
            return psb?Optional.of(new Command(c.name(),hosts)):Optional.empty();
        }
        if(!Set.of("GU","GN","GNP").contains(c.name()))return Optional.empty();
        if(options.size()<4||!options.get(0).name().equals("USING")||options.get(0).operand().isPresent()
                ||!options.get(1).name().equals("PCB"))return Optional.empty();
        var pcb=options.get(1);
        if(pcb.operand().isEmpty()||!host(hosts,pcb,pcb.operand().get(),raw.indexOf('(',pcb.start())+1,Ast.EmbeddedHostRole.READ,raw,true))return Optional.empty();
        boolean segment=false,into=false;var seen=new HashSet<String>();
        for(int i=2;i<options.size();i++) {
            var o=options.get(i);if(o.operand().isEmpty())return Optional.empty();
            String value=o.operand().get();int begin=raw.indexOf('(',o.start())+1;
            if(o.name().equals("SEGMENT")) {
                if(segment&&!into||!name(value.strip()))return Optional.empty();
                segment=true;into=false;seen.clear();continue;
            }
            if(!segment||!seen.add(o.name()))return Optional.empty();
            if(o.name().equals("INTO")) {
                if(!host(hosts,o,value,begin,Ast.EmbeddedHostRole.WRITE,raw,false))return Optional.empty();into=true;
            } else if(o.name().equals("WHERE")) {
                int equal=value.indexOf('=');
                if(equal<1||!name(value.substring(0,equal).strip()))return Optional.empty();
                if(!host(hosts,o,value.substring(equal+1),begin+equal+1,Ast.EmbeddedHostRole.READ,raw,true))return Optional.empty();
            } else if(Set.of("SEGLENGTH","KEYLENGTH").contains(o.name())) {
                if(!host(hosts,o,value,begin,Ast.EmbeddedHostRole.READ,raw,true))return Optional.empty();
            } else return Optional.empty();
        }
        return segment&&into?Optional.of(new Command(c.name(),hosts)):Optional.empty();
    }
    private static boolean host(List<Host> out,CicsCommandSyntax.Option o,String value,int start,Ast.EmbeddedHostRole role,String raw,boolean constant) {
        String stripped=value.strip();
        if(constant&&(CicsCommandSyntax.literal(stripped).isPresent()||!stripped.isEmpty()&&stripped.chars().allMatch(c->c>='0'&&c<='9')))return true;
        if(CicsHostSyntax.parseReference(value,raw,start,0,1,0,0).isEmpty())return false;
        out.add(new Host(o.name(),o.start(),value,start,role));return true;
    }
    private static boolean name(String value) {
        return !value.isEmpty()&&value.length()<=8&&value.chars().allMatch(c->c>='A'&&c<='Z'||c>='a'&&c<='z'||c>='0'&&c<='9');
    }
}
