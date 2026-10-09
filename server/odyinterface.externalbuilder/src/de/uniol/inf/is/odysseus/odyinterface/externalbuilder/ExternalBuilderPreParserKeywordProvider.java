package de.uniol.inf.is.odysseus.odyinterface.externalbuilder;

import java.util.HashMap;
import java.util.Map;

import de.uniol.inf.is.odysseus.script.parser.IPreParserKeyword;
import de.uniol.inf.is.odysseus.script.parser.IPreParserKeywordProvider;

public class ExternalBuilderPreParserKeywordProvider implements IPreParserKeywordProvider {	
	@Override
	public Map<String, Class<? extends IPreParserKeyword>> getKeywords() {
		Map<String, Class<? extends IPreParserKeyword>> keywords = new HashMap<>();
		keywords.put(ExternalBuilderPreParserKeyword.KEYWORD, ExternalBuilderPreParserKeyword.class);
		keywords.put(ExternalBuilderLLVMPreParserKeyword.KEYWORD, ExternalBuilderLLVMPreParserKeyword.class);
		return keywords;
	}
}
