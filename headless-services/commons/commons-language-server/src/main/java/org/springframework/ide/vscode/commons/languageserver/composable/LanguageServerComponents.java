/*******************************************************************************
 * Copyright (c) 2018, 2024 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.commons.languageserver.composable;

import java.util.Optional;
import java.util.Set;

import org.springframework.ide.vscode.commons.languageserver.reconcile.IReconcileEngine;
import org.springframework.ide.vscode.commons.languageserver.semantic.tokens.SemanticTokensHandler;
import org.springframework.ide.vscode.commons.languageserver.util.CodeActionHandler;
import org.springframework.ide.vscode.commons.languageserver.util.CodeLensHandler;
import org.springframework.ide.vscode.commons.languageserver.util.DocumentSymbolHandler;
import org.springframework.ide.vscode.commons.languageserver.util.HoverHandler;
import org.springframework.ide.vscode.commons.languageserver.util.InlayHintHandler;
import org.springframework.ide.vscode.commons.util.text.LanguageId;

public interface LanguageServerComponents {

	Set<LanguageId> getInterestingLanguages();
	default Optional<IReconcileEngine> getReconcileEngine() { return Optional.empty(); }
	default HoverHandler getHoverProvider() { return null; }
	default Optional<CodeActionHandler> getCodeActionProvider() { return Optional.empty(); }
	default Optional<DocumentSymbolHandler> getDocumentSymbolProvider() { return Optional.empty(); }
	default Optional<CodeLensHandler> getCodeLensHandler() { return Optional.empty(); }
	default Optional<InlayHintHandler> getInlayHintHandler() { return Optional.empty(); }
	default Optional<SemanticTokensHandler> getSemanticTokensHandler() { return Optional.empty(); }
}
