/*******************************************************************************
 * Copyright (c) 2019-2020 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.bootiful;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.ide.vscode.boot.app.BootLanguageServerParams;
import org.springframework.ide.vscode.boot.editor.harness.PropertyIndexHarness;
import org.springframework.ide.vscode.boot.index.cache.IndexCache;
import org.springframework.ide.vscode.boot.index.cache.IndexCacheVoid;
import org.springframework.ide.vscode.boot.java.links.SourceLinks;
import org.springframework.ide.vscode.boot.java.links.VSCodeSourceLinks;
import org.springframework.ide.vscode.boot.java.utils.CompilationUnitCache;
import org.springframework.ide.vscode.boot.java.utils.test.MockProjectObserver;
import org.springframework.ide.vscode.boot.metadata.DefaultSpringPropertyIndexProvider;
import org.springframework.ide.vscode.boot.metadata.ValueProviderRegistry;
import org.springframework.ide.vscode.commons.languageserver.java.JavaProjectFinder;
import org.springframework.ide.vscode.commons.languageserver.util.SimpleLanguageServer;
import org.springframework.ide.vscode.commons.util.text.LanguageId;
import org.springframework.ide.vscode.project.harness.BootLanguageServerHarness;

/**
 * @author Alex Boyko
 */
@Configuration
@Import(AdHocPropertyHarnessTestConf.class)
public class SourceLinksTestConf {
	
	@Bean public IndexCache symbolCache() {
		return new IndexCacheVoid();
	}

	@Bean PropertyIndexHarness indexHarness(ValueProviderRegistry valueProviders) {
		return new PropertyIndexHarness(valueProviders);
	}

	@Bean JavaProjectFinder projectFinder(BootLanguageServerParams serverParams) {
		return serverParams.projectFinder;
	}

	@Bean BootLanguageServerHarness harness(SimpleLanguageServer server, BootLanguageServerParams serverParams, PropertyIndexHarness indexHarness, JavaProjectFinder projectFinder) throws Exception {
		return new BootLanguageServerHarness(server, serverParams, indexHarness, projectFinder, LanguageId.JAVA, ".java");
	}

	@Bean BootLanguageServerParams serverParams(SimpleLanguageServer server, ValueProviderRegistry valueProviders) {
		BootLanguageServerParams testDefaults = BootLanguageServerHarness.createTestDefault(server, valueProviders);
		return new BootLanguageServerParams(
				testDefaults.projectFinder,
				new MockProjectObserver(),
				testDefaults.indexProvider,
				testDefaults.typeUtilProvider
		);
	}

	@Bean DefaultSpringPropertyIndexProvider indexProvider(BootLanguageServerParams serverParams) {
		return (DefaultSpringPropertyIndexProvider) serverParams.indexProvider;
	}

	@Bean SourceLinks sourceLinks(CompilationUnitCache cuCache, JavaProjectFinder projectFinder) {
		return new VSCodeSourceLinks(cuCache, projectFinder);
	}

	@Bean MockProjectObserver mockProjectObserver(BootLanguageServerParams params) {
		return (MockProjectObserver) params.projectObserver;
	}

}
