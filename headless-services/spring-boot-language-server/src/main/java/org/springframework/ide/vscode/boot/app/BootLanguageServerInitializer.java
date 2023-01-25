/*******************************************************************************
 * Copyright (c) 2018, 2023 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.app;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.ide.vscode.boot.common.IJavaProjectReconcileEngine;
import org.springframework.ide.vscode.boot.common.ProjectReconcileScheduler;
import org.springframework.ide.vscode.boot.factories.SpringFactoriesLanguageServerComponents;
import org.springframework.ide.vscode.boot.java.BootJavaLanguageServerComponents;
import org.springframework.ide.vscode.boot.java.links.JavaElementLocationProvider;
import org.springframework.ide.vscode.boot.java.links.SourceLinks;
import org.springframework.ide.vscode.boot.java.livehover.v2.SpringProcessLiveDataProvider;
import org.springframework.ide.vscode.boot.java.rewrite.RewriteRecipeRepository;
import org.springframework.ide.vscode.boot.java.utils.CompilationUnitCache;
import org.springframework.ide.vscode.boot.java.utils.SymbolCache;
import org.springframework.ide.vscode.boot.metadata.ProjectBasedPropertyIndexProvider;
import org.springframework.ide.vscode.boot.properties.BootPropertiesLanguageServerComponents;
import org.springframework.ide.vscode.boot.validation.BootVersionValidationEngine;
import org.springframework.ide.vscode.boot.xml.SpringXMLLanguageServerComponents;
import org.springframework.ide.vscode.commons.java.IJavaProject;
import org.springframework.ide.vscode.commons.languageserver.completion.CompositeCompletionEngine;
import org.springframework.ide.vscode.commons.languageserver.completion.ICompletionEngine;
import org.springframework.ide.vscode.commons.languageserver.completion.VscodeCompletionEngineAdapter;
import org.springframework.ide.vscode.commons.languageserver.composable.CompositeLanguageServerComponents;
import org.springframework.ide.vscode.commons.languageserver.composable.LanguageServerComponents;
import org.springframework.ide.vscode.commons.languageserver.config.LanguageServerProperties;
import org.springframework.ide.vscode.commons.languageserver.java.JavaProjectFinder;
import org.springframework.ide.vscode.commons.languageserver.java.ProjectObserver;
import org.springframework.ide.vscode.commons.languageserver.util.HoverHandler;
import org.springframework.ide.vscode.commons.languageserver.util.SimpleLanguageServer;
import org.springframework.ide.vscode.commons.languageserver.util.SimpleTextDocumentService;
import org.springframework.ide.vscode.commons.util.text.TextDocument;
import org.springframework.ide.vscode.commons.yaml.ast.YamlASTProvider;
import org.springframework.ide.vscode.commons.yaml.completion.YamlAssistContextProvider;
import org.springframework.ide.vscode.commons.yaml.structure.YamlStructureProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class BootLanguageServerInitializer implements InitializingBean {
	
	@Autowired SimpleLanguageServer server;
	@Autowired BootLanguageServerParams params;
	@Autowired SourceLinks sourceLinks;
	@Autowired CompilationUnitCache cuCache;
	@Autowired JavaElementLocationProvider javaElementLocationProvider;
	@Autowired YamlASTProvider parser;
	@Autowired YamlStructureProvider yamlStructureProvider;
	@Autowired YamlAssistContextProvider yamlAssistContextProvider;
	@Autowired SymbolCache symbolCache;
	@Autowired SpringProcessLiveDataProvider liveDataProvider;
	@Autowired ApplicationContext appContext;
	@Autowired BootJavaConfig config;
	@Autowired SpringSymbolIndex springIndexer;
	@Autowired(required = false) List<ICompletionEngine> completionEngines;
	@Autowired private JavaProjectFinder projectFinder;
	@Autowired private LanguageServerProperties configProps;
	@Autowired(required = false) private RewriteRecipeRepository recipesRepo;

	@Qualifier("adHocProperties") @Autowired ProjectBasedPropertyIndexProvider adHocProperties;

	private CompositeLanguageServerComponents components;
	private VscodeCompletionEngineAdapter completionEngineAdapter;
	
	private static final Logger log = LoggerFactory.getLogger(BootLanguageServerInitializer.class);

	private ProjectObserver.Listener reconcileDocumentsForProjectChange(SimpleLanguageServer s, CompositeLanguageServerComponents c, JavaProjectFinder projectFinder) {
		return new ProjectObserver.Listener() {
			
			@Override
			public void deleted(IJavaProject project) {
			}
			
			@Override
			public void created(IJavaProject project) {
				validateAll(project);
			}
			
			@Override
			public void changed(IJavaProject project) {
				validateAll(project);
			}
			
			private void validateAll(IJavaProject project) {
				c.getReconcileEngine().ifPresent(reconciler -> {
					log.debug("A project changed {}, triggering reconcile on all project's open documents",
							project.getElementName());
					for (TextDocument doc : s.getTextDocumentService().getAll()) {
						if (projectFinder.find(doc.getId()).orElse(null) == project) {
							s.validateWith(doc.getId(), reconciler);
						}
					}
				});
			}
		};
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		//TODO: CompositeLanguageServerComponents object instance serves no purpose anymore. The constructor really just contains
		// some server intialization code. Migrate that code and get rid of the ComposableLanguageServer class
		CompositeLanguageServerComponents.Builder builder = new CompositeLanguageServerComponents.Builder();
		List<LanguageServerComponents> componentsList = List.of(
			new BootPropertiesLanguageServerComponents(server, params, javaElementLocationProvider, parser, yamlStructureProvider, yamlAssistContextProvider, sourceLinks),
			new BootJavaLanguageServerComponents(appContext),
			new SpringXMLLanguageServerComponents(server, springIndexer, params, config),
			new SpringFactoriesLanguageServerComponents(projectFinder, springIndexer, config)
		);
		
		List<ProjectReconcileScheduler> reconcileSchedulers = new ArrayList<>(componentsList.size() + 1);
		for (LanguageServerComponents c : componentsList) {
			builder.add(c);
			
			if (!configProps.isReconcileOnlyOpenedDocs()) {
				c.getReconcileEngine()
				.filter(IJavaProjectReconcileEngine.class::isInstance)
				.map(IJavaProjectReconcileEngine.class::cast)
				.map(r -> r.getScheduler())
				.ifPresent(reconcileSchedulers::add);
			}
		}
		
		// Version reconciler is for Maven/Gradle build files which are not part of
		// server components because they come from docs we don't support at the moment
		reconcileSchedulers.add(new BootVersionValidationEngine(server, config, params.projectObserver, projectFinder).getScheduler());

		// Kick off project reconcile schedulers
		for (ProjectReconcileScheduler scheduler : reconcileSchedulers) {
			scheduler.start();
			server.onShutdown(() -> scheduler.stop());
		}
		
		components = builder.build(server);
		
		SimpleTextDocumentService documents = server.getTextDocumentService();

		if (!completionEngines.isEmpty()) {
			CompositeCompletionEngine compositeCompletionEngine = new CompositeCompletionEngine();
			completionEngines.forEach(compositeCompletionEngine::add);
			completionEngineAdapter = server.createCompletionEngineAdapter(compositeCompletionEngine);
			completionEngineAdapter.setMaxCompletions(100);
			documents.onCompletion(completionEngineAdapter::getCompletions);
			documents.onCompletionResolve(completionEngineAdapter::resolveCompletion);
		}

		HoverHandler hoverHandler = components.getHoverProvider();
		documents.onHover(hoverHandler);
		
		components.getCodeActionProvider().ifPresent(documents::onCodeAction);
		
		components.getDocumentSymbolProvider().ifPresent(documents::onDocumentSymbol);
		
		
		if (recipesRepo != null) {
			recipesRepo.onRecipesLoaded(v -> {
				// Recipes will start loading only after config has been received. Therefore safe to start listening to config changes now
				// and launch initial project reconcile since both config and recipes are present
				startListeningToPerformReconcile();			
				reconcile();
			});
		} else {
			// Reconcile would occur as listeners will be receiving events
			startListeningToPerformReconcile();			
		}


	}
	
	private void startListeningToPerformReconcile() {
		components.getReconcileEngine().ifPresent(reconcileEngine -> {
			server.getTextDocumentService().onDidChangeContent(params -> {
				TextDocument doc = params.getDocument();
				server.validateWith(doc.getId(), reconcileEngine);
			});
		});
		config.addListener(evt -> reconcile());
		params.projectObserver.addListener(reconcileDocumentsForProjectChange(server, components, params.projectFinder));
		
//		// TODO: index update even happens on every file save. Very expensive to blindly reconcile all projects.
//		// Need to figure out a check if spring index has any changes 
////		springIndexer.onUpdate(v -> reconcile());
	}
	
	private void reconcile() {
		components.getReconcileEngine().ifPresent(reconciler -> {
			log.info("Triggering reconcile on all open documents");
			for (TextDocument doc : server.getTextDocumentService().getAll()) {
				server.validateWith(doc.getId(), reconciler);
			}
		});
	}

	public CompositeLanguageServerComponents getComponents() {
		Assert.notNull(components, "Not yet initialized, can't get components yet.");
		return components;
	}

	public void setMaxCompletions(int number) {
		if (completionEngineAdapter!=null) {
			completionEngineAdapter.setMaxCompletions(number);
		}
	}
	
}
