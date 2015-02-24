
package xls_to_sql_translation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;

import org.mindswap.pellet.jena.PelletInfGraph;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.mindswap.pellet.PelletOptions;
import java.io.*;


/**
 *
 * @author PG
 */
class NoDiffFinder {

    
    String ontURIInitSegment;
    String ontLocation;
    
    OntModel inferredModel;
    
    HashSet<OntModel> swappedModelSet;
    
    OntClass category;
    
    Property subsumes;
    Property isEquivalent;
    
    OntClass nothingClass;
    
    AnnotationProperty hasDomainCodeOld;
    
    HashSet<Statement> stmtMap;
    
    ArrayList<OntClass> classList;
    ArrayList<OntClass> nonVoidClassList;
    
    public static void main(String[] args) {
        
        NoDiffFinder newFinder = new NoDiffFinder();
        newFinder.ontLocation=args[0];
        newFinder.ontURIInitSegment=args[1];
        
        newFinder.loadOntology();
        System.out.println("Ontology loaded");
        
        newFinder.nothingClass = newFinder.inferredModel.getOntClass("http://www.w3.org/2002/07/owl#Nothing");
        newFinder.subsumes = newFinder.inferredModel.getProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf");
        newFinder.isEquivalent = newFinder.inferredModel.getProperty("http://www.w3.org/2002/07/owl#equivalentClass");
        
        
        StatementMaps newStatementMap = newFinder.findStatement();
        
        newFinder.classList = new ArrayList<OntClass>(newFinder.inferredModel.listNamedClasses().toList());
        ArrayList<OntClass> refinedClassList = new ArrayList<OntClass>(newFinder.classList);
        
        PrintWriter out = null;
        
        try {
            out = new PrintWriter("Log"+"_"+newFinder.ontLocation.substring(0, newFinder.ontLocation.lastIndexOf(".")) +".txt");
            
            out.println("SEMANTIC INDISCERNABILITY REPORT FOR "+newFinder.ontLocation.substring(0, newFinder.ontLocation.lastIndexOf(".")));
        } catch (java.io.FileNotFoundException a) {a.printStackTrace();}
        
        for (OntClass ontClass : newFinder.classList)
        {
            if (ontClass.isURIResource())
            {
                if (!ontClass.getNameSpace().contains(newFinder.ontURIInitSegment) || ontClass.getLocalName().startsWith("Anon_"))
                {
                    System.out.println("Removing: "+ontClass);
                    refinedClassList.remove(ontClass);
                }
            }
            else
            {
                System.out.println("Removing non URI: "+ontClass);
                refinedClassList.remove(ontClass);
            }
        }
        
        newFinder.classList=refinedClassList;
        System.out.println("Classes found");
        
        long noClasses = newFinder.classList.size();
        
        out.println("Ontology contains "+noClasses+" named classes");
        out.println("******************************************************************************************************************************************************************");
        out.println("******************************************************************************************************************************************************************");
        out.println("******************************************************************************************************************************************************************");
        
        
        long maxNoCouples = (long) noClasses*(noClasses-1)/2;
        
        out.flush();
        
        newFinder.nonVoidClassList = new ArrayList<OntClass>();
        
        for (OntClass ontClass : newFinder.classList)
        {
            if (newFinder.inferredModel.contains(ontClass, newFinder.subsumes, newFinder.nothingClass))
            {
                System.out.println(ontClass+" is empty!");
            }
            else
            {
                newFinder.nonVoidClassList.add(ontClass);
            }
            
        }
        
        HashSet<HashSet<String>> firstGradeIndiscernibleClassSet = new HashSet<HashSet<String>>();
        HashSet<HashSet<String>> secondGradeIndiscernibleClassSet = new HashSet<HashSet<String>>();
        HashSet<HashSet<String>> thirdGradeIndiscernibleClassSet = new HashSet<HashSet<String>>();
        
        for (int i=0; i < newFinder.classList.size(); i++)
        {
            OntClass firstClass = newFinder.classList.get(i);
            System.out.println(firstClass);
            
            for (int j=i+1; j < newFinder.classList.size(); j++)
            {
                OntClass secondClass = newFinder.classList.get(j);
                //System.out.println(secondClass);
                
                if (newFinder.inferredModel.contains(firstClass, newFinder.isEquivalent, secondClass)) 
                    continue;
                
                Indeterminacy ind = newFinder.findIndeterminacy(newStatementMap, firstClass, secondClass);
                
                if (ind.third)
                {
                    HashSet<String> newIndCouple = new HashSet<String>();
                    newIndCouple.add(firstClass.getURI());
                    newIndCouple.add(secondClass.getURI());
                    thirdGradeIndiscernibleClassSet.add(newIndCouple);
                }
                else
                    if (ind.second)
                    {
                        HashSet<String> newIndCouple = new HashSet<String>();
                        newIndCouple.add(firstClass.getURI());
                        newIndCouple.add(secondClass.getURI());
                        secondGradeIndiscernibleClassSet.add(newIndCouple);
                    }
                    else
                        if (ind.first)
                        {
                            HashSet<String> newIndCouple = new HashSet<String>();
                            newIndCouple.add(firstClass.getURI());
                            newIndCouple.add(secondClass.getURI());
                            firstGradeIndiscernibleClassSet.add(newIndCouple);
                        }
            }
            
            
        }
        
        
        double firstIndNoCouples = ((double) firstGradeIndiscernibleClassSet.size()/maxNoCouples)*100;
        double secondIndNoCouples =  ((double) secondGradeIndiscernibleClassSet.size()/maxNoCouples)*100;
        double thirdIndNoCouples = ((double) thirdGradeIndiscernibleClassSet.size()/maxNoCouples)*100;
        
        out.println("I found "+thirdGradeIndiscernibleClassSet.size()+" couples("+thirdIndNoCouples+" of all couples) of 3rd grade indiscernible named classes:");
        out.println(thirdGradeIndiscernibleClassSet);
        out.println("******************************************************************************************************************************************************************");
        out.flush();
        
        out.println("I found "+secondGradeIndiscernibleClassSet.size()+" couples("+secondIndNoCouples+" of all couples) of 2nd grade indiscernible named classes:");
        out.println(secondGradeIndiscernibleClassSet);
        out.println("******************************************************************************************************************************************************************");
        out.flush();
        
        out.println("I found "+firstGradeIndiscernibleClassSet.size()+" couples("+firstIndNoCouples+" of all couples) of 1st grade indiscernible named classes:");
        out.println(firstGradeIndiscernibleClassSet);
        out.println("******************************************************************************************************************************************************************");
        out.flush();
        
        
        out.close();
    }

void loadOntology ()
{
    PelletOptions.FREEZE_BUILTIN_NAMESPACES=true;
    PelletOptions.IGNORE_UNSUPPORTED_AXIOMS=true;
    PelletOptions.UNDEFINED_DATATYPE_HANDLING=PelletOptions.UNDEFINED_DATATYPE_HANDLING.EMPTY;
    
    this.inferredModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
    OntDocumentManager manager = this.inferredModel.getDocumentManager();
    this.inferredModel.setDynamicImports(true);
    this.inferredModel.read("file:"+this.ontLocation);
    this.inferredModel.loadImports();
    manager.loadImports(this.inferredModel);
    this.saveOntology(this.ontLocation, this.inferredModel);
    this.inferredModel.close();
    this.inferredModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
    this.inferredModel.read("file:"+this.ontLocation);
    this.inferredModel.loadImports();
    manager.loadImports(this.inferredModel);
    
}


StatementMaps findStatement ()
{
    HashMap<Resource, HashSet<Statement>> mapSubject = new HashMap<Resource, HashSet<Statement>>();
    HashMap<Resource, HashSet<Statement>> mapProperty = new HashMap<Resource, HashSet<Statement>>();
    HashMap<Resource, HashSet<Statement>> mapObject = new HashMap<Resource, HashSet<Statement>>();
    
    StatementMaps map = new StatementMaps(mapSubject, mapProperty, mapObject);
    map.allStatements = new HashSet<Statement>();
    
    
    for (Statement statement : this.inferredModel.getBaseModel().listStatements().toSet())
    {
        if (statement.getSubject() != null)
        {
            map.mapSubject=this.appendMap(statement.getSubject(), statement, map.mapSubject);
            map.allStatements.add(statement);
        }
        
        if (statement.getPredicate().isProperty())
        {
            map.mapProperty=this.appendMap(statement.getPredicate(), statement, map.mapProperty);
            map.allStatements.add(statement);
        }
        
        if (statement.getObject().isResource())
        {
            if (statement.getObject().asResource() != null)
            {
                map.mapObject=this.appendMap(statement.getObject().asResource(), statement, map.mapObject);
                map.allStatements.add(statement);
            }
        }
        
    }
    
    return map;
}

HashMap<Resource, HashSet<Statement>> appendMap(Resource resource, Statement statement, HashMap<Resource, HashSet<Statement>> map)
{
    if (map.containsKey(resource))
    {
            map.get(resource).add(statement);
    }
    else
    {
        HashSet<Statement> newStmtSet = new HashSet<Statement>();
        newStmtSet.add(statement);
                        
        map.put(resource, newStmtSet);
    }
    
    return map;
}


Indeterminacy findIndeterminacy (StatementMaps map, OntClass class1, OntClass class2)
{
    Indeterminacy ind = new Indeterminacy();
    ind.first=true;
    ind.second=true;
    ind.third=true;
    
    ArrayList<Statement> swappedStatementList = new ArrayList<Statement>();
    
    if (map.mapSubject.containsKey(class1)) 
        for (Statement statementFor1 : map.mapSubject.get(class1))
        {
            OntClass swappedSubjectInInferredModel = inferredModel.getOntClass(class2.getURI());
            Property predicateInInferredModel = inferredModel.getProperty(statementFor1.getPredicate().getURI());
            RDFNode objectInInferredModel = statementFor1.getObject();
            
            if (objectInInferredModel.isURIResource())
            {
                if (objectInInferredModel.equals(class1))
                {
                    objectInInferredModel=class2;
                }
                else
                    if (objectInInferredModel.equals(class2))
                    {
                        objectInInferredModel=class1;
                    }
            }
            
            Statement newRDFStatement = this.inferredModel.createStatement(swappedSubjectInInferredModel, predicateInInferredModel, objectInInferredModel);
            swappedStatementList.add(newRDFStatement);
            
            if (!this.inferredModel.contains(newRDFStatement)&& //
                this.inferredModel.getAnnotationProperty(predicateInInferredModel.getURI())== null)    
            {
                ind.third=false;
            }
        }
    
    if (map.mapObject.containsKey(class1))
        for (Statement statementFor1 : map.mapObject.get(class1))
        {
            OntClass swappedObjectInInferredModel  = inferredModel.getOntClass(class2.getURI());
            Property predicateInInferredModel = inferredModel.getProperty(statementFor1.getPredicate().getURI());
            RDFNode  subjectInInferredModel = statementFor1.getSubject();
            
            if (subjectInInferredModel.isURIResource())
            {
                if (subjectInInferredModel.equals(class1))
                {
                    subjectInInferredModel=class2;
                }
                else
                    if (subjectInInferredModel.equals(class2))
                    {
                        subjectInInferredModel=class1;
                    }
            }
            
            Statement newRDFStatement = null;
            
            if (subjectInInferredModel.isURIResource())
            {
                if (this.inferredModel.getOntClass(subjectInInferredModel.asResource().getURI()) != null)
                
                {
                    OntClass subjectClass = this.inferredModel.getOntClass(subjectInInferredModel.asResource().getURI());
                    newRDFStatement = this.inferredModel.createStatement(subjectClass, predicateInInferredModel, swappedObjectInInferredModel);
                    swappedStatementList.add(newRDFStatement);
                }
            }
            
            if (newRDFStatement != null)
            {
                if (!this.inferredModel.contains(newRDFStatement) && //
                this.inferredModel.getAnnotationProperty(predicateInInferredModel.getURI())== null)
                {
                    ind.third=false;
                }
            }
        }
    
    if (map.mapSubject.containsKey(class2)) 
        for (Statement statementFor1 : map.mapSubject.get(class2))
        {
            OntClass swappedSubjectInInferredModel = inferredModel.getOntClass(class1.getURI());
            Property predicateInInferredModel = inferredModel.getProperty(statementFor1.getPredicate().getURI());
            RDFNode objectInInferredModel = statementFor1.getObject();
            
            if (objectInInferredModel.isURIResource())
            {
                if (objectInInferredModel.equals(class2))
                {
                    objectInInferredModel=class1;
                }
                else
                    if (objectInInferredModel.equals(class1))
                    {
                        objectInInferredModel=class2;
                    }
            }
            
            Statement newRDFStatement = this.inferredModel.createStatement(swappedSubjectInInferredModel, predicateInInferredModel, objectInInferredModel);
            swappedStatementList.add(newRDFStatement);
            
            if (!this.inferredModel.contains(newRDFStatement)&& //
                this.inferredModel.getAnnotationProperty(predicateInInferredModel.getURI())== null)
            {
                ind.third=false;
            }
        }
    
    if (map.mapObject.containsKey(class2))
        for (Statement statementFor1 : map.mapObject.get(class2))
        {
            OntClass swappedObjectInInferredModel  = inferredModel.getOntClass(class1.getURI());
            Property predicateInInferredModel = inferredModel.getProperty(statementFor1.getPredicate().getURI());
            RDFNode  subjectInInferredModel = statementFor1.getSubject();
            
            if (subjectInInferredModel.isURIResource())
            {
                if (subjectInInferredModel.equals(class2))
                {
                    subjectInInferredModel=class1;
                }
                else
                    if (subjectInInferredModel.equals(class1))
                    {
                        subjectInInferredModel=class2;
                    }
            }
            
            Statement newRDFStatement = null;
            
            if (subjectInInferredModel.isURIResource())
            {
                if (this.inferredModel.getOntClass(subjectInInferredModel.asResource().getURI()) != null)
                
                {
                    OntClass subjectClass = this.inferredModel.getOntClass(subjectInInferredModel.asResource().getURI());
                    newRDFStatement = this.inferredModel.createStatement(subjectClass, predicateInInferredModel, swappedObjectInInferredModel);
                    swappedStatementList.add(newRDFStatement);
                }
            }
            
            if (newRDFStatement != null)
            {
                if (!this.inferredModel.contains(newRDFStatement) && //
                this.inferredModel.getAnnotationProperty(predicateInInferredModel.getURI())== null)
                {
                    ind.third=false;
                }
            }
        }
    
    if (ind.third)
    {
        if (    (!map.mapSubject.containsKey(class1) && map.mapSubject.containsKey(class2)) //
            ||  (!map.mapSubject.containsKey(class2) && map.mapSubject.containsKey(class1)) //
            ||  (!map.mapObject.containsKey(class1) && map.mapObject.containsKey(class2)) //
            ||  (!map.mapObject.containsKey(class2) && map.mapObject.containsKey(class1))//
            ||  (!map.mapSubject.containsKey(class1) && !map.mapSubject.containsKey(class2) && !map.mapObject.containsKey(class1) && !map.mapObject.containsKey(class2))) 
        {
            ind.third=false;
        }
    }
    
    if (!swappedStatementList.isEmpty())
    {
        OntModel tempModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
        tempModel.add(this.inferredModel);
        tempModel.add(swappedStatementList);
        
        PelletInfGraph pelletGraph = (PelletInfGraph)tempModel.getGraph();
        ind.first = pelletGraph.isConsistent();
    
        if (ind.first)
        {
            for (OntClass nonVoidClass : this.nonVoidClassList)
            {
                if (tempModel.contains(nonVoidClass, subsumes, nothingClass))
                {
                    ind.second=false;
                    break;
                }
            }
        }
        else ind.second=false;
        
        pelletGraph.close();
        tempModel.close();
    
        //System.out.println("2nd grade");
    }
    
    return ind;
}


void saveOntology(String location, OntModel model)
{
    try {
                FileOutputStream writer = new FileOutputStream(location);
                
                model.write(writer, null, null);

                try {
                    writer.close();
                    } catch (java.io.IOException ex) {ex.printStackTrace();}
} catch (java.io.FileNotFoundException ex) {ex.printStackTrace();}

}

}












