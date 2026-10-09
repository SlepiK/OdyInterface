package de.uniol.inf.is.odysseus.odyinterface.externalbuilder;

import de.uniol.inf.is.odysseus.core.planmanagement.query.ILogicalPlan;
import de.uniol.inf.is.odysseus.parser.pql.generator.IPQLGenerator;

public class ExternalBuilderHelper {
	private static IPQLGenerator pqlGenerator;
	
	// called by OSGi-DS
	public void bindPQLGenerator(IPQLGenerator serv) {
		pqlGenerator = serv;
	}

	// called by OSGi-DS
	public void unbindPQLGenerator(IPQLGenerator serv) {
		if (pqlGenerator == serv) {
			pqlGenerator = null;
		}
	}
		
	public static String generatePQLStatementFromQueryPart(ILogicalPlan plan) {
		return pqlGenerator.generatePQLStatement(plan);
	}
		
}
