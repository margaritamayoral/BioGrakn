/*
 * BioGrakn - A Knowledge Graph-based Semantic Database for Biomedical Sciences
 * Copyright (C) 2017 - Antonio Messina (xMAnton) <antonio.messina@icar.cnr.it>
 *
 * BioGrakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BioGrakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BioGrakn. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package it.cnr.icar.biograkn;

import static ai.grakn.graql.Graql.var;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import ai.grakn.Grakn;
import ai.grakn.GraknGraph;
import ai.grakn.exception.GraknValidationException;
import ai.grakn.graql.QueryBuilder;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class _16_Uniprot2Pathway {

	private static String timeConversion(long seconds) {

	    final int MINUTES_IN_AN_HOUR = 60;
	    final int SECONDS_IN_A_MINUTE = 60;

	    long minutes = seconds / SECONDS_IN_A_MINUTE;
	    seconds -= minutes * SECONDS_IN_A_MINUTE;

	    long hours = minutes / MINUTES_IN_AN_HOUR;
	    minutes -= hours * MINUTES_IN_AN_HOUR;

	    return hours + " hours " + minutes + " minutes " + seconds + " seconds";
	}
	
	public static void main(String[] args) throws IOException, GraknValidationException {
        disableInternalLogs();

        String homeDir = System.getProperty("user.home");
		
		String fileName = homeDir + "/biodb/reactome/uniprot2pathway.txt";
		String line;
		int entryCounter = 0;
        long startTime = System.currentTimeMillis();

        GraknGraph graph = Grakn.factory(Grakn.DEFAULT_URI, "biograkn").getGraph();
        QueryBuilder qb = graph.graql();
        		
	    BufferedReader reader = new BufferedReader(new FileReader(fileName));

        System.out.print("\nImporting protein to pathway associations from " + fileName + " ");

        while ((line = reader.readLine()) != null) {
        	String datavalue[] = line.split("\t");

        	String uniprotId = datavalue[0];
        	String pathwayId = datavalue[1];
       	
			qb.match(
				var("path").isa("pathway").has("pathwayId", pathwayId),
				var("acc").isa("proteinAccession").has("accession", uniprotId),
				var().isa("entityReference").rel("identifier", "acc").rel("identified", "prot"),
				var("prot").isa("protein")
			).insert(
				var().isa("containing")
				.rel("container", "path")
				.rel("contained", "prot")
			).execute();

    		try {
				graph.commit();
				entryCounter++;
			} catch (GraknValidationException e) {
				graph.rollback();
				entryCounter--;
			}
    		
            if (entryCounter % 10000 == 0) {
            	System.out.print("."); System.out.flush();
            }
        }
        
        graph.close();
        
        long stopTime = (System.currentTimeMillis()-startTime)/1000;
        System.out.println("\n\nCreated " + entryCounter + " relations in " + timeConversion(stopTime));
        
        reader.close();

	}
	
    public static void disableInternalLogs(){
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.OFF);
    }
}
