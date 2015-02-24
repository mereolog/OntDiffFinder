/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xls_to_sql_translation;

import java.util.HashMap;
import java.util.HashSet;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;

/**
 *
 * @author PG
 */
public class StatementMaps {
    HashMap<Resource, HashSet<Statement>> mapSubject;
    HashMap<Resource, HashSet<Statement>> mapProperty;
    HashMap<Resource, HashSet<Statement>> mapObject;
    
    HashSet<Statement> allStatements;
    
    StatementMaps(HashMap<Resource, HashSet<Statement>> s, HashMap<Resource, HashSet<Statement>> p, HashMap<Resource, HashSet<Statement>> o)
    {
        mapSubject=s;
        mapProperty=p;
        mapObject=o;
    }
    
}
