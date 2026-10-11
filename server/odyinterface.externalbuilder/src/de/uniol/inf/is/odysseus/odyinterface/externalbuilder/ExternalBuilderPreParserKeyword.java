package de.uniol.inf.is.odysseus.odyinterface.externalbuilder;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.uniol.inf.is.odysseus.core.collection.Context;
import de.uniol.inf.is.odysseus.core.collection.Resource;
import de.uniol.inf.is.odysseus.core.server.planmanagement.executor.IServerExecutor;
import de.uniol.inf.is.odysseus.core.server.planmanagement.executor.command.IExecutorCommand;
import de.uniol.inf.is.odysseus.core.usermanagement.ISession;
import de.uniol.inf.is.odysseus.script.parser.AbstractPreParserKeyword;
import de.uniol.inf.is.odysseus.script.parser.OdysseusScriptException;
import de.uniol.inf.is.odysseus.core.metadata.IMetaAttribute;
import de.uniol.inf.is.odysseus.core.physicaloperator.ISink;
import de.uniol.inf.is.odysseus.core.planmanagement.executor.exception.PlanManagementException;
import de.uniol.inf.is.odysseus.core.server.metadata.MetadataRegistry;
import de.uniol.inf.is.odysseus.core.server.planmanagement.QueryParseException;
import de.uniol.inf.is.odysseus.core.server.planmanagement.optimization.configuration.ParameterAddMetadata;
import de.uniol.inf.is.odysseus.core.server.planmanagement.optimization.configuration.ParameterMaxSheddingFactor;
import de.uniol.inf.is.odysseus.core.server.planmanagement.optimization.configuration.ParameterQueryName;
import de.uniol.inf.is.odysseus.core.server.planmanagement.optimization.configuration.ParameterQueryParams;
import de.uniol.inf.is.odysseus.core.server.planmanagement.query.querybuiltparameter.ACQueryParameter;
import de.uniol.inf.is.odysseus.core.server.planmanagement.query.querybuiltparameter.IQueryBuildSetting;
import de.uniol.inf.is.odysseus.core.server.planmanagement.query.querybuiltparameter.ParameterPriority;
import de.uniol.inf.is.odysseus.script.keyword.configuration.ActivateRewriteRulePreParserKeyword;
import de.uniol.inf.is.odysseus.script.keyword.configuration.DeActivateRewriteRulePreParserKeyword;
import de.uniol.inf.is.odysseus.script.keyword.configuration.ParserPreParserKeyword;
import de.uniol.inf.is.odysseus.script.keyword.configuration.TransCfgPreParserKeyword;
import de.uniol.inf.is.odysseus.script.parser.keyword.MaxSheddingFactorPreParserKeyword;
import de.uniol.inf.is.odysseus.script.parser.keyword.NoMetadataPreParserKeyword;
import de.uniol.inf.is.odysseus.script.parser.keyword.QueryIDPreParserKeyword;
import de.uniol.inf.is.odysseus.script.parser.keyword.QueryNamePreParserKeyword;
import de.uniol.inf.is.odysseus.script.parser.keyword.QueryParamPreParserKeyword;
import de.uniol.inf.is.odysseus.script.parser.keyword.QueryPriorityPreParserKeyword;

public class ExternalBuilderPreParserKeyword extends AbstractPreParserKeyword {

	public static final String KEYWORD = "COMPILEQUERYWITHLLVM";
			
