package de.uniol.inf.is.odysseus.odyinterface.externalbuilder;

import java.util.List;
import java.util.Map;
import de.uniol.inf.is.odysseus.core.collection.Context;
import de.uniol.inf.is.odysseus.core.collection.OptionMap;
import de.uniol.inf.is.odysseus.core.server.planmanagement.executor.IServerExecutor;
import de.uniol.inf.is.odysseus.core.server.planmanagement.executor.command.IExecutorCommand;
import de.uniol.inf.is.odysseus.core.usermanagement.ISession;
import de.uniol.inf.is.odysseus.script.parser.AbstractPreParserKeyword;
import de.uniol.inf.is.odysseus.script.parser.OdysseusScriptException;

public class ExternalBuilderLLVMPreParserKeyword extends AbstractPreParserKeyword {

	public static final String KEYWORD = "EXTLLVM";
	public static final String BUILDER_PATH = "builder_path";
	public static final String BUILDER_PATH_KEYWORD = KEYWORD + "." + BUILDER_PATH;
	public static final String PLAN_PATH = "plan_path";
	public static final String PLAN_PATH_KEYWORD = KEYWORD + "." + PLAN_PATH;
	
	@Override
	public void validate(Map<String, Object> variables, String parameter, ISession caller, Context context,
			IServerExecutor executor) throws OdysseusScriptException {
		
		if (parameter == null || parameter.isEmpty() || parameter.isBlank()) {
			throw new OdysseusScriptException(
					KEYWORD + " needs the following not optional parameters (" + BUILDER_PATH + "," + PLAN_PATH + ")");
		}

		OptionMap parameters = OptionMap.fromString(parameter);
		
		if (parameters.getKeySet().size() != 1) {
			throw new OdysseusScriptException(
					KEYWORD + " needs the following not optional parameters (" + BUILDER_PATH + "," + PLAN_PATH + ")");
		}
		
		if (!parameters.containsKey(BUILDER_PATH) || !parameters.containsKey(PLAN_PATH)) {
			throw new OdysseusScriptException(
					KEYWORD + " needs the following not optional parameters (" + BUILDER_PATH + "," + PLAN_PATH + ")");
		}
		
	}

	@Override
	public List<IExecutorCommand> execute(Map<String, Object> variables, String parameter, ISession caller,
			Context context, IServerExecutor executor) throws OdysseusScriptException {
		OptionMap parameters = OptionMap.fromString(parameter);
		
		variables.put(BUILDER_PATH_KEYWORD, parameters.get(BUILDER_PATH));
		variables.put(PLAN_PATH_KEYWORD, parameters.get(PLAN_PATH));
		
		return null;
	}

}