	@Override
	public void validate(Map<String, Object> variables, String parameter, ISession caller, Context context,
			IServerExecutor executor) throws OdysseusScriptException {		
		
		try {
			if (parameter.length() == 0) {
				throw new OdysseusScriptException("Found no query text after #COMPILEQUERYWITHLLVM!");
			}

			String parserID = (String) variables.get(ParserPreParserKeyword.PARSER);
			if (parserID == null) {
				throw new OdysseusScriptException("Parser not set");
			}

			String transCfg = (String) variables.get(TransCfgPreParserKeyword.TRANSCFG);
			if (transCfg == null) {
				variables.put(TransCfgPreParserKeyword.TRANSCFG, "Standard");
			}

		} catch (Exception ex) {
			throw new OdysseusScriptException("Exception in query validation ", ex);
		}		
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List<IExecutorCommand> execute(Map<String, Object> variables, String parameter, ISession caller,
			Context context, IServerExecutor executor) throws OdysseusScriptException {
		String parserID = (String) variables.get(ParserPreParserKeyword.PARSER);
		String transCfgName = (String) variables.get(TransCfgPreParserKeyword.TRANSCFG);
		if (transCfgName == null) {
			transCfgName = "Standard";
		}

		List<IQueryBuildSetting<?>> addSettings = (List<IQueryBuildSetting<?>>) variables
				.get(AbstractPreParserKeyword.ADD_TRANS_PARAMS);
		if (addSettings == null) {
			addSettings = new ArrayList<>();
		}

		// Check if metaattribute is set from outside
		IMetaAttribute metaAttribute = getMetaAttribute(variables);
		if (metaAttribute != null) {
			addSettings.add(new ParameterAddMetadata(new ArrayList(MetadataRegistry.toClassNames(metaAttribute.getClasses()))));
		}

		// CALLER
		ISession queryCaller = (ISession) variables.get("USER");
		if (queryCaller == null) {
			queryCaller = caller;
		}

		// QNAME
		String queryName = (String) variables.get(QueryNamePreParserKeyword.QNAME);
		if (queryName != null && queryName.trim().length() > 0) {
			addSettings.add(new ParameterQueryName(new Resource(queryCaller.getUser().getName(), queryName)));
			// Only for this query
			variables.remove(QueryNamePreParserKeyword.QNAME);
		}
		
		if (variables.containsKey(QueryIDPreParserKeyword.QID)){
			context.put(KEYWORD, Integer.parseInt((String)variables.get(QueryIDPreParserKeyword.QID)));
		}

		// QPRIORITY
		// TODO: Query Prio?

		// QPARAMS
		Map<String, String> queryParams = (Map<String, String>) variables.get(QueryParamPreParserKeyword.NAME);
		if (queryParams != null) {
			addSettings.add(new ParameterQueryParams(queryParams));
			// Only for this query
			variables.remove(QueryParamPreParserKeyword.NAME);
		}

		// MAX SHEDDING FACTOR
		Integer maxSheddingFactor = (Integer) variables.get(MaxSheddingFactorPreParserKeyword.SHEDDING_FACTOR_NAME);
		if (maxSheddingFactor == null) {
			maxSheddingFactor = 1;
		}
		addSettings.add(new ParameterMaxSheddingFactor(maxSheddingFactor));

		// PRIO
		if (variables.containsKey(QueryPriorityPreParserKeyword.NAME)) {
			try {
				int priority = Integer.parseInt((String) variables.get(QueryPriorityPreParserKeyword.NAME));

				addSettings.add(new ParameterPriority(priority));
			} catch (Exception e) {
				throw new OdysseusScriptException(
						"Error reading priority " + variables.get(QueryPriorityPreParserKeyword.NAME));
			}
			// Only for this query
			variables.remove(QueryPriorityPreParserKeyword.NAME);

		}

		// AC-QUERIES
		if (isACQuery()) {
			addSettings.add(new ACQueryParameter(isACQuery()));
		}

		try {
			parserID = parserID.trim();
			transCfgName = transCfgName.trim();
			String queryText = parameter.trim();

			List<IExecutorCommand> commands = new LinkedList<>();

			// Add rules to use to context
			if (variables.containsKey(ActivateRewriteRulePreParserKeyword.ACTIVATEREWRITERULE)) {
				context.put(ActivateRewriteRulePreParserKeyword.ACTIVATEREWRITERULE, new ArrayList<String>(
						(ArrayList<String>) variables.get(ActivateRewriteRulePreParserKeyword.ACTIVATEREWRITERULE)));
			}
			if (variables.containsKey(DeActivateRewriteRulePreParserKeyword.DEACTIVATEREWRITERULE)) {
				context.put(DeActivateRewriteRulePreParserKeyword.DEACTIVATEREWRITERULE,
						new ArrayList<String>((ArrayList<String>) variables
								.get(DeActivateRewriteRulePreParserKeyword.DEACTIVATEREWRITERULE)));
			}

			// Metadata
			if (variables.containsKey(NoMetadataPreParserKeyword.NO_METADATA)) {
				context.put(NoMetadataPreParserKeyword.NO_METADATA, "true");
			}		

			ISink defaultSink = variables.containsKey("_defaultSink") ? (ISink) variables.get("_defaultSink") : null;

			if (defaultSink != null) {
				throw new IllegalArgumentException("_Default Sink currently not supported!");
				// appendSinkToQueries(defaultSink, queriesToStart,
				// (IPlanManager) executor);
			}
			
			Collection<Integer> queries = executor.addQuery(queryText, parserID, queryCaller, transCfgName, context,
					addSettings);
						
			for (Integer lId : queries) {
				String pqlquery = ExternalBuilderHelper.generatePQLStatementFromQueryPart(executor.getLogicalQueryById(lId, queryCaller).getLogicalPlan());
			
				Path pathDir = Path.of(variables.get(ExternalBuilderConfigPreParserKeyword.PLAN_PATH_KEYWORD).toString());
				Path output = pathDir.resolve(lId + ".plan");
				
				Files.createDirectories(pathDir);
				Files.writeString(output, queryText);
				
				if ((boolean)variables.get(ExternalBuilderConfigPreParserKeyword.EXC_KEYWORD)) {
					if(Files.notExists(Path.of(variables.get(ExternalBuilderConfigPreParserKeyword.BUILDER_PATH_KEYWORD).toString()))) {
						throw new FileNotFoundException("OdyCompiler not found!");
					}
					
					new ProcessBuilder(
							variables.get(ExternalBuilderConfigPreParserKeyword.BUILDER_PATH_KEYWORD).toString(), 
							"--file",
							output.toString(),
							"--link"
							).start();
				}
			}
						
			return null;

		} catch (QueryParseException ex) {
			throw new OdysseusScriptException("Query could not be parsed!", ex);
		} catch (PlanManagementException ex) {
			throw new OdysseusScriptException("Adding new query failed!", ex);
		} catch (Exception ex) {
			throw new OdysseusScriptException("Error while executing Odysseus script during executing a query!", ex);
		}
	}
	
	protected boolean isACQuery() {
		return false;
	}
}
